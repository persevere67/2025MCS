package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.request.LoginRequest;
import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.SessionStatus;
import com.medical.qna.medical_qna_system.dto.response.UserDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.security.SessionManager;
import com.medical.qna.medical_qna_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
  
    private final AuthService authService;
    private final SessionManager sessionManager;
  
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserDto>> register(
            @Valid @RequestBody RegisterRequest request, 
            HttpServletRequest httpRequest) {
            
        User user = authService.register(request);
        
        // 注册后自动登录
        HttpSession session = httpRequest.getSession(true);
        sessionManager.createUserSession(session, user);
        
        return ResponseEntity.ok(ApiResponse.success(
            "注册成功", 
            UserDto.fromEntity(user)
        ));
    }
  
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto>> login(
            @Valid @RequestBody LoginRequest request, 
            HttpServletRequest httpRequest) {
            
        User user = authService.login(request);
        
        HttpSession session = httpRequest.getSession(true);
        sessionManager.createUserSession(session, user);
        
        return ResponseEntity.ok(ApiResponse.success(
            "登录成功", 
            UserDto.fromEntity(user)
        ));
    }
  
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionManager.invalidateSession(session);
        }
        
        return ResponseEntity.ok(ApiResponse.success("登出成功", null));
    }
  
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session == null || !sessionManager.isSessionValid(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "未登录或Session已过期"));
        }
        
        User currentUser = sessionManager.getCurrentUser(session);
        return ResponseEntity.ok(ApiResponse.success(
            "获取成功", 
            UserDto.fromEntity(currentUser)
        ));
    }
  
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<SessionStatus>> checkSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        SessionStatus status = new SessionStatus();
        
        if (session != null && sessionManager.isSessionValid(session)) {
            User user = sessionManager.getCurrentUser(session);
            status.setAuthenticated(true);
            status.setSessionValid(true);
            status.setUsername(user != null ? user.getUsername() : null);
            status.setRole(user != null ? user.getRole() : null);
        } else {
            status.setAuthenticated(false);
            status.setSessionValid(false);
        }
        
        return ResponseEntity.ok(ApiResponse.success("检查完成", status));
    }
}