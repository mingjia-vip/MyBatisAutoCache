package io.github.mingjia.MyBatisCache.test.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.mingjia.MyBatisCache.MybatisCacheServiceI;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @auther GuiBin
 * @create 18/3/13
 */
public class DefaultCacheService implements MybatisCacheServiceI<String, String, Object> {

    private Cache<String, Map<String, Object>> cache = CacheBuilder.newBuilder()
            .initialCapacity(100)//设置cache的初始大小为100，要合理设置该值
            .concurrencyLevel(10)//设置并发数为10，即同一时间最多只能有10个线程往cache执行写入操作
            .expireAfterWrite(1, TimeUnit.HOURS)//设置cache中的数据在写入之后的存活时间为1小时
            .build();

    /**
     * 从缓存总获得数据
     *
     * @param method
     * @return
     */
    @Override
    public Object getCache(String method) {
        Map<String, Object> kvs = cache.getIfPresent(method);
        return kvs;
    }

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
        if (kvs == null) {
            kvs = new HashMap<>();
            cache.put(method, kvs);
        }
        kvs.put(key, value);
    }

    @Override
    public void delCache(String method) {
        cache.invalidate(method);
    }

    /**
     * 删除缓存数据
     *
     * @param method
     * @param key
     */
    @Override
    public void delCache(String method, String key) {
        Map<String, Object> kvs = cache.getIfPresent(method);
        if (kvs == null) {
            kvs = new HashMap<>();
            cache.put(method, kvs);
        }
        kvs.remove(key);
    }

    /**
     * 删除缓存数据
     *
     * @param method
     * @param keys
     */
    @Override
    public void delCache(String method, Collection<String> keys) {
        Map<String, Object> kvs = cache.getIfPresent(method);
        if (kvs == null) {
            kvs = new HashMap<>();
            cache.put(method, kvs);
        }
        for (String key : keys)
            kvs.remove(key);
    }

    /**
     * 开启锁
     *
     * @param method
     * @return
     */
    @Override
    public Lock startLock(String method) {
        return null;
    }

    /**
     * 开启锁，指定有效时间
     *
     * @param method
     * @param seconds
     * @return
     */
    @Override
    public Lock startLock(String method, long seconds) {
        return null;
    }

    @Override
    public Lock startLock(Collection<String> methods) {
        return null;
    }

    @Override
    public Lock startLock(Collection<String> methods, long seconds) {
        return null;
    }


    public ConcurrentMap<String, Map<String, Object>> getAllCache() {
        return cache.asMap();
    }
}
