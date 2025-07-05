package com.medical.qna.medical_qna_system.dto.response;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnswerResponse {
    private String answer;
    private LocalDateTime timestamp;
    private Long questionId;
    private String disclaimer = "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。";
}