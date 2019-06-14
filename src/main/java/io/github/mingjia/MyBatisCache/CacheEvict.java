package io.github.mingjia.MyBatisCache;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存注解
 *
 * @auther GuiBin
 * @create 18/2/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {
    /**
     * 是否需要清除，默认false，不想使用清除缓存请指定为true，设定为true之后该方法所有操作将不再参与到MybatisCache的功能逻辑当中
     * @return
     */
    public boolean disEvict() default false;

    /**
     * 指定该方法清除的表集合，默认为空，update类型方法会根据实时sql解析出变更的表明，是可以根据表名清除缓存的，提供该属性目的是不依赖sql解析，直接根据配置的表名清除，如有此需求的话，可以通过该属性设置
     * @return
     */
    public String[] tables() default {};

    /**
     * 指定缓存的清除条件，默认为空，直接清除该方法的所有缓存数据（所有的key）
     * 完善清除策略，达到根据key级别的清除策略
     * //todo：目前计划是指定表的主字段名称，然后缓存的时候跟保存主字段的值，然后再Update类型方法的时候获得变更表的逐字段值（需要执行一个select操作），根据查询的值去清除该方法的缓存数据
     * //todo: 如果update方法无法获得主字段的值，那么就根据method清除，首先要保证数据的一致性
     * 使用该属性的场景：
     * 根据逐字段查询
     * @return
     */
    //public String[] evictConditions() default {};
    public String[] tableEvictFields() default {};
}
