package com.simeng.pib.pojo;

public class ConversionResult {
    private final boolean success;
    private final String message;
    private final String method;

    public ConversionResult(boolean success, String message) {
        this(success, message, null);
    }

    public ConversionResult(boolean success, String message, String method) {
        this.success = success;
        this.message = message;
        this.method = method;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getMethod() {
        return method;
    }
}
