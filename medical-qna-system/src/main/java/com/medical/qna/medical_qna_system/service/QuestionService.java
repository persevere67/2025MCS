package com.medical.qna.medical_qna_system.service;

import com.medical.qna.medical_qna_system.dto.AnswerResponse;
import com.medical.qna.medical_qna_system.dto.QuestionRequest;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import com.medical.qna.medical_qna_system.repository.mysql.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class QuestionService {
    @Autowired
    private QuestionAnswerRepository qaRepository;
    
    @Autowired
    private MedicalQueryService medicalQueryService;  // 更新为医疗查询服务
    
    @Autowired
    private UserRepository userRepository;
    
    public AnswerResponse processQuestion(String username, QuestionRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 使用医疗知识图谱查询服务
        String answer = medicalQueryService.queryMedicalKnowledge(request.getQuestion());
        
        // 保存问答记录到MySQL
        QuestionAnswer qa = new QuestionAnswer();
        qa.setUser(user);
        qa.setQuestion(request.getQuestion());
        qa.setAnswer(answer);
        qaRepository.save(qa);
        
        return new AnswerResponse(answer, LocalDateTime.now());
    }
}