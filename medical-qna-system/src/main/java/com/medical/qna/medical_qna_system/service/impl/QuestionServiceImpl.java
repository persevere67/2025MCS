package com.medical.qna.medical_qna_system.service.impl;

import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import com.medical.qna.medical_qna_system.service.QuestionService;
import com.medical.qna.medical_qna_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private final QuestionAnswerRepository questionAnswerRepository;
    private final UserService userService;

    @Override
    public void processQuestionWithStream(String question, Consumer<String> chunkConsumer) {
        try {
            // 调用Python后端的流式接口
            URL url = new URL("http://127.0.0.1:8000/ask");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000); // 连接超时5秒
            connection.setReadTimeout(30000);   // 读取超时30秒

            // 发送请求数据
            String jsonInputString = String.format("{\"question\":\"%s\"}", 
                question.replace("\"", "\\\"").replace("\n", "\\n"));
            
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonInputString);
                writer.flush();
            }

            // 检查响应状态
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.error("Python后端返回错误状态码: {}", responseCode);
                chunkConsumer.accept("抱歉，AI服务暂时不可用，请稍后重试。");
                return;
            }

            // 读取流式响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                
                StringBuilder buffer = new StringBuilder();
                int character;
                
                while ((character = reader.read()) != -1) {
                    char ch = (char) character;
                    buffer.append(ch);
                    
                    // 每读取一定字符就发送一次，或者遇到特定标点符号
                    if (buffer.length() >= 10 || ch == '。' || ch == '\n' || 
                        ch == '！' || ch == '？' || ch == '；' || ch == '，') {
                        
                        String chunk = buffer.toString();
                        if (!chunk.trim().isEmpty()) {
                            chunkConsumer.accept(chunk);
                        }
                        buffer.setLength(0);
                        
                        // 添加小延迟以模拟流式效果
                        Thread.sleep(50);
                    }
                }
                
                // 发送剩余的字符
                if (buffer.length() > 0) {
                    String chunk = buffer.toString();
                    if (!chunk.trim().isEmpty()) {
                        chunkConsumer.accept(chunk);
                    }
                }
            }
            
            connection.disconnect();
            log.info("问题处理完成: {}", question.substring(0, Math.min(question.length(), 50)));
            
        } catch (Exception e) {
            log.error("调用Python后端失败", e);
            chunkConsumer.accept("抱歉，处理您的问题时发生了错误，请检查网络连接或稍后重试。");
        }
    }

    @Override
    @Transactional
    public void saveQuestionAnswer(Long userId, String question, String answer) {
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
        log.info("问答记录保存成功: 用户={}, 问题={}", userId, question.substring(0, Math.min(question.length(), 50)));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<QuestionAnswerDto> getUserHistory(Long userId) {
        try {
            List<QuestionAnswer> history = questionAnswerRepository.findByUserIdOrderByCreateAtDesc(userId);
            return history.stream()
                    .map(qa -> QuestionAnswerDto.builder()
                            .id(qa.getId())
                            .question(qa.getQuestion())
                            .answer(qa.getAnswer())
                            .createAt(qa.getCreateAt())  // 确保字段名匹配
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户历史记录失败: userId={}", userId, e);
            throw new RuntimeException("获取历史记录失败", e);
        }
    }

    @Override
    @Transactional
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

    // 新增方法
    @Override
    @Transactional
    public void clearUserHistory(Long userId) {
        try {
            questionAnswerRepository.deleteByUserId(userId);
            log.info("清空用户历史记录成功: userId={}", userId);
        } catch (Exception e) {
            log.error("清空用户历史记录失败: userId={}", userId, e);
            throw new RuntimeException("清空历史记录失败", e);
        }
    }

    @Override
    @Transactional
    public QuestionAnswer updateQuestionAnswer(Long id, String answer) {
        Optional<QuestionAnswer> qaOpt = questionAnswerRepository.findById(id);
        if (qaOpt.isPresent()) {
            QuestionAnswer qa = qaOpt.get();
            qa.setAnswer(answer);
            return questionAnswerRepository.save(qa);
        } else {
            log.warn("问答记录不存在: id={}", id);
            throw new RuntimeException("问答记录不存在");
        }
    }





    @Override
    public String processQuestion(String question) {
        try {
            // 调用Python后端
            URL url = new URL("http://127.0.0.1:8000/ask");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);

            String jsonInputString = String.format("{\"question\":\"%s\"}", 
                question.replace("\"", "\\\"").replace("\n", "\\n"));
            
            try (OutputStreamWriter writer = new OutputStreamWriter(
                    connection.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(jsonInputString);
                writer.flush();
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            connection.disconnect();
            return response.toString();
            
        } catch (Exception e) {
            log.error("处理问题失败", e);
            return "抱歉，处理您的问题时发生了错误，请稍后重试。";
        }
    }
}