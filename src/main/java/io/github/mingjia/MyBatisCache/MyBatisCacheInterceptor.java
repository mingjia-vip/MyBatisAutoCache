package io.github.mingjia.MyBatisCache;


import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;

import java.util.Properties;
import java.util.Set;

/**
 * 缓存拦截器
 *
 * @auther GuiBin
 * @create 18/2/27
 */
@SuppressWarnings(value = {"unchecked", "rawtypes", "unused"})
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public class MyBatisCacheInterceptor implements Interceptor {

    private static Logger log = Logger.getLogger(MyBatisCacheInterceptor.class);

    public MyBatisCacheInterceptor() {
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];
            Object parameter = args[1];

            String invocationMethodName = invocation.getMethod().getName();
            if (StringUtils.equals("query", invocationMethodName)) {
                //判断方法是否在非缓存集合，在则直接查询数据库
                if (contains(mappedStatement.getId())) {
                    log.debug("读取数据库");
                    return invocation.proceed();
                } else {
                    boolean isPage = false;
                    Executor executor = (Executor) invocation.getTarget();
                    CacheKey cacheKey;
                    BoundSql boundSql;
                    if (args.length == 4) {
                        //4 个参数时
                        boundSql = mappedStatement.getBoundSql(parameter);
                        cacheKey = executor.createCacheKey(mappedStatement, parameter, (RowBounds) args[2], boundSql);
                    } else {
                        //6 个参数时
                        cacheKey = (CacheKey) args[4];
                        boundSql = (BoundSql) args[5];
                    }
                    //String sql = boundSql.getSql();
                    String method = DigestUtils.md5Hex(mappedStatement.getId());
                    log.debug("key:" + cacheKey.toString());
                    log.debug("method:" + mappedStatement.getId());
                    String key = DigestUtils.md5Hex(cacheKey.toString());
                    Object obj = cacheService.getCache(method, key);
                    if (obj == null) {
                        log.debug("读取数据库");
                        obj = invocation.proceed();
                        cacheService.setCache(method, key, obj);
                        return obj;
                    } else {
                        log.debug("读取缓存");
                        return obj;
                    }
                }

            } else if (StringUtils.equals("update", invocation.getMethod().getName())) {
                String sql = mappedStatement.getBoundSql(parameter).getSql();
                if (StringUtils.containsIgnoreCase(sql, "insert")) {
                    String table = sql.split("\\s+")[2].toLowerCase();
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String method : m) {
                        cacheService.delCache(method);
                    }
                } else if (StringUtils.containsIgnoreCase(sql, "delete")) {
                    String table = sql.split("\\s+")[2].toLowerCase();
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String method : m) {
                        cacheService.delCache(method);
                    }
                } else if (StringUtils.containsIgnoreCase(sql, "update")) {
                    String table = sql.split("\\s+")[1].toLowerCase();
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String method : m) {
                        cacheService.delCache(method);
                    }
                }
                return invocation.proceed();
            } else {
                return invocation.proceed();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return invocation.proceed();
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private boolean contains(String currentMethod) {
        boolean contains = false;
        for (String method : MyBatisCacheConfig.DIS_CACHE_METHOD) {
            if (StringUtils.equals(method, currentMethod)) {
                contains = true;
                break;
            }
        }
        return contains;
    }


    private Properties properties;
    private MybatisCacheServiceI cacheService = MybatisCacheServiceI.GUAVA_CACHE;

    /**
     * 设置缓存实现
     *
     * @param cacheService
     */
    public void setCacheService(MybatisCacheServiceI cacheService) {
        this.cacheService = cacheService;
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

}