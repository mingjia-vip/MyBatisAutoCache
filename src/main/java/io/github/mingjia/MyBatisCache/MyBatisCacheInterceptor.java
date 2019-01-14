package io.github.mingjia.MyBatisCache;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
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
import org.apache.log4j.Logger;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

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
            if(cacheService==null)
                initDefaultCacheService();

            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];
            Object parameter = args[1];

            String invocationMethodName = invocation.getMethod().getName();
            if (StringUtils.equals("query", invocationMethodName)) {
                //判断方法是否在非缓存集合，在则直接查询数据库
                if (isNoCacheContains(mappedStatement.getId())) {
                    log.debug("读取数据库");
                    return invocation.proceed();
                } else {
                    boolean isPage = false;
                    Executor executor = (Executor) invocation.getTarget();
                    CacheKey cacheKey;
                    BoundSql boundSql;
                    if (args.length == 4) {
                        //4 个参数时
                        log.error("getBoundSql parameter:"+parameter);
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
                log.debug("sql:"+sql);
                Statement statement = CCJSqlParserUtil.parse(sql);
                if(statement instanceof Delete){
                    String table = ((Delete) statement).getTable().getName();
                    log.debug("table:"+table);
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String method : m) {
                        cacheService.delCache(method);
                    }
                }else if(statement instanceof Update){
                    Update update = (Update) statement;
                    Set<String> dealTableName = new HashSet<>();
                    for(Column column:update.getColumns()){
                        if(column.getTable()!=null)
                            dealTableName.add(column.getTable().getName());
                    }
                    for (Table t : update.getTables()) {
                        log.debug("table:"+t.getName());
                        if(!CollectionUtils.isEmpty(dealTableName) && !dealTableName.contains(t.getName()) && !dealTableName.contains(t.getAlias()))
                            break;
                        Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(t.getName());
                        for (String method : m) {
                            cacheService.delCache(method);
                        }
                    }

                }else if(statement instanceof Insert){
                    String table = ((Insert) statement).getTable().getName();
                    log.debug("table:"+table);
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

    private boolean isNoCacheContains(String currentMethod) {
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
    private MybatisCacheServiceI cacheService = null;

    private void initDefaultCacheService(){
        cacheService = new MybatisCacheServiceI<String, String, Object>() {

            private Cache<String, Map<String, Object>> cache = CacheBuilder.newBuilder()
                    .initialCapacity(100)//设置cache的初始大小为100，要合理设置该值
                    .concurrencyLevel(100)//设置并发数为10，即同一时间最多只能有10个线程往cache执行写入操作
                    .expireAfterWrite(24, TimeUnit.HOURS)//设置cache中的数据在写入之后的存活时间为1小时
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
                if (kvs == null){
                    kvs = new HashMap<>();
                    cache.put(method,kvs);
                }
                kvs.put(key, value);
            }

            @Override
            public void delCache(String method) {
                cache.invalidate(method);
            }

        };
    }

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