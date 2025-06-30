package com.medical.qna.medical_qna_system;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

public class DeepSeekClient {

    // DeepSeek API Key 建议放到配置文件或环境变量
    private static final String API_KEY = "sk-e40b773f247748f1b4d5d831cb3a8987";
    private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    public static String ask(String question) throws Exception {
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
                .header("Authorization", "Bearer " + API_KEY)
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

    // 测试用 main 方法
    public static void main(String[] args) throws Exception {
        String question = "感冒应该吃什么药？";
        String answer = ask(question);
        System.out.println("DeepSeek 回答：");
        System.out.println(answer);
    }
}