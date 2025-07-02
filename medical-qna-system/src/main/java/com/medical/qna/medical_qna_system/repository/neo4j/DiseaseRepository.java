package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Check;
import com.medical.qna.medical_qna_system.entity.neo4j.Department;
import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.entity.neo4j.Drug;
import com.medical.qna.medical_qna_system.entity.neo4j.Food;
import com.medical.qna.medical_qna_system.entity.neo4j.Symptom;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DiseaseRepository extends Neo4jRepository<Disease, Long> {

    Optional<Disease> findByName(String name);
    List<Disease> findByNameContaining(String namePart);

    @Query("MATCH (d:Disease)-[:has_symptom]->(s:Symptom) WHERE d.name = $diseaseName RETURN s")
    Set<Symptom> findSymptomsByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:common_drug]->(dr:Drug) WHERE d.name = $diseaseName RETURN dr")
    Set<Drug> findCommonDrugsByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:recommand_drug]->(dr:Drug) WHERE d.name = $diseaseName RETURN dr")
    Set<Drug> findRecommandDrugsByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:no_eat]->(f:Food) WHERE d.name = $diseaseName RETURN f")
    Set<Food> findNoEatFoodsByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:do_eat]->(f:Food) WHERE d.name = $diseaseName RETURN f")
    Set<Food> findDoEatFoodsByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:recommand_eat]->(f:Food) WHERE d.name = $diseaseName RETURN f")
    Set<Food> findRecommandEatFoodsByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:need_check]->(c:Check) WHERE d.name = $diseaseName RETURN c")
    Set<Check> findChecksByDiseaseName(@Param("diseaseName") String diseaseName);

    @Query("MATCH (d:Disease)-[:belongs_to]->(dept:Department) WHERE d.name = $diseaseName RETURN dept")
    Set<Department> findDepartmentsByDiseaseName(@Param("diseaseName") String diseaseName);

    // acompany_with 关系现在是 Disease 到 Disease
    @Query("MATCH (d1:Disease)-[:acompany_with]->(d2:Disease) WHERE d1.name = $diseaseName RETURN d2")
    Set<Disease> findAcompanyDiseasesByDiseaseName(@Param("diseaseName") String diseaseName);
}