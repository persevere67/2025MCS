package com.medical.qna.medical_qna_system.security;

import com.medical.qna.medical_qna_system.common.enums.ErrorCode;
import com.medical.qna.medical_qna_system.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 6;
    
    public void validate(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD.getCode(), "密码不能为空");
        }
        
        if (password.length() < MIN_LENGTH) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD);
        }
        
        // 简单验证：包含字母和数字
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        if (!hasLetter || !hasDigit) {
            throw new BusinessException(ErrorCode.WEAK_PASSWORD.getCode(), 
                "密码必须包含字母和数字");
        }
    }
}