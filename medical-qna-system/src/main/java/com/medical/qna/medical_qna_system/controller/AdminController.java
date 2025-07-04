package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.dto.request.UpdateUserRequest;
import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.QuestionAnswerDto;
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
     * 添加用户
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<User>> addUser(@Valid @RequestBody RegisterRequest request) {
        User user = adminService.addUser(request);
        return ResponseEntity.ok(ApiResponse.success("用户添加成功", user));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("用户删除成功", null));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        User user = adminService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("用户信息更新成功", user));
    }

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
}