package com.medical.qna.medical_qna_system.entity.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.Set;
import java.util.List; // cure_way 可能是列表

@Node("Disease")
public class Disease {
    @Id
    @GeneratedValue
    private Long id;

    private String name; // 疾病名称
    private String desc; // 描述
    private String prevent; // 预防方法
    private String cause; // 病因
    private String easy_get; // 易感人群
    private String cure_lasttime; // 治疗持续时间
    private String cured_prob; // 治愈概率
    private List<String> cure_way; // 治疗方法，根据Python代码可能是列表

    // 关系
    @Relationship(type = "has_symptom", direction = Relationship.Direction.OUTGOING)
    private Set<Symptom> symptoms;

    // 并发症关系，指向其他疾病节点（因为没有独立的Complication节点）
    @Relationship(type = "acompany_with", direction = Relationship.Direction.OUTGOING)
    private Set<Disease> acompanyDiseases;

    @Relationship(type = "belongs_to", direction = Relationship.Direction.OUTGOING)
    private Set<Department> departments; // 所属科室

    @Relationship(type = "recommand_drug", direction = Relationship.Direction.OUTGOING)
    private Set<Drug> recommandDrugs; // 推荐药品

    @Relationship(type = "common_drug", direction = Relationship.Direction.OUTGOING)
    private Set<Drug> commonDrugs; // 常用药品

    @Relationship(type = "no_eat", direction = Relationship.Direction.OUTGOING)
    private Set<Food> noEatFoods; // 忌吃食物

    @Relationship(type = "do_eat", direction = Relationship.Direction.OUTGOING)
    private Set<Food> doEatFoods; // 宜吃食物

    @Relationship(type = "recommand_eat", direction = Relationship.Direction.OUTGOING)
    private Set<Food> recommandEatFoods; // 推荐食谱

    @Relationship(type = "need_check", direction = Relationship.Direction.OUTGOING)
    private Set<Check> checks; // 诊断检查

    // Constructors
    public Disease() {}

    public Disease(String name) {
        this.name = name;
    }

    // Getters and Setters (与之前一致，这里省略，但请确保在您的代码中包含)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDesc() { return desc; }
    public void setDesc(String desc) { this.desc = desc; }
    public String getPrevent() { return prevent; }
    public void setPrevent(String prevent) { this.prevent = prevent; }
    public String getCause() { return cause; }
    public void setCause(String cause) { this.cause = cause; }
    public String getEasy_get() { return easy_get; }
    public void setEasy_get(String easy_get) { this.easy_get = easy_get; }
    public String getCure_lasttime() { return cure_lasttime; }
    public void setCure_lasttime(String cure_lasttime) { this.cure_lasttime = cure_lasttime; }
    public String getCured_prob() { return cured_prob; }
    public void setCured_prob(String cured_prob) { this.cured_prob = cured_prob; }
    public List<String> getCure_way() { return cure_way; }
    public void setCure_way(List<String> cure_way) { this.cure_way = cure_way; }
    public Set<Symptom> getSymptoms() { return symptoms; }
    public void setSymptoms(Set<Symptom> symptoms) { this.symptoms = symptoms; }
    public Set<Disease> getAcompanyDiseases() { return acompanyDiseases; }
    public void setAcompanyDiseases(Set<Disease> acompanyDiseases) { this.acompanyDiseases = acompanyDiseases; }
    public Set<Department> getDepartments() { return departments; }
    public void setDepartments(Set<Department> departments) { this.departments = departments; }
    public Set<Drug> getRecommandDrugs() { return recommandDrugs; }
    public void setRecommandDrugs(Set<Drug> recommandDrugs) { this.recommandDrugs = recommandDrugs; }
    public Set<Drug> getCommonDrugs() { return commonDrugs; }
    public void setCommonDrugs(Set<Drug> commonDrugs) { this.commonDrugs = commonDrugs; }
    public Set<Food> getNoEatFoods() { return noEatFoods; }
    public void setNoEatFoods(Set<Food> noEatFoods) { this.noEatFoods = noEatFoods; }
    public Set<Food> getDoEatFoods() { return doEatFoods; }
    public void setDoEatFoods(Set<Food> doEatFoods) { this.doEatFoods = doEatFoods; }
    public Set<Food> getRecommandEatFoods() { return recommandEatFoods; }
    public void setRecommandEatFoods(Set<Food> recommandEatFoods) { this.recommandEatFoods = recommandEatFoods; }
    public Set<Check> getChecks() { return checks; }
    public void setChecks(Set<Check> checks) { this.checks = checks; }
}