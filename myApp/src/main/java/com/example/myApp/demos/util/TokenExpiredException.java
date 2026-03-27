package com.example.myApp.demos.util;

public class TokenExpiredException extends RuntimeException{
    private static final long serialVersionUID = -7034897190745766939L;
    private String msg;
    private  int code;
    public TokenExpiredException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return this.code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

}
