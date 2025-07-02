package com.medical.qna.medical_qna_system.repository.neo4j;

import com.medical.qna.medical_qna_system.entity.neo4j.Department;
import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface DepartmentRepository extends Neo4jRepository<Department, Long> {
    Optional<Department> findByName(String name);
    List<Department> findByNameContaining(String namePart);

    @Query("MATCH (d:Disease)-[:belongs_to]->(dept:Department) WHERE dept.name = $departmentName RETURN d")
    Set<Disease> findDiseasesBelongingHereByDepartmentName(@Param("departmentName") String departmentName);

    // 查询子科室：科室A包含科室B，则科室B的belongs_to关系指向科室A。这里是反向查询。
    @Query("MATCH (subDept:Department)-[:belongs_to]->(parentDept:Department) WHERE parentDept.name = $parentDepartmentName RETURN subDept")
    Set<Department> findSubDepartmentsByDepartmentName(@Param("parentDepartmentName") String parentDepartmentName);

    // 如果您的数据中还有父级科室指向子级科室的 believes_to 关系，可能还需要：
    // @Query("MATCH (parentDept:Department)-[:belongs_to]->(subDept:Department) WHERE subDept.name = $subDepartmentName RETURN parentDept")
    // Optional<Department> findParentDepartmentByDepartmentName(@Param("subDepartmentName") String subDepartmentName);
}