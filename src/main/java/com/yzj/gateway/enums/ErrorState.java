package com.yzj.gateway.enums;

/**
 * 错误类别
 */
public enum ErrorState {
    /**
     * 恶意访问
     */
    MALICIOUS(0, "恶意访问"),
    /**
     * 用户权限不足
     */
    DENIED(1, "用户权限不足"),
    /**
     * 用户未登录
     */
    NOTLOGGEDIN(2, "用户未登录"),
    /**
     * 用户重复登录
     */
    DUPLICATE(3, "用户重复登录"),
    /**
     * token失效
     */
    FAILURE(4, "token失效");

    private Integer value;

    private String msg;


    ErrorState(Integer value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public static ErrorState getEnum(Integer value) {
        for (ErrorState errorState : ErrorState.values()) {
            if (errorState.getValue().equals(value)) {
                return errorState;
            }
        }
        return null;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getMsg() {
        return msg;
    }
}
