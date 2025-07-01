package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;

public interface QuestionService {
    
    /**
     * 处理用户问题并返回答案
     */
    QuestionAnswerDto processQuestion(String question, User user);
}