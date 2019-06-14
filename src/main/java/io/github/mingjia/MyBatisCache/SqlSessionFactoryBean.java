package io.github.mingjia.MyBatisCache;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
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
 * 增加了MyBatisCacheCongfig设置
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String pageHelperCountSuffix = "_COUNT";

    public void setPageHelperCountSuffix(String pageHelperCountSuffix) {
        this.pageHelperCountSuffix = pageHelperCountSuffix;
    }

    @Override
    public SqlSessionFactory getObject() throws Exception {

        SqlSessionFactory sqlSessionFactory = super.getObject();
        Configuration conf = sqlSessionFactory.getConfiguration();
        Collection l = conf.getMappedStatements();

        for (Object m : l) {
            if (m instanceof MappedStatement) {
                MappedStatement ms = (MappedStatement) m;

                if (SqlCommandType.SELECT.equals(ms.getSqlCommandType())) {
                    logger.debug("sql id:" + ms.getId());
                    Set<String> tables = new HashSet<>();

                    String mapperClassName = StringUtils.substring(ms.getId(), 0, StringUtils.lastIndexOf(ms.getId(), '.'));
                    String methodName = StringUtils.substring(ms.getId(), StringUtils.lastIndexOf(ms.getId(), '.') + 1);

                    Map<String, Object> selectAnnoation = getSelectCacheAnnoation(mapperClassName, methodName);
                    String[] tableNames = null;
                    String primaries = null;
                    if (selectAnnoation != null) {
                        if ((Boolean) selectAnnoation.get("disCache")) {
                            logger.debug("[disCache!]");
                            continue;
                        }
                        tableNames = (String[]) selectAnnoation.get("tables");
                        primaries = (String) selectAnnoation.get("primaries");
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

                    if (tables.size() == 1 && primaries!=null) {
                        //存储数据库表和mapper中的方法对应关系,数据库表中的数据发生过更改,可以知道要清除哪个方法产生的缓存
                        methods(tables, ms.getId(), primaries);
                    } else {
                        //存储数据库表和mapper中的方法对应关系,数据库表中的数据发生过更改,可以知道要清除哪个方法产生的缓存
                        methods(tables, ms.getId());
                    }


                }

            }
        }

        return sqlSessionFactory;
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
        return MyBatisCacheConfig.CLASS_TYPE.get(clazz.getName()) == null ? clazz : MyBatisCacheConfig.CLASS_TYPE.get(clazz.getName());
    }

    /*private void getDisCacheMethod(Set<String> allClassName) throws Exception {
        for (String className : allClassName) {
            Method[] methods = Class.forName(className).getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(CacheQuery.class)) {
                    Annotation p = method.getAnnotation(CacheQuery.class);
                    Method m = p.getClass().getDeclaredMethod("disCache", null);
                    boolean value = (boolean) m.invoke(p, null);
                    if (value) {
                        String mt = className + "." + method.getName();
                        if (!MyBatisCacheConfig.DIS_CACHE_METHOD.contains(mt)) {
                            MyBatisCacheConfig.DIS_CACHE_METHOD.add(mt);
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
            if (method.getName().equals(methodName) && method.isAnnotationPresent(CacheQuery.class)) {
                Map<String, Object> map = new HashMap<>();
                Annotation p = method.getAnnotation(CacheQuery.class);
                Method m = p.getClass().getDeclaredMethod("disCache", null);
                boolean value = (boolean) m.invoke(p, null);
                if (value) {
                    String mt = mapperClassName + "." + method.getName();
                    if (!MyBatisCacheConfig.DIS_CACHE_METHOD.contains(mt)) {
                        MyBatisCacheConfig.DIS_CACHE_METHOD.add(mt);
                    }
                }
                map.put("disCache", value);
                m = p.getClass().getDeclaredMethod("tables", null);
                Object tables = m.invoke(p, null);
                map.put("tables", tables);

                m = p.getClass().getDeclaredMethod("primaries", null);
                Object primaries = m.invoke(p, null);
                map.put("primaries", primaries);
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

    private void methods(Set<String> tables, String method) {
        methods(tables, method, null);
    }

    private void methods(Set<String> tables, String method, String primaries) {

        String methodCode = DigestUtils.md5Hex(method);
        MyBatisCacheConfig.METHOD_TABLES.put(methodCode,tables);
        for (String table : tables) {
            String methodCountCode = DigestUtils.md5Hex(method + pageHelperCountSuffix);
            if (MyBatisCacheConfig.TABLE_METHODS.get(table) == null) {
                Set<String> s = new HashSet<String>(2);
                MyBatisCacheConfig.TABLE_METHODS.put(table, s);
            }
            Set<String> s = MyBatisCacheConfig.TABLE_METHODS.get(table);
            s.add(methodCode);
            s.add(methodCountCode);

            MyBatisCacheConfig.METHOD_PRIMARYS.put(methodCode, primaries);
            MyBatisCacheConfig.METHOD_PRIMARYS.put(methodCountCode, primaries);

            if (MyBatisCacheConfig.METHOD_DESC.get(methodCode) == null)
                MyBatisCacheConfig.METHOD_DESC.put(methodCode, method);
            if (MyBatisCacheConfig.METHOD_DESC.get(methodCountCode) == null)
                MyBatisCacheConfig.METHOD_DESC.put(methodCountCode, method + pageHelperCountSuffix);


        }
    }
}
