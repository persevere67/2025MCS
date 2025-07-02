package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.request.QuestionRequest;
import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.security.SessionManager;
import com.medical.qna.medical_qna_system.service.QuestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@Slf4j
public class QuestionController {
    
    private final QuestionService questionService;
    private final SessionManager sessionManager;
    
    /**
     * 用户提问接口
     */
    @PostMapping("/ask")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuestionAnswerDto>> askQuestion(
            @Valid @RequestBody QuestionRequest request,
            HttpServletRequest httpRequest) {
        
        HttpSession session = httpRequest.getSession(false);
        User currentUser = sessionManager.getCurrentUser(session);
        
        log.info("收到用户 {} 的提问: {}", currentUser.getUsername(), request.getQuestion());
        
        QuestionAnswerDto result = questionService.processQuestion(request.getQuestion(), currentUser);
        
        return ResponseEntity.ok(ApiResponse.success("问答处理成功", result));
    }
    
    /**
     * 获取用户的问答历史
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> getQuestionHistory(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        User currentUser = sessionManager.getCurrentUser(session);
        

        String message = String.format("用户 %s 的问答历史功能正在开发中...", currentUser.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success("获取成功", message));
    }
}