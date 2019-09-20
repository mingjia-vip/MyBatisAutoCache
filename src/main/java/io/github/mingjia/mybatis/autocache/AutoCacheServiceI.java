package io.github.mingjia.mybatis.autocache;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

/**
 * 缓存服务接口类
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public interface AutoCacheServiceI<M, K, V> {

    //初始化操作
    void init();

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
     * 设置缓存数据
     *
     * @param method
     * @param key
     * @param value
     * @param expireSeconds
     */
    void setCache(M method, K key, V value, long expireSeconds);

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
     * @param lockSeconds
     * @return
     */
    Lock startLock(M method, long lockSeconds);

    /**
     * 开启锁
     * @param method
     * @return
     */
    Lock startTryLock(M method);

    /**
     * 开启锁
     * @param method
     * @param lockSeconds
     * @return
     */
    Lock startTryLock(M method, long lockSeconds);

    /**
     * 开启锁，指定有效时间
     * @param method
     * @param waitSeconds
     * @param lockSeconds
     * @return
     */
    Lock startTryLock(M method, long waitSeconds, long lockSeconds);

    /**
     * 解锁
     * @param lock
     */
    void unLock(Lock lock);
}
