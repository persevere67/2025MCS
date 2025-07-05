package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {
    /**
     * 添加用户
     */
    User addUser(RegisterRequest request);

    /**
     * 删除用户
     */
    void deleteUser(Long userId);



    /**
     * 获取所有用户信息
     */
    List<User> getAllUsers();

    /**
     * 根据用户ID获取用户信息
     */
    User getUserById(Long userId);

    /**
     * 获取用户问答历史（支持分页）
     */
    Page<QuestionAnswerDto> getUserQuestionHistory(Long userId, Pageable pageable);
}