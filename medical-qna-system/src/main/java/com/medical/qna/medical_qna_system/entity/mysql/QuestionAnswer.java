package com.medical.qna.medical_qna_system.entity.mysql;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_answers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 1000)
    private String question;
    
    @Column(nullable = false, length = 2000)
    private String answer;
    
    @CreationTimestamp
    @Column(name = "create_at", updatable = false)
    private LocalDateTime createAt;
    
    @Column(name = "rating")
    private Integer rating; // 1-5分评分
}