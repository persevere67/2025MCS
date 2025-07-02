package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.entity.neo4j.Symptom;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalKnowledgeRepository extends Neo4jRepository<Disease, String> {
  
    @Query("MATCH (d:Disease)-[:HAS_SYMPTOM]->(s:Symptom) WHERE s.name IN $symptoms " +
           "WITH d, COUNT(DISTINCT s) as matchCount " +
           "RETURN d ORDER BY matchCount DESC LIMIT 5")
    List<Disease> findDiseasesBySymptoms(@Param("symptoms") List<String> symptoms);
  
    @Query("MATCH (d:Disease) WHERE d.name CONTAINS $keyword OR d.desc CONTAINS $keyword RETURN d LIMIT 10")
    List<Disease> searchDiseases(@Param("keyword") String keyword);
  
    @Query("MATCH (d:Disease {name: $diseaseName})-[:HAS_SYMPTOM]->(s:Symptom) RETURN s")
    List<Symptom> findSymptomsByDisease(@Param("diseaseName") String diseaseName);
  
    @Query("MATCH (d:Disease {name: $diseaseName})-[:TREATED_BY]->(t:Treatment) RETURN t.name")
    List<String> findTreatmentsByDisease(@Param("diseaseName") String diseaseName);
  
    @Query("MATCH (d:Disease {name: $diseaseName})-[:BELONGS_TO]->(dept:Department)-[:BELONGS_TO_CATEGORY]->(cat:DepartmentCategory) " +
           "RETURN dept.name as department, cat.name as category")
    List<Object> findDepartmentInfoByDisease(@Param("diseaseName") String diseaseName);
  
    @Query("MATCH (d:Disease {name: $diseaseName})-[:GOOD_FOR]->(f:Food) RETURN f.name")
    List<String> findGoodFoodsByDisease(@Param("diseaseName") String diseaseName);
  
    @Query("MATCH (d:Disease {name: $diseaseName})-[:BAD_FOR]->(f:Food) RETURN f.name")
    List<String> findBadFoodsByDisease(@Param("diseaseName") String diseaseName);
  
    @Query("MATCH (d:Disease {name: $diseaseName})-[:HAS_COMPLICATION]->(c:Complication) RETURN c.name")
    List<String> findComplicationsByDisease(@Param("diseaseName") String diseaseName);
  
    @Query("MATCH (s:Symptom) WHERE s.name CONTAINS $keyword RETURN s LIMIT 20")
    List<Symptom> searchSymptoms(@Param("keyword") String keyword);
  
    Optional<Disease> findByName(String name);
}