package com.yuqixue.common.constants;

public enum ErrorCode {
    
    SUCCESS("200", "操作成功"),
    BAD_REQUEST("400", "请求参数错误"),
    UNAUTHORIZED("401", "未授权访问"),
    FORBIDDEN("403", "禁止访问"),
    NOT_FOUND("404", "资源未找到"),
    METHOD_NOT_ALLOWED("405", "请求方法不允许"),
    CONFLICT("409", "资源冲突"),
    INTERNAL_ERROR("500", "服务器内部错误"),
    SERVICE_UNAVAILABLE("503", "服务不可用");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
