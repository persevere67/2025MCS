package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Department")
public class Department {
    @Id
    @GeneratedValue
    private Long id;
    
    private String name;
    
    @Relationship(type = "BELONGS_TO_CATEGORY", direction = Relationship.Direction.OUTGOING)
    private Set<DepartmentCategory> categories;
    
    public Department() {}
    
    public Department(String name) {
        this.name = name;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Set<DepartmentCategory> getCategories() { return categories; }
    public void setCategories(Set<DepartmentCategory> categories) { this.categories = categories; }
}
