// src/main/java/com/medical/qna/medical_qna_system/service/kgqa/KgQaService.java
package com.medical.qna.medical_qna_system.service.kgqa;

import com.medical.qna.medical_qna_system.dto.AssociateResponse;
import com.medical.qna.medical_qna_system.dto.AnswerResponse;

/**
 * 知识图谱问答（KGQA）服务接口。
 * 作为知识图谱问答功能的入口，协调语义理解、联想和知识图谱问答的整个流程。
 */
public interface KgQaService {

    /**
     * 处理用户初始问题，进行语义理解并生成联想问题。
     * @param userQuestion 用户输入的原始问题
     * @return 包含联想问题的响应DTO
     */
    AssociateResponse processInitialQuestionForAssociation(String userQuestion);

    /**
     * 根据用户选择的联想问题（或原始问题），从知识图谱中获取并生成答案。
     * @param selectedQuestion 用户选择的问题内容
     * @return 包含答案的响应DTO
     */
    AnswerResponse getAnswerFromKnowledgeGraph(String selectedQuestion);
}