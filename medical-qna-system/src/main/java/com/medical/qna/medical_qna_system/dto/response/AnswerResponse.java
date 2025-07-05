package com.medical.qna.medical_qna_system.dto.response; // 注意：您的文件路径是 dto.response

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// 导入 SemanticUnderstandingResult，因为它包含了 RecommendedQuestion 的定义
import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult;

// 如果您有通用的 ApiResponse DTO 并且希望在 AnswerResponse 中包含它，请确保导入
// import com.medical.qna.medical_qna_system.dto.response.ApiResponse; // 假设您的通用 ApiResponse 在此路径

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerResponse {
    private String answer; // 你的主要答案内容
    private LocalDateTime timestamp;
    private Long questionId;
    private DiseaseInfoDto diseaseInfo; // 保留，支持其他功能
    private List<String> recommendedDepartments; // 保留，支持其他功能
    private String disclaimer = "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。";

    // ****** 新增字段：用于承载 Python 服务返回的推荐问题 ******
    // 这里的类型是 List<SemanticUnderstandingResult.RecommendedQuestion>
    private List<SemanticUnderstandingResult.RecommendedQuestion> recommendedQuestions;

    // ****** 可选：如果希望像 Python 那样有一个顶层 ApiResponse，但通常 Java Spring RestController 直接返回 AnswerResponse 并在 HTTP Status Code 中体现成功失败 ******
    // private ApiResponse apiResponse; // 如果您希望在 AnswerResponse 内部也包含一个通用的 ApiResponse 状态

    // 内部类或单独的 DTO: DiseaseInfoDto
    // 如果 DiseaseInfoDto 已经在其他文件中定义，请确保它有正确的包路径并被导入。
    // 否则，您需要在这里或单独的文件中定义它。
    // 假设 DiseaseInfoDto 已经存在于 com.medical.qna.medical_qna_system.dto 或其他 dto 包下
    // 如果它还没有定义，这里是一个示例（请根据您的实际数据结构补充）：
    /*
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DiseaseInfoDto {
        private String diseaseName;
        private String description;
        // ... 其他疾病相关信息，如症状、治疗方法等
    }
    */
}