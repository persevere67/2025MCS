package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Food")
public class FoodNode {
    @Id
    private String name; // 食物名称，唯一

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
