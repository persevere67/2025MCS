// DeepSeekClient.java
package com.medical.qna.medical_qna_system.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DeepSeekClient {
  
    @Value("${deepseek.api.url}")
    private String apiUrl;
  
    @Value("${deepseek.api.key}")
    private String apiKey;
  
    private final RestTemplate restTemplate = new RestTemplate();
  
    public String generateAnswer(String question, String context) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
          
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", buildSystemPrompt()),
                Map.of("role", "user", "content", buildUserPrompt(question, context))
            ));
            requestBody.put("max_tokens", 1000);
            requestBody.put("temperature", 0.7);
          
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);
          
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
          
            return "抱歉，暂时无法生成答案，请稍后重试。";
          
        } catch (Exception e) {
            log.error("调用DeepSeek API失败", e);
            return "抱歉，系统暂时不可用，请稍后重试。";
        }
    }
  
    private String buildSystemPrompt() {
        return """
            你是一个专业的医疗问答助手。请根据提供的医学知识和上下文信息，为用户的健康问题提供专业、准确的答案。
          
            回答原则：
            1. 基于提供的疾病信息和医学知识回答
            2. 答案要专业但易于理解
            3. 必须提醒用户：你的回答仅供参考，不能替代专业医生的诊断
            4. 对于严重症状，建议用户及时就医
            5. 避免提供具体的药物剂量建议
            6. 如果有饮食建议，要明确说明推荐和禁忌食物
            """;
    }
  
    private String buildUserPrompt(String question, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户问题：").append(question).append("\n\n");
      
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("相关医学知识和参考信息：\n").append(context).append("\n\n");
        }
      
        prompt.append("请基于上述信息为用户提供专业的医疗建议。如果提供的信息中包含饮食建议，请明确指出。");
        return prompt.toString();
    }
}
