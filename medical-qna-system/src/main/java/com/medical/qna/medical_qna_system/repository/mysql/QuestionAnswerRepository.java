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
     * 修正方法名：findByUserIdOrderByCreateAtDesc -> findByUser_IdOrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUser_IdOrderByCreateAtDesc(Long userId);
    
    /**
     * 按关键词搜索用户的问答记录 (忽略大小写，并按创建时间倒序)
     * 修正方法名：findByUserIdAndQuestionContainingIgnoreCaseOrderByCreateAtDesc -> findByUser_IdAndQuestionContainingIgnoreCaseOrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUser_IdAndQuestionContainingIgnoreCaseOrderByCreateAtDesc(Long userId, String keyword);
    
    /**
     * 统计用户的问答记录数量
     * 修正方法名：countByUserId -> countByUser_Id
     */
    long countByUser_Id(Long userId);
    
    /**
     * 检查用户是否有问答记录
     * 修正方法名：existsByUserId -> existsByUser_Id
     */
    boolean existsByUser_Id(Long userId);

    /**
     * 自定义删除语句 - 返回删除的行数
     * @Query 注解中的 qa.user.id 保持不变，因为这与实体关联匹配
     * 修正方法名：deleteByUserId -> deleteByUser_Id
     */
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.user.id = :userId")
    int deleteByUser_Id(@Param("userId") Long userId);
    
    /**
     * 自定义分页查询 (返回 Page 对象，包含分页信息)
     * 修正方法名：findByUserIdOrderByCreateAtDesc -> findByUser_IdOrderByCreateAtDesc
     */
    Page<QuestionAnswer> findByUser_IdOrderByCreateAtDesc(Long userId, Pageable pageable);

    /**
     * 按答案内容搜索 (忽略大小写，并按创建时间倒序)
     * 修正方法名：findByUserIdAndAnswerContainingIgnoreCaseOrderByCreateAtDesc -> findByUser_IdAndAnswerContainingIgnoreCaseOrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUser_IdAndAnswerContainingIgnoreCaseOrderByCreateAtDesc(Long userId, String keyword);

    /**
     * 搜索问题或答案中包含关键词的记录 (忽略大小写，并按创建时间倒序)
     * @Query 注解中的 qa.user.id 保持不变，因为这与实体关联匹配
     * 修正方法名：findByUserIdAndQuestionOrAnswerContainingIgnoreCase -> findByUser_IdAndQuestionOrAnswerContainingIgnoreCase
     */
    @Query("SELECT qa FROM QuestionAnswer qa WHERE qa.user.id = :userId AND (LOWER(qa.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(qa.answer) LIKE LOWER(CONCAT('%', :keyword, '%'))) ORDER BY qa.createAt DESC")
    List<QuestionAnswer> findByUser_IdAndQuestionOrAnswerContainingIgnoreCase(@Param("userId") Long userId, @Param("keyword") String keyword);
    
    /**
     * 查询指定时间范围内的问答记录 (按创建时间倒序)
     * 修正方法名：findByUserIdAndCreateAtBetweenOrderByCreateAtDesc -> findByUser_IdAndCreateAtBetweenOrderByCreateAtDesc
     */
    List<QuestionAnswer> findByUser_IdAndCreateAtBetweenOrderByCreateAtDesc(Long userId, LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * 批量删除指定ID的记录（需要属于指定用户）
     * @Query 注解中的 qa.user.id 保持不变，因为这与实体关联匹配
     * 修正方法名：deleteByIdsAndUserId -> deleteByIdsAndUser_Id
     */
    @Modifying
    @Query("DELETE FROM QuestionAnswer qa WHERE qa.id IN :ids AND qa.user.id = :userId")
    int deleteByIdsAndUser_Id(@Param("ids") List<Long> ids, @Param("userId") Long userId);

    /**
     * 根据ID和用户ID查找单个记录
     * 修正方法名：findByIdAndUserId -> findByIdAndUser_Id
     */
    Optional<QuestionAnswer> findByIdAndUser_Id(Long id, Long userId);

    /**
     * 新增：按创建时间倒序查询所有问答记录（用于管理员功能）
     */
    Page<QuestionAnswer> findAllByOrderByCreateAtDesc(Pageable pageable);
}
