package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;

import java.util.Set;
import java.util.List;

@Node
@Data
public class Disease {
    @Id
    private String name;
  
    private String desc; // 疾病简介
    private String prevent; // 预防措施
    private String cause; // 病因
    private String easy_get; // 易感人群
    private String cure_lasttime; // 治疗周期
    private String cure_prub; // 治愈率
    private List<String> cury_way; // 治疗方式
  
    @Relationship(type = "HAS_SYMPTOM", direction = Relationship.Direction.OUTGOING)
    private Set<Symptom> symptoms;
  
    @Relationship(type = "ACCOMPANY_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<Disease> Diseases; // 与此疾病并发的疾病

    @Relationship(type = "BELONG_TO", direction = Relationship.Direction.OUTGOING)
    private Set<Department> departments; // 属于哪个科室

    @Relationship(type = "RECOMMEND_DRUG", direction = Relationship.Direction.OUTGOING)
    private Set<Drug> drugs; // 推荐的药物

    @Relationship(type = "COMMEN_DRUG", direction = Relationship.Direction.OUTGOING)
    private Set<Drug> common_drugs; // 常用药物

    @Relationship(type = "DO_EAT", direction = Relationship.Direction.OUTGOING)
    private Set<Food> foods; // 宜吃

    @Relationship(type = "NO_EAT", direction = Relationship.Direction.OUTGOING)
    private Set<Food> no_foods; // 忌吃

    @Relationship(type = "RECOMMEND_EAT", direction = Relationship.Direction.OUTGOING)
    private Set<Food> recommend_foods; // 推荐食物/食谱

    @Relationship(type = "NEED_CHECK", direction = Relationship.Direction.OUTGOING)
    private Set<Check> checks; // 诊断检查
}