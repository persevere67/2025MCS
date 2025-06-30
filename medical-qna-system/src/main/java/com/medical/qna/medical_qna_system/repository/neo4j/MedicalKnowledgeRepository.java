package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalKnowledgeRepository extends Neo4jRepository<Disease, Long> {
    
    // 根据疾病名称查找疾病
    @Query("MATCH (d:Disease) WHERE d.name CONTAINS $diseaseName RETURN d")
    List<Disease> findDiseasesByName(@Param("diseaseName") String diseaseName);
    
    // 根据症状查找疾病
    @Query("MATCH (d:Disease)-[:HAS_SYMPTOM]->(s:Symptom) WHERE s.name CONTAINS $symptomName RETURN d")
    List<Disease> findDiseasesBySymptom(@Param("symptomName") String symptomName);
    
    // 查找疾病的所有症状
    @Query("MATCH (d:Disease)-[:HAS_SYMPTOM]->(s:Symptom) WHERE d.name = $diseaseName RETURN s")
    List<Object> findSymptomsByDisease(@Param("diseaseName") String diseaseName);
    
    // 查找疾病的治疗方法
    @Query("MATCH (d:Disease)-[:TREATED_BY]->(t:Treatment) WHERE d.name = $diseaseName RETURN t")
    List<Object> findTreatmentsByDisease(@Param("diseaseName") String diseaseName);
    
    // 查找疾病的科室信息
    @Query("MATCH (d:Disease)-[:BELONGS_TO]->(dept:Department) WHERE d.name = $diseaseName RETURN dept")
    List<Object> findDepartmentsByDisease(@Param("diseaseName") String diseaseName);
    
    // 查找对疾病有益的食物
    @Query("MATCH (d:Disease)-[:GOOD_FOR]->(f:Food) WHERE d.name = $diseaseName RETURN f")
    List<Object> findGoodFoodsByDisease(@Param("diseaseName") String diseaseName);
    
    // 查找对疾病有害的食物
    @Query("MATCH (d:Disease)-[:BAD_FOR]->(f:Food) WHERE d.name = $diseaseName RETURN f")
    List<Object> findBadFoodsByDisease(@Param("diseaseName") String diseaseName);
    
    // 查找疾病的并发症
    @Query("MATCH (d:Disease)-[:HAS_COMPLICATION]->(c:Complication) WHERE d.name = $diseaseName RETURN c")
    List<Object> findComplicationsByDisease(@Param("diseaseName") String diseaseName);
    
    // 综合查询：根据关键词查找相关疾病信息
    @Query("MATCH (d:Disease) WHERE d.name CONTAINS $keyword OR d.desc CONTAINS $keyword RETURN d LIMIT 10")
    List<Disease> findDiseasesByKeyword(@Param("keyword") String keyword);
    
    // 根据多个症状查找可能的疾病
    @Query("MATCH (d:Disease)-[:HAS_SYMPTOM]->(s:Symptom) " +
           "WHERE s.name IN $symptoms " +
           "WITH d, count(s) as symptomCount " +
           "ORDER BY symptomCount DESC " +
           "RETURN d LIMIT 10")
    List<Disease> findDiseasesByMultipleSymptoms(@Param("symptoms") List<String> symptoms);
}
