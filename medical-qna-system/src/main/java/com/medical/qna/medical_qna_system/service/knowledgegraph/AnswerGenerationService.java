// src/main/java/com/medical/qna/medical_qna_system/service/knowledgegraph/AnswerGenerationService.java
package com.medical.qna.medical_qna_system.service.knowledgegraph;

import java.util.List;
import java.util.Map;

/**
 * 答案生成服务接口。
 * 负责将知识图谱查询的原始结果转化为用户友好的自然语言答案。
 */
public interface AnswerGenerationService {

    /**
     * 根据原始问题和知识图谱查询结果，生成自然语言答案。
     *
     * @param originalQuestion 用户原始或联想的问题（用于判断答案的上下文）
     * @param kgResults 知识图谱查询的原始结果列表
     * @param understandingResult 原始问题的语义理解结果 (可选，用于更精细的答案生成)
     * @return 自然语言答案字符串
     */
    String generateAnswerFromKgResults(String originalQuestion, List<Map<String, Object>> kgResults,
                                       com.medical.qna.medical_qna_system.dto.SemanticUnderstandingResult understandingResult);
}