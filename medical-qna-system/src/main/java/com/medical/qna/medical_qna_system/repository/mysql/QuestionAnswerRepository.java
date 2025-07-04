package com.medical.qna.medical_qna_system.repository.mysql;

import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    
    List<QuestionAnswer> findByUserIdOrderByCreateAtDesc(Long userId);
    List<QuestionAnswer> findByUserIdAndQuestionContaining(Long userId, String keyword);
    long countByUserId(Long userId);
    
    // 自定义删除语句
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
    
    // 自定义分页查询
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}