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
    public static Map<String, Set<String>> TABLE_METHODS = new HashMap<>();
    /**
     * 表和方法映射关系
     */
    public static Map<String, Set<String>> METHOD_TABLES = new HashMap<>();
    /**
     * 方法key与方法名的对应关系
     */
    public static Map<String, String> METHOD_DESC = new HashMap<>();

    /**
     * 方法与主键对应关系
     */
    public static Map<String, String> METHOD_PRIMARYS = new HashMap<>();
    
    public static Map<String,Class> CLASS_TYPE = new HashMap<>();
    static{
        CLASS_TYPE.put("byte", Byte.class);
        CLASS_TYPE.put("long", Long.class);
        CLASS_TYPE.put("short", Short.class);
        CLASS_TYPE.put("int", Integer.class);
        CLASS_TYPE.put("double", Double.class);
        CLASS_TYPE.put("float", Float.class);
        CLASS_TYPE.put("boolean", Boolean.class);
        CLASS_TYPE.put("java.util.List", ArrayList.class);
        CLASS_TYPE.put("java.util.Set", HashSet.class);

        CLASS_TYPE.put("byte[]", Byte[].class);
        CLASS_TYPE.put("long[]", Long[].class);
        CLASS_TYPE.put("short[]", Short[].class);
        CLASS_TYPE.put("int[]", Integer[].class);
        CLASS_TYPE.put("integer[]", Integer[].class);
        CLASS_TYPE.put("double[]", Double[].class);
        CLASS_TYPE.put("float[]", Float[].class);
        CLASS_TYPE.put("boolean[]", Boolean[].class);

        CLASS_TYPE.put("java.util.List[]", ArrayList[].class);
        CLASS_TYPE.put("java.util.Set[]", HashSet[].class);

    }


}
