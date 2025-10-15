package edu.uestc.iscssl.itsbackend.controller.agent.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * @Author: wangwei
 * @Description:
 * @Date: 22:20 2019/6/17
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiJsonProperty {

    String key();  //key

    String example() default "";

    String type() default "string";  //支持string 和 int

    String description() default "";

}