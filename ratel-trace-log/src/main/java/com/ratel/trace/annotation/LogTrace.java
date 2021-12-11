package com.ratel.trace.annotation;

import com.ratel.trace.enums.LogLevelEnum;

import java.lang.annotation.*;

/**
 * @author zhangxn
 * @date 2021/12/5  21:01
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface LogTrace {
    /**
     * 日志级别(通过日志级别控制是否打印日志)，会同时作用于方法入参和返回值
     */
    LogLevelEnum logLevel() default LogLevelEnum.INFO;

    /**
     * 是否记录方法参数 true 记录
     */
    boolean logParameter() default true;

    /**
     * 是否记录方法返回值 true 记录
     */
    boolean logReturnValue() default true;


    /**
     * 设置索引名称，以便每个方法都能进行唯一跟踪
     */
    String indexName() default "";

}
