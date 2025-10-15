package edu.uestc.iscssl.itsbackend.utils;

/**
 * 所有业务异常都放这里
 */
public class ITSException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String msg;
    private int code = 500;

    public ITSException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public ITSException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public ITSException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public ITSException(String msg, int code, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }


}
