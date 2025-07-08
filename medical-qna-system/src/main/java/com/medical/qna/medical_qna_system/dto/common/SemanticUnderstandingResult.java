package com.medical.qna.medical_qna_system.dto.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data // Lombok注解：生成所有字段的Getter/Setter, toString, equals, hashCode
@NoArgsConstructor // Lombok注解：生成无参构造函数
@AllArgsConstructor // Lombok注解：生成包含所有字段的构造函数
public class SemanticUnderstandingResult {

    private String originalQuery;
    private List<String> identifiedKeywords;
    private String identifiedIntent;
    private List<RecommendedQuestion> recommendedQuestions;
    private ApiResponse apiResponse; // 匹配 Python 返回的 "apiResponse" 嵌套对象

    // 嵌套类：RecommendedQuestion
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedQuestion {
        private String questionText;
        private String questionId;
        private List<RelevantEntity> relevantEntities;
        private float relevanceScore;
    }

    // 嵌套类：RelevantEntity
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelevantEntity {
        private String id;
        private String name;
        private String type;
    }

    // 嵌套类：ApiResponse (如果您在dto.response包下已经有一个，可以考虑复用或将其移到dto.common)
    // 为了防止循环依赖或混淆，这里暂时作为嵌套类定义。
    // 如果您在 com.medical.qna.medical_qna_system.dto.response.ApiResponse 已经定义好了，
    // 并且想要复用它，那么这里可以修改为 import 并删除这个嵌套定义。
    // 但根据Python的返回结构，它是一个独立于主响应体外的嵌套，所以在这里作为嵌套类是安全的。
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse {
        private int code;
        private String message;
    }
}