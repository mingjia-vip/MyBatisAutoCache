package io.github.mingjia.mybatis.autocache.service.redisson;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;


/**
 * 缓存初始化参数配置
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public class RedissonConfig {


    public static final Logger logger = LoggerFactory.getLogger(RedissonConfig.class);
    public static boolean isConfigOk;
    public static String configProperties = "autoCache.properties";

    /*static {
        loadParams();
    }*/

    /*** base config ***/

    public static String appName;
    public static String AUTO_CACHE_PREFIX = "AutoCache:";
    public static String AUTO_LOCK_PREFIX = "AutoLock:";
    public static Long defaultExpireMinutes = 60l;
    public static Long autoUnLockTime = 5000l;
    public static Long tryLockWaitTime = 5000l;
    public static Boolean mulityThread = true;

    /*** redis config ***/

    public static String clusterIp;
    public static String singleIp;
    public static String cloudIp;
    public static String sentinelIp;
    public static String sentinelMaster;

    public static String clientName;
    public static String password;

    public static int scanInterval = 1000;
    public static String readMode = "SLAVE";
    public static String subscriptionMode = "SLAVE";
    public static String loadBalancer = "org.redisson.connection.balancer.RoundRobinLoadBalancer";
    public static int subscriptionConnectionMinimumIdleSize = 1;
    public static int subscriptionConnectionPoolSize = 50;
    public static int masterConnectionPoolSize = 64;
    public static int slaveConnectionPoolSize = 64;
    public static int masterConnectionMinimumIdleSize = 10;
    public static int slaveConnectionMinimumIdleSize = 10;
    public static int connectTimeout = 10000;
    public static int timeout = 3000;
    public static int retryAttempts = 3;
    public static int retryInterval = 1500;
    public static int idleConnectionTimeout = 10000;
    public static int reconnectionTimeout = 3000;
    public static int failedAttempts = 3;
    public static int subscriptionsPerConnection = 5;
    public static Boolean sslEnableEndpointIdentification = true;
    public static String sslProvider = "JDK";
    public static String sslTruststore;
    public static String sslTruststorePassword;
    public static String sslKeystore;
    public static String sslKeystorePassword;
    public static int database = 1;
    public static Boolean dnsMonitoring = false;
    public static int dnsMonitoringInterval = 5000;


    public static boolean loadParams() {
        return loadParams(null);
    }

    public static boolean loadParams(String configProperties) {

        Properties props = new Properties();
        try {
            if (configProperties != null)
                props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(configProperties));
            else
                props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("autoCache.properties"));


            /*** base config ***/
            appName = props.getProperty("cache.appname");
            if (StringUtils.isNotBlank(appName)) {
                AUTO_CACHE_PREFIX = appName + "-" + AUTO_CACHE_PREFIX;
                AUTO_LOCK_PREFIX = appName + "-" + AUTO_LOCK_PREFIX;
            }
            if (props.getProperty("cache.expireMinutes") != null)
                defaultExpireMinutes = Long.valueOf(props.getProperty("cache.expireMinutes"));
            if (props.getProperty("redissonCache.lock.autoUnLockTime") != null)
                autoUnLockTime = Long.valueOf(props.getProperty("cache.lock.autoUnlockTime"));
            if (props.getProperty("cache.lock.tryLockWaitTime") != null)
                tryLockWaitTime = Long.valueOf(props.getProperty("cache.lock.tryLockWaitTime"));
            if (props.getProperty("cache.clean.mulityThread") != null)
                mulityThread = Boolean.valueOf(props.getProperty("cache.clean.mulityThread"));


            /*** redis config ***/

            clusterIp = props.getProperty("redissonCache.redis.clusterIp");
            singleIp = props.getProperty("redissonCache.redis.singleIp");
            cloudIp = props.getProperty("redissonCache.redis.cloudIp");
            sentinelIp = props.getProperty("cache.redis.sentinelIp");
            sentinelMaster = props.getProperty("cache.redis.sentinelMaster");

            clientName = props.getProperty("redissonCache.redis.clientName");
            password = props.getProperty("redissonCache.redis.password");

            if (props.getProperty("redissonCache.redis.scanInterval") != null)
                scanInterval = Integer.valueOf(props.getProperty("redissonCache.redis.scanInterval"));

            if (props.getProperty("redissonCache.redis.readMode") != null)
                readMode = props.getProperty("redissonCache.redis.readMode");

            if (props.getProperty("redissonCache.redis.subscriptionMode") != null)
                subscriptionMode = props.getProperty("redissonCache.redis.subscriptionMode");

            if (props.getProperty("redissonCache.redis.loadBalancer:org.redisson.connection.balancer.RoundRobinLoadBalancer") != null)
                loadBalancer = props.getProperty("redissonCache.redis.loadBalancer:org.redisson.connection.balancer.RoundRobinLoadBalancer");

            if (props.getProperty("redissonCache.redis.subscriptionConnectionMinimumIdleSize") != null)
                subscriptionConnectionMinimumIdleSize = Integer.valueOf(props.getProperty("redissonCache.redis.subscriptionConnectionMinimumIdleSize"));

            if (props.getProperty("redissonCache.redis.subscriptionConnectionPoolSize") != null)
                subscriptionConnectionPoolSize = Integer.valueOf(props.getProperty("redissonCache.redis.subscriptionConnectionPoolSize"));

            if (props.getProperty("cache.redis.masterConnectionPoolSize") != null)
                masterConnectionPoolSize = Integer.valueOf(props.getProperty("cache.redis.masterConnectionPoolSize"));

            if (props.getProperty("cache.redis.slaveConnectionPoolSize") != null)
                slaveConnectionPoolSize = Integer.valueOf(props.getProperty("cache.redis.slaveConnectionPoolSize"));

            if (props.getProperty("cache.redis.masterConnectionMinimumIdleSize") != null)
                masterConnectionMinimumIdleSize = Integer.valueOf(props.getProperty("cache.redis.masterConnectionMinimumIdleSize"));

            if (props.getProperty("cache.redis.slaveConnectionMinimumIdleSize") != null)
                slaveConnectionMinimumIdleSize = Integer.valueOf(props.getProperty("cache.redis.slaveConnectionMinimumIdleSize"));

            if (props.getProperty("cache.redis.connectTimeout") != null)
                connectTimeout = Integer.valueOf(props.getProperty("cache.redis.connectTimeout"));

            if (props.getProperty("redissonCache.redis.timeout") != null)
                timeout = Integer.valueOf(props.getProperty("redissonCache.redis.timeout"));

            if (props.getProperty("redissonCache.redis.retryAttempts") != null)
                retryAttempts = Integer.valueOf(props.getProperty("redissonCache.redis.retryAttempts"));

            if (props.getProperty("redissonCache.redis.retryInterval") != null)
                retryInterval = Integer.valueOf(props.getProperty("redissonCache.redis.retryInterval"));

            if (props.getProperty("redissonCache.redis.idleConnectionTimeout") != null)
                idleConnectionTimeout = Integer.valueOf(props.getProperty("redissonCache.redis.idleConnectionTimeout"));

            if (props.getProperty("redissonCache.redis.reconnectionTimeout") != null)
                reconnectionTimeout = Integer.valueOf(props.getProperty("redissonCache.redis.reconnectionTimeout"));

            if (props.getProperty("redissonCache.redis.failedAttempts") != null)
                failedAttempts = Integer.valueOf(props.getProperty("redissonCache.redis.failedAttempts"));

            if (props.getProperty("redissonCache.redis.subscriptionsPerConnection") != null)
                subscriptionsPerConnection = Integer.valueOf(props.getProperty("redissonCache.redis.subscriptionsPerConnection"));

            if (props.getProperty("redissonCache.redis.sslEnableEndpointIdentification") != null)
                sslEnableEndpointIdentification = Boolean.valueOf(props.getProperty("redissonCache.redis.sslEnableEndpointIdentification"));

            if (props.getProperty("redissonCache.redis.sslProvider") != null)
                sslProvider = props.getProperty("redissonCache.redis.sslProvider");

            sslTruststore = props.getProperty("redissonCache.redis.sslTruststore");
            sslTruststorePassword = props.getProperty("redissonCache.redis.sslTruststorePassword");
            sslKeystore = props.getProperty("redissonCache.redis.sslKeystore");
            sslKeystorePassword = props.getProperty("redissonCache.redis.sslKeystorePassword");

            if (props.getProperty("cache.redis.database") != null)
                database = Integer.valueOf(props.getProperty("cache.redis.database"));

            if (props.getProperty("redissonCache.redis.dnsMonitoring") != null)
                dnsMonitoring = Boolean.valueOf(props.getProperty("redissonCache.redis.dnsMonitoring"));

            if (props.getProperty("redissonCache.redis.dnsMonitoringInterval") != null)
                dnsMonitoringInterval = Integer.valueOf(props.getProperty("redissonCache.redis.dnsMonitoringInterval"));


            isConfigOk = true;
        } catch (IOException e) {

            isConfigOk = false;
            logger.error("Cant't load autoCache.properties ... ");
        }

        return isConfigOk;
    }


}
