package com.mszlu.blog.common.cache;


import java.lang.annotation.*;

@Target({ElementType.METHOD})//描述注解的适用范围
@Retention(RetentionPolicy.RUNTIME)//RetentionPoicy.RUNTIME:在运行时有效（即运行时保留）
@Documented
public @interface Cache {

    long expire() default 1 * 60 * 1000;//1分钟
    // 缓存标识 key
    String name() default "";

}
