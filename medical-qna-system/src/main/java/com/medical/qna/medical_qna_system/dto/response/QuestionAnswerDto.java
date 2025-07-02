package com.medical.qna.medical_qna_system.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswerDto {
    private Long id;
    private String question;
    private String answer;
    private LocalDateTime timestamp;
    private String disclaimer;
    
    public static QuestionAnswerDto create(String question, String answer) {
        return QuestionAnswerDto.builder()
                .question(question)
                .answer(answer)
                .timestamp(LocalDateTime.now())
                .disclaimer("此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。")
                .build();
    }
}