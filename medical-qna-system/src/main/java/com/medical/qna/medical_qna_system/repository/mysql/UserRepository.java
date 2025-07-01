package com.medical.qna.medical_qna_system.repository.mysql;

import com.medical.qna.medical_qna_system.common.enums.UserRole;
import com.medical.qna.medical_qna_system.entity.mysql.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByRole(UserRole role);
}