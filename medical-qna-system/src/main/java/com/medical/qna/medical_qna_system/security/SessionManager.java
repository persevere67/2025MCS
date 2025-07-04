package com.medical.qna.medical_qna_system.security;

import com.medical.qna.medical_qna_system.entity.mysql.User;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
public class SessionManager {
    
    private static final String USER_SESSION_KEY = "AUTH_USER";
    private static final String LOGIN_TIME_KEY = "LOGIN_TIME";
    private static final int SESSION_TIMEOUT = 1800; // 30分钟
    
    public void createUserSession(HttpSession session, User user) {
        session.setAttribute(USER_SESSION_KEY, user);
        session.setAttribute(LOGIN_TIME_KEY, LocalDateTime.now());
        session.setMaxInactiveInterval(SESSION_TIMEOUT);
        log.info("用户 {} 创建会话成功 | Session ID: {}", user.getUsername(), session.getId());
    }
    
    public User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(USER_SESSION_KEY);
    }
    
    public void invalidateSession(HttpSession session) {
        if (session != null) {
            try {
                User user = getCurrentUser(session);
                session.invalidate();
                if (user != null) {
                    log.info("用户 {} 会话已失效 | Session ID: {}", user.getUsername(), session.getId());
                }
            } catch (IllegalStateException e) {
                log.warn("会话已经失效: {}", e.getMessage());
            }
        }
    }
    
    public void refreshSession(HttpSession session) {
        if (session != null && isSessionValid(session)) {
            session.setMaxInactiveInterval(SESSION_TIMEOUT);
        }
    }
    
    public boolean isSessionValid(HttpSession session) {
        if (session == null) {
            return false;
        }
        
        try {
            return session.getAttribute(USER_SESSION_KEY) != null;
        } catch (IllegalStateException e) {
            log.warn("检查会话有效性时发生异常: {}", e.getMessage());
            return false;
        }
    }
}