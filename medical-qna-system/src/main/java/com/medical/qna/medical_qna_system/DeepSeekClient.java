package com.medical.qna.medical_qna_system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// 使用 @Service 注解，让 Spring 能够管理这个类
@Service
public class DeepSeekClient {

    // 使用 @Value 注解从 application.properties 文件中读取配置
    @Value("${deepseek.api.key}")
    private String apiKey;

    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    public String ask(String question) throws Exception {
        // 检查 API Key 是否成功注入
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("DeepSeek API Key is not configured in application.properties.");
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-chat");

        JSONArray messages = new JSONArray();
        messages.put(new JSONObject()
            .put("role", "system")
            .put("content", "你是一个专业医学问答助手，只能根据已知药品说明书和医学常识回答问题。不得虚构、不得回答非医学内容。回答应简洁准确，使用专业术语。"));
        messages.put(new JSONObject()
            .put("role", "user")
            .put("content", question));
        requestBody.put("messages", messages);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey) // 使用注入的 apiKey
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
}