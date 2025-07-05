package com.medical.qna.medical_qna_system.controller;

import com.medical.qna.medical_qna_system.dto.request.LoginRequest;
import com.medical.qna.medical_qna_system.dto.request.RegisterRequest;
import com.medical.qna.medical_qna_system.dto.response.ApiResponse;
import com.medical.qna.medical_qna_system.dto.response.UserDto;
import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.security.JwtTokenUtil;
import com.medical.qna.medical_qna_system.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, allowCredentials = "true")
public class AuthController {
  
    private final AuthService authService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            
            // 生成JWT token
            String token = jwtTokenUtil.generateToken(
                user.getUsername(), 
                user.getRole().name(), 
                user.getId()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", UserDto.fromEntity(user));
            data.put("expiresIn", 86400); // 24小时
            
            return ResponseEntity.ok(ApiResponse.success("注册成功", data));
        } catch (Exception e) {
            log.error("注册失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("REGISTRATION_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = authService.login(request);
            
            // 生成JWT token
            String token = jwtTokenUtil.generateToken(
                user.getUsername(), 
                user.getRole().name(), 
                user.getId()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", UserDto.fromEntity(user));
            data.put("expiresIn", 86400000); // 24小时
            
            return ResponseEntity.ok(ApiResponse.success("登录成功", data));
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTHENTICATION_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT是无状态的，前端删除token即可实现登出
        return ResponseEntity.ok(ApiResponse.success("登出成功", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(HttpServletRequest request) {
        try {
            // 从request中获取当前用户信息（由JwtAuthenticationFilter设置）
            User currentUser = (User) request.getAttribute("currentUser");
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", "请重新登录"));
            }
            
            // 生成新的token
            String newToken = jwtTokenUtil.generateToken(
                currentUser.getUsername(), 
                currentUser.getRole().name(), 
                currentUser.getId()
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", newToken);
            data.put("expiresIn", 86400);
            
            return ResponseEntity.ok(ApiResponse.success("Token刷新成功", data));
        } catch (Exception e) {
            log.error("Token刷新失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("TOKEN_REFRESH_FAILED", "Token刷新失败"));
        }
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(HttpServletRequest request) {
        try {
            User currentUser = (User) request.getAttribute("currentUser");
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("UNAUTHORIZED", "请重新登录"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", UserDto.fromEntity(currentUser)));
        } catch (Exception e) {
            log.error("获取当前用户失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "获取用户信息失败"));
        }
    }
    
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAuth(HttpServletRequest request) {
        User currentUser = (User) request.getAttribute("currentUser");
        
        Map<String, Object> data = new HashMap<>();
        if (currentUser != null) {
            data.put("authenticated", true);
            data.put("user", UserDto.fromEntity(currentUser));
        } else {
            data.put("authenticated", false);
        }
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}