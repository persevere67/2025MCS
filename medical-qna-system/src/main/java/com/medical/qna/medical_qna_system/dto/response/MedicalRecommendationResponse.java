package com.medical.qna.medical_qna_system.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
// 引入 SuggestedMedicalQuestionDto
import com.medical.qna.medical_qna_system.dto.response.SuggestedMedicalQuestionDto; // 确保路径正确

// 引入 ApiResponse
import com.medical.qna.medical_qna_system.dto.response.ApiResponse; // 确保路径正确，假设ApiResponse在同一个response包下

/**
 * 医药推荐问题生成模块的响应DTO。
 * 包含根据原始问题生成的推荐问题列表。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecommendationResponse {
    /**
     * 用户提交的原始医药查询文本，用于回显或上下文。
     */
    private String originalQuery;

    /**
     * 从原始查询中识别出的核心关键词或短语列表。
     * (可选，如果推荐模块会返回此信息，可帮助调试或前端展示)
     */
    private List<String> identifiedKeywords;

    /**
     * 识别出的用户意图。
     * (可选，如果推荐模块会返回此信息，可帮助调试或前端展示)
     */
    private String identifiedIntent;

    /**
     * 根据知识图谱关系生成的三个推荐医药问题列表。
     */
    private List<SuggestedMedicalQuestionDto> recommendedQuestions;

    /**
     * 通用API响应状态，指示推荐生成操作是否成功。
     */
    private ApiResponse apiResponse;
}