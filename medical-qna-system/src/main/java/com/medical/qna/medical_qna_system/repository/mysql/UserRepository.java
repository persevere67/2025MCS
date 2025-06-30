package com.medical.qna.medical_qna_system.repository.mysql;

import com.medical.qna.medical_qna_system.enums.UserRole;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByRole(UserRole role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.role = 'USER' ORDER BY u.createdAt DESC")
    Page<User> findAllUsersOrderByCreatedAtDesc(Pageable pageable);
}