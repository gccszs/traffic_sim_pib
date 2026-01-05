package com.traffic.sim.common.exception;

import com.traffic.sim.common.constant.ErrorCode;

/**
 * 服务异常
 * 
 * @author traffic-sim
 */
public class ServiceException extends RuntimeException {
    
    private final String errorCode;
    
    public ServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public ServiceException(String message) {
        super(message);
        this.errorCode = ErrorCode.ERR_INTERNAL;
    }
    
    public ServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

