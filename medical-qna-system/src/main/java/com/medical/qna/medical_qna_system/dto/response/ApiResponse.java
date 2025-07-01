package com.medical.qna.medical_qna_system.dto.response;


import com.medical.qna.medical_qna_system.common.enums.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private String code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
            ErrorCode.SUCCESS.getCode(),
            ErrorCode.SUCCESS.getMessage(),
            data,
            LocalDateTime.now()
        );
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
            ErrorCode.SUCCESS.getCode(),
            message,
            data,
            LocalDateTime.now()
        );
    }
    
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(
            errorCode.getCode(),
            errorCode.getMessage(),
            null,
            LocalDateTime.now()
        );
    }
    
    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }
}