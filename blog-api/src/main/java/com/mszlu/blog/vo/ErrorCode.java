package com.mszlu.blog.vo;

public enum ErrorCode {
    SYSTEM_ERROR(-999, "系统异常"),
    PARAMS_ERROR(501,"参数有误"),
    ACCOUNT_NOT_EXIST(502,"该用户不存在"),
    ACCOUNT_PWD_IS_NULL(503,"用户名或密码不能为空"),
    ACCOUNT_PWD_NICKNAME_IS_NULL(503,"用户名或昵称或密码不能为空"),
    PWD_ERROR(505,"密码错误"),
    TOKEN_ERROR(506,"token不合法"),

    ACCOUNT_NICKNAME_EXIST(507,"该用户名或昵称已存在"),
    NO_PERMISSION(701,"无访问权限"),
    SESSION_TIME_OUT(901,"会话超时"),
    NO_LOGIN(902,"请先登录");

    private int code;
    private String msg;

    ErrorCode(int code, String msg){
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

