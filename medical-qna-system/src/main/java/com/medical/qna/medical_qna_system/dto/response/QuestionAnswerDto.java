package com.medical.qna.medical_qna_system.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionAnswerDto {
    private Long id;
    private String question;
    private String answer;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createAt;  // 与实体类保持一致
    
    // 添加兼容性方法，以防前端使用不同的字段名
    public LocalDateTime getCreateTime() {
        return this.createAt;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createAt = createTime;
    }

    public static QuestionAnswerDto create(String question, String answer) {
        return QuestionAnswerDto.builder()
                .question(question)
                .answer(answer)
                .build();
    }
}