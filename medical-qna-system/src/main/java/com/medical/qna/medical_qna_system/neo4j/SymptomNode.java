package com.medical.qna.medical_qna_system.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Symptom")
public class SymptomNode {
    @Id
    private String name; // 症状名称，唯一

    // getter/setter 省略
}