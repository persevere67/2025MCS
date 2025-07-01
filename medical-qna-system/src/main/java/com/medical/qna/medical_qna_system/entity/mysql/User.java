package com.medical.qna.medical_qna_system.entity.mysql;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medical.qna.medical_qna_system.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @JsonIgnore
    @Column(nullable = false)
    private String password;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;
    
    @CreationTimestamp
    @Column(name = "create_at", updatable = false)
    private LocalDateTime createAt;
    
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}