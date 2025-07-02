package com.medical.qna.medical_qna_system.repository.mysql;


import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
  
    Page<QuestionAnswer> findByUserOrderByCreateAtDesc(User user, Pageable pageable);
  
    long countByCreateAtAfter(LocalDateTime dateTime);
  
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.question LIKE %:keyword% OR qa.answer LIKE %:keyword%")
    Page<QuestionAnswer> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
  
    List<QuestionAnswer> findTop10ByOrderByCreateAtDesc();
}