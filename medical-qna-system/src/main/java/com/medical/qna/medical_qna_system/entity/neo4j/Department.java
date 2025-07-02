package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.Data;

@Node
@Data
public class Department {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 科室名称

    // 关系：一个大科室包含多个小科室 (belongs_to 的反向)
    @Relationship(type = "belongs_to", direction = Relationship.Direction.INCOMING)
    private Set<Department> subDepartments;

    // 关系：一个疾病属于某个科室 (belongs_to 的反向)
    @Relationship(type = "belongs_to", direction = Relationship.Direction.INCOMING)
    private Set<Disease> diseasesBelongingHere;


    public Department() {}

    public Department(String name) {
        this.name = name;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Department> getSubDepartments() { return subDepartments; }
    public void setSubDepartments(Set<Department> subDepartments) { this.subDepartments = subDepartments; }

    public Set<Disease> getDiseasesBelongingHere() { return diseasesBelongingHere; }
    public void setDiseasesBelongingHere(Set<Disease> diseasesBelongingHere) { this.diseasesBelongingHere = diseasesBelongingHere; }
}