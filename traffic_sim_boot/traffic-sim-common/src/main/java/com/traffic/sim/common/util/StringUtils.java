package com.traffic.sim.common.util;

/**
 * 字符串工具类
 * 封装Apache Commons Lang3的StringUtils功能
 * 
 * @author traffic-sim
 */
public class StringUtils {
    
    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isEmpty(cs);
    }
    
    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isNotEmpty(cs);
    }
    
    /**
     * 判断字符串是否为空白
     */
    public static boolean isBlank(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isBlank(cs);
    }
    
    /**
     * 判断字符串是否不为空白
     */
    public static boolean isNotBlank(CharSequence cs) {
        return org.apache.commons.lang3.StringUtils.isNotBlank(cs);
    }
    
    /**
     * 去除字符串两端空白
     */
    public static String trim(String str) {
        return org.apache.commons.lang3.StringUtils.trim(str);
    }
    
    /**
     * 去除字符串两端空白，如果为null则返回null
     */
    public static String trimToNull(String str) {
        return org.apache.commons.lang3.StringUtils.trimToNull(str);
    }
    
    /**
     * 去除字符串两端空白，如果为null则返回空字符串
     */
    public static String trimToEmpty(String str) {
        return org.apache.commons.lang3.StringUtils.trimToEmpty(str);
    }
}

