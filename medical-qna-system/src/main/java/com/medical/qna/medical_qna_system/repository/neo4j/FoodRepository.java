package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.entity.neo4j.Food;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface FoodRepository extends Neo4jRepository<Food, Long> {
    Optional<Food> findByName(String name);
    List<Food> findByNameContaining(String namePart);

    @Query("MATCH (d:Disease)-[:no_eat]->(f:Food) WHERE f.name = $foodName RETURN d")
    Set<Disease> findNoEatForDiseasesByFoodName(@Param("foodName") String foodName);

    @Query("MATCH (d:Disease)-[:do_eat]->(f:Food) WHERE f.name = $foodName RETURN d")
    Set<Disease> findDoEatForDiseadesByFoodName(@Param("foodName") String foodName);

    @Query("MATCH (d:Disease)-[:recommand_eat]->(f:Food) WHERE f.name = $foodName RETURN d")
    Set<Disease> findRecommandEatForDiseasesByFoodName(@Param("foodName") String foodName);
}