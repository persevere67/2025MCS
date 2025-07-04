// src/main/java/com/medical/qna/medical_qna_system/dto/SemanticUnderstandingResult.java
// 或者如果您有 common/model 这样的目录，可以考虑放在那里
package com.medical.qna.medical_qna_system.dto; // 保持在dto包根目录

import java.util.List;
import java.util.Map;

/**
 * 封装语义理解的输出结果。
 * 包含识别出的实体、意图和关键词。
 */
public class SemanticUnderstandingResult {
    // 识别出的实体，键为实体类型（如 "DISEASE", "SYMPTOM", "DRUG"），值为实体名称列表
    private Map<String, List<String>> entities;
    // 识别出的用户意图（如 "查询症状", "查询治疗方法", "查询药物"）
    private String intent;
    // 提取出的关键词列表
    private List<String> keywords;

    public SemanticUnderstandingResult() {
    }

    public SemanticUnderstandingResult(Map<String, List<String>> entities, String intent, List<String> keywords) {
        this.entities = entities;
        this.intent = intent;
        this.keywords = keywords;
    }

    public Map<String, List<String>> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, List<String>> entities) {
        this.entities = entities;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @Override
    public String toString() {
        return "SemanticUnderstandingResult{" +
               "entities=" + entities +
               ", intent='" + intent + '\'' +
               ", keywords=" + keywords +
               '}';
    }
}