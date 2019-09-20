package io.github.mingjia.mybatis.autocache.service.redisson;

import io.github.mingjia.mybatis.autocache.AutoCacheServiceI;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.*;
import org.redisson.connection.balancer.LoadBalancer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;


/**
 * @auther GuiBin
 * @create 18/2/27
 */
public class RedissonCacheService implements AutoCacheServiceI<String, String, Object> {
    private static Logger logger = Logger.getLogger(RedissonCacheService.class);

    private Config config = null;
    private RedissonClient redissonClient;

    public RedissonCacheService(){
        //init();
    }

    public void init() {
        if (StringUtils.isBlank(RedissonConfig.clusterIp) && StringUtils.isBlank(RedissonConfig.sentinelIp) && StringUtils.isBlank(RedissonConfig.cloudIp) && StringUtils.isBlank(RedissonConfig.singleIp)) {
            logger.error("redis ip没有配置");
            return;
        }
        try {
            config = new Config();
            if (StringUtils.isNotBlank(RedissonConfig.clusterIp)) {
                clusterServer(RedissonConfig.clusterIp.split(","));
            } else if (StringUtils.isNotBlank(RedissonConfig.cloudIp)) {
                replicatedServer(RedissonConfig.cloudIp.split(","));
            } else if (StringUtils.isNotBlank(RedissonConfig.singleIp)) {
                singleServer(RedissonConfig.singleIp.split(","));
            } else if (StringUtils.isNotBlank(RedissonConfig.sentinelIp)) {
                sentinelServer(RedissonConfig.sentinelIp.split(","));
            }
        } catch (Exception e) {
            logger.error("redis配置失败", e);
        }

        try {
            redissonClient = Redisson.create(config);
        } catch (Exception e) {
            logger.error("redis连接失败", e);
        }
    }

