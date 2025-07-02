package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Check")
public class Check {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 检查项目名称

    // 关系：检查项目是某些疾病所需的 (need_check 的反向)
    @Relationship(type = "need_check", direction = Relationship.Direction.INCOMING)
    private Set<Disease> diseasesRequiringCheck;

    public Check() {}

    public Check(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Disease> getDiseasesRequiringCheck() { return diseasesRequiringCheck; }
    public void setDiseasesRequiringCheck(Set<Disease> diseasesRequiringCheck) { this.diseasesRequiringCheck = diseasesRequiringCheck; }
}