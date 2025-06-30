package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;

@Node("Disease")
public class Disease {
    @Id
    @GeneratedValue
    private Long id;
    
    private String name;
    private String desc;
    private String diagnosis;
    private String insurance_covered;
    private String prevalence_rate;
    private String susceptible_population;
    private String transmission_mode;
    private String nursing;
    private String treatment_duration;
    private String cure_rate;
    private String treatment_cost;
    
    @Relationship(type = "HAS_SYMPTOM", direction = Relationship.Direction.OUTGOING)
    private Set<Symptom> symptoms;
    
    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private Set<Department> departments;
    
    @Relationship(type = "TREATED_BY", direction = Relationship.Direction.OUTGOING)
    private Set<Treatment> treatments;
    
    @Relationship(type = "GOOD_FOR", direction = Relationship.Direction.OUTGOING)
    private Set<Food> goodFoods;
    
    @Relationship(type = "BAD_FOR", direction = Relationship.Direction.OUTGOING)
    private Set<Food> badFoods;
    
    @Relationship(type = "HAS_COMPLICATION", direction = Relationship.Direction.OUTGOING)
    private Set<Complication> complications;
    
    // Constructors
    public Disease() {}
    
    public Disease(String name) {
        this.name = name;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    
    public String getInsurance_covered() { return insurance_covered; }
    public void setInsurance_covered(String insurance_covered) { this.insurance_covered = insurance_covered; }
    
    public String getPrevalence_rate() { return prevalence_rate; }
    public void setPrevalence_rate(String prevalence_rate) { this.prevalence_rate = prevalence_rate; }
    
    public String getSusceptible_population() { return susceptible_population; }
    public void setSusceptible_population(String susceptible_population) { this.susceptible_population = susceptible_population; }
    
    public String getTransmission_mode() { return transmission_mode; }
    public void setTransmission_mode(String transmission_mode) { this.transmission_mode = transmission_mode; }
    
    public String getNursing() { return nursing; }
    public void setNursing(String nursing) { this.nursing = nursing; }
    
    public String getTreatment_duration() { return treatment_duration; }
    public void setTreatment_duration(String treatment_duration) { this.treatment_duration = treatment_duration; }
    
    public String getCure_rate() { return cure_rate; }
    public void setCure_rate(String cure_rate) { this.cure_rate = cure_rate; }
    
    public String getTreatment_cost() { return treatment_cost; }
    public void setTreatment_cost(String treatment_cost) { this.treatment_cost = treatment_cost; }
    
    public Set<Symptom> getSymptoms() { return symptoms; }
    public void setSymptoms(Set<Symptom> symptoms) { this.symptoms = symptoms; }
    
    public Set<Department> getDepartments() { return departments; }
    public void setDepartments(Set<Department> departments) { this.departments = departments; }
    
    public Set<Treatment> getTreatments() { return treatments; }
    public void setTreatments(Set<Treatment> treatments) { this.treatments = treatments; }
    
    public Set<Food> getGoodFoods() { return goodFoods; }
    public void setGoodFoods(Set<Food> goodFoods) { this.goodFoods = goodFoods; }
    
    public Set<Food> getBadFoods() { return badFoods; }
    public void setBadFoods(Set<Food> badFoods) { this.badFoods = badFoods; }
    
    public Set<Complication> getComplications() { return complications; }
    public void setComplications(Set<Complication> complications) { this.complications = complications; }
}