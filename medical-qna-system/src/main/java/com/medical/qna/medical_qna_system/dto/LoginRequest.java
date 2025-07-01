package com.medical.qna.medical_qna_system.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
  
    @NotBlank(message = "密码不能为空")
    private String password;
}