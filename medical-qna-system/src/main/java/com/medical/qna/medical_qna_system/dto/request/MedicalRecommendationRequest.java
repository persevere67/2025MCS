package com.medical.qna.medical_qna_system.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医药推荐问题生成模块的请求DTO。
 * 包含用户输入的原始医药问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecommendationRequest {
    /**
     * 用户输入的原始医药问题文本。
     * 例如：“感冒发烧怎么办？”
     */
    private String queryText;

    /**
     * (可选) 会话ID，用于上下文管理，如果此模块需要。
     */
    private String sessionId;

    /**
     * (可选) 用户ID，用于个性化或日志记录，如果此模块需要。
     */
    private String userId;
}