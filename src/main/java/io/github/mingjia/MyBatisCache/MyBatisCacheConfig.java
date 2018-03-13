package io.github.mingjia.MyBatisCache;


import java.util.*;

/**
 * 缓存初始化配置
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public class MyBatisCacheConfig {

    /**
     * 不需要缓存的方法，直接查询数据库
     */
    public static List<String> DIS_CACHE_METHOD = new ArrayList<>();
    /**
     * 表和方法映射关系
     */
    public static Map<String, Set<String>> TABLE_METHOD = new HashMap<>();
    /**
     * 方法key与方法名的对一个关系
     */
    public static Map<String, String> METHOD_DESC = new HashMap<>();


}
