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
public @interface SelectCache {
    public boolean disCache() default false;

    public String[] tables() default {};
}
