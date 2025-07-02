package com.medical.qna.medical_qna_system.common.enums;


public enum ErrorCode {
    SUCCESS("0000", "操作成功"),
    SYSTEM_ERROR("1000", "系统错误"),
    PARAM_ERROR("1001", "参数错误"),
    
    // 认证相关
    AUTH_FAILED("2001", "用户名或密码错误"),
    UNAUTHORIZED("2002", "未授权访问"),
    SESSION_EXPIRED("2003", "会话已过期"),
    
    // 用户相关
    USERNAME_EXISTS("3001", "用户名已存在"),
    EMAIL_EXISTS("3002", "邮箱已被注册"),
    USER_NOT_FOUND("3003", "用户不存在"),
    WEAK_PASSWORD("3004", "密码强度不足"),
    
    // 业务相关
    QUESTION_TOO_LONG("4001", "问题长度超过限制");
    
    private final String code;
    private final String message;
    
    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() { return code; }
    public String getMessage() { return message; }
}