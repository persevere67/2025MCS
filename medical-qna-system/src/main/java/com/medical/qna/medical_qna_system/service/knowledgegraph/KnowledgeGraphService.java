// src/main/java/com/medical/qna/medical_qna_system/service/knowledgegraph/KnowledgeGraphService.java
package com.medical.qna.medical_qna_system.service.knowledgegraph;

import com.medical.qna.medical_qna_system.dto.SemanticUnderstandingResult;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱服务接口。
 * 负责与Neo4j知识图谱进行交互，包括构建查询和执行查询。
 */
public interface KnowledgeGraphService {

    /**
     * 根据语义理解结果和用户/联想问题，构建Neo4j Cypher查询语句。
     * 这是将自然语言转化为图谱查询的关键逻辑。
     *
     * @param understandingResult 问题的语义理解结果
     * @param selectedQuestion 用户选择的完整问题字符串（用于辅助判断意图或填充模板）
     * @return 构建好的Cypher查询语句，如果无法构建则返回null
     */
    String buildGraphQuery(SemanticUnderstandingResult understandingResult, String selectedQuestion);

    /**
     * 执行给定的Cypher查询语句，并返回查询结果。
     *
     * @param cypherQuery 要执行的Cypher查询语句
     * @return 查询结果列表，每个Map代表一行记录，键为返回字段名，值为对应的属性值
     */
    List<Map<String, Object>> executeQuery(String cypherQuery);
}