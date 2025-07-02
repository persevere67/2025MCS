package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Drug")
public class Drug {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 药品名称

    // 关系：药品被生产商生产 (produces 的反向)
    @Relationship(type = "produces", direction = Relationship.Direction.INCOMING)
    private Producer producer;

    // 关系：药品是某些疾病的常用药 (common_drug 的反向)
    @Relationship(type = "common_drug", direction = Relationship.Direction.INCOMING)
    private Set<Disease> commonForDiseases;

    // 关系：药品是某些疾病的推荐药 (recommand_drug 的反向)
    @Relationship(type = "recommand_drug", direction = Relationship.Direction.INCOMING)
    private Set<Disease> recommandForDiseases;

    public Drug() {}

    public Drug(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Producer getProducer() { return producer; }
    public void setProducer(Producer producer) { this.producer = producer; }

    public Set<Disease> getCommonForDiseases() { return commonForDiseases; }
    public void setCommonForDiseases(Set<Disease> commonForDiseases) { this.commonForDiseases = commonForDiseases; }

    public Set<Disease> getRecommandForDiseases() { return recommandForDiseases; }
    public void setRecommandForDiseases(Set<Disease> recommandForDiseases) { this.recommandForDiseases = recommandForDiseases; }
}