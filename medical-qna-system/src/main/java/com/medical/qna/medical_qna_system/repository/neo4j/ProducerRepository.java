package com.medical.qna.medical_qna_system.repository.neo4j;

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
public interface ProducerRepository extends Neo4jRepository<Producer, Long> {
    Optional<Producer> findByName(String name);
    List<Producer> findByNameContaining(String namePart);

    @Query("MATCH (p:Producer)-[:produces]->(dr:Drug) WHERE p.name = $producerName RETURN dr")
    Set<Drug> findProducedDrugsByProducerName(@Param("producerName") String producerName);
}