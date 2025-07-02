package com.medical.qna.medical_qna_system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class DeepSeekClient {

    @Value("${deepseek.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    /**
     * 使用知识图谱的检索结果，生成增强的提示，并调用 DeepSeek API。
     *
     * @param originalQuestion 用户原始问题
     * @param kgResult         从知识图谱中获取的结构化信息
     * @return DeepSeek API 生成的答案
     */
    public String askWithKnowledge(String originalQuestion, KnowledgeGraphResult kgResult) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DeepSeek API Key is not configured in application.properties.");
        }

        // --- 核心部分：构建增强的提示 (Prompt Engineering) ---
        // 这是一个关键步骤，决定了 DeepSeek 的回答质量。
        // 我们将知识图谱的答案作为上下文提供给 AI。
        String systemPromptContent = "你是一个专业的医学问答助手，请根据提供的【已知信息】和你的医学常识，简洁准确地回答用户的问题。回答必须使用专业术语。";
        String userPromptContent = String.format(
            "用户问题：%s\n" +
            "【已知信息】：关于%s的%s是：%s\n" +
            "请根据上述信息回答用户的问题。",
            originalQuestion,
            kgResult.getEntity(),
            kgResult.getQueryTopic(),
            kgResult.getKnowledge()
        );

        System.out.println("--- 发送给 DeepSeek 的提示 ---");
        System.out.println("System Prompt: " + systemPromptContent);
        System.out.println("User Prompt: " + userPromptContent);
        System.out.println("------------------------------");

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-chat");

        JSONArray messages = new JSONArray();
        // 角色：系统，提供角色和约束
        messages.put(new JSONObject()
            .put("role", "system")
            .put("content", systemPromptContent));
        // 角色：用户，提供任务和上下文
        messages.put(new JSONObject()
            .put("role", "user")
            .put("content", userPromptContent));
        requestBody.put("messages", messages);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(response.body());
        return json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
    }

    // 原始的 ask 方法可以保留或移除，取决于你的业务逻辑
    // public String ask(String question) throws Exception { ... }
}