package io.github.mingjia.mybatis.autocache;


import io.github.mingjia.mybatis.autocache.expression.SimpleExpression;
import io.github.mingjia.mybatis.autocache.service.redisson.RedissonConfig;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * 缓存拦截器
 *
 * @auther GuiBin
 * @create 18/2/27
 */
@SuppressWarnings(value = {"unchecked", "rawtypes", "unused"})
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
        ,@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
        ,@Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
        ,@Signature(type = Executor.class, method = "commit", args = {boolean.class})
        ,@Signature(type = Executor.class, method = "rollback", args = {boolean.class})
        ,@Signature(type = Executor.class, method = "close", args = {boolean.class})
})
public class AutoCacheInterceptor implements Interceptor {

    private final Logger logger = LoggerFactory.getLogger(AutoCacheInterceptor.class);

    private SimpleExpression expression = new SimpleExpression();

    public AutoCacheInterceptor() {
    }


    private Properties properties;
    private AutoCacheServiceI cacheService = null;

    /**
     * 设置缓存实现
     *
     * @param cacheService
     */
    public void setCacheService(AutoCacheServiceI cacheService) {
        this.cacheService = cacheService;
        AutoCacheCleanHolder.cacheService = cacheService;
    }

    /**
     * 设置属性值
     *
     * @param properties 属性值
     */
    @Override
    public void setProperties(Properties properties) {
        //countSuffix = properties.getProperty("pageHelperCountSuffix");
    }


    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        if (cacheService == null)
            return invocation.proceed();

        String invocationMethodName = invocation.getMethod().getName();
        if (StringUtils.equals("query", invocationMethodName)) {

            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];
            Object parameter = args[1];

            RowBounds rowBounds = (RowBounds) args[2];
            ResultHandler resultHandler = (ResultHandler) args[3];

            Executor executor = (Executor) invocation.getTarget();
            CacheKey cacheKey;
            BoundSql boundSql;
            if (args.length == 4) {
                //4 个参数时
                //logger.error("getBoundSql parameter:" + parameter);
                boundSql = mappedStatement.getBoundSql(parameter);
                cacheKey = executor.createCacheKey(mappedStatement, parameter, rowBounds, boundSql);
            } else {
                //6 个参数时
                cacheKey = (CacheKey) args[4];
                boundSql = (BoundSql) args[5];
            }

