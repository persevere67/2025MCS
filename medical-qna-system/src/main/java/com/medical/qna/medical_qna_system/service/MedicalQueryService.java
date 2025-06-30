package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.repository.neo4j.MedicalKnowledgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalQueryService {
    
    @Autowired
    private MedicalKnowledgeRepository medicalRepository;
    
    public String queryMedicalKnowledge(String question) {
        StringBuilder answer = new StringBuilder();
        
        try {
            // 分析问题类型并调用相应的查询方法
            if (isSymptomQuery(question)) {
                answer.append(handleSymptomQuery(question));
            } else if (isDiseaseInfoQuery(question)) {
                answer.append(handleDiseaseInfoQuery(question));
            } else if (isTreatmentQuery(question)) {
                answer.append(handleTreatmentQuery(question));
            } else if (isFoodQuery(question)) {
                answer.append(handleFoodQuery(question));
            } else if (isDepartmentQuery(question)) {
                answer.append(handleDepartmentQuery(question));
            } else {
                // 通用关键词查询
                answer.append(handleGeneralQuery(question));
            }
            
        } catch (Exception e) {
            return "查询过程中出现错误，请稍后重试。";
        }
        
        return answer.length() > 0 ? answer.toString() : "抱歉，没有找到相关的医疗信息。";
    }
    
    private boolean isSymptomQuery(String question) {
        return question.contains("症状") || question.contains("表现") || 
               question.contains("什么病") || question.contains("哪些病");
    }
    
    private boolean isDiseaseInfoQuery(String question) {
        return question.contains("什么是") || question.contains("介绍") || 
               question.contains("简介") || question.contains("是什么");
    }
    
    private boolean isTreatmentQuery(String question) {
        return question.contains("治疗") || question.contains("怎么治") || 
               question.contains("如何治疗") || question.contains("治疗方法");
    }
    
    private boolean isFoodQuery(String question) {
        return question.contains("吃什么") || question.contains("食物") || 
               question.contains("饮食") || question.contains("不能吃");
    }
    
    private boolean isDepartmentQuery(String question) {
        return question.contains("科室") || question.contains("挂号") || 
               question.contains("哪个科") || question.contains("去哪里看");
    }
    
    private String handleSymptomQuery(String question) {
        String[] symptoms = extractKeywords(question);
        List<String> symptomList = Arrays.stream(symptoms)
            .filter(s -> !isStopWord(s))
            .collect(Collectors.toList());
        
        if (!symptomList.isEmpty()) {
            List<Disease> diseases = medicalRepository.findDiseasesByMultipleSymptoms(symptomList);
            if (!diseases.isEmpty()) {
                StringBuilder result = new StringBuilder("根据您描述的症状，可能的疾病包括：\n");
                for (Disease disease : diseases) {
                    result.append("• ").append(disease.getName());
                    if (disease.getDesc() != null && !disease.getDesc().isEmpty()) {
                        result.append("：").append(disease.getDesc());
                    }
                    result.append("\n");
                }
                result.append("\n建议您及时就医确诊。");
                return result.toString();
            }
        }
        return "";
    }
    
    private String handleDiseaseInfoQuery(String question) {
        String[] keywords = extractKeywords(question);
        for (String keyword : keywords) {
            List<Disease> diseases = medicalRepository.findDiseasesByName(keyword);
            if (!diseases.isEmpty()) {
                Disease disease = diseases.get(0);
                StringBuilder result = new StringBuilder();
                result.append("关于").append(disease.getName()).append("的信息：\n");
                
                if (disease.getDesc() != null) {
                    result.append("疾病简介：").append(disease.getDesc()).append("\n");
                }
                if (disease.getSusceptible_population() != null) {
                    result.append("易感人群：").append(disease.getSusceptible_population()).append("\n");
                }
                if (disease.getPrevalence_rate() != null) {
                    result.append("患病比例：").append(disease.getPrevalence_rate()).append("\n");
                }
                if (disease.getCure_rate() != null) {
                    result.append("治愈率：").append(disease.getCure_rate()).append("\n");
                }
                
                return result.toString();
            }
        }
        return "";
    }
    
    private String handleTreatmentQuery(String question) {
        String[] keywords = extractKeywords(question);
        for (String keyword : keywords) {
            List<Disease> diseases = medicalRepository.findDiseasesByName(keyword);
            if (!diseases.isEmpty()) {
                Disease disease = diseases.get(0);
                StringBuilder result = new StringBuilder();
                result.append(disease.getName()).append("的治疗信息：\n");
                
                // 查询治疗方法
                List<Object> treatments = medicalRepository.findTreatmentsByDisease(disease.getName());
                if (!treatments.isEmpty()) {
                    result.append("治疗方法：");
                    treatments.forEach(treatment -> result.append(treatment.toString()).append("；"));
                    result.append("\n");
                }
                
                if (disease.getTreatment_duration() != null) {
                    result.append("治疗周期：").append(disease.getTreatment_duration()).append("\n");
                }
                if (disease.getTreatment_cost() != null) {
                    result.append("治疗费用：").append(disease.getTreatment_cost()).append("\n");
                }
                if (disease.getNursing() != null) {
                    result.append("护理建议：").append(disease.getNursing()).append("\n");
                }
                
                return result.toString();
            }
        }
        return "";
    }
    
    private String handleFoodQuery(String question) {
        String[] keywords = extractKeywords(question);
        for (String keyword : keywords) {
            List<Disease> diseases = medicalRepository.findDiseasesByName(keyword);
            if (!diseases.isEmpty()) {
                Disease disease = diseases.get(0);
                StringBuilder result = new StringBuilder();
                result.append(disease.getName()).append("的饮食建议：\n");
                
                // 查询有益食物
                List<Object> goodFoods = medicalRepository.findGoodFoodsByDisease(disease.getName());
                if (!goodFoods.isEmpty()) {
                    result.append("推荐食物：");
                    goodFoods.forEach(food -> result.append(food.toString()).append("；"));
                    result.append("\n");
                }
                
                // 查询有害食物
                List<Object> badFoods = medicalRepository.findBadFoodsByDisease(disease.getName());
                if (!badFoods.isEmpty()) {
                    result.append("禁忌食物：");
                    badFoods.forEach(food -> result.append(food.toString()).append("；"));
                    result.append("\n");
                }
                
                return result.toString();
            }
        }
        return "";
    }
    
    private String handleDepartmentQuery(String question) {
        String[] keywords = extractKeywords(question);
        for (String keyword : keywords) {
            List<Disease> diseases = medicalRepository.findDiseasesByName(keyword);
            if (!diseases.isEmpty()) {
                Disease disease = diseases.get(0);
                StringBuilder result = new StringBuilder();
                result.append(disease.getName()).append("建议就诊科室：\n");
                
                List<Object> departments = medicalRepository.findDepartmentsByDisease(disease.getName());
                if (!departments.isEmpty()) {
                    departments.forEach(dept -> result.append("• ").append(dept.toString()).append("\n"));
                } else {
                    result.append("请咨询医院导诊台获取准确的科室信息。");
                }
                
                return result.toString();
            }
        }
        return "";
    }
    
    private String handleGeneralQuery(String question) {
        String[] keywords = extractKeywords(question);
        StringBuilder result = new StringBuilder();
        
        for (String keyword : keywords) {
            if (!isStopWord(keyword)) {
                List<Disease> diseases = medicalRepository.findDiseasesByKeyword(keyword);
                if (!diseases.isEmpty()) {
                    result.append("关于").append(keyword).append("的相关信息：\n");
                    for (Disease disease : diseases) {
                        result.append("• ").append(disease.getName());
                        if (disease.getDesc() != null && !disease.getDesc().isEmpty()) {
                            result.append("：").append(disease.getDesc().substring(0, 
                                Math.min(50, disease.getDesc().length()))).append("...");
                        }
                        result.append("\n");
                    }
                    break;
                }
            }
        }
        
        return result.toString();
    }
    
    private String[] extractKeywords(String question) {
        return question.replaceAll("[？?！!。.,，]", "").split("\\s+");
    }
    
    private boolean isStopWord(String word) {
        String[] stopWords = {"什么", "是", "的", "了", "有", "在", "和", "与", "或者", "如何", "怎么", "哪些", "什么样"};
        return Arrays.asList(stopWords).contains(word) || word.length() < 2;
    }
}