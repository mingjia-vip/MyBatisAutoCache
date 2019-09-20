package io.github.mingjia.mybatis.autocache.util;

import io.github.mingjia.mybatis.autocache.exceptions.AutoCacheException;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjia on 19/5/10.
 */
public class ExecutorUtil {


    private static Field additionalParametersField;

    static {
        try {
            additionalParametersField = BoundSql.class.getDeclaredField("additionalParameters");
            additionalParametersField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new AutoCacheException("获取 BoundSql 属性 additionalParameters 失败: " + e, e);
        }
    }

    public static List<Map<String, Object>> primaryKeyValues(Executor executor, MappedStatement selectPrimaryMs,
                                                             BoundSql boundSql, ResultHandler resultHandler, String targetTable, String[] primaryFields) throws SQLException {
        Map<String, Object> additionalParameters = getAdditionalParameter(boundSql);
        //创建 count 查询的缓存 key
        CacheKey countKey = executor.createCacheKey(selectPrimaryMs, boundSql.getParameterObject(), RowBounds.DEFAULT, boundSql);
        //调用方言获取 count sql
        Map<String, Object> map = PrimaryFieldParser.getSelectPrimarySql(boundSql.getSql(), targetTable, primaryFields);
        if (map == null || map.get("sql") == null)
            return null;
        String selectPrimarySql = map.get("sql").toString();
        List<ParameterMapping> pms = new ArrayList<>(boundSql.getParameterMappings().size());
        int c = (Integer) map.get("removeParamCount");
        Iterator<ParameterMapping> it = boundSql.getParameterMappings().iterator();
        while (it.hasNext()) {
            if (c-- < 1)
                pms.add(it.next());
            else
                it.next();
        }
        BoundSql selectPrimaryBoundSql = new BoundSql(selectPrimaryMs.getConfiguration(), selectPrimarySql, pms, boundSql.getParameterObject());
        //当使用动态 SQL 时，可能会产生临时的参数，这些参数需要手动设置到新的 BoundSql 中
        for (String key : additionalParameters.keySet()) {
            selectPrimaryBoundSql.setAdditionalParameter(key, additionalParameters.get(key));
        }
        //执行查询
        Object result = executor.query(selectPrimaryMs, boundSql.getParameterObject(), RowBounds.DEFAULT, resultHandler, countKey, selectPrimaryBoundSql);
        return (List<Map<String, Object>>) result;
    }

    /**
     * 获取 BoundSql 属性值 additionalParameters
     *
     * @param boundSql
     * @return
     */
    public static Map<String, Object> getAdditionalParameter(BoundSql boundSql) {
        try {
            return (Map<String, Object>) additionalParametersField.get(boundSql);
        } catch (IllegalAccessException e) {
            throw new AutoCacheException("获取 BoundSql 属性值 additionalParameters 失败: " + e, e);
        }
    }
}
