package com.medical.qna.medical_qna_system.dto.response;

import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult; // 导入 SemanticUnderstandingResult
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List; // 导入 List

@Data
@AllArgsConstructor // 确保有全参数构造函数
@NoArgsConstructor // 确保有无参构造函数
public class AnswerResponse {
    private String answer;
    private LocalDateTime timestamp; // 使用 timestamp 字段名，与 QuestionController 保持一致
    private Long questionId;
    private String disclaimer = "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。";

    // 新增字段，用于承载语义理解结果
    private String identifiedIntent; // 识别出的意图
    private List<String> identifiedKeywords; // 识别出的关键词
    private String sourceInfo; // 来源信息，例如RAG检索到的文档摘要
    private List<SemanticUnderstandingResult.RecommendedQuestion> recommendedQuestions; // 推荐问题列表

    // 为了兼容旧的构造函数调用，可以提供一个简化的构造函数
    // 注意：如果您确定所有 AnswerResponse 都会使用新的完整构造函数，可以移除此简化构造函数。
    // 但为了兼容性，暂时保留。
    public AnswerResponse(String answer, LocalDateTime timestamp, Long questionId, String disclaimer) {
        this.answer = answer;
        this.timestamp = timestamp;
        this.questionId = questionId;
        this.disclaimer = disclaimer;
    }

    // 提供一个更完整的构造函数，用于在 QuestionServiceImpl 中构建响应
    // Lombok 的 @AllArgsConstructor 会自动生成一个包含所有字段的构造函数，
    // 如果您需要自定义构造函数，可以手动编写，但要确保与 @AllArgsConstructor 不冲突。
    // 这里为了清晰，手动列出这个完整构造函数，它将覆盖 @AllArgsConstructor 生成的默认行为（如果字段顺序不同）。
    // 建议依赖 @AllArgsConstructor，并确保字段顺序在类定义中是您期望的。
    // 如果您只使用 @AllArgsConstructor，那么 AnswerResponse 的构造函数将是 (String answer, LocalDateTime timestamp, Long questionId, String disclaimer, String identifiedIntent, List<String> identifiedKeywords, String sourceInfo, List<SemanticUnderstandingResult.RecommendedQuestion> recommendedQuestions)
    // 请确保 QuestionServiceImpl 中的 new AnswerResponse(...) 调用与实际的构造函数签名匹配。
    // 为了与 QuestionServiceImpl 中的调用匹配，我们假设 AnswerResponse 有一个包含所有字段的构造函数。
    // 如果您只使用 @AllArgsConstructor，并且字段顺序如上，那么 QuestionServiceImpl 中的构造函数调用会匹配。
    // 如果您希望自定义构造函数，请确保其签名与 QuestionServiceImpl 中的调用完全匹配。
}
