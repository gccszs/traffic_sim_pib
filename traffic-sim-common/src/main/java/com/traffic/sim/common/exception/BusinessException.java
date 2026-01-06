package com.traffic.sim.common.exception;

import com.traffic.sim.common.constant.ErrorCode;

/**
 * 业务异常
 * 
 * @author traffic-sim
 */
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String message) {
        super(message);
        this.errorCode = ErrorCode.ERR_UNKNOWN;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

