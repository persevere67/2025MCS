// src/main/java/com/medical/qna/medical_qna_system/controller/QnaController.java
package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.AssociateResponse;
import com.medical.qna.medical_qna_system.dto.AnswerRequest;
import com.medical.qna.medical_qna_system.dto.AnswerResponse;
import com.medical.qna.medical_qna_system.service.kgqa.KgQaService; // 引入 KgQaService
// import com.medical.qna.medical_qna_system.service.MedicalQnAService; // 假设您的模型问答服务

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/qna") // 统一的API前缀
public class QnaController {

    private static final Logger logger = LoggerFactory.getLogger(QnaController.class);

    // 假设您的模型问答服务，如果需要的话可以注入
    // @Autowired
    // private MedicalQnAService medicalQnAService;

    @Autowired
    private KgQaService kgQaService; // 注入新的知识图谱问答服务

    // 如果有现有的模型问答API，可以保持或调整
    // @PostMapping("/model-ask")
    // public ResponseEntity<AnswerResponse> askModel(@RequestBody QuestionRequest request) {
    //     logger.info("Received model-based question: {}", request.getQuestion());
    //     String answer = medicalQnAService.askQuestion(request.getQuestion()); // 假设这个方法存在
    //     return ResponseEntity.ok(new AnswerResponse(answer));
    // }

    /**
     * 处理用户初始问题，进行语义理解并生成联想问题。
     * API: POST /api/qna/kg/associate
     * 请求体: {"question": "用户问题"}
     * 响应体: {"associatedQuestions": ["联想问题1", "联想问题2", ...]}
     */
    @PostMapping("/kg/associate")
    public ResponseEntity<AssociateResponse> getAssociatedQuestions(@RequestBody QuestionRequest request) {
        logger.info("Received request for KG association for question: {}", request.getQuestion());
        AssociateResponse response = kgQaService.processInitialQuestionForAssociation(request.getQuestion());
        return ResponseEntity.ok(response);
    }

    /**
     * 根据用户选择的联想问题（或原始问题），从知识图谱中获取并生成答案。
     * API: POST /api/qna/kg/answer
     * 请求体: {"selectedQuestion": "用户选择的联想问题"}
     * 响应体: {"answer": "知识图谱生成的答案"}
     */
    @PostMapping("/kg/answer")
    public ResponseEntity<AnswerResponse> getKgAnswer(@RequestBody AnswerRequest request) {
        logger.info("Received request for KG answer for selected question: {}", request.getSelectedQuestion());
        AnswerResponse response = kgQaService.getAnswerFromKnowledgeGraph(request.getSelectedQuestion());
        return ResponseEntity.ok(response);
    }
}