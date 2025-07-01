package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import lombok.Data;

@Node
@Data
public class Complication {
    @Id
    private String name;
}