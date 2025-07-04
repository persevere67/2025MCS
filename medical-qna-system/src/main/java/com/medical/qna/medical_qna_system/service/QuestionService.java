package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;

import java.util.List;
import java.util.function.Consumer;

public interface QuestionService {
    
    /**
     * 处理问题并流式返回答案
     * @param question 用户问题
     * @param chunkConsumer 用于接收答案片段的回调函数
     */
    void processQuestionWithStream(String question, Consumer<String> chunkConsumer);
    
    /**
     * 处理问题并返回完整答案
     * @param question 用户问题
     * @return 完整答案
     */
    String processQuestion(String question);
    
    /**
     * 保存问答记录
     * @param userId 用户ID
     * @param question 用户问题
     * @param answer 用户答案
     */
    void saveQuestionAnswer(Long userId, String question, String answer);
    
    /**
     * 获取用户历史记录
     * @param userId 用户ID
     * @return 历史记录列表
     */
    List<QuestionAnswerDto> getUserHistory(Long userId);
    
    /**
     * 删除问答记录
     * @param id 记录ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteQuestionAnswer(Long id, Long userId);
    
    /**
     * 清空用户的所有历史记录
     * @param userId 用户ID
     */
    void clearUserHistory(Long userId);

    QuestionAnswer updateQuestionAnswer(Long id, String answer);
}