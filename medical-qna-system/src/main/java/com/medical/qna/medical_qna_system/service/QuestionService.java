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
    private Neo4jQueryService neo4jService;
    
    @Autowired
    private UserRepository userRepository;
    
    public AnswerResponse processQuestion(String username, QuestionRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        String answer = neo4jService.queryKnowledgeGraph(request.getQuestion());
        
        QuestionAnswer qa = new QuestionAnswer();
        qa.setUser(user);
        qa.setQuestion(request.getQuestion());
        qa.setAnswer(answer);
        qaRepository.save(qa);
        
        return new AnswerResponse(answer, LocalDateTime.now());
    }
}