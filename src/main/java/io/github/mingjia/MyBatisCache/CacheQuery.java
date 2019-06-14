package io.github.mingjia.MyBatisCache;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Query类缓存注解，针对的是查询方法，对于Update类型的方法无效
 *
 * @auther GuiBin
 * @create 18/2/27
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheQuery {
    /**
     * 是否需要缓存，默认false，不想使用缓存请指定为true，设定为true之后该方法所有操作将不再参与到MybatisCache的功能逻辑当中
     * @return
     */
    public boolean disCache() default false;

    /**
     * 指定该方法涉及的表集合，默认为空，自动解析sql中的表，但对于sql的动态性，不能保证sql解析出的表完全正确，所以，特殊情况下需要手动指定，保证正确性；
     * //todo:如果可以完善sql解析的话，就不需要该属性了
     * @return
     */
    public String[] tables() default {};

    /**
     * 指定缓存的清除条件，默认为空，直接清除该方法的所有缓存数据（所有的key）
     * 完善清除策略，达到根据key级别的清除策略
     * //todo：目前计划是指定表的主字段名称，然后缓存的时候跟保存主字段的值，然后再Update类型方法的时候获得变更表的逐字段值（需要执行一个select操作），根据查询的值去清除该方法的缓存数据
     * //todo: 目前只针对单表操作，先判断sql是否是单表查询，如果是则继续逻辑，不是则终止该逻辑，
     * //todo: 如果update方法无法获得主字段的值，那么就根据method清除，首先要保证数据的一致性
     * 使用该属性的场景：
     * 根据逐字段查询
     * @return
     */
    //public String[] evictConditions() default {};
    public String primaries() default "";
}
