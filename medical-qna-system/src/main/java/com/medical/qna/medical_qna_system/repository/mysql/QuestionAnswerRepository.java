package com.medical.qna.medical_qna_system.repository.mysql;


import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    List<QuestionAnswer> findByUserIdOrderByCreatedAtDesc(Long userId);
    Page<QuestionAnswer> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<QuestionAnswer> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    long countByCreatedAtAfter(LocalDateTime date);
    
    @Query("SELECT COUNT(qa) FROM QuestionAnswer qa WHERE DATE(qa.createdAt) = CURRENT_DATE")
    long countTodayQuestions();
    
    @Query("SELECT qa.user, COUNT(qa) as questionCount, MAX(qa.createdAt) as lastQuestionTime " +
           "FROM QuestionAnswer qa GROUP BY qa.user ORDER BY questionCount DESC")
    List<Object[]> findUserQuestionStats(Pageable pageable);
}
