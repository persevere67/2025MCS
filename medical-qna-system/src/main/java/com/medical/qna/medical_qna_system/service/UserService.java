package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.RegisterRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.enums.UserRole;
import com.medical.qna.medical_qna_system.repository.mysql.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    public void register(RegisterRequest request) {
        // 检查用户名或邮箱是否已存在
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("邮箱已存在");
        }
        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole(UserRole.USER); // 默认普通用户

        userRepository.save(user);
    }

    // 获取用户信息
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User updateUser(Long userId, User updatedUser) {
        return userRepository.findById(userId)
                             .map(existingUser -> {
                                 // 更新用户信息，根据需要更新相应的字段
                                 existingUser.setUsername(updatedUser.getUsername());
                                 existingUser.setEmail(updatedUser.getEmail());

                                 // 保存更新后的用户信息
                                 return userRepository.save(existingUser);
                             }).orElse(null); // 或者抛出异常，取决于你的业务需求
    }
}