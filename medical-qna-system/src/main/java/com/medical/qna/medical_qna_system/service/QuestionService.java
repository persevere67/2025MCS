package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest; // 确保导入 QuestionRequest
import com.medical.qna.medical_qna_system.dto.response.AnswerResponse; // 确保导入 AnswerResponse
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto; // 确保导入 QuestionAnswerDto
import java.util.List;
import java.util.function.Consumer; // 用于流式处理的回调函数

public interface QuestionService {

    /**
     * 处理用户问题，进行语义理解并获取答案和推荐问题。
     * 此方法主要用于控制器中接收 QuestionRequest 并返回 AnswerResponse 的场景。
     * @param request 包含用户问题的请求DTO
     * @return 包含答案和推荐问题的响应DTO
     */
    AnswerResponse processQuestion(QuestionRequest request); // *** 保持此方法签名，以兼容 QuestionController ***

    /**
     * 处理问题并流式返回答案。
     * 此方法用于需要分块接收答案的场景，例如SSE。
     * @param question 用户问题
     * @param chunkConsumer 用于接收答案片段的回调函数
     */
    void processQuestionWithStream(String question, Consumer<String> chunkConsumer);

    /**
     * 处理问题并返回完整答案。
     * 此方法用于不需要流式处理，直接返回完整答案的场景。
     * 注意：如果您的 QuestionController 已经使用 processQuestion(QuestionRequest request)
     * 并且通过 SSE 接收流，那么这个方法可能需要调整其使用场景或作为内部辅助方法。
     * @param question 用户问题
     * @return 完整答案
     */
    String processQuestion(String question); // *** 您的原始方法，与上面重载 ***

    /**
     * 保存问答记录。
     * @param userId 用户ID
     * @param question 用户问题
     * @param answer 用户答案
     */
    void saveQuestionAnswer(Long userId, String question, String answer);

    /**
     * 获取用户历史记录。
     * @param userId 用户ID
     * @return 历史记录列表
     */
    List<QuestionAnswerDto> getUserHistory(Long userId);

    /**
     * 删除问答记录。
     * @param id 记录ID
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteQuestionAnswer(Long id, Long userId);

    /**
     * 清空用户的所有历史记录。
     * @param userId 用户ID
     */
    void clearUserHistory(Long userId);
}
