package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Department")
public class DepartmentNode {
    @Id
    private String name; // 科室名称，唯一

    @Relationship(type = "BELONGS_TO_CATEGORY")
    private DepartmentCategoryNode category;

    // getter/setter 省略
}