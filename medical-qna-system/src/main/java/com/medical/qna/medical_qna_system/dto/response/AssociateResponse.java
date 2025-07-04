// src/main/java/com/medical/qna/medical_qna_system/dto/response/AssociateResponse.java
package com.medical.qna.medical_qna_system.dto.response;

import java.util.List;

/**
 * 联想问题响应DTO。
 * 包含根据用户原始问题联想到的相关问题列表。
 */
public class AssociateResponse {
    private List<String> associatedQuestions;

    public AssociateResponse() {
    }

    public AssociateResponse(List<String> associatedQuestions) {
        this.associatedQuestions = associatedQuestions;
    }

    public List<String> getAssociatedQuestions() {
        return associatedQuestions;
    }

    public void setAssociatedQuestions(List<String> associatedQuestions) {
        this.associatedQuestions = associatedQuestions;
    }
}