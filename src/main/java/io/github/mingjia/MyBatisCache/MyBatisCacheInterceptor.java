package io.github.mingjia.MyBatisCache;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.mingjia.MyBatisCache.expression.SimpleExpression;
import io.github.mingjia.MyBatisCache.parser.PrimaryFieldParser;
import io.github.mingjia.MyBatisCache.util.ExecutorUtil;
import io.github.mingjia.MyBatisCache.util.MSUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

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

    private final Logger logger = LoggerFactory.getLogger(MyBatisCacheInterceptor.class);

    public MyBatisCacheInterceptor() {
    }

    private SimpleExpression expression = new SimpleExpression();

    private String _PRIMARY = "_primary";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            if (cacheService == null)
                initDefaultCacheService();

            Object[] args = invocation.getArgs();
            MappedStatement mappedStatement = (MappedStatement) args[0];
            Object parameter = args[1];

            String invocationMethodName = invocation.getMethod().getName();
            if (StringUtils.equals("query", invocationMethodName)) {

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

                //判断方法是否在非缓存集合，在则直接查询数据库
                if (isNoCacheContains(mappedStatement.getId())) {
                    logger.debug("只读取数据库");
                    return invocation.proceed();
                } else {
                    boolean isPage = false;


                    String method = DigestUtils.md5Hex(mappedStatement.getId());
                    logger.debug("key:" + cacheKey.toString());
                    logger.debug("method:" + mappedStatement.getId());
                    String key = DigestUtils.md5Hex(cacheKey.toString());
                    Object obj = cacheService.getCache(method + _PRIMARY, key);
                    if (obj == null)
                        obj = cacheService.getCache(method, key);
                    if (obj == null) {
                        logger.debug("读取数据库");
                        //分布式锁开始，保证数据一致性
                        List<String> loclMethods = new ArrayList<>(2);
                        loclMethods.add(method);
                        loclMethods.add(method + _PRIMARY);
                        Lock lock = cacheService.startLock(loclMethods);
                        try {
                            logger.info("start set key:" + key);
                            obj = invocation.proceed();

                            try {
                                //为了提高缓存效率，增加了根据主键清除缓存的逻辑，但是只针对单表操作
                                Set<String> tables = MyBatisCacheConfig.METHOD_TABLES.get(method);
                                if (tables != null && tables.size() == 1) {
                                    String[] primaries = getPrimaries(method);
                                    List<String> primaryValues = null;
                                    //目前只针对单表进行处理
                                    for (String table : tables) {
                                        if (primaries != null && primaries.length > 0) {
                                            primaryValues = new ArrayList<>(primaries.length);
                                            for (String primary : primaries) {
                                                String primaryFiled = getPrimaryField(primary);
                                                //验证primaryFiled：该字段是否出现在where条件语句中，并且是=？
                                                String sql = boundSql.getSql();
                                                Statement statement = CCJSqlParserUtil.parse(sql);
                                                Select selectStatement = (Select) statement;
                                                Object whereValue = PrimaryFieldParser.parserWhere(selectStatement.getSelectBody(), table, primaryFiled);
                                                if (whereValue != null && whereValue.toString().trim().equals("?")) {
                                                    if (parameter instanceof Map) {
                                                        String primaryValue = expression.evaluate(getPrimaryEL(primary), Object.class, (Map) parameter).toString();
                                                        primaryValues.add(primaryValue);
                                                    } else {
                                                        Map<String, Object> map = new HashMap<>(1);
                                                        map.put("param1", parameter);
                                                        map.put(parameter.getClass().getSimpleName(), parameter);
                                                        map.put(parameter.getClass().getSimpleName(), parameter);
                                                        String primaryValue = expression.evaluate(getPrimaryEL(primary), Object.class, map).toString();
                                                        primaryValues.add(primaryValue);
                                                    }
                                                } else {
                                                    throw new CacheException("primaries配置的主键字段未在sql中使用：" + mappedStatement.getId());
                                                }
                                            }
                                        }
                                    }
                                    if (primaryValues != null && primaryValues.size() == primaries.length) {
                                        method += _PRIMARY;
                                        StringBuffer primaryKey = new StringBuffer();
                                        for (int i = 0; i < primaryValues.size(); i++) {
                                            if (i > 0)
                                                primaryKey.append("_");
                                            primaryKey.append(primaryValues.get(i));
                                        }
                                        Object oldvalue = cacheService.getCache(method + "_map", primaryKey.toString());
                                        if (oldvalue == null)
                                            cacheService.setCache(method + "_map", primaryKey.toString(), key);
                                        else
                                            cacheService.setCache(method + "_map", primaryKey.toString(), oldvalue.toString() + ";" + key);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                if (method.endsWith(_PRIMARY)) {
                                    cacheService.delCache(method + "_map");
                                    method = method.replace(_PRIMARY, "");
                                }
                            }


                            cacheService.setCache(method, key, obj);

                        } finally {
                            if (lock != null) {
                                logger.info("end set key:" + key);
                                lock.unlock();
                            }
                        }
                        //分布式锁开始，保证数据一致性
                        return obj;
                    } else {
                        logger.debug("读取缓存");
                        return obj;
                    }
                }

            } else if (StringUtils.equals("update", invocation.getMethod().getName())) {
                BoundSql boundSql = mappedStatement.getBoundSql(parameter);
                String sql = boundSql.getSql();
                logger.debug("sql:" + sql);
                Statement statement = CCJSqlParserUtil.parse(sql);
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

                Set<String> cleanMethods = new HashSet<>();
                for (String table : tables) {
                    logger.debug("table:" + table);
                    Set<String> m = MyBatisCacheConfig.TABLE_METHODS.get(table);
                    if (!CollectionUtils.isEmpty(m))
                        cleanMethods.addAll(m);
                }

                //开始清除缓存
                if (!CollectionUtils.isEmpty(cleanMethods)) {
                    //分布式锁开始，保证数据一致性
                    List<String> lockMethods = new ArrayList<>(cleanMethods.size() * 2);
                    for (String method : cleanMethods) {
                        lockMethods.add(method);
                        lockMethods.add(method + _PRIMARY);
                    }
                    Lock lock = cacheService.startLock(lockMethods);
                    try {
                        logger.info("start clean cache");
                        for (String method : cleanMethods) {
                            //清除非primary方式的
                            cacheService.delCache(method);

                            if (cacheService.getCache(method + _PRIMARY) != null) {
                                String[] primaryFields = getPrimaryFields(method);
                                if (primaryFields != null && primaryFields.length > 0) {
                                    //如果该方法指定主键的话需要计算出变动的主键数据
                                    Set<String> tableSet = MyBatisCacheConfig.METHOD_TABLES.get(method);
                                    if (tableSet != null && tableSet.size() == 1) {
                                        MappedStatement selectPrimaryMs = MSUtils.newPrimarySelectMappedStatement(mappedStatement);
                                        String targetTable = null;
                                        for (String table : tableSet) {
                                            targetTable = table;
                                            break;
                                        }
                                        //执行变更前先修改sql为select，查询并更涉及到得主键值，
                                        List<Map<String, Object>> values = ExecutorUtil.primaryKeyValues(
                                                (Executor) invocation.getTarget(),
                                                selectPrimaryMs,
                                                    /*parameter,*/
                                                boundSql,
                                                Executor.NO_RESULT_HANDLER,
                                                targetTable,
                                                primaryFields);

                                        //然后根据这些值移除缓存
                                        if (values != null && values.size() > 0) {
                                            Set<String> keys = new HashSet<>(values.size());
                                            for (Map<String, Object> map : values) {
                                                StringBuffer key = new StringBuffer();
                                                for (int i = 0; i < primaryFields.length; i++) {
                                                    /*if(map.get(primaryFields[i])!=null){
                                                        key.append(map.get(field).toString()).append("_");
                                                    }*/
                                                    if (i > 0)
                                                        key.append("_");
                                                    key.append(map.get(primaryFields[i].toUpperCase()));
                                                }
                                                Object cacheKeys = cacheService.getCache(method + _PRIMARY + "_map", key.toString());
                                                if (cacheKeys != null) {
                                                    String[] keyArr = cacheKeys.toString().split(";");
                                                    cacheService.delCache(method + _PRIMARY, CollectionUtils.arrayToList(keyArr));
                                                }
                                                keys.add(key.toString());
                                            }
                                            if (!CollectionUtils.isEmpty(keys)) {
                                                cacheService.delCache(method + _PRIMARY + "_map", keys);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return invocation.proceed();
                    } finally {
                        if (lock != null) {
                            logger.info("end clean cache");
                            lock.unlock();
                        }
                    }
                    //分布式锁开始，保证数据一致性
                } else {
                    return invocation.proceed();
                }

            } else {
                return invocation.proceed();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return invocation.proceed();
        }
    }


    private String[] getPrimaries(String method) {
        if (MyBatisCacheConfig.METHOD_PRIMARYS.get(method) == null || MyBatisCacheConfig.METHOD_PRIMARYS.get(method).trim().length() < 1)
            return null;
        return MyBatisCacheConfig.METHOD_PRIMARYS.get(method).split(";");
    }

    private String[] getPrimaryFields(String method) {
        String[] primaries = getPrimaries(method);
        if (primaries == null)
            return null;
        String[] fields = new String[primaries.length];
        for (int i = 0; i < primaries.length; i++) {
            fields[i] = getPrimaryField(primaries[i]);
        }
        return fields;
    }

    private String getPrimaryField(String primary) {
        return primary.substring(0, primary.indexOf("=")).trim().toUpperCase();
    }

    private String getPrimaryEL(String primary) {
        return primary.substring(primary.indexOf("=") + 1).trim();
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

    private void initDefaultCacheService() {
        cacheService = new MybatisCacheServiceI<String, String, Object>() {

            private Cache<String, Map<String, Object>> cache = CacheBuilder.newBuilder()
                    .initialCapacity(100)//设置cache的初始大小为100，要合理设置该值
                    .concurrencyLevel(100)//设置并发数为10，即同一时间最多只能有10个线程往cache执行写入操作
                    .expireAfterWrite(24, TimeUnit.HOURS)//设置cache中的数据在写入之后的存活时间为1小时
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

            @Override
            public Lock startLock(String method) {
                return null;
            }

            @Override
            public Lock startLock(Collection<String> methods) {
                return null;
            }

            @Override
            public Lock startLock(String method, long seconds) {
                return null;
            }

            @Override
            public Lock startLock(Collection<String> methods, long seconds) {
                return null;
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