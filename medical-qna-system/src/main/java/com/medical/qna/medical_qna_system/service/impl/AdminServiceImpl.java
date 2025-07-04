package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.common.enums.ErrorCode;
import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.dto.request.UpdateUserRequest;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.exception.BusinessException;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import com.medical.qna.medical_qna_system.repository.mysql.UserRepository;
import com.medical.qna.medical_qna_system.service.AdminService;
import com.medical.qna.medical_qna_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserService userService;

    @Override
    public User addUser(RegisterRequest request) {
        return userService.register(request);
    }

    @Override
    public void deleteUser(Long userId) {
        // 删除关联记录
        questionAnswerRepository.deleteByUserId(userId);

        // 删除用户
        User user = userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
        log.info("用户 {} (ID: {}) 已删除", user.getUsername(), user.getId());
    }

    @Override
    public User updateUser(Long userId, UpdateUserRequest request) {
        return userService.updateUser(userId, request);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
               .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 确保方法签名与接口完全一致
    @Override
    @Transactional(readOnly = true)
    public Page<QuestionAnswerDto> getUserQuestionHistory(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        User user = userRepository.getReferenceById(userId);
        Page<QuestionAnswer> questionAnswerPage = questionAnswerRepository.findByUserOrderByCreateAtDesc(user, pageable);

        List<QuestionAnswerDto> questionAnswerDtos = questionAnswerPage.getContent().stream()
               .map(qa -> QuestionAnswerDto.create(qa.getQuestion(), qa.getAnswer()))
               .collect(Collectors.toList());

        return new PageImpl<>(questionAnswerDtos, pageable, questionAnswerPage.getTotalElements());
    }
}