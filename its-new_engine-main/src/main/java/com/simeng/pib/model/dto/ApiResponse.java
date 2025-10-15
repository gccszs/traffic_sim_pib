package com.simeng.pib.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一API响应格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     */
    private String res;
    
    /**
     * 响应消息
     */
    private String msg;
    
    /**
     * 附加数据
     */
    private T addition;
    
    public ApiResponse(String res, String msg) {
        this.res = res;
        this.msg = msg;
    }
    
    public static <T> ApiResponse<T> success(String msg) {
        return new ApiResponse<>("ERR_OK", msg);
    }
    
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>("ERR_OK", msg, data);
    }
    
    public static <T> ApiResponse<T> error(String code, String msg) {
        return new ApiResponse<>(code, msg);
    }
}
