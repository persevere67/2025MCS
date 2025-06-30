package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.AnswerResponse;
import com.medical.qna.medical_qna_system.dto.QuestionRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.repository.mysql.QuestionAnswerRepository;
import com.medical.qna.medical_qna_system.config.SessionUtils;  // 修复导入路径
import com.medical.qna.medical_qna_system.service.QuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/question")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
@Slf4j
public class QuestionController {
  
    private final QuestionService questionService;
    private final QuestionAnswerRepository questionAnswerRepository;
  
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@Valid @RequestBody QuestionRequest request, 
                                       HttpServletRequest httpRequest) {
        try {
            HttpSession session = SessionUtils.getSession(httpRequest);
          
            if (session == null || !SessionUtils.isSessionValid(session)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }
          
            User currentUser = SessionUtils.getCurrentUser(session);
            AnswerResponse answer = questionService.answerQuestion(request, currentUser);
          
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", answer);
          
            log.info("用户 {} 提问成功", currentUser.getUsername());
          
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("处理问题失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
  
    @GetMapping("/history")
    public ResponseEntity<?> getQuestionHistory(HttpServletRequest request,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        try {
            HttpSession session = SessionUtils.getSession(request);
          
            if (session == null || !SessionUtils.isSessionValid(session)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请先登录");
                return ResponseEntity.status(401).body(response);
            }
          
            User currentUser = SessionUtils.getCurrentUser(session);
            Page<QuestionAnswer> history = questionAnswerRepository
                    .findByUserOrderByCreateAtDesc(currentUser, PageRequest.of(page, size));
          
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
          
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("获取问答历史失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "系统错误");
            return ResponseEntity.status(500).body(response);
        }
    }
  
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentQuestions() {
        try {
            var recentQuestions = questionAnswerRepository.findTop10ByOrderByCreateAtDesc();
          
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", recentQuestions);
          
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("获取最近问题失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "系统错误");
            return ResponseEntity.status(500).body(response);
        }
    }
}