package com.medical.qna.medical_qna_system.dto.response;

import com.medical.qna.medical_qna_system.common.enums.UserRole;
import lombok.Data;

@Data
public class SessionStatus {
    private boolean authenticated;
    private boolean sessionValid;
    private String username;
    private UserRole role;
}