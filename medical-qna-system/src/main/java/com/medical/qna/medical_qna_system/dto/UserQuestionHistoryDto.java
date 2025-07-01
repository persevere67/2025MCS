package com.medical.qna.medical_qna_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuestionHistoryDto {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime userCreatedAt;
    private List<QuestionAnswerDto> questionAnswers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionAnswerDto {
        private Long id;
        private String question;
        private String answer;
        private LocalDateTime createdAt;
    }
}