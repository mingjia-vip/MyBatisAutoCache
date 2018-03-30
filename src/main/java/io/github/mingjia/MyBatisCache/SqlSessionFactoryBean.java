package io.github.mingjia.MyBatisCache;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * extends from {@link org.mybatis.spring.SqlSessionFactoryBean}
 * 增加了MyBatisCacheCongfig设置
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public class SqlSessionFactoryBean extends org.mybatis.spring.SqlSessionFactoryBean {

    private String pageHelperCountSuffix = "_COUNT";

    public void setPageHelperCountSuffix(String pageHelperCountSuffix) {
        this.pageHelperCountSuffix = pageHelperCountSuffix;
    }

    @Override
    public SqlSessionFactory getObject() throws Exception {

        SqlSessionFactory sqlSessionFactory = super.getObject();
        Collection l = sqlSessionFactory.getConfiguration().getMappedStatements();

        Set<String> allClassName = new HashSet<String>();
        for (Object m : l) {
            if (m instanceof MappedStatement) {
                MappedStatement ms = (MappedStatement) m;
                //System.out.println("=============="+ms.getId());
                String sql = ms.getBoundSql(null).getSql();
                if (StringUtils.containsIgnoreCase(sql, "select")) {
                    Statement statement = CCJSqlParserUtil.parse(sql);
                    Select selectStatement = (Select) statement;
                    TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
                    List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
                    Set<String> tables = tables(tableList);
                    //存储数据库表和mapper中的方法对应关系,数据库表中的数据发生过更改,可以知道要清除哪个方法产生的缓存
                    methods(tables, ms.getId());
                } else if (StringUtils.containsIgnoreCase(sql, "insert")) {
                    // System.out.println(sql.split("\\s+")[2]);
                } else if (StringUtils.containsIgnoreCase(sql, "delete")) {
                    // System.out.println(sql.split("\\s+")[2]);
                } else if (StringUtils.containsIgnoreCase(sql, "update")) {
                    // System.out.println(sql.split("\\s+")[1]);
                }
                //记录所有的Mapper类
                allClassName.add(StringUtils.substring(ms.getId(), 0, StringUtils.lastIndexOf(ms.getId(), '.')));
            }
        }
        //mapper中含有@MyBatisCache(disCache = true)的方法,直接查数据库
        getDisCacheMethod(allClassName);


        return sqlSessionFactory;
    }

    private void getDisCacheMethod(Set<String> allClassName) throws Exception {
        for (String className : allClassName) {
            Method[] methods = Class.forName(className).getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(MyBatisCache.class)) {
                    Annotation p = method.getAnnotation(MyBatisCache.class);
                    Method m = p.getClass().getDeclaredMethod("disCache", null);
                    boolean value = (boolean) m.invoke(p, null);
                    if (value) {
                        String mt = className + "." + method.getName();
                        if (!MyBatisCacheConfig.DIS_CACHE_METHOD.contains(mt)) {
                            MyBatisCacheConfig.DIS_CACHE_METHOD.add(mt);
                        }
                    }
                }
            }
        }
    }


    private Set<String> tables(List<String> t) {
        Set<String> s = new HashSet<String>();
        for (String tn : t) {
            s.add(tn.replaceAll("`", "").toLowerCase());
        }
        return s;
    }

    private void methods(Set<String> tables, String method) {
        for (String table : tables) {
            String methodCode = DigestUtils.md5Hex(method);
            String methodCountCode = DigestUtils.md5Hex(method + pageHelperCountSuffix);
            if (MyBatisCacheConfig.TABLE_METHOD.get(table) == null) {
                Set<String> s = new HashSet<String>(2);
                MyBatisCacheConfig.TABLE_METHOD.put(table, s);
            }
            Set<String> s = MyBatisCacheConfig.TABLE_METHOD.get(table);
            s.add(methodCode);
            s.add(methodCountCode);

            if (MyBatisCacheConfig.METHOD_DESC.get(methodCode) == null)
                MyBatisCacheConfig.METHOD_DESC.put(methodCode, method);
            if (MyBatisCacheConfig.METHOD_DESC.get(methodCountCode) == null)
                MyBatisCacheConfig.METHOD_DESC.put(methodCountCode, method + pageHelperCountSuffix);
        }
    }
}
