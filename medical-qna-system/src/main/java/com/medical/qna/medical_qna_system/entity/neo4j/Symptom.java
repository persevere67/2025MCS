package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Symptom")
public class Symptom {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 症状名称

    // 关系：一个症状可能由多种疾病引起 (has_symptom 的反向)
    @Relationship(type = "has_symptom", direction = Relationship.Direction.INCOMING)
    private Set<Disease> diseasesWithThisSymptom;

    public Symptom() {}

    public Symptom(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<Disease> getDiseasesWithThisSymptom() { return diseasesWithThisSymptom; }
    public void setDiseasesWithThisSymptom(Set<Disease> diseasesWithThisSymptom) { this.diseasesWithThisSymptom = diseasesWithThisSymptom; }
}