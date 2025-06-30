package com.medical.qna.medical_qna_system.config;

import com.medical.qna.medical_qna_system.entity.mysql.User;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class SessionUtils {
  
    private static final String USER_SESSION_KEY = "current_user";
    private static final String LOGIN_TIME_KEY = "login_time";
  
    public static void setCurrentUser(HttpSession session, User user) {
        session.setAttribute(USER_SESSION_KEY, user);
        session.setAttribute(LOGIN_TIME_KEY, LocalDateTime.now());
        log.info("用户 {} 已登录，Session ID: {}", user.getUsername(), session.getId());
    }
  
    public static User getCurrentUser(HttpSession session) {
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute(USER_SESSION_KEY);
    }
  
    public static void clearCurrentUser(HttpSession session) {
        if (session != null) {
            User user = getCurrentUser(session);
            if (user != null) {
                log.info("用户 {} 已登出，Session ID: {}", user.getUsername(), session.getId());
            }
          
            session.removeAttribute(USER_SESSION_KEY);
            session.removeAttribute(LOGIN_TIME_KEY);
          
            try {
                session.invalidate();
            } catch (IllegalStateException e) {
                log.warn("Session已经失效: {}", e.getMessage());
            }
        }
    }
  
    public static boolean isSessionValid(HttpSession session) {
        if (session == null) {
            return false;
        }
      
        try {
            User user = getCurrentUser(session);
            return user != null;
        } catch (IllegalStateException e) {
            log.warn("Session无效: {}", e.getMessage());
            return false;
        }
    }
  
    public static HttpSession getSession(HttpServletRequest request) {
        return request.getSession(false);
    }
  
    public static HttpSession getOrCreateSession(HttpServletRequest request) {
        return request.getSession(true);
    }
}