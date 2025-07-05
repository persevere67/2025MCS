package com.medical.qna.medical_qna_system.dto.common;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

// 引入您已有的 DiseaseInfoDto，根据您提供的包路径
import com.medical.qna.medical_qna_system.dto.response.DiseaseInfoDto;

/**
 * 表示医药知识图谱中的一个实体（节点）。
 * 这是一个通用DTO，可表示疾病、症状、药品等所有节点类型。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // 仅序列化非空的字段
public class MedicalKnowledgeGraphEntityDto {

    /**
     * 实体的唯一标识符。对应Neo4j节点的ID。
     */
    private String id; // 在Neo4j中是Long，这里用String方便传输

    /**
     * 实体的名称。
     * 例如：“感冒”、“头痛”、“布洛芬”、“肝炎科”。
     */
    private String name;

    /**
     * 实体的类型。
     * 对应Neo4j中的节点标签，例如：“Disease”, “Symptom”, “Drug”, “Food”, “Check”, “Department”, “Producer”。
     */
    private String type;

    /**
     * (可选) 实体的通用描述。
     * 可用于除Disease以外的其他节点类型，如果它们有简要描述的话。
     */
    private String description;

    /**
     * 仅当 type 为 "Disease" 时填充。
     * 包含疾病的详细信息，使用您现有的 DiseaseInfoDto 结构。
     */
    private DiseaseInfoDto diseaseInfo; // 如果 type == "Disease", 则填充此字段

    /**
     * (可选) 其他通用属性的键值对。
     * 可用于捕获任何未被上述字段明确定义的，或各类型节点特有的额外属性。
     */
    private Map<String, String> properties;
}