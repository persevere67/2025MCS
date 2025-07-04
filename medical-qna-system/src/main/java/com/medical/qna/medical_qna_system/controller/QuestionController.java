package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 保存问答记录
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveQuestionAnswer(
            @Valid @RequestBody QuestionRequest request,
            HttpServletRequest httpRequest) {

        log.info("保存问答记录: {}", request.getQuestion());
        
        // 从JWT token中获取用户信息
        User user = (User) httpRequest.getAttribute("currentUser");

        // 检查用户认证
        if (user == null) {
            log.warn("用户未认证，拒绝请求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "用户未登录或认证已过期，请重新登录"));
        }

        // 验证问题内容
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", "问题内容不能为空"));
        }

        // 验证答案内容
        if (request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_INPUT", "答案内容不能为空"));
        }

        try {
            // 保存到数据库
            questionService.saveQuestionAnswer(
                    user.getId(),
                    request.getQuestion(),
                    request.getAnswer()
            );
            
            log.info("问答记录已保存到数据库，用户: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("保存成功"));
            
        } catch (Exception e) {
            log.error("保存问答记录时发生错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "保存失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户历史记录
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<QuestionAnswerDto>>> getHistory(HttpServletRequest request) {
        
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            log.warn("获取历史记录失败：用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "请重新登录"));
        }
        
        try {
            List<QuestionAnswerDto> history = questionService.getUserHistory(user.getId());
            log.info("获取用户 {} 的历史记录，共 {} 条", user.getUsername(), history.size());
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (Exception e) {
            log.error("获取历史记录失败，用户: {}", user.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "获取历史记录失败"));
        }
    }

    /**
     * 删除历史记录
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ApiResponse<String>> deleteHistory(
            @PathVariable Long id, HttpServletRequest request) {
            
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            log.warn("删除历史记录失败：用户未认证");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "请重新登录"));
        }
        
        try {
            boolean deleted = questionService.deleteQuestionAnswer(id, user.getId());
            if (deleted) {
                log.info("用户 {} 删除历史记录 {} 成功", user.getUsername(), id);
                return ResponseEntity.ok(ApiResponse.success("删除成功"));
            } else {
                log.warn("用户 {} 尝试删除不存在的历史记录 {}", user.getUsername(), id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("NOT_FOUND", "记录不存在或无权限"));
            }
        } catch (Exception e) {
            log.error("删除历史记录失败，用户: {}, 记录ID: {}", user.getUsername(), id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "删除失败"));
        }
    }

    /**
     * 获取问答统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(HttpServletRequest request) {
    
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "请重新登录"));
        }
    
        try {
            // 使用优化的方法获取统计信息
            long totalQuestions = questionService.getUserQuestionCount(user.getId());
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalQuestions", totalQuestions);
            stats.put("username", user.getUsername());
            stats.put("userRole", user.getRole());
            stats.put("hasQuestions", questionService.hasUserQuestions(user.getId()));
            
            // 如果有问答记录，获取最新的一条来显示最后提问时间
            if (totalQuestions > 0) {
                List<QuestionAnswerDto> recentHistory = questionService.getUserRecentHistory(user.getId(), 0, 1);
                if (!recentHistory.isEmpty()) {
                    stats.put("lastQuestionTime", recentHistory.get(0).getCreateAt());
                }
            } else {
                stats.put("lastQuestionTime", null);
            }
        
            log.info("获取用户 {} 的统计信息", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(stats));
        
        } catch (Exception e) {
            log.error("获取统计信息失败，用户: {}", user.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "获取统计信息失败"));
        }
    }

    /**
     * 清空用户历史记录
     */
    @DeleteMapping("/history")
    public ResponseEntity<ApiResponse<String>> clearHistory(HttpServletRequest request) {
        
        User user = (User) request.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "请重新登录"));
        }
        
        try {
            questionService.clearUserHistory(user.getId());
            log.info("用户 {} 清空历史记录成功", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success("历史记录已清空"));
            
        } catch (Exception e) {
            log.error("清空历史记录失败，用户: {}", user.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SERVER_ERROR", "清空历史记录失败"));
        }
    }
}