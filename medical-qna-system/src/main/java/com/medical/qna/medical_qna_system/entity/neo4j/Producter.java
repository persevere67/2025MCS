package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;

import java.util.Set;

@Node
@Data
public class Producter {
    @Id
    private String name; // 生产商名称

    @Relationship(type = "PRODUCES", direction = Relationship.Direction.OUTGOING)
    private Set<Drug> drugs; // 生产的药品
}   