    private void replicatedServer(String[] ipPorts)
            throws URISyntaxException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        ReplicatedServersConfig rsc = config.useReplicatedServers();
        rsc.setScanInterval(RedissonConfig.scanInterval);
        if (ReadMode.SLAVE.toString().equals(RedissonConfig.readMode)) {
            rsc.setReadMode(ReadMode.SLAVE);
        } else if (ReadMode.MASTER.toString().equals(RedissonConfig.readMode)) {
            rsc.setReadMode(ReadMode.MASTER);
        } else if (ReadMode.MASTER_SLAVE.toString().equals(RedissonConfig.readMode)) {
            rsc.setReadMode(ReadMode.MASTER_SLAVE);
        } else {
            rsc.setReadMode(ReadMode.SLAVE);
        }
        if (SubscriptionMode.SLAVE.toString().equals(RedissonConfig.subscriptionMode)) {
            rsc.setSubscriptionMode(SubscriptionMode.SLAVE);
        } else if (SubscriptionMode.MASTER.toString().equals(RedissonConfig.subscriptionMode)) {
            rsc.setSubscriptionMode(SubscriptionMode.MASTER);
        } else {
            rsc.setSubscriptionMode(SubscriptionMode.SLAVE);
        }
        rsc.setLoadBalancer((LoadBalancer) Class.forName(RedissonConfig.loadBalancer).newInstance());
        rsc.setSubscriptionConnectionMinimumIdleSize(RedissonConfig.subscriptionConnectionMinimumIdleSize);
        rsc.setSubscriptionConnectionPoolSize(RedissonConfig.subscriptionConnectionPoolSize);
        rsc.setMasterConnectionPoolSize(RedissonConfig.masterConnectionPoolSize);
        rsc.setSlaveConnectionPoolSize(RedissonConfig.slaveConnectionPoolSize);
        rsc.setMasterConnectionMinimumIdleSize(RedissonConfig.masterConnectionMinimumIdleSize);
        rsc.setSlaveConnectionMinimumIdleSize(RedissonConfig.slaveConnectionMinimumIdleSize);
        rsc.setIdleConnectionTimeout(RedissonConfig.idleConnectionTimeout);
        rsc.setConnectTimeout(RedissonConfig.connectTimeout);
        rsc.setTimeout(RedissonConfig.timeout);
        rsc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        rsc.setRetryAttempts(RedissonConfig.retryAttempts);
        rsc.setRetryInterval(RedissonConfig.retryInterval);
        if (SslProvider.JDK.toString().equals(RedissonConfig.sslProvider)) {
            rsc.setSslProvider(SslProvider.JDK);
        } else if (SslProvider.OPENSSL.toString().equals(RedissonConfig.sslProvider)) {
            rsc.setSslProvider(SslProvider.OPENSSL);
        } else {
            rsc.setSslProvider(SslProvider.JDK);
        }
        rsc.setFailedAttempts(RedissonConfig.failedAttempts);
        rsc.setClientName(RedissonConfig.clientName);
        rsc.setPassword(RedissonConfig.password);
        rsc.setSubscriptionsPerConnection(RedissonConfig.subscriptionsPerConnection);
        rsc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        rsc.setSslEnableEndpointIdentification(RedissonConfig.sslEnableEndpointIdentification);
        rsc.setSslTruststore(RedissonConfig.sslTruststore == null ? null : new URI(RedissonConfig.sslTruststore));
        rsc.setSslTruststorePassword(RedissonConfig.sslTruststorePassword);
        rsc.setSslKeystore(RedissonConfig.sslKeystore == null ? null : new URI(RedissonConfig.sslKeystore));
        rsc.setSslKeystorePassword(RedissonConfig.sslKeystorePassword);
        rsc.setDatabase(RedissonConfig.database);
        for (String ipPort : ipPorts) {
            rsc.addNodeAddress(ipPort);
        }
    }

    private void singleServer(String[] ipPorts) throws URISyntaxException {
        SingleServerConfig ssc = config.useSingleServer();
        ssc.setAddress(ipPorts[0]);
        ssc.setSubscriptionConnectionMinimumIdleSize(RedissonConfig.subscriptionConnectionMinimumIdleSize);
        ssc.setSubscriptionConnectionPoolSize(RedissonConfig.subscriptionConnectionPoolSize);
        ssc.setConnectionMinimumIdleSize(RedissonConfig.masterConnectionMinimumIdleSize);
        ssc.setConnectionPoolSize(RedissonConfig.masterConnectionPoolSize);

        //ssc.setDnsMonitoring(dnsMonitoring);
        if (RedissonConfig.dnsMonitoring)
            ssc.setDnsMonitoringInterval(RedissonConfig.dnsMonitoringInterval);

        ssc.setDnsMonitoringInterval(RedissonConfig.dnsMonitoringInterval);
        ssc.setIdleConnectionTimeout(RedissonConfig.idleConnectionTimeout);
        ssc.setConnectTimeout(RedissonConfig.connectTimeout);
        ssc.setTimeout(RedissonConfig.timeout);
        ssc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        ssc.setRetryAttempts(RedissonConfig.retryAttempts);
        ssc.setRetryInterval(RedissonConfig.retryInterval);
        ssc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        if (SslProvider.JDK.toString().equals(RedissonConfig.sslProvider)) {
            ssc.setSslProvider(SslProvider.JDK);
        } else if (SslProvider.OPENSSL.toString().equals(RedissonConfig.sslProvider)) {
            ssc.setSslProvider(SslProvider.OPENSSL);
        } else {
            ssc.setSslProvider(SslProvider.JDK);
        }
        ssc.setFailedAttempts(RedissonConfig.failedAttempts);
        ssc.setDatabase(RedissonConfig.database);
        ssc.setClientName(RedissonConfig.clientName);
        ssc.setPassword(RedissonConfig.password);
        ssc.setSubscriptionsPerConnection(RedissonConfig.subscriptionsPerConnection);
        ssc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        ssc.setSslEnableEndpointIdentification(RedissonConfig.sslEnableEndpointIdentification);
        ssc.setSslTruststore(RedissonConfig.sslTruststore == null ? null : new URI(RedissonConfig.sslTruststore));
        ssc.setSslTruststorePassword(RedissonConfig.sslTruststorePassword);
        ssc.setSslKeystore(RedissonConfig.sslKeystore == null ? null : new URI(RedissonConfig.sslKeystore));
        ssc.setSslKeystorePassword(RedissonConfig.sslKeystorePassword);
    }

    private void sentinelServer(String[] ipPorts) throws URISyntaxException {
        if (StringUtils.isBlank(RedissonConfig.sentinelMaster)) {
            logger.error("哨兵模式没有配置Master名称");
            throw new RuntimeException("哨兵模式没有配置Master名称");
        }
        SentinelServersConfig ssc = config.useSentinelServers();
        ssc.setMasterName(RedissonConfig.sentinelMaster).addSentinelAddress(ipPorts);

        ssc.setSubscriptionConnectionMinimumIdleSize(RedissonConfig.subscriptionConnectionMinimumIdleSize);
        ssc.setSubscriptionConnectionPoolSize(RedissonConfig.subscriptionConnectionPoolSize);
        ssc.setIdleConnectionTimeout(RedissonConfig.idleConnectionTimeout);
        ssc.setConnectTimeout(RedissonConfig.connectTimeout);
        ssc.setTimeout(RedissonConfig.timeout);
        ssc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        ssc.setRetryAttempts(RedissonConfig.retryAttempts);
        ssc.setRetryInterval(RedissonConfig.retryInterval);
        ssc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        if (SslProvider.JDK.toString().equals(RedissonConfig.sslProvider)) {
            ssc.setSslProvider(SslProvider.JDK);
        } else if (SslProvider.OPENSSL.toString().equals(RedissonConfig.sslProvider)) {
            ssc.setSslProvider(SslProvider.OPENSSL);
        } else {
            ssc.setSslProvider(SslProvider.JDK);
        }
        ssc.setFailedAttempts(RedissonConfig.failedAttempts);
        ssc.setDatabase(RedissonConfig.database);
        ssc.setClientName(RedissonConfig.clientName);
        ssc.setPassword(RedissonConfig.password);
        ssc.setSubscriptionsPerConnection(RedissonConfig.subscriptionsPerConnection);
        ssc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        ssc.setSslEnableEndpointIdentification(RedissonConfig.sslEnableEndpointIdentification);
        ssc.setSslTruststore(RedissonConfig.sslTruststore == null ? null : new URI(RedissonConfig.sslTruststore));
        ssc.setSslTruststorePassword(RedissonConfig.sslTruststorePassword);
        ssc.setSslKeystore(RedissonConfig.sslKeystore == null ? null : new URI(RedissonConfig.sslKeystore));
        ssc.setSslKeystorePassword(RedissonConfig.sslKeystorePassword);
    }

    private void clusterServer(String[] ipPorts)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, URISyntaxException {
        ClusterServersConfig csc = config.useClusterServers();
        csc.setScanInterval(RedissonConfig.scanInterval);
        if (ReadMode.SLAVE.toString().equals(RedissonConfig.readMode)) {
            csc.setReadMode(ReadMode.SLAVE);
        } else if (ReadMode.MASTER.toString().equals(RedissonConfig.readMode)) {
            csc.setReadMode(ReadMode.MASTER);
        } else if (ReadMode.MASTER_SLAVE.toString().equals(RedissonConfig.readMode)) {
            csc.setReadMode(ReadMode.MASTER_SLAVE);
        } else {
            csc.setReadMode(ReadMode.SLAVE);
        }
        if (SubscriptionMode.SLAVE.toString().equals(RedissonConfig.subscriptionMode)) {
            csc.setSubscriptionMode(SubscriptionMode.SLAVE);
        } else if (SubscriptionMode.MASTER.toString().equals(RedissonConfig.subscriptionMode)) {
            csc.setSubscriptionMode(SubscriptionMode.MASTER);
        } else {
            csc.setSubscriptionMode(SubscriptionMode.SLAVE);
        }
        csc.setLoadBalancer((LoadBalancer) Class.forName(RedissonConfig.loadBalancer).newInstance());
        csc.setSubscriptionConnectionMinimumIdleSize(RedissonConfig.subscriptionConnectionMinimumIdleSize);
        csc.setSubscriptionConnectionPoolSize(RedissonConfig.subscriptionConnectionPoolSize);
        csc.setMasterConnectionPoolSize(RedissonConfig.masterConnectionPoolSize);
        csc.setSlaveConnectionPoolSize(RedissonConfig.slaveConnectionPoolSize);
        csc.setMasterConnectionMinimumIdleSize(RedissonConfig.masterConnectionMinimumIdleSize);
        csc.setSlaveConnectionMinimumIdleSize(RedissonConfig.slaveConnectionMinimumIdleSize);
        csc.setIdleConnectionTimeout(RedissonConfig.idleConnectionTimeout);
        csc.setConnectTimeout(RedissonConfig.connectTimeout);
        csc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        csc.setTimeout(RedissonConfig.timeout);
        csc.setRetryAttempts(RedissonConfig.retryAttempts);
        csc.setRetryInterval(RedissonConfig.retryInterval);
        if (SslProvider.JDK.toString().equals(RedissonConfig.sslProvider)) {
            csc.setSslProvider(SslProvider.JDK);
        } else if (SslProvider.OPENSSL.toString().equals(RedissonConfig.sslProvider)) {
            csc.setSslProvider(SslProvider.OPENSSL);
        } else {
            csc.setSslProvider(SslProvider.JDK);
        }
        csc.setFailedAttempts(RedissonConfig.failedAttempts);
        csc.setClientName(RedissonConfig.clientName);
        csc.setPassword(RedissonConfig.password);
        csc.setSubscriptionsPerConnection(RedissonConfig.subscriptionsPerConnection);
        csc.setReconnectionTimeout(RedissonConfig.reconnectionTimeout);
        csc.setSslEnableEndpointIdentification(RedissonConfig.sslEnableEndpointIdentification);
        csc.setSslTruststore(RedissonConfig.sslTruststore == null ? null : new URI(RedissonConfig.sslTruststore));
        csc.setSslTruststorePassword(RedissonConfig.sslTruststorePassword);
        csc.setSslKeystore(RedissonConfig.sslKeystore == null ? null : new URI(RedissonConfig.sslKeystore));
        csc.setSslKeystorePassword(RedissonConfig.sslKeystorePassword);
        for (String ipPort : ipPorts) {
            csc.addNodeAddress(ipPort);
        }
    }



    /**
     * 根据key获得map的value
     *
     * @param mapName
     * @return
     */
    public <K, V> Map<K, V> getMap(String mapName) {
        if (mapName == null) {
            throw new RuntimeException("mapName不可为null");
        } else {
            Map<K, V> map = redissonClient.getMap(mapName);
            return map;
        }
    }




    /**
     * 获得缓存数据集
     * @param method
     * @return
     */
    @Override
    public Object getCache(String method) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            Map<String, Object> map = getMap(method);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从缓存总获得数据
     *
     * @param method
     * @return
     */
    @Override
    public Object getCache(String method, String key) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            Map<String, Object> map = getMap(method);
            if (map != null) {
                return map.get(key);
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 设置缓存数据
     *
     * @param method
     * @param key
     * @param obj
     */
    @Override
    public void setCache(String method, String key, Object obj) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            setMapWithoutLock(method, key, obj, RedissonConfig.defaultExpireMinutes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setCache(String method, String key, Object obj, long expireSeconds) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            setMapWithoutLock(method, key, obj, expireSeconds/1000/60);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 删除缓存数据
     *
     * @param method
     */
    @Override
    public void delCache(String method) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            delMapWithoutLock(method);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delCache(String method, String key) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            delMapWithoutLock(method,key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delCache(String method, Collection<String> keys) {
        method = RedissonConfig.AUTO_CACHE_PREFIX+method;
        try {
            String[] keyArr = new String[keys.size()];
            delMapWithoutLock(method,keys.toArray(keyArr));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setMapWithoutLock(String mapName, String key, Object obj, long expireMinutes) {
        if (mapName != null && key != null && obj != null) {
            RMap<Object, Object> map = redissonClient.getMap(mapName);
            map.fastPut(key, obj);
            map.expire(expireMinutes, TimeUnit.MINUTES);
        } else {
            throw new RuntimeException("mapName,key,obj不可为null");
        }
    }

    public void delMapWithoutLock(String mapName) {
        if (mapName != null) {
            RMap<Object, Object> map = redissonClient.getMap(mapName);
            if (map != null) {
                map.delete();
            }
        } else {
            throw new RuntimeException("mapName不可为null");
        }
    }

    public void delMapWithoutLock(String mapName, String key) {
        if (mapName != null) {
            RMap<Object, Object> map = redissonClient.getMap(mapName);
            if (map != null) {
                map.fastRemove(key);
            }
        } else {
            throw new RuntimeException("mapName不可为null");
        }
    }

    public void delMapWithoutLock(String mapName, String[] keys) {
        if (mapName != null) {
            RMap<Object, Object> map = redissonClient.getMap(mapName);
            if (map != null) {
                map.fastRemove(keys);
            }
        } else {
            throw new RuntimeException("mapName不可为null");
        }
    }

    @Override
    public Lock startLock(String method) {
        return startLock(method,RedissonConfig.autoUnLockTime/1000);
    }

    /*@Override
    public Lock startLock(Collection<String> methods) {
        return startLock(methods,RedissonConfig.autoUnLockTime/1000);
    }*/

    @Override
    public Lock startLock(String method, long seconds) {
        RReadWriteLock lock = redissonClient.getReadWriteLock(RedissonConfig.AUTO_LOCK_PREFIX+method);
        lock.writeLock().lock(seconds, TimeUnit.SECONDS);
        return lock.writeLock();
    }

    /*@Override
    public Lock startLock(Collection<String> methods, long seconds) {
        if(methods!=null && methods.size()>0){
            RLock[] locks = new RLock[methods.size()];
            Iterator<String> it = methods.iterator();
            int i=0;
            while(it.hasNext()){
                locks[i]=redissonClient.getReadWriteLock(RedissonConfig.AUTO_LOCK_PREFIX+it.next()).writeLock();
                i++;
            }
            RedissonMultiLock lock = new RedissonMultiLock(locks);
            lock.lock(seconds, TimeUnit.SECONDS);
            return lock;
        }
        return null;
    }*/

    @Override
    public Lock startTryLock(String method) {
        return startTryLock(method,0,RedissonConfig.autoUnLockTime/1000);
    }

    @Override
    public Lock startTryLock(String method, long waitSeconds) {
        return startTryLock(method,0,waitSeconds);
    }

    @Override
    public Lock startTryLock(String method, long waitSeconds, long lockSeconds) {
        RReadWriteLock lock = redissonClient.getReadWriteLock(RedissonConfig.AUTO_LOCK_PREFIX+method);
        try {
            boolean isLock = lock.writeLock().tryLock(waitSeconds, lockSeconds, TimeUnit.SECONDS);
            if(isLock)
                return lock.writeLock();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    public void unLock(Lock lock) {
        try{
            if(lock!=null)
                lock.unlock();
        }catch (Exception e){
            logger.warn("unLock fail!");
        }
    }
}
