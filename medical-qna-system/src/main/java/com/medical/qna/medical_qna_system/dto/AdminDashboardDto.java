package com.medical.qna.medical_qna_system.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardDto {
    private long totalUsers;
    private long totalQuestions;
    private long questionsToday;
    private List<UserSummaryDto> recentActiveUsers;
    
    // Constructors, getters and setters
    public AdminDashboardDto() {}
    
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }
    
    public long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(long totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public long getQuestionsToday() { return questionsToday; }
    public void setQuestionsToday(long questionsToday) { this.questionsToday = questionsToday; }
    
    public List<UserSummaryDto> getRecentActiveUsers() { return recentActiveUsers; }
    public void setRecentActiveUsers(List<UserSummaryDto> recentActiveUsers) { this.recentActiveUsers = recentActiveUsers; }
    
    public static class UserSummaryDto {
        private Long userId;
        private String username;
        private long questionCount;
        private LocalDateTime lastQuestionTime;
        
        public UserSummaryDto() {}
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public long getQuestionCount() { return questionCount; }
        public void setQuestionCount(long questionCount) { this.questionCount = questionCount; }
        
        public LocalDateTime getLastQuestionTime() { return lastQuestionTime; }
        public void setLastQuestionTime(LocalDateTime lastQuestionTime) { this.lastQuestionTime = lastQuestionTime; }
    }
}