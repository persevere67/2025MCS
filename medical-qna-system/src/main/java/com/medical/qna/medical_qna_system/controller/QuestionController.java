package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.AnswerResponse; // 导入 AnswerResponse
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult; // 导入语义理解结果DTO
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.service.QuestionService; // 假设QuestionService现在只处理数据库操作
import com.medical.qna.medical_qna_system.service.semantic.SemanticUnderstandingService; // 导入语义理解服务

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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

// ObjectMapper 用于将推荐问题列表序列化为 JSON 字符串
import com.fasterxml.jackson.databind.ObjectMapper;


@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
public class QuestionController {

    private final QuestionService questionService;
    private final SemanticUnderstandingService semanticUnderstandingService; // 注入语义理解服务
    private final WebClient webClient; // 用于调用RAG回答模型
    private final ObjectMapper objectMapper; // 用于序列化推荐问题列表

    // --- Python 服务配置 ---
    // Python AC 自动机服务 (app.py)
    private static final String AC_AUTOMATON_API_URL = "http://localhost:5000/process_query";
    private static final String AC_AUTOMATON_HEALTH_URL = "http://localhost:5000/health"; // 假设app.py未来会添加/health端点

    // Python RAG 回答模型服务 (main.py)
    private static final String ANSWER_MODEL_API_URL = "http://localhost:8000/ask"; // RAG模型问答接口
    private static final String ANSWER_MODEL_HEALTH_URL = "http://localhost:8000/health"; // RAG模型健康检查接口

    private static final long SSE_TIMEOUT = 300_000L; // 5分钟超时

    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);


    /**
     * Spring Boot服务健康检查 - 不依赖Python服务
     */
    @GetMapping("/spring-health")
    public ResponseEntity<Map<String, Object>> springHealthCheck() {
        log.info("Spring Boot健康检查");

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "healthy");
        healthInfo.put("service", "Spring Boot Medical QNA");
        healthInfo.put("timestamp", System.currentTimeMillis());
        healthInfo.put("version", "1.0.0");
        healthInfo.put("environment", "development");

        return ResponseEntity.ok(healthInfo);
    }

    /**
     * 完整系统健康检查 - 包括Python AC自动机服务和RAG回答模型服务
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("收到完整健康检查请求");

        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("springBootService", "available");
        healthInfo.put("timestamp", System.currentTimeMillis());

        // 1. 检查 Python AC 自动机服务 (app.py)
        try {
            String acResponse = webClient.get()
                    .uri(AC_AUTOMATON_HEALTH_URL) // 尝试调用app.py的健康检查
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Python AC自动机服务响应: {}", acResponse);
            healthInfo.put("pythonAcService", "available");
            healthInfo.put("pythonAcResponse", acResponse);
        } catch (Exception e) {
            log.warn("Python AC自动机服务检查失败: {}", e.getMessage());
            healthInfo.put("pythonAcService", "unavailable");
            healthInfo.put("pythonAcError", e.getMessage());
        }

        // 2. 检查 Python RAG 回答模型服务 (main.py)
        try {
            String ragResponse = webClient.get()
                    .uri(ANSWER_MODEL_HEALTH_URL) // 调用RAG模型的健康检查
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Python RAG回答模型服务响应: {}", ragResponse);
            healthInfo.put("pythonRagService", "available");
            healthInfo.put("pythonRagResponse", ragResponse);
        } catch (Exception e) {
            log.warn("Python RAG回答模型服务检查失败: {}", e.getMessage());
            healthInfo.put("pythonRagService", "unavailable");
            healthInfo.put("pythonRagError", e.getMessage());
        }

        // 综合判断系统状态
        if (healthInfo.containsKey("pythonAcService") && "available".equals(healthInfo.get("pythonAcService")) &&
            healthInfo.containsKey("pythonRagService") && "available".equals(healthInfo.get("pythonRagService"))) {
            healthInfo.put("status", "healthy");
        } else {
            healthInfo.put("status", "degraded");
        }

        return ResponseEntity.ok(healthInfo);
    }

    /**
     * 主要的问答接口 - 使用SSE流式响应
     * 流程：
     * 1. 调用AC自动机服务获取语义理解结果和推荐问题。
     * 2. 调用RAG回答模型服务获取流式答案。
     * 3. 将流式答案和推荐问题通过SSE发送给前端。
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<String>> saveQuestionAnswer(@Valid @RequestBody QuestionRequest request,
                                  HttpServletRequest httpRequest) {

        log.info("保存问答记录: {}", request.getQuestion());
        
        // 从JWT token中获取用户信息
        User user = (User) httpRequest.getAttribute("currentUser");

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
            List<QuestionAnswerDto> history = questionService.getUserHistory(user.getId());

            stats.put("totalQuestions", history.size());
            // 修复：使用 createAt 而不是 getCreateTime()
            stats.put("lastQuestionTime", history.isEmpty() ? null : history.get(0).getCreateAt());
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

    /**
     * 测试Python RAG服务连接 (这里的RAG指的是您的第二个Python服务，即回答模型)
     * 注意：这个方法需要更新 PYTHON_API_URL 和 PYTHON_HEALTH_URL 到您的第二个Python服务的地址
     */
    @GetMapping("/test-python")
    public ResponseEntity<Map<String, Object>> testPythonConnection() {
        log.info("测试Python回答模型服务连接");

        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());

        // *** 这里的 PYTHON_API_URL 和 PYTHON_HEALTH_URL 需要指向您的第二个Python服务 ***
        // 假设您的回答模型服务运行在 http://localhost:8000，并且有一个 /health 端点
        String answerModelHealthUrl = "http://localhost:8000/health"; // 假设回答模型有健康检查
        String answerModelApiUrl = "http://localhost:8000/ask"; // 假设回答模型有问答API

        try {
            // 测试Python回答模型服务健康检查
            String healthResponse = webClient.get()
                    .uri(answerModelHealthUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            result.put("health", "success");
            result.put("healthResponse", healthResponse);

            // 测试问答接口
            String testResponse = webClient.post()
                    .uri(answerModelApiUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("question", "测试连接")) // 假设回答模型期望 "question" 键
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            result.put("ask", "success");
            result.put("askResponse", testResponse);
            result.put("status", "回答模型连接正常");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Python回答模型服务连接测试失败", e);
            result.put("status", "连接失败");
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
    }
}
