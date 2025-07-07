package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
import com.medical.qna.medical_qna_system.entity.mysql.QuestionAnswer;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;


    /**
     * 获取所有用户信息
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("获取所有用户信息成功", users));
    }

    /**
     * 根据用户ID获取用户信息
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long userId) {
        User user = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("获取用户信息成功", user));
    }

    /**
     * 获取用户问答历史
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<ApiResponse<Page<QuestionAnswerDto>>> getUserQuestionHistory(@PathVariable Long userId, Pageable pageable) {
        Page<QuestionAnswerDto> history = adminService.getUserQuestionHistory(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("获取用户问答历史成功", history));
    }

    @PutMapping("/question-answers/{id}")
    public ResponseEntity<ApiResponse<QuestionAnswer>> updateQuestionAnswer(@PathVariable Long id, @Valid @RequestBody QuestionAnswerDto request) {
        QuestionAnswer questionAnswer = adminService.updateQuestionAnswer(id, request);
        return ResponseEntity.ok(ApiResponse.success("问答记录更新成功", questionAnswer));
    }

    /**
     * 删除问答记录
     */
    @DeleteMapping("/question-answers/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuestionAnswer(@PathVariable Long id) {
        adminService.deleteQuestionAnswer(id);
        return ResponseEntity.ok(ApiResponse.success("问答记录删除成功", null));
    }


    @GetMapping("/question-answers")
    public ResponseEntity<ApiResponse<Page<QuestionAnswerDto>>> getAllQuestionAnswers(Pageable pageable) {
        Page<QuestionAnswerDto> allRecords = adminService.getAllQuestionAnswers(pageable);
        return ResponseEntity.ok(ApiResponse.success("获取所有问答记录成功", allRecords));
    }

}