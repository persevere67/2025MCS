package com.medical.qna.medical_qna_system.entity.mysql;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_answer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
  
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
  
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;
  
    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer;
  
    @CreationTimestamp
    @Column(name = "create_at")
    private LocalDateTime createAt;
}
