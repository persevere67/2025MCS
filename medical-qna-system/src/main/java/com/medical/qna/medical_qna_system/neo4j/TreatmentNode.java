package com.medical.qna.medical_qna_system.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Treatment")
public class TreatmentNode {
    @Id
    private String name; // 治疗方式名称，唯一

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
