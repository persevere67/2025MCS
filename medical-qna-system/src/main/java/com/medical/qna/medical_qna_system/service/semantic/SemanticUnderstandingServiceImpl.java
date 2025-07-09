package com.medical.qna.medical_qna_system.service.semantic;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult;

// 用于调用Python服务的WebClient
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException; // 用于更细致的错误处理
import reactor.core.publisher.Mono; // 用于WebClient的非阻塞式返回类型

import java.util.Map;
import java.util.HashMap;

@Service
@Slf4j // 用于日志输出
public class SemanticUnderstandingServiceImpl implements SemanticUnderstandingService {

    // 用于调用Python服务的WebClient
    private final WebClient webClient;

    // 构造函数注入WebClient.Builder
    // Spring Boot 会自动配置一个 WebClient.Builder bean
    public SemanticUnderstandingServiceImpl(WebClient.Builder webClientBuilder) {
        // Python服务的基础URL，确保与您的Flask服务地址匹配
        // 如果Flask服务在同一台机器上，通常是 http://localhost:5000
        this.webClient = webClientBuilder.baseUrl("http://localhost:5000").build(); 
    }

    @Override // 实现接口方法
    public SemanticUnderstandingResult analyze(String question) {
        log.info("Calling Flask service for semantic understanding with question: {}", question);

        // 构建请求体，匹配 Flask 服务的期望
        // 根据您app.py中的process_query路由，它期望一个包含 "queryText" 的JSON
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("queryText", question);
        // 如果您的Flask服务还需要 sessionId 和 userId，也请在这里添加
        // requestBody.put("sessionId", "some_session_id");
        // requestBody.put("userId", "some_user_id");

        try {
            // 发送 POST 请求并接收 SemanticUnderstandingResult
            // 使用 retrieve().bodyToMono().block() 进行阻塞式调用
            // 在生产环境中，更推荐使用非阻塞式的方式（例如直接返回 Mono<SemanticUnderstandingResult>）
            SemanticUnderstandingResult result = webClient.post()
                    .uri("/process_query") // Python Flask服务的具体API路径，根据app.py中定义的路由
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody) // 将Map作为JSON请求体发送
                    .retrieve()
                    .bodyToMono(SemanticUnderstandingResult.class)
                    .block(); // 阻塞等待响应

            if (result != null) {
                log.info("Successfully received semantic understanding result: {}", result);
                return result;
            } else {
                log.warn("Flask service returned an empty body for question: {}", question);
                // 返回一个带有错误信息的 SemanticUnderstandingResult
                SemanticUnderstandingResult errorResult = new SemanticUnderstandingResult();
                errorResult.setApiResponse(new SemanticUnderstandingResult.ApiResponse(500, "Flask服务返回空响应"));
                return errorResult;
            }
        } catch (WebClientResponseException e) {
            // 处理HTTP错误响应，例如4xx或5xx
            log.error("Flask service returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            SemanticUnderstandingResult errorResult = new SemanticUnderstandingResult();
            errorResult.setApiResponse(new SemanticUnderstandingResult.ApiResponse(e.getRawStatusCode(), "Flask服务HTTP错误: " + e.getResponseBodyAsString()));
            return errorResult;
        } catch (Exception e) {
            // 处理其他IO或网络错误
            log.error("Error calling Flask semantic understanding service for question: {}. Error: {}", question, e.getMessage(), e);
            SemanticUnderstandingResult errorResult = new SemanticUnderstandingResult();
            errorResult.setApiResponse(new SemanticUnderstandingResult.ApiResponse(500, "调用Flask服务失败: " + e.getMessage()));
            return errorResult;
        }
    }
}
