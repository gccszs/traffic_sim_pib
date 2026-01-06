package com.traffic.sim.common.response;

import com.traffic.sim.common.constant.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一API响应格式
 * 
 * @param <T> 数据类型
 * @author traffic-sim
 */
@Data
public class ApiResponse<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 响应码 */
    private String res;
    
    /** 响应消息 */
    private String msg;
    
    /** 响应数据 */
    private T data;
    
    /** 时间戳 */
    private Long timestamp;
    
    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ApiResponse(String res, String msg, T data) {
        this.res = res;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.ERR_OK, "ok", data);
    }
    
    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ErrorCode.ERR_OK, "ok", null);
    }
    
    /**
     * 成功响应（自定义消息）
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ErrorCode.ERR_OK, message, data);
    }
    
    /**
     * 错误响应
     */
    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return new ApiResponse<>(errorCode, message, null);
    }
    
    /**
     * 错误响应（默认错误码）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(ErrorCode.ERR_UNKNOWN, message, null);
    }
    
    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return ErrorCode.ERR_OK.equals(this.res);
    }
}

