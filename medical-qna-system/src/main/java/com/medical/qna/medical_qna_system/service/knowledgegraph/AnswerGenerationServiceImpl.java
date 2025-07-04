// src/main/java/com/medical/qna/medical_qna_system/service/knowledgegraph/impl/AnswerGenerationServiceImpl.java
package com.medical.qna.medical_qna_system.service.knowledgegraph.impl;

import com.medical.qna.medical_qna_system.dto.SemanticUnderstandingResult;
import com.medical.qna.medical_qna_system.service.knowledgegraph.AnswerGenerationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 答案生成服务实现类。
 * 负责将知识图谱查询的原始结果转化为用户友好的自然语言答案。
 * 答案生成基于预设模板和查询结果的类型。
 */
@Service
public class AnswerGenerationServiceImpl implements AnswerGenerationService {

    @Override
    public String generateAnswerFromKgResults(String originalQuestion, List<Map<String, Object>> kgResults,
                                              SemanticUnderstandingResult understandingResult) {
        if (kgResults == null || kgResults.isEmpty()) {
            return "抱歉，根据我目前的知识，暂时无法回答您关于“" + originalQuestion + "”的问题。";
        }

        String intent = understandingResult != null ? understandingResult.getIntent() : null;
        String mainEntityName = null;
        if (understandingResult != null && understandingResult.getEntities().containsKey("DISEASE") && !understandingResult.getEntities().get("DISEASE").isEmpty()) {
            mainEntityName = understandingResult.getEntities().get("DISEASE").get(0);
        } else if (understandingResult != null && understandingResult.getEntities().containsKey("DRUG") && !understandingResult.getEntities().get("DRUG").isEmpty()) {
            mainEntityName = understandingResult.getEntities().get("DRUG").get(0);
        } // 可以在这里添加更多主实体类型

        // 根据意图和查询结果的结构生成答案
        if (intent != null) {
            switch (intent) {
                // ---------- 疾病相关答案生成 ----------
                case "查询症状":
                    if (kgResults.get(0).containsKey("symptom") && mainEntityName != null) {
                        List<String> symptoms = kgResults.stream()
                                .map(row -> (String) row.get("symptom"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!symptoms.isEmpty()) {
                            return mainEntityName + "的症状包括：" + String.join("、", symptoms) + "。";
                        }
                    }
                    break;
                case "查询治疗方法":
                    if (kgResults.get(0).containsKey("cure_way") && mainEntityName != null) {
                        Object cureWayObj = kgResults.get(0).get("cure_way");
                        if (cureWayObj instanceof List) {
                            List<String> cureWays = (List<String>) cureWayObj;
                            if (!cureWays.isEmpty()) {
                                return mainEntityName + "的治疗方法有：" + String.join("、", cureWays) + "。";
                            }
                        } else if (cureWayObj instanceof String) { // 如果在Neo4j中存储的是逗号分隔的字符串
                            return mainEntityName + "的治疗方法有：" + cureWayObj + "。";
                        }
                    }
                    break;
                case "查询疾病描述":
                    if (kgResults.get(0).containsKey("desc") && mainEntityName != null) {
                        String desc = (String) kgResults.get(0).get("desc");
                        if (desc != null && !desc.isEmpty()) {
                            return mainEntityName + "的描述是：" + desc;
                        }
                    }
                    break;
                case "查询预防方法":
                    if (kgResults.get(0).containsKey("prevent") && mainEntityName != null) {
                        String prevent = (String) kgResults.get(0).get("prevent");
                        if (prevent != null && !prevent.isEmpty()) {
                            return mainEntityName + "的预防方法是：" + prevent;
                        }
                    }
                    break;
                case "查询病因":
                    if (kgResults.get(0).containsKey("cause") && mainEntityName != null) {
                        String cause = (String) kgResults.get(0).get("cause");
                        if (cause != null && !cause.isEmpty()) {
                            return mainEntityName + "的病因是：" + cause;
                        }
                    }
                    break;
                case "查询易感人群":
                    if (kgResults.get(0).containsKey("easy_get") && mainEntityName != null) {
                        String easyGet = (String) kgResults.get(0).get("easy_get");
                        if (easyGet != null && !easyGet.isEmpty()) {
                            return mainEntityName + "的易感人群是：" + easyGet;
                        }
                    }
                    break;
                case "查询治疗持续时间":
                    if (kgResults.get(0).containsKey("cure_lasttime") && mainEntityName != null) {
                        String cureLasttime = (String) kgResults.get(0).get("cure_lasttime");
                        if (cureLasttime != null && !cureLasttime.isEmpty()) {
                            return mainEntityName + "的治疗持续时间通常是：" + cureLasttime;
                        }
                    }
                    break;
                case "查询治愈概率":
                    if (kgResults.get(0).containsKey("cured_prob") && mainEntityName != null) {
                        String curedProb = (String) kgResults.get(0).get("cured_prob");
                        if (curedProb != null && !curedProb.isEmpty()) {
                            return mainEntityName + "的治愈概率是：" + curedProb;
                        }
                    }
                    break;
                case "查询治疗药物":
                    if (kgResults.get(0).containsKey("drug_name") && mainEntityName != null) {
                        List<String> drugs = kgResults.stream()
                                .map(row -> (String) row.get("drug_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!drugs.isEmpty()) {
                            return mainEntityName + "的治疗药物有：" + String.join("、", drugs) + "。";
                        }
                    }
                    break;
                case "查询推荐检查":
                    if (kgResults.get(0).containsKey("check_name") && mainEntityName != null) {
                        List<String> checks = kgResults.stream()
                                .map(row -> (String) row.get("check_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!checks.isEmpty()) {
                            return mainEntityName + "需要进行的检查有：" + String.join("、", checks) + "。";
                        }
                    }
                    break;
                case "查询所属科室":
                    if (kgResults.get(0).containsKey("department_name") && mainEntityName != null) {
                        List<String> departments = kgResults.stream()
                                .map(row -> (String) row.get("department_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!departments.isEmpty()) {
                            return mainEntityName + "一般属于：" + String.join("、", departments) + "。";
                        }
                    }
                    break;
                case "查询忌食食物":
                    if (kgResults.get(0).containsKey("food_name") && mainEntityName != null) {
                        List<String> foods = kgResults.stream()
                                .map(row -> (String) row.get("food_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!foods.isEmpty()) {
                            return mainEntityName + "的患者应该忌食：" + String.join("、", foods) + "。";
                        }
                    }
                    break;
                case "查询宜食食物":
                    if (kgResults.get(0).containsKey("food_name") && mainEntityName != null) {
                        List<String> foods = kgResults.stream()
                                .map(row -> (String) row.get("food_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!foods.isEmpty()) {
                            return mainEntityName + "的患者可以多吃：" + String.join("、", foods) + "。";
                        }
                    }
                    break;
                // ---------- 症状相关答案生成 ----------
                case "查询导致疾病":
                case "查询症状相关疾病":
                    if (kgResults.get(0).containsKey("disease_name") && mainEntityName != null) {
                        List<String> diseases = kgResults.stream()
                                .map(row -> (String) row.get("disease_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!diseases.isEmpty()) {
                            return "导致或与“" + mainEntityName + "”相关的疾病有：" + String.join("、", diseases) + "。";
                        }
                    }
                    break;
                // ---------- 药物相关答案生成 ----------
                case "查询药物治疗疾病":
                    if (kgResults.get(0).containsKey("disease_name") && mainEntityName != null) {
                        List<String> diseases = kgResults.stream()
                                .map(row -> (String) row.get("disease_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!diseases.isEmpty()) {
                            return mainEntityName + "可以治疗：" + String.join("、", diseases) + "等疾病。";
                        }
                    }
                    break;
                case "查询药物作用症状":
                    if (kgResults.get(0).containsKey("symptom_name") && mainEntityName != null) {
                        List<String> symptoms = kgResults.stream()
                                .map(row -> (String) row.get("symptom_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!symptoms.isEmpty()) {
                            return mainEntityName + "主要作用于：" + String.join("、", symptoms) + "等症状。";
                        }
                    }
                    break;
                case "查询药物生产厂家":
                    if (kgResults.get(0).containsKey("producer_name") && mainEntityName != null) {
                        List<String> producers = kgResults.stream()
                                .map(row -> (String) row.get("producer_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!producers.isEmpty()) {
                            return mainEntityName + "的生产厂家有：" + String.join("、", producers) + "。";
                        }
                    }
                    break;
                // ---------- 检查相关答案生成 ----------
                case "查询检查诊断疾病":
                    if (kgResults.get(0).containsKey("disease_name") && mainEntityName != null) {
                        List<String> diseases = kgResults.stream()
                                .map(row -> (String) row.get("disease_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!diseases.isEmpty()) {
                            return mainEntityName + "可以用于诊断：" + String.join("、", diseases) + "等疾病。";
                        }
                    }
                    break;
                // ---------- 科室相关答案生成 ----------
                case "查询科室擅长疾病":
                    if (kgResults.get(0).containsKey("disease_name") && mainEntityName != null) {
                        List<String> diseases = kgResults.stream()
                                .map(row -> (String) row.get("disease_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!diseases.isEmpty()) {
                            return mainEntityName + "擅长治疗：" + String.join("、", diseases) + "等疾病。";
                        }
                    }
                    break;
                // ---------- 食物相关答案生成 ----------
                case "查询食物宜忌疾病":
                    if (kgResults.get(0).containsKey("disease_name") && mainEntityName != null) {
                        List<String> diseases = kgResults.stream()
                                .map(row -> (String) row.get("disease_name"))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        if (!diseases.isEmpty()) {
                            if (originalQuestion.contains("适合") || originalQuestion.contains("宜")) {
                                return mainEntityName + "适合患有：" + String.join("、", diseases) + "等疾病的患者食用。";
                            } else if (originalQuestion.contains("不适合") || originalQuestion.contains("忌")) {
                                return mainEntityName + "不适合患有：" + String.join("、", diseases) + "等疾病的患者食用。";
                            }
                        }
                    }
                    break;
                // ... 可以根据需要添加更多意图和答案生成逻辑
                default:
                    // 默认处理，或者更通用的答案生成
                    return generateGenericAnswer(originalQuestion, kgResults);
            }
        }

        // 如果没有匹配到特定意图，尝试通用处理
        return generateGenericAnswer(originalQuestion, kgResults);
    }

    private String generateGenericAnswer(String originalQuestion, List<Map<String, Object>> kgResults) {
        StringBuilder sb = new StringBuilder("关于“").append(originalQuestion).append("”，我找到以下信息：\n");
        for (Map<String, Object> row : kgResults) {
            row.forEach((key, value) -> {
                if (value instanceof List) {
                    sb.append(key).append("：").append(String.join("、", (List<String>) value)).append("；\n");
                } else {
                    sb.append(key).append("：").append(value).append("；\n");
                }
            });
        }
        if (kgResults.isEmpty()) {
             return "抱歉，根据我目前的知识，暂时无法回答您关于“" + originalQuestion + "”的问题。";
        }
        return sb.toString();
    }
}