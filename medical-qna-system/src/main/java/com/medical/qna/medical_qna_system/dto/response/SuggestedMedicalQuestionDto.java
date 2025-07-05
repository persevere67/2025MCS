package com.medical.qna.medical_qna_system.dto.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

// 引入 MedicalKnowledgeGraphEntityDto
import com.medical.qna.medical_qna_system.dto.common.MedicalKnowledgeGraphEntityDto; // 确保路径正确

/**
 * 表示一个从医药知识图谱中生成或抽取出来的推荐医药问题。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedMedicalQuestionDto {
    /**
     * 推荐问题的文本内容。
     * 例如：“感冒吃什么药好得快？”
     */
    private String questionText;

    /**
     * (可选) 如果该推荐问题在知识图谱中有唯一ID，可用于后续精确查询其答案。
     */
    private String questionId;

    /**
     * (可选) 与该推荐问题直接相关的医药知识图谱实体列表。
     * 例如，如果推荐问题是“阿司匹林有哪些副作用？”，则这里可能包含一个 type="Drug", name="阿司匹林" 的 MedicalKnowledgeGraphEntityDto。
     * 这有助于前端在展示推荐问题时，可以同时展示相关实体的简要信息。
     */
    private List<MedicalKnowledgeGraphEntityDto> relevantEntities;

    /**
     * (可选) 该推荐问题与用户原始查询的相关性分数。
     */
    private Double relevanceScore;
}