// src/main/java/com/medical/qna/medical_qna_system/service/association/QuestionAssociationService.java
package com.medical.qna.medical_qna_system.service.association;

import com.medical.qna.medical_qna_system.dto.SemanticUnderstandingResult;

import java.util.List;

/**
 * 问题联想服务接口。
 * 负责根据语义理解结果生成用户可能感兴趣的联想问题。
 */
public interface QuestionAssociationService {

    /**
     * 根据语义理解结果生成相关的联想问题。
     * @param understandingResult 原始问题的语义理解结果
     * @return 联想问题列表
     */
    List<String> generateAssociatedQuestions(SemanticUnderstandingResult understandingResult);
}