package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.response.AnswerResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto; // 确保导入
import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult;
import com.medical.qna.medical_qna_system.service.QuestionService;
import com.medical.qna.medical_qna_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux; // 用于流式响应
import reactor.core.publisher.Mono; // 用于非流式响应

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserService userService;


    // --- 以下是模拟的数据库操作方法，您需要根据实际情况填充 ---
    // 确保这些方法在您的实际项目中与数据库交互

    @Override
    public void saveQuestionAnswer(Long userId, String question, String answer) {
        try {
            User user = userService.getUserById(userId);
            if (user == null) {
                log.error("用户不存在: {}", userId);
                throw new RuntimeException("用户不存在");
            }
            
            QuestionAnswer qa = QuestionAnswer.builder()
                    .user(user)
                    .question(question)
                    .answer(answer)
                    .createAt(LocalDateTime.now())
                    .build();
            
            questionAnswerRepository.save(qa);
            log.info("问答记录保存成功: 用户={}, 问题长度={}, 答案长度={}", 
                    userId, question.length(), answer != null ? answer.length() : 0);
                    
        } catch (Exception e) {
            log.error("保存问答记录失败: userId={}", userId, e);
            throw new RuntimeException("保存问答记录失败", e);
        }
    }

    @Override
    public List<QuestionAnswerDto> getUserHistory(Long userId) {
        try {
            List<QuestionAnswer> history = questionAnswerRepository.findByUserIdOrderByCreateAtDesc(userId);
            List<QuestionAnswerDto> result = history.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt())
                            .build())
                    .collect(Collectors.toList());
                    
            log.info("获取用户历史记录成功: userId={}, 记录数={}", userId, result.size());
            return result;
            
        } catch (Exception e) {
            log.error("获取用户历史记录失败: userId={}", userId, e);
            throw new RuntimeException("获取历史记录失败", e);
        }
    }

    @Override
    public boolean deleteQuestionAnswer(Long id, Long userId) {
        try {
            Optional<QuestionAnswer> qaOpt = questionAnswerRepository.findById(id);
            if (qaOpt.isPresent() && qaOpt.get().getUser().getId().equals(userId)) {
                questionAnswerRepository.deleteById(id);
                log.info("删除问答记录成功: id={}, userId={}", id, userId);
                return true;
            }
            log.warn("删除问答记录失败: 记录不存在或无权限 id={}, userId={}", id, userId);
            return false;
        } catch (Exception e) {
            log.error("删除问答记录异常: id={}, userId={}", id, userId, e);
            return false;
        }
    }

    @Override
    public void clearUserHistory(Long userId) {
        try {
            // 使用自定义的批量删除方法，并记录删除的行数
            questionAnswerRepository.deleteByUserId(userId);
            // 注意：由于Repository中的方法返回void，我们无法获取删除的行数
            // 如果需要返回删除行数，可以修改Repository方法返回int
            log.info("清空用户历史记录成功: userId={}", userId);
        } catch (Exception e) {
            log.error("清空用户历史记录失败: userId={}", userId, e);
            throw new RuntimeException("清空历史记录失败", e);
        }
    }

    /**
     * 根据关键词搜索用户的问答记录
     * @param userId 用户ID
     * @param keyword 搜索关键词
     * @return 匹配的问答记录列表
     */
    @Transactional(readOnly = true)
    public List<QuestionAnswerDto> searchUserHistory(Long userId, String keyword) {
        try {
            List<QuestionAnswer> searchResults = questionAnswerRepository
                    .findByUserIdAndQuestionContaining(userId, keyword);
            
            List<QuestionAnswerDto> result = searchResults.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt())
                            .build())
                    .collect(Collectors.toList());
                    
            log.info("搜索用户历史记录成功: userId={}, keyword={}, 结果数={}", 
                    userId, keyword, result.size());
            return result;
            
        } catch (Exception e) {
            log.error("搜索用户历史记录失败: userId={}, keyword={}", userId, keyword, e);
            throw new RuntimeException("搜索历史记录失败", e);
        }
    }

    /**
     * 获取用户问答记录总数
     * @param userId 用户ID
     * @return 记录总数
     */
    @Override
    @Transactional(readOnly = true)
    public long getUserQuestionCount(Long userId) {
        try {
            long count = questionAnswerRepository.countByUserId(userId);
            log.info("获取用户问答记录总数: userId={}, count={}", userId, count);
            return count;
        } catch (Exception e) {
            log.error("获取用户问答记录总数失败: userId={}", userId, e);
            return 0;
        }
    }

    /**
     * 分页获取用户最近的问答记录
     * @param userId 用户ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 分页的问答记录列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<QuestionAnswerDto> getUserRecentHistory(Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            List<QuestionAnswer> recentHistory = questionAnswerRepository
                    .findRecentByUserId(userId, pageable);
            
            List<QuestionAnswerDto> result = recentHistory.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt())
                            .build())
                    .collect(Collectors.toList());
                    
            log.info("分页获取用户最近历史记录成功: userId={}, page={}, size={}, 结果数={}", 
                    userId, page, size, result.size());
            return result;
            
        } catch (Exception e) {
            log.error("分页获取用户最近历史记录失败: userId={}, page={}, size={}", 
                    userId, page, size, e);
            throw new RuntimeException("获取分页历史记录失败", e);
        }
    }

    /**
     * 批量删除用户的问答记录
     * @param ids 要删除的记录ID列表
     * @param userId 用户ID
     * @return 成功删除的记录数
     */
    @Transactional
    public int batchDeleteQuestionAnswers(List<Long> ids, Long userId) {
        try {
            int deletedCount = 0;
            for (Long id : ids) {
                if (deleteQuestionAnswer(id, userId)) {
                    deletedCount++;
                }
            }
            log.info("批量删除问答记录: userId={}, 请求删除={}条, 成功删除={}条", 
                    userId, ids.size(), deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("批量删除问答记录失败: userId={}, ids={}", userId, ids, e);
            throw new RuntimeException("批量删除失败", e);
        }
    }

    /**
     * 检查用户是否有问答记录
     * @param userId 用户ID
     * @return 是否有记录
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserQuestions(Long userId) {
        try {
            long count = questionAnswerRepository.countByUserId(userId);
            return count > 0;
        } catch (Exception e) {
            log.error("检查用户是否有问答记录失败: userId={}", userId, e);
            return false;
        }
    }

}