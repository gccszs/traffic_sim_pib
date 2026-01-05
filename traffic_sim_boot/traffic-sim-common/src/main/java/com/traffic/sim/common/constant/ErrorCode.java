package com.traffic.sim.common.constant;

/**
 * 错误码常量定义
 * 
 * @author traffic-sim
 */
public class ErrorCode {
    
    /** 成功 */
    public static final String ERR_OK = "ERR_OK";
    
    /** 认证失败 */
    public static final String ERR_AUTH = "ERR_AUTH";
    
    /** 参数错误 */
    public static final String ERR_ARG = "ERR_ARG";
    
    /** 资源已存在 */
    public static final String ERR_EXIST = "ERR_EXIST";
    
    /** 资源不存在 */
    public static final String ERR_NOT_FOUND = "ERR_NOT_FOUND";
    
    /** 创建失败 */
    public static final String ERR_CREATE = "ERR_CREATE";
    
    /** 更新失败 */
    public static final String ERR_UPDATE = "ERR_UPDATE";
    
    /** 删除失败 */
    public static final String ERR_DELETE = "ERR_DELETE";
    
    /** 引擎未初始化 */
    public static final String ERR_ENGINE = "ERR_ENGINE";
    
    /** 未知错误 */
    public static final String ERR_UNKNOWN = "ERR_UNKNOWN";
    
    /** 服务器内部错误 */
    public static final String ERR_INTERNAL = "ERR_INTERNAL";
    
    /** 权限不足 */
    public static final String ERR_PERMISSION = "ERR_PERMISSION";
    
    /** 操作不允许 */
    public static final String ERR_FORBIDDEN = "ERR_FORBIDDEN";
    
    private ErrorCode() {
        // 工具类，禁止实例化
    }
}

