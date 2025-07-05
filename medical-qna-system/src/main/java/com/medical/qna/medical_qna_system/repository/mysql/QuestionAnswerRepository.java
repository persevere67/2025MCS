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
    
    /**
     * 按创建时间倒序查询用户的问答记录
     */
    List<QuestionAnswer> findByUserIdOrderByCreateAtDesc(Long userId);
    
    /**
     * 按关键词搜索用户的问答记录
     */
    List<QuestionAnswer> findByUserIdAndQuestionContaining(Long userId, String keyword);
    
    /**
     * 统计用户的问答记录数量
     */
    long countByUserId(Long userId);
    
    /**
     * 自定义删除语句 - 返回删除的行数
     */
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 自定义分页查询
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 查询用户最新的N条记录
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findTopNByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * 按答案内容搜索
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId AND qa.answer LIKE %:keyword% ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findByUserIdAndAnswerContaining(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    /**
     * 搜索问题或答案中包含关键词的记录
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId AND (qa.question LIKE %:keyword% OR qa.answer LIKE %:keyword%) ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findByUserIdAndQuestionOrAnswerContaining(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    /**
     * 查询指定时间范围内的问答记录
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId AND qa.createAt BETWEEN :startDate AND :endDate ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findByUserIdAndCreateAtBetween(@Param("userId") Long userId, 
                                                        @Param("startDate") java.time.LocalDateTime startDate, 
                                                        @Param("endDate") java.time.LocalDateTime endDate);
    
    /**
     * 批量删除指定ID的记录（需要属于指定用户）
     */
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.id IN :ids AND qa.user.id = :userId")
    int deleteByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}