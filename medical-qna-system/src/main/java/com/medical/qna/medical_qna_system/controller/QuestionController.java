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

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
public class QuestionController {

    private final QuestionService questionService;
    private final WebClient webClient;
    private final ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2);
    
    private static final String PYTHON_API_URL = "http://localhost:8000/ask";
    private static final String PYTHON_HEALTH_URL = "http://localhost:8000/health";
    private static final long SSE_TIMEOUT = 300_000L; // 5分钟超时

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
     * 完整系统健康检查 - 包括Python RAG服务
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("收到完整健康检查请求");
        
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("springBootService", "available");
        healthInfo.put("timestamp", System.currentTimeMillis());
        
        try {
            // 检查Python RAG服务
            String response = webClient.get()
                    .uri(PYTHON_HEALTH_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            
            log.info("Python RAG服务响应: {}", response);
            
            healthInfo.put("status", "healthy");
            healthInfo.put("pythonService", "available");
            healthInfo.put("pythonResponse", response);
            
            return ResponseEntity.ok(healthInfo);
            
        } catch (Exception e) {
            log.warn("Python RAG服务检查失败: {}", e.getMessage());
            
            healthInfo.put("status", "degraded");
            healthInfo.put("pythonService", "unavailable");
            healthInfo.put("error", e.getMessage());
            
            // 即使Python服务不可用，Spring Boot服务仍然可用
            return ResponseEntity.ok(healthInfo);
        }
    }

    /**
     * 主要的问答接口 - 使用SSE流式响应
     */
    @PostMapping("/ask")
    public SseEmitter askQuestion(@Valid @RequestBody QuestionRequest request,
                                  HttpServletRequest httpRequest) {

        log.info("收到问题: {}", request.getQuestion());
        
        // 从JWT token中获取用户信息
        User user = (User) httpRequest.getAttribute("currentUser");
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 检查用户认证
        if (user == null) {
            log.warn("用户未认证，拒绝请求");
            sendErrorAndComplete(emitter, "用户未登录或认证已过期，请重新登录");
            return emitter;
        }

        log.info("用户 {} 请求问答服务", user.getUsername());

        // 验证问题内容
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            sendErrorAndComplete(emitter, "问题内容不能为空");
            return emitter;
        }

        // 启动心跳
        startHeartbeat(emitter);

        // 异步处理问题
        CompletableFuture.runAsync(() -> processQuestionAsync(request, user, emitter))
                .exceptionally(throwable -> {
                    log.error("异步处理问题时发生错误", throwable);
                    sendErrorAndComplete(emitter, "处理问题时发生内部错误");
                    return null;
                });

        return emitter;
    }

    /**
     * 异步处理问题的核心方法
     */
    private void processQuestionAsync(QuestionRequest request, User user, SseEmitter emitter) {
        AtomicReference<StringBuilder> fullAnswer = new AtomicReference<>(new StringBuilder());
        
        try {
            // 发送开始事件
            emitter.send(SseEmitter.event().name("start").data("AI助手正在思考您的问题..."));
            
            log.info("开始调用Python RAG API");
            
            // 调用Python RAG API
            Flux<String> responseFlux = webClient.post()
                    .uri(PYTHON_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("question", request.getQuestion()))
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMinutes(5)) // 5分钟超时
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        log.error("调用Python API失败: {} - {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                        return Flux.just("抱歉，AI服务暂时不可用，请稍后重试。错误代码：" + ex.getStatusCode().value());
                    })
                    .onErrorResume(Exception.class, ex -> {
                        log.error("调用Python API时发生未知错误", ex);
                        return Flux.just("抱歉，处理您的问题时发生错误，请稍后重试。");
                    });

            // 处理流式响应
            responseFlux
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
                        log.error("流处理发生错误", error);
                        sendErrorAndComplete(emitter, "获取答案时发生错误: " + error.getMessage());
                    })
                    .doOnComplete(() -> {
                        try {
                            String completeAnswer = fullAnswer.get().toString();
                            log.info("答案接收完成，总长度: {}", completeAnswer.length());
                            
                            if (!completeAnswer.isEmpty()) {
                                // 保存到数据库
                                questionService.saveQuestionAnswer(
                                        user.getId(),
                                        request.getQuestion(),
                                        completeAnswer
                                );
                                log.info("问答记录已保存到数据库，用户: {}", user.getUsername());
                            }
                            
                            // 发送完成事件
                            emitter.send(SseEmitter.event().name("complete").data("回答完成"));
                            emitter.complete();
                            log.info("SSE连接正常完成，用户: {}", user.getUsername());
                            
                        } catch (Exception e) {
                            log.error("完成处理时发生错误", e);
                            sendErrorAndComplete(emitter, "保存答案时出错");
                        }
                    })
                    .subscribe(); // 启动流处理
                    
        } catch (Exception e) {
            log.error("处理问题时发生异常", e);
            sendErrorAndComplete(emitter, "处理问题时发生错误: " + e.getMessage());
        }
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
     * 测试Python RAG服务连接
     */
    @GetMapping("/test-python")
    public ResponseEntity<Map<String, Object>> testPythonConnection() {
        log.info("测试Python RAG服务连接");
        
        Map<String, Object> result = new HashMap<>();
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 测试Python服务健康检查
            String healthResponse = webClient.get()
                    .uri(PYTHON_HEALTH_URL)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            result.put("health", "success");
            result.put("healthResponse", healthResponse);
            
            // 测试问答接口
            String testResponse = webClient.post()
                    .uri(PYTHON_API_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("question", "测试连接"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            result.put("ask", "success");
            result.put("askResponse", testResponse);
            result.put("status", "连接正常");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Python服务连接测试失败", e);
            result.put("status", "连接失败");
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
        }
    }
}