package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import java.util.List;

@Node("Disease")
public class DiseaseNode {
    @Id
    private String name; // 疾病名称，唯一

    private String desc;
    private String diagnosis;
    private Boolean insuranceCovered;
    private String prevalenceRate;
    private String susceptiblePopulation;
    private String transmissionMode;
    private String nursing;
    private String treatmentDuration;
    private String cureRate;
    private String treatmentCost;

    @Relationship(type = "HAS_SYMPTOM")
    private List<SymptomNode> symptoms;

    @Relationship(type = "BELONGS_TO")
    private DepartmentNode department;

    @Relationship(type = "TREATED_BY")
    private List<TreatmentNode> treatments;

    @Relationship(type = "GOOD_FOR")
    private List<FoodNode> goodFoods;

    @Relationship(type = "BAD_FOR")
    private List<FoodNode> badFoods;

    @Relationship(type = "HAS_COMPLICATION")
    private List<ComplicationNode> complications;

    // getter/setter 省略
}