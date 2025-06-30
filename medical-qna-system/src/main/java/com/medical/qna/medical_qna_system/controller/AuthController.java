package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.LoginRequest;
import com.medical.qna.medical_qna_system.dto.RegisterRequest;
import com.medical.qna.medical_qna_system.entity.mysql.User;  // 修复导入路径
import com.medical.qna.medical_qna_system.service.AuthService;
import com.medical.qna.medical_qna_system.config.SessionUtils;  // 修复导入路径
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"}, allowCredentials = "true")
@Slf4j
public class AuthController {
  
    private final AuthService authService;
  
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, 
                                    HttpServletRequest httpRequest) {
        try {
            User user = authService.register(request);
          
            // 注册后自动登录
            HttpSession session = SessionUtils.getOrCreateSession(httpRequest);
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(request.getUsername());
            loginRequest.setPassword(request.getPassword());
            
            authService.login(loginRequest, session);
          
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "注册成功");
            response.put("user", buildUserResponse(user));
          
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("用户注册失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
  
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, 
                                 HttpServletRequest httpRequest) {
        try {
            HttpSession session = SessionUtils.getOrCreateSession(httpRequest);
            User user = authService.login(request, session);
          
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("user", buildUserResponse(user));
          
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("用户登录失败: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
  
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            HttpSession session = SessionUtils.getSession(request);
            authService.logout(session);
          
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "登出成功");
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("用户登出失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "登出失败");
            return ResponseEntity.badRequest().body(response);
        }
    }
  
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            HttpSession session = SessionUtils.getSession(request);
          
            if (session == null || !SessionUtils.isSessionValid(session)) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "未登录或Session已过期");
                return ResponseEntity.status(401).body(response);
            }
          
            User currentUser = SessionUtils.getCurrentUser(session);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("user", buildUserResponse(currentUser));
          
            return ResponseEntity.ok(response);
          
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "系统错误");
            return ResponseEntity.status(500).body(response);
        }
    }
  
    @GetMapping("/check")
    public ResponseEntity<?> checkSession(HttpServletRequest request) {
        HttpSession session = SessionUtils.getSession(request);
        Map<String, Object> response = new HashMap<>();
      
        if (session != null && SessionUtils.isSessionValid(session)) {
            User user = SessionUtils.getCurrentUser(session);
            response.put("authenticated", true);
            response.put("sessionValid", true);
            response.put("username", user != null ? user.getUsername() : null);
        } else {
            response.put("authenticated", false);
            response.put("sessionValid", false);
        }
      
        return ResponseEntity.ok(response);
    }
  
    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", user.getRole());
        userInfo.put("createAt", user.getCreateAt());
        return userInfo;
    }
}