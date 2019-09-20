package io.github.mingjia.mybatis.autocache.annoations;


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
public @interface AutoCacheEvict {
    /**
     * 是否需要清除，默认false，不想使用清除缓存请指定为true，设定为true之后该方法所有操作将不再参与到MybatisCache的功能逻辑当中
     * 使用场景：如频繁更新某表的update_time字段，并且不存在缓存方法中查询该表的update_time字段
     * @return
     */
    public boolean disEvict() default false;

    /**
     * 指定该方法清除的表集合，默认为空，update类型方法会根据实时sql解析出变更的表明，是可以根据表名清除缓存的，提供该属性目的是不依赖sql解析，直接根据配置的表名清除，如有此需求的话，可以通过该属性设置
     * @return
     */
    public String[] tables() default {};

    /**
     * 指定清除的shards
     * @return
     */
    public String[] evictShards() default {};
}
