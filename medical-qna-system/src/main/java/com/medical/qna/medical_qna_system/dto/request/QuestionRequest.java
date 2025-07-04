package com.medical.qna.medical_qna_system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionRequest {
    
    @NotBlank(message = "问题内容不能为空")
    @Size(max = 1500, message = "问题内容不能超过1500个字符")
    private String question;
    
    // 答案字段，用于保存问答记录时使用
    private String answer;
}