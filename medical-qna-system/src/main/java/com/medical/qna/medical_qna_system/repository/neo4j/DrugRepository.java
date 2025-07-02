package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.entity.neo4j.Drug;
import com.medical.qna.medical_qna_system.entity.neo4j.Producer;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DrugRepository extends Neo4jRepository<Drug, Long> {
    Optional<Drug> findByName(String name);
    List<Drug> findByNameContaining(String namePart);

    @Query("MATCH (d:Disease)-[:common_drug]->(dr:Drug) WHERE dr.name = $drugName RETURN d")
    Set<Disease> findCommonForDiseasesByDrugName(@Param("drugName") String drugName);

    @Query("MATCH (d:Disease)-[:recommand_drug]->(dr:Drug) WHERE dr.name = $drugName RETURN d")
    Set<Disease> findRecommandForDiseasesByDrugName(@Param("drugName") String drugName);

    @Query("MATCH (p:Producer)-[:produces]->(dr:Drug) WHERE dr.name = $drugName RETURN p")
    Optional<Producer> findProducerByDrugName(@Param("drugName") String drugName); // 假设一个药品只有一个生产商
}