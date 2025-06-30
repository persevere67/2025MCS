package com.medical.qna.medical_qna_system.dto;


import java.time.LocalDateTime;

public class AnswerResponse {
    private String answer;
    private LocalDateTime timestamp;
    
    public AnswerResponse() {}
    
    public AnswerResponse(String answer, LocalDateTime timestamp) {
        this.answer = answer;
        this.timestamp = timestamp;
    }
    
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