            //判断方法是否在非缓存集合，是则直接查询数据库
            if (isDisCache(mappedStatement.getId())) {
                logger.debug("只读取数据库");
                return invocation.proceed();
            } else {
                boolean isPage = false;

                //logger.debug("key:" + cacheKey.toString());
                logger.debug("query method:" + mappedStatement.getId());
                //method code
                String method = DigestUtils.md5Hex(mappedStatement.getId());
                String key = DigestUtils.md5Hex(cacheKey.toString());
                String shard = AutoCacheRuntime.METHOD_SHARD.get(method);
                String shardName = getShardStr(shard, parameter);
                boolean _isShard = StringUtils.isNotBlank(shardName) ? true : false;
                logger.debug("_isShard:" + _isShard);

                //判断在不在清理表中，有则直接读取数据库，不缓存
                Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/> cleanMaps = AutoCacheCleanHolder.get(1);
                if (cleanMaps != null && cleanMaps.keySet().contains(method)) {
                    logger.debug("开启事务中，存在对该缓存的清理，不读取缓存");
                    return invocation.proceed();
                }

                String cacheMethod = method;
                if (_isShard)
                    cacheMethod = AutoCacheCleanHolder.getShardCacheMethodName(method, shardName);

                //读取缓存
                Object obj = cacheService.getCache(cacheMethod, key);

                if (obj == null) {
                    logger.debug("没有缓存，读取数据库");

                    //获得执行序号
                    Object orderFlag = cacheService.getCache("orderFlag", cacheMethod);

                    //select result
                    obj = invocation.proceed();

                    //目前是锁的方式保证放入数据和清理数据的有序，todo:可以考虑redis的CAS方式保证orderFlag有变动时不将数据放入缓存
                    Lock lock = cacheService.startTryLock("OrderFlag-redissonCache" + cacheMethod);
                    if (lock != null && !Thread.currentThread().isInterrupted()) {
                        try {
                            boolean isCache = true;
                            Object newFlag = cacheService.getCache("orderFlag", cacheMethod);
                            if (newFlag == null) {
                                if (orderFlag != null) {
                                    isCache = false;
                                }
                            } else {
                                if (orderFlag == null || !((Integer) orderFlag).equals((Integer) newFlag))
                                    isCache = false;
                            }
                            if (isCache) {
                                if (_isShard) {
                                    //执行shard逻辑
                                    cacheService.setCache(cacheMethod, key, obj);
                                    if (logger.isDebugEnabled())
                                        logger.debug("缓存:[{}],{}", shardName, AutoCacheRuntime.METHOD_DESC.get(method));

                                } else {
                                    //正常走普通逻辑
                                    cacheService.setCache(cacheMethod, key, obj);
                                    if (logger.isDebugEnabled())
                                        logger.debug("缓存:{}", AutoCacheRuntime.METHOD_DESC.get(method));
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();

                        } finally {
                            cacheService.unLock(lock);
                        }
                    } else {
                        logger.warn(Thread.currentThread().getName() + " does not get lock.");
                    }

                    return obj;

                } else {
                    logger.debug("读取缓存");
                    return obj;
                }
            }

        } else if (StringUtils.equals("update", invocation.getMethod().getName())) {
            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];
            Object parameter = args[1];
            BoundSql boundSql = mappedStatement.getBoundSql(parameter);
            String sql = boundSql.getSql();
            logger.debug("sql:" + sql);
            Statement statement = CCJSqlParserUtil.parse(sql);
            List<String> tables = getUpdateTables(statement);
            //根据变更表获得需要清除的方法
            Set<String> cleanMethods = new HashSet<>();
            for (String table : tables) {
                logger.debug("table:" + table);
                Set<String> m = AutoCacheRuntime.TABLE_METHODS.get(table);
                if (!CollectionUtils.isEmpty(m))
                    cleanMethods.addAll(m);
            }

            //shard清理逻辑，获得所有需要清除的shardName数组
            String evictMethod = DigestUtils.md5Hex(mappedStatement.getId());
            List<String> evictShardNames = getEvictShardNames(evictMethod, parameter);

            logger.debug("update method:{} {}",mappedStatement.getId(),evictShardNames);

            //开始记录清除数据
            if (!CollectionUtils.isEmpty(cleanMethods)) {

                Map<String/*cleanMethod*/, Set<String>/*evictShardNames*/> cleanMaps = AutoCacheCleanHolder.get(1);//1开启事务
                if (cleanMaps == null) {
                    cleanMaps = AutoCacheCleanHolder.get(0);//0自动事务
                    if (cleanMaps == null) {
                        cleanMaps = new HashMap<>();
                        AutoCacheCleanHolder.set(0,cleanMaps);
                    }
                }
                //记录清理数据
                for (String cleanMethod : cleanMethods) {
                    Set<String> set = cleanMaps.get(cleanMethod);
                    if (set == null) {
                        set = new HashSet<>();
                    }
                    if (evictShardNames != null && evictShardNames.size() > 0) {
                        for (String evictShardName : evictShardNames) {
                            set.add(evictShardName);
                        }
                    }
                    cleanMaps.put(cleanMethod, set);
                }

            }
            return invocation.proceed();

        } else if (StringUtils.equals("commit", invocation.getMethod().getName())) {

            //当事务有spring管理时，commit执行但不起作用，使用mybatis管理事务时才有效
            Object[] args = invocation.getArgs();
            if(AutoCacheCleanHolder.get(1)==null){
                logger.debug("commit({}) AutoCacheCleanHolder.cleanCache(0)",args[0]);
                //清理自动事务数据
                if (RedissonConfig.mulityThread)
                    AutoCacheCleanHolder.mulityThreadCleanCache(0);
                else
                    AutoCacheCleanHolder.cleanCache(0);
            }
            return invocation.proceed();

        } else if (StringUtils.equals("rollback", invocation.getMethod().getName())) {

            //当事务有spring管理时，rollback不会执行，使用mybatis管理事务时才执行并有效
            Object[] args = invocation.getArgs();
            if(AutoCacheCleanHolder.get(1)==null){
                logger.debug("rollback({}) AutoCacheCleanHolder.remove()",args[0]);
                //移除
                AutoCacheCleanHolder.remove();
            }
            return invocation.proceed();

        } else if (StringUtils.equals("close", invocation.getMethod().getName())) {

            Object[] args = invocation.getArgs();
            if(AutoCacheCleanHolder.get(1)==null){
                logger.debug("close({}) AutoCacheCleanHolder.remove()",args[0]);
                //移除
                AutoCacheCleanHolder.remove();
            }
            return invocation.proceed();

        } else {
            return invocation.proceed();
        }
    }


    /**
     * 判断是否为非缓存
     *
     * @param currentMethod
     * @return
     */
    private boolean isDisCache(String currentMethod) {
        boolean contains = false;
        for (String method : AutoCacheRuntime.DIS_CACHE_METHOD) {
            if (StringUtils.equals(method, currentMethod)) {
                contains = true;
                break;
            }
        }
        return contains;
    }





    private String getShardStr(String shard, Object parameter) {
        if (shard != null && shard.startsWith("#")) {
            //解析表达式
            if (parameter instanceof Map) {
                Object v = expression.evaluate(shard, Object.class, (Map) parameter);
                return v==null?null:v.toString();
            } else {
                Map<String, Object> map = new HashMap<>(1);
                map.put("param1", parameter);
                map.put(parameter.getClass().getSimpleName(), parameter);
                Object v = expression.evaluate(shard, Object.class, map).toString();
                return v==null?null:v.toString();
            }
        }
        return shard;
    }



    /**
     * 根据Statement 类型获得变动的表名
     *
     * @param statement
     * @return
     */
    private List<String> getUpdateTables(Statement statement) {
        List<String> tables = new ArrayList<>();
        //Set<String> cleanMethods = new HashSet<>();
        if (statement instanceof Delete) {
            String table = ((Delete) statement).getTable().getName();
            tables.add(table);
        } else if (statement instanceof Update) {
            Update update = (Update) statement;
            Set<String> dealTableName = new HashSet<>();
            for (Column column : update.getColumns()) {
                if (column.getTable() != null)
                    dealTableName.add(column.getTable().getName());
            }
            for (Table t : update.getTables()) {
                tables.add(t.getName());
            }

        } else if (statement instanceof Insert) {
            String table = ((Insert) statement).getTable().getName();
            tables.add(table);
        }
        return tables;
    }


    /**
     * 获得变更方法需要移除的shardName数组
     *
     * @param method
     * @param parameter
     * @return
     */
    private List<String> getEvictShardNames(String method, Object parameter) {
        String[] evictShards = AutoCacheRuntime.METHOD_EVICT_SHARDS.get(method);
        List<String> evictShardList = new ArrayList<>(evictShards.length);
        if (evictShards != null && evictShards.length > 0) {
            for (int i = 0; i < evictShards.length; i++) {
                evictShardList.add(getShardStr(evictShards[i], parameter));
            }
        }
        return evictShardList;
    }

}