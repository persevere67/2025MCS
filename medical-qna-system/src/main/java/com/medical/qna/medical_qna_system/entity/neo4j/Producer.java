package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Producer")
public class Producer {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 生产商名称

    // 关系：生产商生产多种药品 (produces 的正向)
    @Relationship(type = "produces", direction = Relationship.Direction.OUTGOING)
    private Set<Drug> producedDrugs;

    public Producer() {}

    public Producer(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Drug> getProducedDrugs() { return producedDrugs; }
    public void setProducedDrugs(Set<Drug> producedDrugs) { this.producedDrugs = producedDrugs; }
}