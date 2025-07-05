package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.response.AnswerResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.dto.common.SemanticUnderstandingResult;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import com.medical.qna.medical_qna_system.service.QuestionService;
import com.medical.qna.medical_qna_system.service.UserService;
import com.medical.qna.medical_qna_system.service.semantic.SemanticUnderstandingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok注解，自动生成包含所有final字段的构造函数
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserService userService;
    private final SemanticUnderstandingService semanticUnderstandingService;
    private final WebClient answerModelWebClient; // 注入 WebClient

    // 完整的构造函数，以确保所有依赖都被正确注入
    // 如果您使用 @RequiredArgsConstructor，这个构造函数是隐式生成的，无需手动编写
    // 但为了清晰起见，这里可以显式列出
    // @Autowired // 如果使用 @RequiredArgsConstructor，通常不需要显式写 @Autowired
    // public QuestionServiceImpl(QuestionAnswerRepository questionAnswerRepository,
    //                            UserService userService,
    //                            SemanticUnderstandingService semanticUnderstandingService,
    //                            WebClient.Builder webClientBuilder) {
    //     this.questionAnswerRepository = questionAnswerRepository;
    //     this.userService = userService;
    //     this.semanticUnderstandingService = semanticUnderstandingService;
    //     this.answerModelWebClient = webClientBuilder.baseUrl("http://localhost:8000").build(); // RAG模型基础URL
    // }

    private static final String ANSWER_MODEL_BASE_URL = "http://localhost:8000"; // 确保与您的Python RAG模型服务匹配
    private static final String ANSWER_MODEL_ASK_PATH = "/ask"; // RAG模型的问答路径


    /**
     * 实现 QuestionService 接口的 processQuestion 方法
     * 处理用户问题，进行语义理解并获取答案和推荐问题。
     * 此方法主要用于控制器中接收 QuestionRequest 并返回 AnswerResponse 的场景。
     */
    @Override
    public AnswerResponse processQuestion(QuestionRequest request) {
        String userQuestion = request.getQuestion();
        log.info("Processing user question (full response): {}", userQuestion);

        // 1. 调用语义理解服务 (AC 自动机)
        SemanticUnderstandingResult semanticResult = semanticUnderstandingService.analyze(userQuestion);

        if (semanticResult == null || semanticResult.getApiResponse() == null || semanticResult.getApiResponse().getCode() != 200) {
            log.warn("Semantic understanding failed for question: {}", userQuestion);
            // 返回一个默认的错误响应
            // 修复 AnswerResponse 构造函数调用，确保参数匹配 @AllArgsConstructor 生成的 8 个参数构造函数
            return new AnswerResponse(
                "抱歉，我暂时无法理解您的问题，请换个说法试试。", // answer
                LocalDateTime.now(), // timestamp
                null, // questionId
                "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。", // disclaimer
                null, // identifiedIntent
                null, // identifiedKeywords
                null, // sourceInfo
                null  // recommendedQuestions
            );
        }

        List<SemanticUnderstandingResult.RecommendedQuestion> recommendedQuestions = semanticResult.getRecommendedQuestions();

        // 2. 调用 RAG 回答模型服务（阻塞式）
        String answerContent = callAnswerModelBlocking(userQuestion);

        // 修复 AnswerResponse 构造函数调用，确保参数匹配 @AllArgsConstructor 生成的 8 个参数构造函数
        return new AnswerResponse(
            answerContent, // answer
            LocalDateTime.now(), // timestamp
            null, // questionId，此处暂为null，因为尚未保存到数据库
            "此回答仅供参考，不能替代专业医生的诊断。如有严重症状，请及时就医。", // disclaimer
            semanticResult.getIdentifiedIntent(), // identifiedIntent
            semanticResult.getIdentifiedKeywords(), // identifiedKeywords
            null, // sourceInfo 可以根据需要填充其他信息
            recommendedQuestions // recommendedQuestions
        );
    }

    /**
     * 实现 QuestionService 接口的 processQuestionWithStream 方法
     * 处理问题并流式返回答案。
     * 此方法用于需要分块接收答案的场景，例如SSE。
     */
    @Override
    public void processQuestionWithStream(String question, Consumer<String> chunkConsumer) {
        log.info("Processing user question (streamed response): {}", question);

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question", question);

            answerModelWebClient.post()
                    .uri(ANSWER_MODEL_ASK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class) // 期望流式字符串响应
                    .timeout(Duration.ofMinutes(5)) // 设置超时
                    .doOnNext(chunkConsumer) // 将每个数据块传递给 Consumer
                    .doOnError(error -> log.error("RAG answer model stream error: {}", error.getMessage(), error))
                    .doOnComplete(() -> log.info("RAG answer model stream completed for question: {}", question))
                    .blockLast(); // 阻塞直到流完成
        } catch (WebClientResponseException e) {
            log.error("RAG answer model stream returned error status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            chunkConsumer.accept("回答模型服务错误: " + e.getStatusCode().value() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error calling RAG answer model for streaming: {}", e.getMessage(), e);
            chunkConsumer.accept("调用回答模型服务失败，请稍后重试。");
        }
    }

    // 辅助方法，用于阻塞式调用回答模型
    private String callAnswerModelBlocking(String question) {
        log.info("Calling RAG answer model (blocking) for question: {}", question);

        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("question", question);

            String answer = answerModelWebClient.post()
                    .uri(ANSWER_MODEL_ASK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class) // 期望单个字符串响应
                    .timeout(Duration.ofSeconds(60)) // 设置超时
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

    @Override
    @Transactional // 添加事务注解
    public void saveQuestionAnswer(Long userId, String question, String answer) {
        try {
            User user = userService.getUserById(userId); // 假设 UserService 有 getUserById 方法
            if (user == null) {
                log.error("用户不存在: {}", userId);
                throw new RuntimeException("用户不存在");
            }

            QuestionAnswer qa = QuestionAnswer.builder()
                    .user(user) // 关联 User 实体
                    .question(question)
                    .answer(answer)
                    .createAt(LocalDateTime.now()) // 修复：使用 createAt 字段
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
    @Transactional(readOnly = true) // 添加事务注解
    public List<QuestionAnswerDto> getUserHistory(Long userId) {
        try {
            List<QuestionAnswer> history = questionAnswerRepository.findByUserIdOrderByCreateAtDesc(userId); // 修复：使用 findByUserIdOrderByCreateAtDesc
            List<QuestionAnswerDto> result = history.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt()) // 修复：使用 getCreateAt()
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
    @Transactional // 添加事务注解
    public boolean deleteQuestionAnswer(Long id, Long userId) {
        try {
            // 修复：使用 findByIdAndUserId 确保记录属于该用户
            Optional<QuestionAnswer> qaOpt = questionAnswerRepository.findByIdAndUserId(id, userId);
            if (qaOpt.isPresent()) { // 如果找到了且属于该用户
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
    @Transactional // 添加事务注解
    public void clearUserHistory(Long userId) {
        try {
            // 修复：deleteByUserId 现在返回 int
            int deletedCount = questionAnswerRepository.deleteByUserId(userId);
            log.info("清空用户历史记录成功: userId={}, 删除 {} 条记录", userId, deletedCount);
        } catch (Exception e) {
            log.error("清空用户历史记录失败: userId={}", userId, e);
            throw new RuntimeException("清空历史记录失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true) // 添加事务注解
    public List<QuestionAnswerDto> searchUserHistory(Long userId, String keyword) {
        try {
            // 修复：使用 findByUserIdAndQuestionContainingIgnoreCaseOrderByCreateAtDesc
            List<QuestionAnswer> searchResults = questionAnswerRepository
                    .findByUserIdAndQuestionContainingIgnoreCaseOrderByCreateAtDesc(userId, keyword);

            List<QuestionAnswerDto> result = searchResults.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt()) // 修复：使用 getCreateAt()
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

    @Override
    @Transactional(readOnly = true) // 添加事务注解
    public long getUserQuestionCount(Long userId) {
        try {
            long count = questionAnswerRepository.countByUserId(userId); // 确保Repository有此方法
            log.info("获取用户问答记录总数: userId={}, count={}", userId, count);
            return count;
        } catch (Exception e) {
            log.error("获取用户问答记录总数失败: userId={}", userId, e);
            return 0; // 返回0表示失败或无记录
        }
    }

    @Override
    @Transactional(readOnly = true) // 添加事务注解
    public List<QuestionAnswerDto> getUserRecentHistory(Long userId, int page, int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            // 修复：使用 findByUserIdOrderByCreateAtDesc 返回 Page
            List<QuestionAnswer> recentHistory = questionAnswerRepository
                    .findByUserIdOrderByCreateAtDesc(userId, pageable).getContent(); // getContent() 获取实际列表

            List<QuestionAnswerDto> result = recentHistory.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt()) // 修复：使用 getCreateAt()
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

    @Override
    @Transactional // 添加事务注解
    public int batchDeleteQuestionAnswers(List<Long> ids, Long userId) {
        try {
            // 修复：直接调用 deleteByIdsAndUserId 进行批量删除
            int deletedCount = questionAnswerRepository.deleteByIdsAndUserId(ids, userId);
            log.info("批量删除问答记录: userId={}, 请求删除={}条, 成功删除={}条",
                    userId, ids.size(), deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("批量删除问答记录失败: userId={}, ids={}", userId, ids, e);
            throw new RuntimeException("批量删除失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true) // 添加事务注解
    public boolean hasUserQuestions(Long userId) {
        try {
            boolean exists = questionAnswerRepository.existsByUserId(userId); // 修复：使用 existsByUserId
            log.info("检查用户是否有问答记录: userId={}, exists={}", userId, exists);
            return exists;
        } catch (Exception e) {
            log.error("检查用户是否有问答记录失败: userId={}", userId, e);
            return false;
        }
    }
}
