package com.medical.qna.medical_qna_system.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class QuestionRequest {
    @NotBlank(message = "问题不能为空")
    @Size(max = 1000, message = "问题长度不能超过1000个字符")
    private String question;
}