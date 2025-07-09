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
        // 修正：根据 UserService 的实际方法名，通常是 register 而不是 registerNewUser
        return userService.register(request);
    }

    @Override
    public void deleteUser(Long userId) {
        // 删除关联记录
        // 修正：调用 deleteByUser_Id
        questionAnswerRepository.deleteByUser_Id(userId);

        // 删除用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
        log.info("用户 {} (ID: {}) 已删除", user.getUsername(), user.getId());
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

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionAnswerDto> getUserQuestionHistory(Long userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 修正：调用 findByUser_IdOrderByCreateAtDesc
        Page<QuestionAnswer> questionAnswerPage = questionAnswerRepository.findByUser_IdOrderByCreateAtDesc(userId, pageable);

        List<QuestionAnswerDto> questionAnswerDtos = questionAnswerPage.getContent().stream()
                .map(qa -> QuestionAnswerDto.builder()
                        .id(qa.getId())
                        .question(qa.getQuestion())
                        .answer(qa.getAnswer())
                        .createAt(qa.getCreateAt())
                        .build())
                .collect(Collectors.toList());

        return new PageImpl<>(questionAnswerDtos, pageable, questionAnswerPage.getTotalElements());
    }

    @Override
    public QuestionAnswer updateQuestionAnswer(Long id, QuestionAnswerDto request) {
        QuestionAnswer questionAnswer = questionAnswerRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_ANSWER_NOT_FOUND)); // 使用 ErrorCode 枚举
        questionAnswer.setQuestion(request.getQuestion());
        questionAnswer.setAnswer(request.getAnswer());
        return questionAnswerRepository.save(questionAnswer);
    }

    @Override
    public void deleteQuestionAnswer(Long id) {
        // 检查记录是否存在，并抛出业务异常
        if (!questionAnswerRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.QUESTION_ANSWER_NOT_FOUND);
        }
        questionAnswerRepository.deleteById(id);
        log.info("问答记录 {} 已删除", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionAnswerDto> getAllQuestionAnswers(Pageable pageable) {
        // 修正：调用 findAllByOrderByCreateAtDesc
        Page<QuestionAnswer> questionAnswerPage = questionAnswerRepository.findAllByOrderByCreateAtDesc(pageable);
        
        // 转换为DTO并包含用户ID (从 QuestionAnswer 实体通过 User 关联获取 ID)
        return questionAnswerPage.map(qa -> QuestionAnswerDto.builder()
                .id(qa.getId())
                .userId(qa.getUser().getId()) // 修正：从 qa.getUser().getId() 获取用户ID
                .question(qa.getQuestion())
                .answer(qa.getAnswer())
                .createAt(qa.getCreateAt())
                .build());
    }
}
