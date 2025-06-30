package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;

@Node
@Data
public class Department {
    @Id
    private String name;
  
    @Relationship(type = "BELONGS_TO_CATEGORY", direction = Relationship.Direction.OUTGOING)
    private DepartmentCategory category;
}