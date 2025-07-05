package com.medical.qna.medical_qna_system.service.semantic;

import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult;

/**
 * 语义理解服务接口。
 * 负责对自然语言问题进行分词、命名实体识别、意图识别等。
 * 此接口的具体实现（如调用Python AC自动机服务）需要您根据实际情况完成。
 */
public interface SemanticUnderstandingService {

    /**
     * 对用户问题进行语义分析。
     * @param question 用户问题
     * @return 语义理解结果，包含实体、关键词、意图等
     */
    SemanticUnderstandingResult analyze(String question);
}
