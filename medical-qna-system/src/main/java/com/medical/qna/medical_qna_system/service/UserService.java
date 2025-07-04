package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;

import java.util.Optional;

public interface UserService {
    
    /**
     * 用户注册
     */
    User register(RegisterRequest request);

    /**
     * 根据id获取用户
     */
    Optional<User> findById(Long id);
    
    /**
     * 根据id获取用户（直接返回User对象，兼容现有代码）
     */
    User getUserById(Long id);
    /**
     * 根据用户名查找用户
     */
    User findByUsername(String username);
    
    /**
     * 根据用户名获取用户
     */
    Optional<User> getUserByUsername(String username);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
}