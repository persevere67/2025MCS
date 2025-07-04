// src/main/java/com.medical/qna/medical_qna_system/service/association/impl/QuestionAssociationServiceImpl.java
package com.medical.qna.medical_qna_system.service.association.impl;

import com.medical.qna.medical_qna_system.dto.SemanticUnderstandingResult;
import com.medical.qna.medical_qna_system.service.association.QuestionAssociationService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 问题联想服务实现类。
 * 这是一个简易的实现，基于识别出的实体和意图来生成联想问题。
 * 在实际项目中，这部分会更复杂，可能涉及相似度计算、图谱路径探索等。
 */
@Service
public class QuestionAssociationServiceImpl implements QuestionAssociationService {

    @Override
    public List<String> generateAssociatedQuestions(SemanticUnderstandingResult understandingResult) {
        List<String> associatedQuestions = new ArrayList<>();

        if (understandingResult == null || understandingResult.getEntities() == null || understandingResult.getEntities().isEmpty()) {
            // 如果没有识别到实体，提供一些通用热门问题或保持为空
            associatedQuestions.add("什么是感冒？");
            associatedQuestions.add("发烧应该怎么办？");
            return associatedQuestions;
        }

        String intent = understandingResult.getIntent();
        Map<String, List<String>> entities = understandingResult.getEntities();

        // 优先根据识别到的主要实体进行联想
        String diseaseName = entities.containsKey("DISEASE") && !entities.get("DISEASE").isEmpty() ? entities.get("DISEASE").get(0) : null;
        String symptomName = entities.containsKey("SYMPTOM") && !entities.get("SYMPTOM").isEmpty() ? entities.get("SYMPTOM").get(0) : null;
        String drugName = entities.containsKey("DRUG") && !entities.get("DRUG").isEmpty() ? entities.get("DRUG").get(0) : null;

        if (diseaseName != null) {
            // 围绕疾病进行联想
            associatedQuestions.add(diseaseName + "的症状有哪些？");
            associatedQuestions.add(diseaseName + "怎么治疗？");
            associatedQuestions.add(diseaseName + "的病因是什么？");
            associatedQuestions.add(diseaseName + "吃什么药好？");
            associatedQuestions.add(diseaseName + "需要做哪些检查？");
            associatedQuestions.add(diseaseName + "属于哪个科室？");
            associatedQuestions.add(diseaseName + "不能吃什么？");
            associatedQuestions.add(diseaseName + "应该吃什么？");
        } else if (symptomName != null) {
            // 围绕症状进行联想
            associatedQuestions.add(symptomName + "可能是什么疾病引起的？");
            associatedQuestions.add(symptomName + "应该挂什么科？");
            associatedQuestions.add("治疗" + symptomName + "的药物有哪些？");
        } else if (drugName != null) {
            // 围绕药物进行联想
            associatedQuestions.add(drugName + "能治疗什么疾病？");
            associatedQuestions.add(drugName + "有什么副作用？"); // 假设有副作用属性或关系
            associatedQuestions.add(drugName + "的生产厂家是？");
        }

        // 如果联想问题少于3个，可以补充一些通用问题
        if (associatedQuestions.size() < 3) {
            if (!associatedQuestions.contains("什么是感冒？")) associatedQuestions.add("什么是感冒？");
            if (!associatedQuestions.contains("什么是高血压？")) associatedQuestions.add("什么是高血压？");
            if (!associatedQuestions.contains("感冒了应该怎么办？")) associatedQuestions.add("感冒了应该怎么办？");
        }

        // 限制联想问题数量
        return associatedQuestions.subList(0, Math.min(associatedQuestions.size(), 3));
    }
}