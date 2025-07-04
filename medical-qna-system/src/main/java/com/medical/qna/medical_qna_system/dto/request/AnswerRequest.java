//用于接收用户从联想问题列表中选择的问题，作为知识图谱查询的输入
// src/main/java/com/medical/qna/medical_qna_system/dto/request/AnswerRequest.java
package com.medical.qna.medical_qna_system.dto.request;

/**
 * 用户选择联想问题后，发送的请求DTO。
 * 包含用户最终选择的问题内容。
 */
public class AnswerRequest {
    private String selectedQuestion;

    public AnswerRequest() {
    }

    public AnswerRequest(String selectedQuestion) {
        this.selectedQuestion = selectedQuestion;
    }

    public String getSelectedQuestion() {
        return selectedQuestion;
    }

    public void setSelectedQuestion(String selectedQuestion) {
        this.selectedQuestion = selectedQuestion;
    }
}