package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.common.enums.ErrorCode;
import com.medical.qna.medical_qna_system.dto.request.LoginRequest;
import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.exception.BusinessException;
import com.medical.qna.medical_qna_system.service.AuthService;
import com.medical.qna.medical_qna_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public User register(RegisterRequest request) {
        return userService.register(request);
    }
    
    @Override
    public User login(LoginRequest request) {
        User user = userService.getUserByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_FAILED));
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("用户 {} 登录失败：密码错误", request.getUsername());
            throw new BusinessException(ErrorCode.AUTH_FAILED);
        }
        
        log.info("用户 {} 登录成功", user.getUsername());
        return user;
    }
}