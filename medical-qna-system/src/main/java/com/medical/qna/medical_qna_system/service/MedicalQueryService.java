package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.DiseaseInfoDto;
import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.entity.neo4j.Symptom;
import com.medical.qna.medical_qna_system.repository.neo4j.MedicalKnowledgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalQueryService {
  
    private final MedicalKnowledgeRepository knowledgeRepository;
  
    // 症状关键词库
    private static final List<String> SYMPTOM_KEYWORDS = Arrays.asList(
        "发热", "发烧", "头痛", "头疼", "咳嗽", "胸痛", "腹痛", "肚子痛", 
        "恶心", "呕吐", "腹泻", "拉肚子", "便秘", "乏力", "疲劳", "头晕", 
        "眩晕", "失眠", "食欲不振", "体重减轻", "皮疹", "瘙痒", "关节痛", 
        "肌肉痛", "心悸", "气短", "呼吸困难", "胸闷", "咽痛", "喉咙痛",
        "鼻塞", "流鼻涕", "打喷嚏", "耳鸣", "听力下降", "视力模糊",
        "尿频", "尿急", "尿痛", "血尿", "便血", "黄疸", "水肿", "浮肿"
    );
  
    public List<Disease> findDiseasesByQuestion(String question) {
        // 提取症状
        List<String> symptoms = extractSymptoms(question);
      
        if (symptoms.isEmpty()) {
            // 如果没有识别出症状，尝试通过关键词搜索疾病
            return searchDiseasesByKeywords(question);
        }
      
        // 通过症状查找疾病
        return knowledgeRepository.findDiseasesBySymptoms(symptoms);
    }
  
    public DiseaseInfoDto getDiseaseInfo(String diseaseName) {
        Optional<Disease> diseaseOpt = knowledgeRepository.findByName(diseaseName);
        if (diseaseOpt.isEmpty()) {
            return null;
        }
      
        Disease disease = diseaseOpt.get();
        DiseaseInfoDto dto = new DiseaseInfoDto();
      
        // 基本信息
        dto.setName(disease.getName());
        dto.setDescription(disease.getDesc());
        dto.setInsuranceCovered(disease.getInsuranceCovered());
        dto.setTreatmentDuration(disease.getTreatmentDuration());
        dto.setCureRate(disease.getCureRate());
        dto.setTreatmentCost(disease.getTreatmentCost());
        dto.setNursing(disease.getNursing());
      
        // 症状
        List<Symptom> symptoms = knowledgeRepository.findSymptomsByDisease(diseaseName);
        dto.setSymptoms(symptoms.stream().map(Symptom::getName).collect(Collectors.toList()));
      
        // 治疗方式
        dto.setTreatments(knowledgeRepository.findTreatmentsByDisease(diseaseName));
      
        // 食物建议
        dto.setGoodFoods(knowledgeRepository.findGoodFoodsByDisease(diseaseName));
        dto.setBadFoods(knowledgeRepository.findBadFoodsByDisease(diseaseName));
      
        // 并发症
        dto.setComplications(knowledgeRepository.findComplicationsByDisease(diseaseName));
      
        // 科室信息
        List<Object> deptInfo = knowledgeRepository.findDepartmentInfoByDisease(diseaseName);
        if (!deptInfo.isEmpty()) {
            Map<String, String> info = (Map<String, String>) deptInfo.get(0);
            dto.setDepartment(info.get("department"));
            dto.setDepartmentCategory(info.get("category"));
        }
      
        return dto;
    }
  
    private List<String> extractSymptoms(String question) {
        List<String> extractedSymptoms = new ArrayList<>();
        String lowerQuestion = question.toLowerCase();
      
        // 使用症状关键词匹配
        for (String symptom : SYMPTOM_KEYWORDS) {
            if (lowerQuestion.contains(symptom)) {
                extractedSymptoms.add(symptom);
            }
        }
      
        // 使用正则表达式提取更多症状模式
        Pattern pattern = Pattern.compile("(\\S{2,4}[痛疼])|(\\S{2,4}不[适舒])");
        Matcher matcher = pattern.matcher(question);
        while (matcher.find()) {
            String symptom = matcher.group();
            if (!extractedSymptoms.contains(symptom)) {
                extractedSymptoms.add(symptom);
            }
        }
      
        // 从Neo4j中搜索匹配的症状
        Set<String> validSymptoms = new HashSet<>();
        for (String symptom : extractedSymptoms) {
            List<Symptom> searchResults = knowledgeRepository.searchSymptoms(symptom);
            validSymptoms.addAll(searchResults.stream()
                    .map(Symptom::getName)
                    .collect(Collectors.toList()));
        }
      
        return new ArrayList<>(validSymptoms);
    }
  
    private List<Disease> searchDiseasesByKeywords(String question) {
        // 提取可能的疾病关键词
        String[] words = question.split("[\\s，。！？,.!?]+");
        List<Disease> allDiseases = new ArrayList<>();
      
        for (String word : words) {
            if (word.length() >= 2) {
                List<Disease> diseases = knowledgeRepository.searchDiseases(word);
                allDiseases.addAll(diseases);
            }
        }
      
        // 去重并返回
        return allDiseases.stream()
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
    }
  
    public List<String> getRecommendedDepartments(List<Disease> diseases) {
        Set<String> departments = new HashSet<>();
      
        for (Disease disease : diseases) {
            List<Object> deptInfo = knowledgeRepository.findDepartmentInfoByDisease(disease.getName());
            if (!deptInfo.isEmpty()) {
                Map<String, String> info = (Map<String, String>) deptInfo.get(0);
                String dept = info.get("department");
                if (dept != null) {
                    departments.add(dept);
                }
            }
        }
      
        return new ArrayList<>(departments);
    }
}