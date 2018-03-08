package io.github.mingjia.MyBatisCache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * 缓存服务接口类
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public interface MybatisCacheServiceI<M, K, V> {

    /**
     * 从缓存总获得数据
     *
     * @param method
     * @return
     */
    V getCache(M method, K key);

    /**
     * 设置缓存数据
     *
     * @param method
     * @param key
     * @param value
     */
    void setCache(M method, K key, V value);

    /**
     * 删除缓存数据
     *
     * @param method
     */
    void delCache(M method);

    MybatisCacheServiceI<String, String, Object> GUAVA_CACHE = new MybatisCacheServiceI<String, String, Object>() {

        private Cache<String, Map<String, Object>> cache = CacheBuilder.newBuilder()
                .initialCapacity(100)//设置cache的初始大小为100，要合理设置该值
                .concurrencyLevel(10)//设置并发数为10，即同一时间最多只能有10个线程往cache执行写入操作
                .expireAfterWrite(1, TimeUnit.HOURS)//设置cache中的数据在写入之后的存活时间为1小时
                .build();

        @Override
        public Object getCache(String method, String key) {
            Map<String, Object> kvs = cache.getIfPresent(method);
            if (kvs != null)
                return kvs.get(key);
            return null;
        }

        @Override
        public void setCache(String method, String key, Object value) {
            Map<String, Object> kvs = cache.getIfPresent(method);
            if (kvs == null)
                kvs = new HashMap<>();
            kvs.put(key, value);
        }

        @Override
        public void delCache(String method) {
            cache.invalidate(method);
        }

    };
}
