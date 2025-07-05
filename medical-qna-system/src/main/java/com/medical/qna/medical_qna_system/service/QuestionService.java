package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import java.time.LocalDateTime;
import java.util.List;

public interface QuestionService {
    
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

    /**
     * 根据关键词搜索用户的问答记录
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 匹配的问答记录列表
     */
    List<QuestionAnswerDto> searchUserHistory(Long userId, String keyword);

    /**
     * 获取用户问答记录总数
     * @param userId 用户ID
     * @return 记录总数
     */
    long getUserQuestionCount(Long userId);

    /**
     * 分页获取用户最近的问答记录
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的问答记录列表
     */
    List<QuestionAnswerDto> getUserRecentHistory(Long userId, int page, int size);

    /**
     * 批量删除用户的问答记录
     * @param ids 要删除的记录ID列表
     * @param userId 用户ID
     * @return 成功删除的记录数
     */
    int batchDeleteQuestionAnswers(List<Long> ids, Long userId);

    /**
     * 检查用户是否有问答记录
     * @param userId 用户ID
     * @return 是否有记录
     */
    boolean hasUserQuestions(Long userId);

}