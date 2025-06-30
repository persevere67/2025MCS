package com.medical.qna.medical_qna_system.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Complication")
public class ComplicationNode {
    @Id
    private String name; // 并发症名称，唯一

    // getter/setter 省略
}