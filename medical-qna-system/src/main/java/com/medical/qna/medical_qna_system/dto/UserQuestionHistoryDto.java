package com.medical.qna.medical_qna_system.dto;


import java.time.LocalDateTime;
import java.util.List;

public class UserQuestionHistoryDto {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime userCreatedAt;
    private List<QuestionAnswerDto> questionAnswers;
    
    // Constructors, getters and setters
    public UserQuestionHistoryDto() {}
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public LocalDateTime getUserCreatedAt() { return userCreatedAt; }
    public void setUserCreatedAt(LocalDateTime userCreatedAt) { this.userCreatedAt = userCreatedAt; }
    
    public List<QuestionAnswerDto> getQuestionAnswers() { return questionAnswers; }
    public void setQuestionAnswers(List<QuestionAnswerDto> questionAnswers) { this.questionAnswers = questionAnswers; }
    
    public static class QuestionAnswerDto {
        private Long id;
        private String question;
        private String answer;
        private LocalDateTime createdAt;
        
        public QuestionAnswerDto() {}
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
