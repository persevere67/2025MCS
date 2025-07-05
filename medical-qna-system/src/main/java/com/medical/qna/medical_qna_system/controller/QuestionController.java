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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
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
    @PostMapping("/ask")
    public SseEmitter askQuestion(@Valid @RequestBody QuestionRequest request,
                                  HttpServletRequest httpRequest) {

        log.info("收到问题: {}", request.getQuestion());

        User user = (User) httpRequest.getAttribute("currentUser");
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        if (user == null) {
            log.warn("用户未认证或未提供有效token，但该端点允许匿名访问。");
        } else {
            log.info("用户 {} 请求问答服务", user.getUsername());
        }

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            sendErrorAndComplete(emitter, "问题内容不能为空");
            return emitter;
        }

        startHeartbeat(emitter);

        CompletableFuture.runAsync(() -> {
            AtomicReference<StringBuilder> fullAnswer = new AtomicReference<>(new StringBuilder());
            try {
                emitter.send(SseEmitter.event().name("start").data("AI助手正在思考您的问题..."));

                // --- 步骤 1: 调用语义理解服务 (AC 自动机) ---
                log.info("开始调用Python AC自动机服务进行语义理解");
                SemanticUnderstandingResult semanticResult = semanticUnderstandingService.analyze(request.getQuestion());

                if (semanticResult == null || semanticResult.getApiResponse() == null || semanticResult.getApiResponse().getCode() != 200) {
                    String errorMessage = "语义理解服务出错: " + (semanticResult != null && semanticResult.getApiResponse() != null ? semanticResult.getApiResponse().getMessage() : "未知错误");
                    log.error(errorMessage);
                    sendErrorAndComplete(emitter, errorMessage);
                    return;
                }
                log.info("语义理解结果: 意图={}, 关键词={}", semanticResult.getIdentifiedIntent(), semanticResult.getIdentifiedKeywords());


                // --- 步骤 2: 调用 RAG 回答模型服务并流式处理答案 ---
                log.info("开始调用Python RAG回答模型服务");
                Flux<String> answerFlux = webClient.post()
                        .uri(ANSWER_MODEL_API_URL) // 调用RAG模型的/ask接口
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("question", request.getQuestion())) // RAG模型期望 "question" 键
                        .retrieve()
                        .bodyToFlux(String.class) // RAG模型返回的是文本流
                        .timeout(Duration.ofMinutes(5)) // 5分钟超时
                        .onErrorResume(WebClientResponseException.class, ex -> {
                            log.error("调用Python RAG回答模型API失败: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                            return Flux.just("抱歉，回答模型服务暂时不可用，请稍后重试。错误代码：" + ex.getStatusCode().value());
                        })
                        .onErrorResume(Exception.class, ex -> {
                            log.error("调用Python RAG回答模型API时发生未知错误", ex);
                            return Flux.just("抱歉，处理您的问题时发生错误，请稍后重试。");
                        });

                // 处理流式响应，并发送给前端
                answerFlux
                    .doOnNext(chunk -> {
                        try {
                            if (chunk != null && !chunk.isEmpty()) {
                                fullAnswer.get().append(chunk);
                                emitter.send(SseEmitter.event().name("data").data(chunk));
                                log.debug("发送数据块: {}", chunk.substring(0, Math.min(chunk.length(), 50)) + "...");
                            }
                        } catch (IOException e) {
                            log.error("发送数据块失败", e);
                            throw new RuntimeException(e);
                        }
                    })
                    .doOnError(error -> {
                        log.error("RAG回答模型流处理发生错误", error);
                        sendErrorAndComplete(emitter, "获取答案时发生错误: " + error.getMessage());
                    })
                    .doOnComplete(() -> {
                        try {
                            String completeAnswer = fullAnswer.get().toString();
                            log.info("答案接收完成，总长度: {}", completeAnswer.length());

                            if (user != null && !completeAnswer.isEmpty()) { // 只有在用户存在且有答案时才保存
                                // 保存到数据库
                                questionService.saveQuestionAnswer(
                                        user.getId(),
                                        request.getQuestion(),
                                        completeAnswer
                                );
                                log.info("问答记录已保存到数据库，用户: {}", user.getUsername());
                            } else if (user == null) {
                                log.warn("匿名用户问答，不保存历史记录。");
                            }

                            // --- 步骤 3: 发送推荐问题 (从语义理解结果中获取) ---
                            if (semanticResult.getRecommendedQuestions() != null && !semanticResult.getRecommendedQuestions().isEmpty()) {
                                try {
                                    // 将 List<RecommendedQuestion> 序列化为 JSON 字符串发送
                                    String recommendedQuestionsJson = objectMapper.writeValueAsString(semanticResult.getRecommendedQuestions());
                                    emitter.send(SseEmitter.event().name("recommendedQuestions").data(recommendedQuestionsJson));
                                    log.info("发送推荐问题: {}", recommendedQuestionsJson.substring(0, Math.min(recommendedQuestionsJson.length(), 100)) + "...");
                                } catch (IOException e) {
                                    log.error("序列化或发送推荐问题失败", e);
                                    sendErrorAndComplete(emitter, "发送推荐问题失败");
                                }
                            }

                            // 发送完成事件
                            emitter.send(SseEmitter.event().name("complete").data("回答完成"));
                            emitter.complete();
                            log.info("SSE连接正常完成，用户: {}", user != null ? user.getUsername() : "匿名");

                        } catch (Exception e) {
                            log.error("完成处理时发生错误", e);
                            sendErrorAndComplete(emitter, "保存答案或发送完成事件时出错");
                        }
                    })
                    .subscribe(); // 启动流处理

            } catch (Exception e) {
                log.error("处理问题时发生异常", e);
                sendErrorAndComplete(emitter, "处理问题时发生错误: " + e.getMessage());
            }
        }, Executors.newCachedThreadPool()); // 使用一个线程池来异步执行，避免阻塞主线程

        return emitter;
    }

    /**
     * 启动SSE心跳机制
     */
    private void startHeartbeat(SseEmitter emitter) {
        ScheduledFuture<?> heartbeatFuture = heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
                log.debug("发送心跳");
            } catch (Exception e) {
                log.debug("心跳发送失败，可能连接已关闭: {}", e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS); // 30秒心跳间隔

        // 连接结束时取消心跳
        emitter.onCompletion(() -> {
            heartbeatFuture.cancel(true);
            log.debug("心跳任务已取消 - 连接完成");
        });
        emitter.onTimeout(() -> {
            heartbeatFuture.cancel(true);
            log.warn("SSE连接超时，心跳任务已取消");
        });
        emitter.onError(ex -> {
            heartbeatFuture.cancel(true);
            log.error("SSE连接发生错误，心跳任务已取消", ex);
        });
    }

    /**
     * 发送错误消息并完成SSE连接
     */
    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message));
            emitter.complete();
            log.info("已发送错误消息并完成连接: {}", message);
        } catch (Exception ex) {
            log.error("发送错误消息失败", ex);
            emitter.completeWithError(ex);
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
            // 获取用户问答统计
            Map<String, Object> stats = new HashMap<>();
            List<QuestionAnswerDto> history = questionService.getUserHistory(user.getId());

            stats.put("totalQuestions", history.size());
            // 修复：使用 createAt 而不是 getCreateTime()
            stats.put("lastQuestionTime", history.isEmpty() ? null : history.get(0).getCreateAt());
            stats.put("username", user.getUsername());
            stats.put("userRole", user.getRole());

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
