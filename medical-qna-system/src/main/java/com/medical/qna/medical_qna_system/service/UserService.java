package com.medical.qna.medical_qna_system.service;


import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.repository.mysql.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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