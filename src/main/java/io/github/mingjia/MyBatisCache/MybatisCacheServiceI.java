package io.github.mingjia.MyBatisCache;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

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
    V getCache(M method);

    /**
     * 从缓存总获得数据
     *
     * @param method
     * @param key
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

    /**
     * 删除缓存数据
     *
     * @param method
     * @param key
     */
    void delCache(M method, K key);

    /**
     * 删除缓存数据
     *
     * @param method
     */
    void delCache(M method, Collection<K> keys);


    /**
     * 开启锁
     * @param method
     * @return
     */
    Lock startLock(M method);

    /**
     * 开启锁，指定有效时间
     * @param method
     * @param seconds
     * @return
     */
    Lock startLock(M method, long seconds);

    Lock startLock(Collection<M> methods);

    Lock startLock(Collection<M> methods, long seconds);
}
