package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.client.DeepSeekClient;
import com.medical.qna.medical_qna_system.client.SentenceBertService;
import com.medical.qna.medical_qna_system.dto.AnswerResponse;
import com.medical.qna.medical_qna_system.dto.DiseaseInfoDto;
import com.medical.qna.medical_qna_system.dto.QuestionRequest;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.entity.neo4j.Disease;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {
  
    private final QuestionAnswerRepository questionAnswerRepository;
    private final MedicalQueryService medicalQueryService;
    private final DeepSeekClient deepSeekClient;
    private final SentenceBertService sentenceBertService;
  
    @Transactional
    public AnswerResponse answerQuestion(QuestionRequest request, User user) {
        throw new UnsupportedOperationException("This method is not implemented yet.");
    }
        
  
    private String buildContext(DiseaseInfoDto diseaseInfo, List<QuestionAnswer> similarQuestions) {
        StringBuilder context = new StringBuilder();
      
        if (diseaseInfo != null) {
            context.append("相关疾病信息：\n");
            context.append("疾病名称：").append(diseaseInfo.getName()).append("\n");
          
            if (diseaseInfo.getDescription() != null) {
                context.append("疾病描述：").append(diseaseInfo.getDescription()).append("\n");
            }
          
            if (!diseaseInfo.getSymptoms().isEmpty()) {
                context.append("主要症状：").append(String.join("、", diseaseInfo.getSymptoms())).append("\n");
            }
          
            if (!diseaseInfo.getTreatments().isEmpty()) {
                context.append("治疗方式：").append(String.join("、", diseaseInfo.getTreatments())).append("\n");
            }
          
            if (diseaseInfo.getTreatmentDuration() != null) {
                context.append("治疗周期：").append(diseaseInfo.getTreatmentDuration()).append("\n");
            }
          
            if (diseaseInfo.getCureRate() != null) {
                context.append("治愈率：").append(diseaseInfo.getCureRate()).append("\n");
            }
          
            if (!diseaseInfo.getGoodFoods().isEmpty()) {
                context.append("推荐食物：").append(String.join("、", diseaseInfo.getGoodFoods())).append("\n");
            }
          
            if (!diseaseInfo.getBadFoods().isEmpty()) {
                context.append("忌口食物：").append(String.join("、", diseaseInfo.getBadFoods())).append("\n");
            }
          
            if (diseaseInfo.getNursing() != null) {
                context.append("护理建议：").append(diseaseInfo.getNursing()).append("\n");
            }
        }
      
        if (!similarQuestions.isEmpty()) {
            context.append("\n相似问题参考：\n");
            for (QuestionAnswer qa : similarQuestions) {
                context.append("Q: ").append(qa.getQuestion()).append("\n");
                context.append("A: ").append(qa.getAnswer()).append("\n\n");
            }
        }
      
        return context.toString();
    }
}
