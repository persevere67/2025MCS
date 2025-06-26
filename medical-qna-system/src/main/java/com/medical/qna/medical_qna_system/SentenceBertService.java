package com.medical.qna.medical_qna_system; // 确保这里是 medical_qna_system

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service; // 引入 Spring 的 Service 注解

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SentenceBertService 类用于调用独立的 Python Sentence-BERT 嵌入服务，
 * 以生成文本的嵌入向量。
 */
@Service // 将此标记为 Spring 服务组件，方便 Spring 容器管理和注入
public class SentenceBertService {

    // Python 嵌入服务的 URL。
    // 如果 Python 服务运行在同一台机器的 5000 端口，则使用 localhost。
    // 如果部署在其他服务器，请替换为实际的 IP 地址或域名。
    private static final String EMBEDDING_SERVICE_URL = "http://localhost:5000/embed";

    // ObjectMapper 用于 JSON 序列化和反序列化
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 调用外部 Sentence-BERT 嵌入服务，获取给定文本列表的嵌入向量。
     *
     * @param texts 待生成嵌入的文本列表。
     * @return 包含每个文本对应嵌入向量的列表。每个嵌入向量本身是一个浮点数列表。
     * @throws IOException 如果 HTTP 请求或响应处理失败。
     */
    public List<List<Double>> getEmbeddings(List<String> texts) throws IOException {
        // 检查输入文本是否为空，避免不必要的 API 调用
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 try-with-resources 确保 HttpClient 资源被正确关闭
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(EMBEDDING_SERVICE_URL);
            // 设置请求头，告知服务器发送的是 JSON 数据
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json"); // 期望接收 JSON 响应

            // 构建 JSON 请求体：{"texts": ["text1", "text2", ...]}
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode textsArray = requestBody.putArray("texts");
            texts.forEach(textsArray::add); // 将所有文本添加到 JSON 数组中

            // 将 JSON 请求体设置为 HTTP 请求的实体
            httpPost.setEntity(new StringEntity(requestBody.toString(), StandardCharsets.UTF_8));

            System.out.println("Sending embedding request to: " + EMBEDDING_SERVICE_URL);
            System.out.println("Request body: " + requestBody.toString());

            // 执行 HTTP POST 请求并处理响应
            return httpClient.execute(httpPost, response -> {
                int statusCode = response.getCode();
                String responseString = EntityUtils.toString(response.getEntity());

                if (statusCode != 200) {
                    // 如果不是 200 OK，则抛出异常
                    System.err.println("Embedding service returned non-200 status: " + statusCode);
                    System.err.println("Response: " + responseString);
                    throw new IOException("Failed to get embeddings. Status: " + statusCode + ", Response: " + responseString);
                }

                // 解析 JSON 响应
                JsonNode rootNode = objectMapper.readTree(responseString);
                List<List<Double>> embeddings = new ArrayList<>();

                // 提取 "embeddings" 数组
                JsonNode embeddingsNode = rootNode.path("embeddings");
                if (embeddingsNode.isArray()) {
                    for (JsonNode embeddingArray : embeddingsNode) {
                        List<Double> embedding = new ArrayList<>();
                        if (embeddingArray.isArray()) {
                            // 遍历单个嵌入向量的维度值
                            for (JsonNode value : embeddingArray) {
                                embedding.add(value.asDouble());
                            }
                        }
                        embeddings.add(embedding);
                    }
                } else {
                    System.err.println("Unexpected response format: 'embeddings' field is not an array.");
                    throw new IOException("Unexpected response format from embedding service.");
                }
                System.out.println("Successfully received embeddings for " + embeddings.size() + " texts.");
                return embeddings;
            });

        } catch (IOException e) {
            System.err.println("Error communicating with embedding service: " + e.getMessage());
            throw e; // 重新抛出异常，让调用者处理
        }
    }

    // 可以在这里添加一个 main 方法用于独立测试这个服务调用逻辑
    public static void main(String[] args) {
        SentenceBertService service = new SentenceBertService();
        try {
            List<String> textsToEmbed = List.of(
                "布洛芬的用法用量是多少？",
                "高血压的常见症状有哪些？",
                "感冒了应该吃什么药？"
            );
            List<List<Double>> embeddings = service.getEmbeddings(textsToEmbed);

            if (!embeddings.isEmpty()) {
                System.out.println("\n--- Embeddings Received ---");
                for (int i = 0; i < textsToEmbed.size(); i++) {
                    System.out.println("Text: '" + textsToEmbed.get(i) + "'");
                    // 打印前5个维度，避免输出过长
                    System.out.println("Embedding (first 5 dims): " + embeddings.get(i).subList(0, Math.min(5, embeddings.get(i).size())) + "...");
                    System.out.println("Dimension: " + embeddings.get(i).size());
                    System.out.println("---");
                }
            } else {
                System.out.println("No embeddings received.");
            }
        } catch (IOException e) {
            System.err.println("Error during main test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
