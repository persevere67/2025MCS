package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.common.enums.ErrorCode;
import com.medical.qna.medical_qna_system.common.enums.UserRole;
import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.dto.request.UpdateUserRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.exception.BusinessException;
import com.medical.qna.medical_qna_system.repository.mysql.UserRepository;
import com.medical.qna.medical_qna_system.security.PasswordValidator;
import com.medical.qna.medical_qna_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Override
    public User register(RegisterRequest request) {
        // 验证用户名
        if (existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        // 验证邮箱
        if (existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        // 验证密码强度
        passwordValidator.validate(request.getPassword());

        User user = User.builder()
               .username(request.getUsername())
               .password(passwordEncoder.encode(request.getPassword()))
               .email(request.getEmail())
               .role(UserRole.USER)
               .createAt(LocalDateTime.now())
               .enabled(true)
               .build();

        User savedUser = userRepository.save(user);
        log.info("新用户注册成功: {} (ID: {})", savedUser.getUsername(), savedUser.getId());

        return savedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 检查邮箱是否被其他用户使用
        if (!user.getEmail().equals(request.getEmail()) && existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }

        // 检查用户名是否被其他用户使用
        if (!user.getUsername().equals(request.getUsername()) && existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }

        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());

        User updatedUser = userRepository.save(user);
        log.info("用户信息更新成功: {} (ID: {})", updatedUser.getUsername(), updatedUser.getId());

        return updatedUser;
    }

    // ...existing code...

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // ...existing code...

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // ...existing code...
    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

}