package io.github.mingjia.mybatis.autocache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 * Created by mingjia on 19/8/23.
 */
public class AutoCacheCleanHolder {

    private static final Logger logger = LoggerFactory.getLogger(AutoCacheCleanHolder.class);

    private static final ThreadLocal<Map<Integer/*isOpenTransaction*/, Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/>>> holder = new ThreadLocal<>();

    public static AutoCacheServiceI cacheService = null;


    public static void set(int type, Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/> cleanMaps) {
        Map<Integer, Map<String, Set<String>>> map = new HashMap<>(1);
        map.put(type, cleanMaps);
        holder.set(map);
    }

    public static Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/> get(int type) {
        Map<Integer, Map<String, Set<String>>> map = holder.get();
        if (map == null)
            return null;
        else
            return map.get(type);
    }

    public static void remove() {
        holder.remove();
    }

    public static void cleanCache(int type) {
        //清理缓存
        try {
            Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/> map = AutoCacheCleanHolder.get(type);
            if (map != null && map.size() > 0) {
                //变更查询方法的有效执行序号,锁冲突：等待查询方法的锁结束，不等待清理方法的锁
                for (String cleanMethod : map.keySet()) {
                    updateOrderFlag(cleanMethod);

                    if (map.get(cleanMethod) != null && map.get(cleanMethod).size() > 0) {
                        for (String evictShardName : map.get(cleanMethod)) {
                            updateOrderFlag(getShardCacheMethodName(cleanMethod, evictShardName));
                        }
                    }
                }

                try {
                    logger.info("start clean redissonCache");
                    for (String cleanMethod : map.keySet()) {

                        //清除method的
                        cacheService.delCache(cleanMethod);
                        if (logger.isDebugEnabled())
                            logger.debug("清除缓存:{}", AutoCacheRuntime.METHOD_DESC.get(cleanMethod));

                        //清除shards的
                        if (map.get(cleanMethod) != null && map.get(cleanMethod).size() > 0) {

                            for (String evictShard : map.get(cleanMethod)) {
                                cacheService.delCache(getShardCacheMethodName(cleanMethod, evictShard));
                                if (logger.isDebugEnabled())
                                    logger.debug("清除缓存:[{}],{}", evictShard, AutoCacheRuntime.METHOD_DESC.get(cleanMethod));
                            }
                        }

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("AutoCacheCleanHolder clean redissonCache error.");

                }
            }
        } finally {
            remove();
        }
    }

    /**
     * 变更orderFlag
     *
     * @param methodKey
     */
    public static boolean updateOrderFlag(String methodKey) {
        //分布式环境中多清理线程对同一个方法的锁定不等待，遇到锁定直接放弃，由得到锁的线程负责清理（todo:但是要确保得到所的线程一定要得到OrderFlag-cache的锁并且完成清理工作）
        Lock cleanLock = cacheService.startTryLock("OrderFlag-clean" + methodKey);

        if (cleanLock != null && !Thread.currentThread().isInterrupted()) {
            try {

                //todo:确保清理线程一定能都得到该锁，并且成功清理
                Lock queryLock = cacheService.startLock("OrderFlag-redissonCache" + methodKey);
                if (queryLock != null && !Thread.currentThread().isInterrupted()) {
                    try {


                        Object orderFlag = cacheService.getCache("orderFlag", methodKey);
                        if (orderFlag == null)
                            cacheService.setCache("orderFlag", methodKey, new Integer(1));
                        else {
                            cacheService.setCache("orderFlag", methodKey, ((Integer) orderFlag) + 1);
                        }

                        return true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    } finally {
                        cacheService.unLock(queryLock);
                    }
                } else {
                    logger.debug("OrderFlag-redissonCache 获取失败");
                }
            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                cacheService.unLock(cleanLock);
            }
        } else {
            logger.debug("OrderFlag-clean 获取失败");
        }
        return true;
    }

    public static String getShardCacheMethodName(String queryMethod, String shardName) {
        //return "Shard_" + queryMethod + "_" + shardName;
        return shardName + ":" + queryMethod;
    }


    /**
     * 多线程清理
     *
     * @param type
     */
    public static void mulityThreadCleanCache(int type) {
        //清理缓存
        try {
            logger.info("清理start");
            Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/> map = AutoCacheCleanHolder.get(type);
            if (map != null && map.size() > 0) {
                int threadNum=0;
                for(Set<String> set:map.values()){
                    threadNum++;
                    threadNum+=set.size();
                }
                CountDownLatch endFlag = new CountDownLatch(threadNum);
                //变更查询方法的有效执行序号,锁冲突：等待查询方法的锁结束，不等待清理方法的锁
                for (String cleanMethod : map.keySet()) {
                    //updateOrderFlag(cleanMethod);
                    executor.submit(new CleanTask(endFlag, cleanMethod));
                    if (map.get(cleanMethod) != null && map.get(cleanMethod).size() > 0) {
                        for (String evictShardName : map.get(cleanMethod)) {
                            //updateOrderFlag(getShardCacheMethodName(cleanMethod, evictShardName));
                            executor.submit(new CleanTask(endFlag, cleanMethod, evictShardName));
                        }
                    }
                }
                endFlag.await();
            }

            logger.info("清理end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            remove();
        }
    }

    static ThreadPoolExecutor executor = new ThreadPoolExecutor(50, 100, 60, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(100), new ThreadPoolExecutor.CallerRunsPolicy());

    public static class CleanTask implements Callable<Boolean> {

        private CountDownLatch endFlag;
        private String cleanMethod;
        private String shardName;

        public CleanTask(CountDownLatch endFlag, String cleanMethod) {
            this.endFlag = endFlag;
            this.cleanMethod = cleanMethod;
        }

        public CleanTask(CountDownLatch endFlag, String cleanMethod, String shardName) {
            this.endFlag = endFlag;
            this.cleanMethod = cleanMethod;
            this.shardName = shardName;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                String cleanMethodStr = shardName == null ? cleanMethod : getShardCacheMethodName(cleanMethod, shardName);
                boolean r = updateOrderFlag(cleanMethodStr);


                //清除method的
                cacheService.delCache(cleanMethodStr);

                if (logger.isDebugEnabled()) {
                    logger.debug("cleanMethod:{}",cleanMethod);
                    if (shardName == null)
                        logger.debug("清除缓存({}):{}", r, AutoCacheRuntime.METHOD_DESC.get(cleanMethod));
                    else
                        logger.debug("清除缓存({}):[{}],{}", r, shardName, AutoCacheRuntime.METHOD_DESC.get(cleanMethod));
                }

                //修改
                endFlag.countDown();

                return r;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

    }

}
