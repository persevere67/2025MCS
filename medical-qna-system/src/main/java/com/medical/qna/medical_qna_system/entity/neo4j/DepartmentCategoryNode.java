package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("DepartmentCategory")
public class DepartmentCategoryNode {
    @Id
    private String name; // 科室分类名称，唯一

    // getter/setter 省略
}