package com.medical.qna.medical_qna_system.entity.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_answers")
@Data
@Builder // Lombok Builder 会根据字段名生成方法，例如 createAt()
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;
    
    @Column(name = "create_at") // 确保数据库列名是 create_at
    private LocalDateTime createAt; // 时间戳字段名为 createAt
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
