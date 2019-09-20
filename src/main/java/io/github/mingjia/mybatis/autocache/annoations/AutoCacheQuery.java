package io.github.mingjia.mybatis.autocache.annoations;


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
public @interface AutoCacheQuery {
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
     * 为cache增加分片，可以是固定的字符串，也可以通过表达式指定（动态的）
     * 在@AutoCacheEvict中有相对应的evictShards（移除标签），用来指定清除相应shard的cache，evictShards可以指定清除多个shard
     * 如果在update方法中没有指定evictShards，则清除相应select方法的全部数据
     * @return
     */
    public String shard() default "";
}
