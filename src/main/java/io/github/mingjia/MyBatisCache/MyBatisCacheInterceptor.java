package io.github.mingjia.MyBatisCache;


import com.github.pagehelper.Dialect;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageException;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.cache.Cache;
import com.github.pagehelper.cache.CacheFactory;
import com.github.pagehelper.util.MSUtils;
import com.github.pagehelper.util.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;

/**
 * 缓存拦截器
 * 依赖PageHelper v5 TODO：PageHelper v5和v4的实现有别，需要分别处理
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

    private MybatisCacheServiceI cacheService = MybatisCacheServiceI.GUAVA_CACHE;

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
                    log.trace("读取数据库");
                    if (PageHelper.getLocalPage() != null) {
                        log.trace("分页拦截");
                        return pageIntercept(invocation);
                    } else {
                        log.trace("普通拦截");
                        return invocation.proceed();
                    }
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
                    String sql = boundSql.getSql();

                    String method = DigestUtils.md5Hex(mappedStatement.getId());

                    //方法参数的变化会自动体现到CacheKey上
                    StringBuffer sb = new StringBuffer(cacheKey.toString());
                    Object parameterObject = boundSql.getParameterObject();
                    //判断方法参数是否为空，不为空需要将参数信息写入sb，作为key的一部分
                    if (parameterObject != null) {
                        String parameterObjectType = parameterObject.getClass().getSimpleName();

                        //参数信息
                        if (PageHelper.getLocalPage() != null) {
                            isPage = true;
                            sb.append(":").append(parameterObjectType);

                            //拦截前获得分页数据
                            sb.append(":").append("pageNum");
                            sb.append(":").append(PageHelper.getLocalPage().getPageNum());
                            sb.append(":").append("pageSize");
                            sb.append(":").append(PageHelper.getLocalPage().getPageSize());
                        }
                    }
                    log.trace("method:"+mappedStatement.getId()+",code:"+method);
                    //log.trace(sb.toString());
                    String key = DigestUtils.md5Hex(sb.toString());
                    Object obj = cacheService.getCache(method,key);
                    if (obj == null) {
                        if (isPage) {
                            obj = pageIntercept(invocation,cacheKey,boundSql);
                            cacheService.setCache(method, key, parsePage((Page) obj));

                        } else {
                            obj = invocation.proceed();
                            cacheService.setCache(method, key, obj);
                        }
                        log.trace("读取数据库");
                        return obj;
                    } else {
                        log.trace("读取缓存");
                        if (isPage) {
                            Page page = parseMap((Map<String, Object>) obj);
                            if (PageHelper.getLocalPage() != null)
                                PageHelper.clearPage();
                            return page;

                        } else {
                            return obj;
                        }

                    }
                }

            } else if (StringUtils.equals("update", invocation.getMethod().getName())) {
                String sql = mappedStatement.getBoundSql(parameter).getSql();
                if (StringUtils.containsIgnoreCase(sql, "insert")) {
                    String table = sql.split("\\s+")[2].toLowerCase();
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String mapName : m) {
                        cacheService.delCache(mapName);
                    }
                } else if (StringUtils.containsIgnoreCase(sql, "delete")) {
                    String table = sql.split("\\s+")[2].toLowerCase();
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String mapName : m) {
                        cacheService.delCache(mapName);
                    }
                } else if (StringUtils.containsIgnoreCase(sql, "update")) {
                    String table = sql.split("\\s+")[1].toLowerCase();
                    Set<String> m = MyBatisCacheConfig.TABLE_METHOD.get(table);
                    for (String mapName : m) {
                        cacheService.delCache(mapName);
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

    /**
     * Mybatis拦截器方法
     *
     * @param invocation 拦截器入参
     * @return 返回执行结果
     * @throws Throwable 抛出异常
     */
    public Object pageIntercept(Invocation invocation) throws Throwable {
        return pageIntercept(invocation,null,null);
    }

    /**
     * Mybatis拦截器方法
     *
     * @param invocation 拦截器入参
     * @return 返回执行结果
     * @throws Throwable 抛出异常
     */
    public Object pageIntercept(Invocation invocation, CacheKey cacheKey, BoundSql boundSql) throws Throwable {
        try {
            Object[] args = invocation.getArgs();
            MappedStatement ms = (MappedStatement) args[0];
            Object parameter = args[1];
            RowBounds rowBounds = (RowBounds) args[2];
            ResultHandler resultHandler = (ResultHandler) args[3];
            Executor executor = (Executor) invocation.getTarget();
            //由于逻辑关系，只会进入一次
            if (args.length == 4) {
                //4 个参数时
                if (boundSql == null)
                    boundSql = ms.getBoundSql(parameter);
                if (cacheKey == null)
                    cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
            } else {
                //6 个参数时
                if (cacheKey == null)
                    cacheKey = (CacheKey) args[4];
                if (boundSql == null)
                    boundSql = (BoundSql) args[5];
            }
            List resultList;
            //调用方法判断是否需要进行分页，如果不需要，直接返回结果
            if (!dialect.skip(ms, parameter, rowBounds)) {
                //反射获取动态参数
                String msId = ms.getId();
                Configuration configuration = ms.getConfiguration();
                Map<String, Object> additionalParameters = (Map<String, Object>) additionalParametersField.get(boundSql);
                //判断是否需要进行 count 查询
                if (dialect.beforeCount(ms, parameter, rowBounds)) {
                    String countMsId = msId + countSuffix;
                    Long count;
                    //先判断是否存在手写的 count 查询
                    MappedStatement countMs = getExistedMappedStatement(configuration, countMsId);
                    if (countMs != null) {
                        count = executeManualCount(executor, countMs, parameter, boundSql, resultHandler);
                    } else {
                        countMs = msCountMap.get(countMsId);
                        //自动创建
                        if (countMs == null) {
                            //根据当前的 ms 创建一个返回值为 Long 类型的 ms
                            countMs = MSUtils.newCountMappedStatement(ms, countMsId);
                            msCountMap.put(countMsId, countMs);
                        }
                        count = executeAutoCount(executor, countMs, parameter, boundSql, rowBounds, resultHandler);
                    }
                    //处理查询总数
                    //返回 true 时继续分页查询，false 时直接返回
                    if (!dialect.afterCount(count, parameter, rowBounds)) {
                        //当查询总数为 0 时，直接返回空的结果
                        return dialect.afterPage(new ArrayList(), parameter, rowBounds);
                    }
                }
                //判断是否需要进行分页查询
                if (dialect.beforePage(ms, parameter, rowBounds)) {
                    //生成分页的缓存 key
                    CacheKey pageKey = cacheKey;
                    //处理参数对象
                    parameter = dialect.processParameterObject(ms, parameter, boundSql, pageKey);
                    //调用方言获取分页 sql
                    String pageSql = dialect.getPageSql(ms, boundSql, parameter, rowBounds, pageKey);
                    BoundSql pageBoundSql = new BoundSql(configuration, pageSql, boundSql.getParameterMappings(), parameter);
                    //设置动态参数
                    for (String key : additionalParameters.keySet()) {
                        pageBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
                    }
                    //执行分页查询
                    resultList = executor.query(ms, parameter, RowBounds.DEFAULT, resultHandler, pageKey, pageBoundSql);
                } else {
                    //不执行分页的情况下，也不执行内存分页
                    resultList = executor.query(ms, parameter, RowBounds.DEFAULT, resultHandler, cacheKey, boundSql);
                }
            } else {
                //rowBounds用参数值，不使用分页插件处理时，仍然支持默认的内存分页
                resultList = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            }
            return dialect.afterPage(resultList, parameter, rowBounds);
        } finally {
            dialect.afterAll();
        }
    }

    private Map<String, Object> parsePage(Page page) {
        Map<String, Object> data = new HashMap<String, Object>(2);
        data.put("data", page);
        Map<String, Object> pageInfo = new HashMap<String, Object>(4);
        pageInfo.put("pageNo", page.getPageNum());
        pageInfo.put("pageSize", page.getPageSize());
        pageInfo.put("totalCount", page.getTotal());
        pageInfo.put("totalPages", page.getPages());
        data.put("pageInfo", pageInfo);
        return data;
    }

    private Page parseMap(Map<String, Object> map) {
        Page p = new Page();
        List<Object> l = (List) map.get("data");
        for (Object o : l) {
            p.add(o);
        }
        Map<String, Object> pageInfo = (Map<String, Object>) map.get("pageInfo");
        p.setPageNum((int) pageInfo.get("pageNo"));
        p.setPageSize((int) pageInfo.get("pageSize"));
        p.setTotal((long) pageInfo.get("totalCount"));
        p.setPages((int) pageInfo.get("totalPages"));
        return p;
    }


    private Properties properties;
    protected Cache<String, MappedStatement> msCountMap = null;
    private Dialect dialect;
    private String default_dialect_class = "com.github.pagehelper.PageHelper";
    private Field additionalParametersField;
    private String countSuffix = "_COUNT";

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
        this.msCountMap = CacheFactory.createCache(properties.getProperty("msCountCache"), "ms", properties);
        String dialectClass = properties.getProperty("dialect");
        if (StringUtil.isEmpty(dialectClass)) {
            dialectClass = this.default_dialect_class;
        }

        try {
            Class countSuffix = Class.forName(dialectClass);
            this.dialect = (Dialect) countSuffix.newInstance();
        } catch (Exception var6) {
            throw new PageException(var6);
        }

        this.dialect.setProperties(properties);
        String countSuffix1 = properties.getProperty("countSuffix");
        if (StringUtil.isNotEmpty(countSuffix1)) {
            this.countSuffix = countSuffix1;
        }

        try {
            this.additionalParametersField = BoundSql.class.getDeclaredField("additionalParameters");
            this.additionalParametersField.setAccessible(true);
        } catch (NoSuchFieldException var5) {
            throw new PageException(var5);
        }
    }


    /**
     * 尝试获取已经存在的在 MS，提供对手写count和page的支持
     *
     * @param configuration
     * @param msId
     * @return
     */
    private MappedStatement getExistedMappedStatement(Configuration configuration, String msId) {
        MappedStatement mappedStatement = null;
        try {
            mappedStatement = configuration.getMappedStatement(msId, false);
        } catch (Throwable t) {
            //ignore
        }
        return mappedStatement;
    }

    /**
     * 执行手动设置的 count 查询，该查询支持的参数必须和被分页的方法相同
     *
     * @param executor
     * @param countMs
     * @param parameter
     * @param boundSql
     * @param resultHandler
     * @return
     * @throws IllegalAccessException
     * @throws SQLException
     */
    private Long executeManualCount(Executor executor, MappedStatement countMs,
                                    Object parameter, BoundSql boundSql,
                                    ResultHandler resultHandler) throws IllegalAccessException, SQLException {
        CacheKey countKey = executor.createCacheKey(countMs, parameter, RowBounds.DEFAULT, boundSql);
        BoundSql countBoundSql = countMs.getBoundSql(parameter);
        Object countResultList = executor.query(countMs, parameter, RowBounds.DEFAULT, resultHandler, countKey, countBoundSql);
        Long count = ((Number) ((List) countResultList).get(0)).longValue();
        return count;
    }

    /**
     * 执行自动生成的 count 查询
     *
     * @param executor
     * @param countMs
     * @param parameter
     * @param boundSql
     * @param rowBounds
     * @param resultHandler
     * @return
     * @throws IllegalAccessException
     * @throws SQLException
     */
    private Long executeAutoCount(Executor executor, MappedStatement countMs,
                                  Object parameter, BoundSql boundSql,
                                  RowBounds rowBounds, ResultHandler resultHandler) throws IllegalAccessException, SQLException {
        Map<String, Object> additionalParameters = (Map<String, Object>) additionalParametersField.get(boundSql);
        //创建 count 查询的缓存 key
        CacheKey countKey = executor.createCacheKey(countMs, parameter, RowBounds.DEFAULT, boundSql);
        //调用方言获取 count sql
        String countSql = dialect.getCountSql(countMs, boundSql, parameter, rowBounds, countKey);
        //countKey.update(countSql);
        BoundSql countBoundSql = new BoundSql(countMs.getConfiguration(), countSql, boundSql.getParameterMappings(), parameter);
        //当使用动态 SQL 时，可能会产生临时的参数，这些参数需要手动设置到新的 BoundSql 中
        for (String key : additionalParameters.keySet()) {
            countBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
        }
        //执行 count 查询
        Object countResultList = executor.query(countMs, parameter, RowBounds.DEFAULT, resultHandler, countKey, countBoundSql);
        Long count = (Long) ((List) countResultList).get(0);
        return count;
    }

}