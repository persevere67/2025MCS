package com.medical.qna.medical_qna_system.repository.mysql;

import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page; // 导入 Page
import java.time.LocalDateTime; // 导入 LocalDateTime
import java.util.List;
import java.util.Optional; // 导入 Optional

@Repository
public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {
    
    /**
     * 按创建时间倒序查询用户的问答记录
     */
    List<QuestionAnswer> findByUserIdOrderByCreateAtDesc(Long userId);
    
    /**
     * 按关键词搜索用户的问答记录 (忽略大小写，并按创建时间倒序)
     * 修复：添加 IgnoreCase 和 OrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUserIdAndQuestionContainingIgnoreCaseOrderByCreateAtDesc(Long userId, String keyword);
    
    /**
     * 统计用户的问答记录数量
     */
    long countByUserId(Long userId);
    
    /**
     * 检查用户是否有问答记录
     * 新增此方法，用于 QuestionServiceImpl 中的 hasUserQuestions
     */
    boolean existsByUserId(Long userId);

    /**
     * 自定义删除语句 - 返回删除的行数
     */
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
    
    /**
     * 自定义分页查询 (返回 Page 对象，包含分页信息)
     * 修复：修改返回类型为 Page，以支持 QuestionServiceImpl 中的分页逻辑
     */
    Page<QuestionAnswer> findByUserIdOrderByCreateAtDesc(Long userId, Pageable pageable);

    // 移除 findRecentByUserId 和 findTopNByUserId，因为它们的功能可以通过 findByUserIdOrderByCreateAtDesc(userId, Pageable pageable) 实现
    // 例如，获取最近N条记录：questionAnswerRepository.findByUserIdOrderByCreateAtDesc(userId, PageRequest.of(0, N)).getContent();

    /**
     * 按答案内容搜索 (忽略大小写，并按创建时间倒序)
     * 修复：添加 IgnoreCase 和 OrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUserIdAndAnswerContainingIgnoreCaseOrderByCreateAtDesc(Long userId, String keyword);

    /**
     * 搜索问题或答案中包含关键词的记录 (忽略大小写，并按创建时间倒序)
     * 修复：添加 IgnoreCase 和 OrderByCreateAtDesc
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId AND (LOWER(qa.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(qa.answer) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findByUserIdAndQuestionOrAnswerContainingIgnoreCase(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    /**
     * 查询指定时间范围内的问答记录 (按创建时间倒序)
     * 修复：添加 OrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUserIdAndCreateAtBetweenOrderByCreateAtDesc(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 批量删除指定ID的记录（需要属于指定用户）
     */
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.id IN :ids AND qa.user.id = :userId")
    int deleteByIdsAndUserId(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 根据ID和用户ID查找单个记录
     * 新增此方法，用于 QuestionServiceImpl 中的 deleteQuestionAnswer
     */
    Optional<QuestionAnswer> findByIdAndUserId(Long id, Long userId);
}
