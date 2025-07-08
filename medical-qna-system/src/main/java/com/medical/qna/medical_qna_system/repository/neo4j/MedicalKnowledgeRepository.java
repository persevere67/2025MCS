package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.entity.neo4j.Symptom;
import com.medical.qna.medical_qna_system.entity.neo4j.Drug;
import com.medical.qna.medical_qna_system.entity.neo4j.Department;
import com.medical.qna.medical_qna_system.entity.neo4j.Food;
import com.medical.qna.medical_qna_system.entity.neo4j.Check;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface MedicalKnowledgeRepository extends Neo4jRepository<Disease, String> {
    
    // 通过疾病名称查找
    Disease findByName(String name);
    
    // 通过症状名称查找相关疾病
    @Query("MATCH (s:Symptom)<-[:HAS_SYMPTOM]-(d:Disease) WHERE s.name = $symptomName RETURN d")
    List<Disease> findBySymptomName(String symptomName);
    
    // 通过多个症状精确匹配疾病
    @Query("MATCH (d:Disease)-[:HAS_SYMPTOM]->(s:Symptom) " +
           "WHERE s.name IN $symptoms " +
           "WITH d, COUNT(s) AS matchedCount " +
           "WHERE matchedCount = SIZE($symptoms) " +
           "RETURN d")
    List<Disease> findByAllSymptoms(@Param("symptoms") Set<String> symptoms);
    
    // 查找并发症
    @Query("MATCH (d:Disease)-[:ACCOMPANY_WITH]->(ac:Disease) " +
           "WHERE d.name = $name RETURN ac")
    List<Disease> findComorbidities(String name);
    
    // 按科室查找疾病
    @Query("MATCH (d:Disease)-[:BELONG_TO]->(dept:Department) " +
           "WHERE dept.name = $department RETURN d")
    List<Disease> findByDepartment(String department);
    
    // 疾病名称模糊搜索
    @Query("MATCH (d:Disease) WHERE d.name CONTAINS $keyword RETURN d LIMIT 10")
    List<Disease> searchByDiseaseKeyword(String keyword);

    // 药品名称模糊搜索
    @Query("MATCH (d:Drug) WHERE d.name CONTAINS $keyword RETURN d LIMIT 10")
    List<Drug> searchsByDrugKeyword(String keyword);
    
    // 获取疾病的完整信息
    @Query("MATCH (d:Disease {name: $name}) " +
           "OPTIONAL MATCH (d)-[:HAS_SYMPTOM]->(s:Symptom) " +
           "OPTIONAL MATCH (d)-[:BELONG_TO]->(dept:Department) " +
           "OPTIONAL MATCH (d)-[:RECOMMEND_DRUG|:COMMON_DRUG]->(dr:Drug) " +
           "OPTIONAL MATCH (d)-[:DO_EAT|:NO_EAT|:RECOMMEND_EAT]->(f:Food) " +
           "OPTIONAL MATCH (d)-[:NEED_CHECK]->(c:Check) " +
           "RETURN d AS disease, " +
           "COLLECT(DISTINCT s) AS symptoms, " +
           "COLLECT(DISTINCT dept) AS departments, " +
           "COLLECT(DISTINCT dr) AS recommendDrugs, " +
           "COLLECT(DISTINCT f) AS foods, " +
           "COLLECT(DISTINCT c) AS checks")
    DiseaseDetailDTO getDiseaseDetails(@Param("name") String diseaseName);
    
    // DTO接口：疾病详细信息
    public interface DiseaseDetailDTO {
        Disease getDisease();
        List<Symptom> getSymptoms();
        List<Department> getDepartments();
        List<Drug> getRecommendDrugs();
        List<Food> getFoods();
        List<Check> getChecks();
    }
}
