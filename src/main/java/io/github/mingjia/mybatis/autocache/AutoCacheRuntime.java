package io.github.mingjia.mybatis.autocache;


import java.util.*;

/**
 * 缓存运行时配置
 *
 * @auther GuiBin
 * @create 18/2/27
 */
public class AutoCacheRuntime {


    /**
     * 不需要触发移除缓存操作的方法（变更类型方法），直接查询数据库
     */
    public static List<String> DIS_EVICT_METHOD = new ArrayList<>();
    /**
     * 表和方法映射关系（查询类方法）
     */
    public static Map<String, String[]> METHOD_EVICT_TABLES = new HashMap<>();
    /**
     * 方法与tags对应关系（查询类的方法）
     */
    public static Map<String, String[]> METHOD_EVICT_SHARDS = new HashMap<>();


    /**
     * 不需要缓存的方法（查询类型方法），直接查询数据库
     */
    public static List<String> DIS_CACHE_METHOD = new ArrayList<>();
    /**
     * 表和方法映射关系（查询类方法；变更方法执行时，根据变更的表名查询需要清除哪些方法的cache）
     */
    public static Map<String, Set<String>> TABLE_METHODS = new HashMap<>();
    /**
     * 表和方法映射关系（查询类方法）
     */
    public static Map<String, Set<String>> METHOD_SELECT_TABLES = new HashMap<>();
    /**
     * 方法key与方法名的对应关系
     */
    public static Map<String, String> METHOD_DESC = new HashMap<>();

    /**
     * 方法与shard对应关系（查询类的方法）
     */
    public static Map<String, String> METHOD_SHARD = new HashMap<>();


    
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
