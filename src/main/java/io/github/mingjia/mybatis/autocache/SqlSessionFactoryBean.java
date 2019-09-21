package io.github.mingjia.mybatis.autocache;

import io.github.mingjia.mybatis.autocache.annoations.AutoCacheEvict;
import io.github.mingjia.mybatis.autocache.annoations.AutoCacheQuery;
import io.github.mingjia.mybatis.autocache.service.redisson.RedissonCacheService;
import io.github.mingjia.mybatis.autocache.service.redisson.RedissonConfig;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * extends from {@link org.mybatis.spring.SqlSessionFactoryBean}
 * 增加了自动缓存加载及拦截器
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private boolean isCache = true;

    public void setIsCache(String isCache) {
        this.isCache = Boolean.valueOf(isCache);
    }

    private String configProperties;

    public void setConfigProperties(String configProperties) {
        this.configProperties = configProperties;
    }

    private AutoCacheServiceI cacheService = null;

    public void setCacheService(AutoCacheServiceI cacheService) {
        this.cacheService = cacheService;
        //AutoCacheCleanHolder.cacheService = cacheService;
    }


    private boolean havePageHelper = false;
    private String pageHelperCountSuffix = "_COUNT";

    public void setPageHelperCountSuffix(String pageHelperCountSuffix) {
        this.pageHelperCountSuffix = pageHelperCountSuffix;
    }


    @Override
    public SqlSessionFactory getObject() throws Exception {

        SqlSessionFactory sqlSessionFactory = super.getObject();
        if (isCache) {
            if (cacheService == null) {
                //没有指定cacheService时，加载配置文件，创建默认cacheService
                if (RedissonConfig.loadParams(configProperties))
                    cacheService = new RedissonCacheService();
            }
            if (cacheService != null) {
                //service初始化操作
                cacheService.init();

                Configuration conf = sqlSessionFactory.getConfiguration();

                //判断有没有分页插件
                for (Interceptor interceptor : conf.getInterceptors()) {
                    if (interceptor.getClass().getName().startsWith("com.github.pagehelper")) {
                        havePageHelper = true;
                        break;
                    }
                }
                //添加缓存拦截器
                AutoCacheInterceptor interceptor = new AutoCacheInterceptor();
                interceptor.setCacheService(cacheService);
                conf.addInterceptor(interceptor);

                Collection l = conf.getMappedStatements();
                for (Object m : l) {
                    if (m instanceof MappedStatement) {
                        MappedStatement ms = (MappedStatement) m;

                        String mapperClassName = StringUtils.substring(ms.getId(), 0, StringUtils.lastIndexOf(ms.getId(), '.'));
                        String methodName = StringUtils.substring(ms.getId(), StringUtils.lastIndexOf(ms.getId(), '.') + 1);

                        if (SqlCommandType.SELECT.equals(ms.getSqlCommandType())) {
                            logger.debug("sql id:" + ms.getId());
                            Set<String> tables = new HashSet<>();

                            Map<String, Object> selectAnnoation = getSelectCacheAnnoation(mapperClassName, methodName);
                            String[] tableNames = null;
                            String shard = null;
                            if (selectAnnoation != null) {
                                if ((Boolean) selectAnnoation.get("disCache")) {
                                    logger.debug("[disCache!]");
                                    continue;
                                }
                                tableNames = (String[]) selectAnnoation.get("tables");
                                shard = (String) selectAnnoation.get("shard");
                            }

                            if (tableNames != null && tableNames.length > 0) {
                                for (String tableName : tableNames)
                                    tables.add(tableName);
                            } else {
                                String sql = ms.getBoundSql(null).getSql();
                                //logger.debug("sql:"+sql);
                                Statement statement = CCJSqlParserUtil.parse(sql);
                                Select selectStatement = (Select) statement;
                                TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
                                List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
                                tables.addAll(tables(tableList));
                            }
                            logger.debug("tables:" + tables);

                            //存储数据库表和mapper中的方法对应关系,数据库表中的数据发生过更改,可以知道要清除哪个方法产生的缓存
                            selectMethods(tables, ms.getId(), shard);

                        } else {
                            Map<String, Object> evictAnnoation = getEvictCacheAnnoation(mapperClassName, methodName);
                            if (evictAnnoation != null) {
                                /*if ((Boolean) evictAnnoation.get("disEvict")) {
                                    logger.debug("[disEvict!]");
                                    continue;
                                }*/
                                String[] tableNames = (String[]) evictAnnoation.get("tables");
                                String[] evictShards = (String[]) evictAnnoation.get("evictShards");

                                evictMethods(tableNames, ms.getId(), evictShards);
                            }
                        }

                    }
                }

                if (logger.isDebugEnabled()) {
                    showConfig();
                }
            }
        }

        return sqlSessionFactory;
    }

    public void showConfig() {
        for (String table : AutoCacheRuntime.DIS_CACHE_METHOD) {
            logger.debug(table);
        }
        Iterator<String> it = AutoCacheRuntime.TABLE_METHODS.keySet().iterator();
        while (it.hasNext()) {
            String table = it.next();
            Set<String> methodCodes = AutoCacheRuntime.TABLE_METHODS.get(table);
            for (String methodCode : methodCodes) {
                logger.debug(table + ":" + methodCode + ":" + AutoCacheRuntime.METHOD_DESC.get(methodCode));
            }
        }
    }


    private String[] getParameterNames(String className, String methodName, Configuration configuration) throws Exception {
        Method[] methods = Class.forName(className).getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName))
                continue;
            ParamNameResolver resolver = new ParamNameResolver(configuration, method);
            String[] names = resolver.getNames();
            return names;
        }
        return null;
    }


    private Map<String, Class> getParameterMap(String className, String methodName) throws Exception {
        Method[] methods = Class.forName(className).getDeclaredMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName))
                continue;
            Map<String, Class> map = new HashMap<>();
            Parameter[] parameters = method.getParameters();
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(Param.class)) {
                    Annotation p = parameter.getAnnotation(Param.class);
                    Method m = p.getClass().getDeclaredMethod("value", null);
                    String value = (String) m.invoke(p, null);
                    if (StringUtils.isNotEmpty(value)) {
                        map.put(value, dealParamClass(parameter.getType()));
                    }
                } else {
                    map.put(parameter.getName().replace("arg", "param"), dealParamClass(parameter.getType()));
                }
            }
            return map;
        }
        return null;
    }

    private Class dealParamClass(Class clazz) {
        return AutoCacheRuntime.CLASS_TYPE.get(clazz.getName()) == null ? clazz : AutoCacheRuntime.CLASS_TYPE.get(clazz.getName());
    }

    /*private void getDisCacheMethod(Set<String> allClassName) throws Exception {
        for (String className : allClassName) {
            Method[] methods = Class.forName(className).getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(AutoCacheQuery.class)) {
                    Annotation p = method.getAnnotation(AutoCacheQuery.class);
                    Method m = p.getClass().getDeclaredMethod("disCache", null);
                    boolean value = (boolean) m.invoke(p, null);
                    if (value) {
                        String mt = className + "." + method.getName();
                        if (!AutoCacheRuntime.DIS_CACHE_METHOD.contains(mt)) {
                            AutoCacheRuntime.DIS_CACHE_METHOD.add(mt);
                        }
                    }
                    m = p.getClass().getDeclaredMethod("tables", null);
                }
            }
        }
    }*/

    private Map<String, Object> getSelectCacheAnnoation(String mapperClassName, String methodName) throws Exception {

        Method[] methods = Class.forName(mapperClassName).getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(AutoCacheQuery.class)) {
                Map<String, Object> map = new HashMap<>();
                Annotation p = method.getAnnotation(AutoCacheQuery.class);

                Method m = p.getClass().getDeclaredMethod("disCache", null);
                boolean value = (boolean) m.invoke(p, null);
                if (value) {
                    String mt = mapperClassName + "." + method.getName();
                    if (!AutoCacheRuntime.DIS_CACHE_METHOD.contains(mt)) {
                        AutoCacheRuntime.DIS_CACHE_METHOD.add(mt);
                    }
                }
                map.put("disCache", value);

                m = p.getClass().getDeclaredMethod("tables", null);
                Object tables = m.invoke(p, null);
                map.put("tables", tables);

                m = p.getClass().getDeclaredMethod("shard", null);
                Object shard = m.invoke(p, null);
                map.put("shard", shard);
                return map;
            }
        }
        return null;
    }

    private Map<String, Object> getEvictCacheAnnoation(String mapperClassName, String methodName) throws Exception {

        Method[] methods = Class.forName(mapperClassName).getDeclaredMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(AutoCacheEvict.class)) {
                Map<String, Object> map = new HashMap<>();
                Annotation p = method.getAnnotation(AutoCacheEvict.class);

                Method m = p.getClass().getDeclaredMethod("disEvict", null);
                boolean value = (boolean) m.invoke(p, null);
                if (value) {
                    String mt = mapperClassName + "." + method.getName();
                    if (!AutoCacheRuntime.DIS_EVICT_METHOD.contains(mt)) {
                        AutoCacheRuntime.DIS_EVICT_METHOD.add(mt);
                    }
                }
                map.put("disEvict", value);

                m = p.getClass().getDeclaredMethod("tables", null);
                Object tables = m.invoke(p, null);
                map.put("tables", tables);

                m = p.getClass().getDeclaredMethod("evictShards", null);
                Object evictShards = m.invoke(p, null);
                map.put("evictShards", evictShards);
                return map;
            }
        }
        return null;
    }


    private Set<String> tables(List<String> t) {
        Set<String> s = new HashSet<String>();
        for (String tn : t) {
            s.add(tn.replaceAll("`", "").toLowerCase());
        }
        return s;
    }

    private void selectMethods(Set<String> tables, String method, String shard) {

        String methodCode = DigestUtils.md5Hex(method);
        AutoCacheRuntime.METHOD_SELECT_TABLES.put(methodCode, tables);
        for (String table : tables) {
            //考虑到分页插件自动查询总量的sql
            String methodCountCode = havePageHelper ? DigestUtils.md5Hex(method + pageHelperCountSuffix) : null;
            if (AutoCacheRuntime.TABLE_METHODS.get(table) == null) {
                Set<String> s = new HashSet<String>(methodCountCode == null ? 1 : 2);
                AutoCacheRuntime.TABLE_METHODS.put(table, s);
            }
            Set<String> s = AutoCacheRuntime.TABLE_METHODS.get(table);
            s.add(methodCode);
            if (methodCountCode != null)
                s.add(methodCountCode);

            if (shard != null) {
                //method-shard
                AutoCacheRuntime.METHOD_SHARD.put(methodCode, shard);
                if(methodCountCode!=null)
                AutoCacheRuntime.METHOD_SHARD.put(methodCountCode, shard);
            }

            if (AutoCacheRuntime.METHOD_DESC.get(methodCode) == null)
                AutoCacheRuntime.METHOD_DESC.put(methodCode, method);
            if (methodCountCode!=null && AutoCacheRuntime.METHOD_DESC.get(methodCountCode) == null)
                AutoCacheRuntime.METHOD_DESC.put(methodCountCode, method + pageHelperCountSuffix);

        }
    }

    private void evictMethods(String[] tables, String method, String[] evictShards) {

        String methodCode = DigestUtils.md5Hex(method);
        if (tables != null && tables.length > 0)
            AutoCacheRuntime.METHOD_EVICT_TABLES.put(methodCode, tables);

        if (evictShards != null) {
            //method evict shards
            AutoCacheRuntime.METHOD_EVICT_SHARDS.put(methodCode, evictShards);
        }

        if (AutoCacheRuntime.METHOD_DESC.get(methodCode) == null)
            AutoCacheRuntime.METHOD_DESC.put(methodCode, method);

    }
}
