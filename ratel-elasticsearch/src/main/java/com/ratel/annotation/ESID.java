package com.ratel.annotation;

import java.lang.annotation.*;

/***
 * @description
 * @author zhangxn
 * @date 2021/12/9 0:32
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Documented
public @interface ESID {
}
