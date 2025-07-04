// src/main/java/com/medical/qna/medical_qna_system/service/knowledgegraph/impl/KnowledgeGraphServiceImpl.java
package com.medical.qna.medical_qna_system.service.knowledgegraph.impl;

import com.medical.qna.medical_qna_system.dto.SemanticUnderstandingResult;
import com.medical.qna.medical_qna_system.service.knowledgegraph.KnowledgeGraphService;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 知识图谱服务实现类。
 * 负责与Neo4j知识图谱进行交互，包括构建查询和执行查询。
 */
@Service
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeGraphServiceImpl.class);

    private final Driver driver;

    @Autowired
    public KnowledgeGraphServiceImpl(Driver driver) {
        this.driver = driver;
    }

    /**
     * 根据语义理解结果和用户/联想问题，构建Neo4j Cypher查询语句。
     * 这是将自然语言转化为图谱查询的关键逻辑。
     *
     * @param understandingResult 问题的语义理解结果
     * @param selectedQuestion 用户选择的完整问题字符串（用于辅助判断意图或填充模板）
     * @return 构建好的Cypher查询语句，如果无法构建则返回null
     */
    @Override
    public String buildGraphQuery(SemanticUnderstandingResult understandingResult, String selectedQuestion) {
        if (understandingResult == null || understandingResult.getEntities() == null || understandingResult.getEntities().isEmpty()) {
            logger.warn("SemanticUnderstandingResult or entities are null/empty, cannot build query.");
            return null;
        }

        String intent = understandingResult.getIntent();
        Map<String, List<String>> entities = understandingResult.getEntities();

        String cypherQuery = null;

        // 获取识别出的疾病实体名称 (假设每个意图通常与一个主要实体相关)
        String diseaseName = entities.containsKey("DISEASE") && !entities.get("DISEASE").isEmpty() ? entities.get("DISEASE").get(0) : null;
        String symptomName = entities.containsKey("SYMPTOM") && !entities.get("SYMPTOM").isEmpty() ? entities.get("SYMPTOM").get(0) : null;
        String drugName = entities.containsKey("DRUG") && !entities.get("DRUG").isEmpty() ? entities.get("DRUG").get(0) : null;
        String departmentName = entities.containsKey("DEPARTMENT") && !entities.get("DEPARTMENT").isEmpty() ? entities.get("DEPARTMENT").get(0) : null;
        String checkName = entities.containsKey("CHECK") && !entities.get("CHECK").isEmpty() ? entities.get("CHECK").get(0) : null;
        String foodName = entities.containsKey("FOOD") && !entities.get("FOOD").isEmpty() ? entities.get("FOOD").get(0) : null;
        String producerName = entities.containsKey("PRODUCER") && !entities.get("PRODUCER").isEmpty() ? entities.get("PRODUCER").get(0) : null;


        // 根据意图和识别出的实体构建查询
        // 注意：这里的意图识别和实体映射需要NLP模块的准确支持
        if (intent != null) {
            switch (intent) {
                // ---------- 疾病相关查询 ----------
                case "查询症状":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:has_symptom]->(s:Symptom) WHERE d.name = '%s' RETURN s.name AS symptom", diseaseName);
                    }
                    break;
                case "查询治疗方法":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.cure_way AS cure_way", diseaseName);
                    }
                    break;
                case "查询疾病描述":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.desc AS desc", diseaseName);
                    }
                    break;
                case "查询预防方法":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.prevent AS prevent", diseaseName);
                    }
                    break;
                case "查询病因":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.cause AS cause", diseaseName);
                    }
                    break;
                case "查询易感人群":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.easy_get AS easy_get", diseaseName);
                    }
                    break;
                case "查询治疗持续时间":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.cure_lasttime AS cure_lasttime", diseaseName);
                    }
                    break;
                case "查询治愈概率":
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease) WHERE d.name = '%s' RETURN d.cured_prob AS cured_prob", diseaseName);
                    }
                    break;
                case "查询治疗药物": // 疾病对应的治疗药物
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:recommends_drug]->(dr:Drug) WHERE d.name = '%s' RETURN dr.name AS drug_name", diseaseName);
                    }
                    break;
                case "查询推荐检查": // 疾病对应的推荐检查
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:recommends_check]->(c:Check) WHERE d.name = '%s' RETURN c.name AS check_name", diseaseName);
                    }
                    break;
                case "查询所属科室": // 疾病所属科室
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:belongs_to_department]->(dep:Department) WHERE d.name = '%s' RETURN dep.name AS department_name", diseaseName);
                    }
                    break;
                case "查询忌食食物": // 疾病忌食的食物
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:avoid_food]->(f:Food) WHERE d.name = '%s' RETURN f.name AS food_name", diseaseName);
                    }
                    break;
                case "查询宜食食物": // 疾病宜食的食物
                    if (diseaseName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:eat_food]->(f:Food) WHERE d.name = '%s' RETURN f.name AS food_name", diseaseName);
                    }
                    break;
                // ---------- 症状相关查询 ----------
                case "查询导致疾病":
                    if (symptomName != null) {
                        cypherQuery = String.format("MATCH (s:Symptom)-[:causes_disease]->(d:Disease) WHERE s.name = '%s' RETURN d.name AS disease_name", symptomName);
                    }
                    break;
                case "查询症状相关疾病": // 同上，但可能用不同意图触发
                    if (symptomName != null) {
                        cypherQuery = String.format("MATCH (d:Disease)-[:has_symptom]->(s:Symptom) WHERE s.name = '%s' RETURN d.name AS disease_name", symptomName);
                    }
                    break;
                // ---------- 药物相关查询 ----------
                case "查询药物治疗疾病": // 药物治疗的疾病
                    if (drugName != null) {
                        cypherQuery = String.format("MATCH (dr:Drug)-[:cures]->(d:Disease) WHERE dr.name = '%s' RETURN d.name AS disease_name", drugName);
                    }
                    break;
                case "查询药物作用症状": // 药物作用于什么症状
                    if (drugName != null) {
                        cypherQuery = String.format("MATCH (dr:Drug)-[:acts_on_symptom]->(s:Symptom) WHERE dr.name = '%s' RETURN s.name AS symptom_name", drugName);
                    }
                    break;
                case "查询药物生产厂家":
                    if (drugName != null) {
                        cypherQuery = String.format("MATCH (dr:Drug)-[:produced_by]->(p:Producer) WHERE dr.name = '%s' RETURN p.name AS producer_name", drugName);
                    }
                    break;
                // ---------- 检查相关查询 ----------
                case "查询检查诊断疾病":
                    if (checkName != null) {
                        cypherQuery = String.format("MATCH (c:Check)-[:diagnoses]->(d:Disease) WHERE c.name = '%s' RETURN d.name AS disease_name", checkName);
                    }
                    break;
                // ---------- 科室相关查询 ----------
                case "查询科室擅长疾病":
                    if (departmentName != null) {
                        cypherQuery = String.format("MATCH (dep:Department)-[:specializes_in]->(d:Disease) WHERE dep.name = '%s' RETURN d.name AS disease_name", departmentName);
                    }
                    break;
                // ---------- 食物相关查询 ----------
                case "查询食物宜忌疾病": // 针对食物的，这个食物适合/不适合什么疾病
                    if (foodName != null) {
                        // 假设食物可能既宜食又忌食
                        if (selectedQuestion.contains("适合") || selectedQuestion.contains("宜")) {
                            cypherQuery = String.format("MATCH (f:Food)-[:eat_food]->(d:Disease) WHERE f.name = '%s' RETURN d.name AS disease_name", foodName);
                        } else if (selectedQuestion.contains("不适合") || selectedQuestion.contains("忌")) {
                            cypherQuery = String.format("MATCH (f:Food)-[:avoid_food]->(d:Disease) WHERE f.name = '%s' RETURN d.name AS disease_name", foodName);
                        }
                    }
                    break;
                // ... 可以根据需要添加更多意图和查询逻辑
                default:
                    logger.warn("Unknown intent: {}", intent);
                    break;
            }
        } else {
             // 如果没有明确的意图，尝试通过关键词或实体关系推断
             // 这是一个更复杂的部分，暂时留空或进行通用匹配
             logger.info("No specific intent recognized, trying to infer from entities and keywords for question: {}", selectedQuestion);
             // 简单的通用查询示例：查找与疾病相关的任何信息（可能返回太多）
             if (diseaseName != null) {
                 cypherQuery = String.format("MATCH (d:Disease)-[r]-(n) WHERE d.name = '%s' RETURN type(r) AS relationType, n.name AS relatedEntityName", diseaseName);
             }
        }

        logger.info("Generated Cypher Query for [{}]: {}", selectedQuestion, cypherQuery);
        return cypherQuery;
    }

    /**
     * 执行给定的Cypher查询语句，并返回查询结果。
     *
     * @param cypherQuery 要执行的Cypher查询语句
     * @return 查询结果列表，每个Map代表一行记录，键为返回字段名，值为对应的属性值
     */
    @Override
    public List<Map<String, Object>> executeQuery(String cypherQuery) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (cypherQuery == null || cypherQuery.trim().isEmpty()) {
            logger.warn("Empty or null Cypher query provided.");
            return results;
        }

        try (Session session = driver.session()) {
            Result result = session.run(cypherQuery);
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> recordMap = new HashMap<>();
                for (String key : record.keys()) {
                    Value value = record.get(key);
                    // 处理Neo4j的Value类型到Java基本类型或List
                    if (value.isList()) {
                        recordMap.put(key, value.asList()); // for d.cure_way
                    } else if (value.isMap()) {
                        recordMap.put(key, value.asMap());
                    }
                    else {
                        recordMap.put(key, value.asObject());
                    }
                }
                results.add(recordMap);
            }
        } catch (Exception e) {
            logger.error("Error executing Cypher query: {}", cypherQuery, e);
            // 可以在此处抛出自定义异常或进行更详细的错误处理
        }
        return results;
    }
}