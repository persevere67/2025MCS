package com.medical.qna.medical_qna_system.dto.response;

import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.common.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserDto {
    private Long id;
    private String username;
    private String email;
    private UserRole role;
    private LocalDateTime createAt;
    private boolean enabled;
    
    public static UserDto fromEntity(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createAt(user.getCreateAt())
                .enabled(user.isEnabled())
                .build();
    }
}