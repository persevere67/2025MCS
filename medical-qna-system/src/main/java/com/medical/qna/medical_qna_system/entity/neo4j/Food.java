package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Food")
public class Food {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 食物名称

    // 关系：食物是某些疾病的忌吃食物 (no_eat 的反向)
    @Relationship(type = "no_eat", direction = Relationship.Direction.INCOMING)
    private Set<Disease> noEatForDiseases;

    // 关系：食物是某些疾病的宜吃食物 (do_eat 的反向)
    @Relationship(type = "do_eat", direction = Relationship.Direction.INCOMING)
    private Set<Disease> doEatForDiseases;

    // 关系：食物是某些疾病的推荐食谱 (recommand_eat 的反向)
    @Relationship(type = "recommand_eat", direction = Relationship.Direction.INCOMING)
    private Set<Disease> recommandEatForDiseases;

    public Food() {}

    public Food(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Disease> getNoEatForDiseases() { return noEatForDiseases; }
    public void setNoEatForDiseases(Set<Disease> noEatForDiseases) { this.noEatForDiseases = noEatForDiseases; }

    public Set<Disease> getDoEatForDiseases() { return doEatForDiseases; }
    public void setDoEatForDiseases(Set<Disease> doEatForDiseases) { this.doEatForDiseases = doEatForDiseases; }

    public Set<Disease> getRecommandEatForDiseases() { return recommandEatForDiseases; }
    public void setRecommandEatForDiseases(Set<Disease> recommandEatForDiseases) { this.recommandEatForDiseases = recommandEatForDiseases; }
}