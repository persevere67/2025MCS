package com.medical.qna.medical_qna_system.exception;

import com.medical.qna.medical_qna_system.common.enums.ErrorCode;

public class BusinessException extends RuntimeException {
    private final String code;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
    
    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
}
