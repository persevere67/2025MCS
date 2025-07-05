package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;

import java.util.Set;

@Node
@Data
public class Disease {
    @Id
    private String name;
  
    private String desc; // 疾病简介
    private String diagnosis; // 诊断信息
    private Boolean insuranceCovered; // 是否医保疾病
    private String prevalenceRate; // 患病比例
    private String susceptiblePopulation; // 易感人群
    private String transmissionMode; // 传染方式
    private String nursing; // 护理信息
    private String treatmentDuration; // 治疗周期
    private String cureRate; // 治愈率
    private String treatmentCost; // 治疗费用
  
    @Relationship(type = "HAS_SYMPTOM", direction = Relationship.Direction.OUTGOING)
    private Set<Symptom> symptoms;
  
    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private Department department;
  
    @Relationship(type = "TREATED_BY", direction = Relationship.Direction.OUTGOING)
    private Set<Treatment> treatments;
  
    @Relationship(type = "GOOD_FOR", direction = Relationship.Direction.OUTGOING)
    private Set<Food> goodFoods;
  
    @Relationship(type = "BAD_FOR", direction = Relationship.Direction.OUTGOING)
    private Set<Food> badFoods;
  
    @Relationship(type = "HAS_COMPLICATION", direction = Relationship.Direction.OUTGOING)
    private Set<Complication> complications;
}