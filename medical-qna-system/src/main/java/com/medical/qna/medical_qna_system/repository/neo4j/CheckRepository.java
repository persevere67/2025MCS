package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Check;
import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CheckRepository extends Neo4jRepository<Check, Long> {
    Optional<Check> findByName(String name);
    List<Check> findByNameContaining(String namePart);

    @Query("MATCH (d:Disease)-[:need_check]->(c:Check) WHERE c.name = $checkName RETURN d")
    Set<Disease> findDiseasesRequiringCheckByCheckName(@Param("checkName") String checkName);
}