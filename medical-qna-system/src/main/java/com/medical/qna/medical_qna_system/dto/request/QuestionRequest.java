package com.medical.qna.medical_qna_system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {
    @NotBlank(message = "问题不能为空")
    private String question;
}