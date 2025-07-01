package com.medical.qna.medical_qna_system.entity.mysql;

import com.medical.qna.medical_qna_system.enums.UserRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
  
    @Column(unique = true, nullable = false, length = 50)
    private String username;
  
    @Column(nullable = false)
    private String password;
  
    @Column(unique = true, nullable = false, length = 100)
    private String email;
  
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.USER;
  
    @CreationTimestamp
    @Column(name = "create_at")
    private LocalDateTime createAt;
  
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<QuestionAnswer> questionAnswers;
}
