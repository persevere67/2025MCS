package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.request.LoginRequest;
import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;

public interface AuthService {
    
    /**
     * 用户注册
     */
    User register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    User login(LoginRequest request);
}