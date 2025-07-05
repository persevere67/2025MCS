package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.response.AnswerResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto; // 确保导入
import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult;
import com.medical.qna.medical_qna_system.service.QuestionService;
import com.medical.qna.medical_qna_system.service.semantic.SemanticUnderstandingService;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.ArrayList; // 用于模拟历史记录
import java.util.function.Consumer; // 用于流式处理的回调函数

@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final SemanticUnderstandingService semanticUnderstandingService;
    private final WebClient answerModelWebClient; // 用于调用回答问题的大模型 (RAG)

    // --- Python RAG 回答模型服务 (main.py) 配置 ---
    private static final String ANSWER_MODEL_BASE_URL = "http://localhost:8000"; // RAG模型基础URL
    private static final String ANSWER_MODEL_ASK_PATH = "/ask"; // RAG模型问答接口路径

    @Autowired
    public QuestionServiceImpl(SemanticUnderstandingService semanticUnderstandingService, WebClient.Builder webClientBuilder) {
        this.semanticUnderstandingService = semanticUnderstandingService;
        // 使用传入的WebClient.Builder来构建针对回答模型的WebClient实例
        this.answerModelWebClient = webClientBuilder.baseUrl(ANSWER_MODEL_BASE_URL).build();
    }

    /**
     * 处理用户问题，进行语义理解并获取答案和推荐问题。
     * 此方法主要用于控制器中接收 QuestionRequest 并返回 AnswerResponse 的场景。
     */
    @Override
    public AnswerResponse processQuestion(QuestionRequest request) {
        String userQuestion = request.getQuestion();
        log.info("Processing user question (full response): {}", userQuestion);

        // 1. 调用语义理解服务（AC自动机 Python 服务）
        SemanticUnderstandingResult semanticResult = semanticUnderstandingService.analyze(userQuestion);

        if (semanticResult == null || semanticResult.getApiResponse() == null || semanticResult.getApiResponse().getCode() != 200) {
            log.warn("Semantic understanding failed for question: {}", userQuestion);
            return new AnswerResponse(
                "抱歉，我暂时无法理解您的问题，请换个说法试试。",
                LocalDateTime.now(),
                null, null, null,
                "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。",
                null
            );
        }

        // 2. 从语义理解结果中获取推荐问题
        List<SemanticUnderstandingResult.RecommendedQuestion> recommendedQuestions = semanticResult.getRecommendedQuestions();

        // 3. 调用 RAG 回答模型来生成完整答案 (这里调用的是内部的辅助方法)
        String answerContent = callAnswerModelBlocking(userQuestion);

        // 4. 构建并返回最终响应
        return new AnswerResponse(
            answerContent,
            LocalDateTime.now(),
            null, // questionId 暂时为null，如果需要从数据库获取
            null, // diseaseInfo 暂时为null，如果需要填充
            null, // recommendedDepartments 暂时为null，如果需要填充
            "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。",
            recommendedQuestions // 填充推荐问题
        );
    }

    /**
     * 处理问题并流式返回答案。
     * 此方法用于需要分块接收答案的场景，例如SSE。
     */
    @Override
    public void processQuestionWithStream(String question, Consumer<String> chunkConsumer) {
        log.info("Processing user question (streamed response): {}", question);

        // 1. (可选) 如果流式处理也需要语义理解，可以在这里调用
        // SemanticUnderstandingResult semanticResult = semanticUnderstandingService.analyze(question);
        // log.info("Semantic understanding for stream: {}", semanticResult);

        // 2. 调用 RAG 回答模型服务并流式处理答案
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question", question);

            answerModelWebClient.post()
                    .uri(ANSWER_MODEL_ASK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class) // RAG模型返回的是文本流
                    .timeout(Duration.ofMinutes(5))
                    .doOnNext(chunkConsumer) // 将每个数据块传递给Consumer
                    .doOnError(error -> log.error("RAG answer model stream error: {}", error.getMessage(), error))
                    .doOnComplete(() -> log.info("RAG answer model stream completed for question: {}", question))
                    .blockLast(); // 阻塞直到流完成，确保Consumer处理完所有数据
                                 // 注意：在Webflux/Spring WebFlux Controller中通常不会blockLast()，
                                 // 但这里作为Service层方法，如果Controller是SSE，则需要确保流被消费。
        } catch (WebClientResponseException e) {
            log.error("RAG answer model stream returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            chunkConsumer.accept("回答模型服务错误: " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling RAG answer model for streaming: {}", e.getMessage(), e);
            chunkConsumer.accept("调用回答模型服务失败，请稍后重试。");
        }
    }

    /**
     * 处理问题并返回完整答案。
     * 此方法用于不需要流式处理，直接返回完整答案的场景。
     * (这是您原始接口中定义的重载方法)
     */
    @Override
    public String processQuestion(String question) {
        log.info("Processing user question (string response): {}", question);
        // 直接调用RAG模型获取完整答案
        return callAnswerModelBlocking(question);
    }

    /**
     * 内部辅助方法：阻塞式调用RAG回答模型获取完整答案
     * @param question 用户问题
     * @return 大模型生成的完整答案字符串
     */
    private String callAnswerModelBlocking(String question) {
        log.info("Calling RAG answer model (blocking) for question: {}", question);

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question", question);

            // 发送 POST 请求并接收答案
            // RAG模型返回的是流式文本，但在这里我们将其聚合为单个字符串接收
            String answer = answerModelWebClient.post()
                    .uri(ANSWER_MODEL_ASK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class) // 将整个流聚合为一个String
                    .timeout(Duration.ofSeconds(60)) // 适当延长超时时间
                    .block(); // 阻塞等待响应

            if (answer != null && !answer.isEmpty()) {
                log.info("Received answer from RAG model (blocking): {}", answer.substring(0, Math.min(answer.length(), 100)) + "...");
                return answer;
            } else {
                log.warn("RAG answer model returned empty or null answer (blocking) for question: {}", question);
                return "抱歉，回答模型未能生成有效答案。";
            }
        } catch (WebClientResponseException e) {
            log.error("RAG answer model returned error status (blocking) {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "回答模型服务错误: " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString();
        } catch (Exception e) {
            log.error("Error calling RAG answer model (blocking) for question: {}. Error: {}", question, e.getMessage(), e);
            return "调用回答模型服务失败，请稍后重试。";
        }
    }


    // --- 以下是模拟的数据库操作方法，您需要根据实际情况填充 ---
    // 确保这些方法在您的实际项目中与数据库交互

    @Override
    public void saveQuestionAnswer(Long userId, String question, String answer) {
        log.info("模拟保存问答记录：用户ID={}, 问题='{}', 答案='{}'", userId, question, answer);
        // TODO: 在这里实现实际的数据库保存逻辑
        // 例如：questionAnswerRepository.save(new QuestionAnswerEntity(userId, question, answer, LocalDateTime.now()));
    }

    @Override
    public List<QuestionAnswerDto> getUserHistory(Long userId) {
        log.info("模拟获取用户历史记录：用户ID={}", userId);
        // TODO: 在这里实现实际的数据库查询逻辑
        // 返回模拟数据，或者从数据库获取
        List<QuestionAnswerDto> history = new ArrayList<>();
        history.add(new QuestionAnswerDto(1L, "模拟问题1", "模拟答案1", LocalDateTime.now()));
        history.add(new QuestionAnswerDto(2L, "模拟问题2", "模拟答案2", LocalDateTime.now()));
        return history;
    }

    @Override
    public boolean deleteQuestionAnswer(Long id, Long userId) {
        log.info("模拟删除问答记录：记录ID={}, 用户ID={}", id, userId);
        // TODO: 在这里实现实际的数据库删除逻辑
        return true; // 模拟删除成功
    }

    @Override
    public void clearUserHistory(Long userId) {
        log.info("模拟清空用户历史记录：用户ID={}", userId);
        // TODO: 在这里实现实际的数据库清空逻辑
    }
}
