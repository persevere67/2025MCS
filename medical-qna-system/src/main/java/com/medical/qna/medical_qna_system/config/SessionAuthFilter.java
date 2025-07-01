package com.medical.qna.medical_qna_system.config;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import jakarta.servlet.ServletException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Collections;

import com.medical.qna.medical_qna_system.entity.mysql.User;

@Component  
public class SessionAuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain chain) throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        
        // 1. 检查 Session 中是否存在认证信息
        if (session != null && session.getAttribute("AUTH_USER") != null) {
            User user = (User) session.getAttribute("AUTH_USER");
            
            // 2. 构建 Spring Security 认证对象
            List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );
            
            Authentication auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), 
                null, 
                authorities
            );
            
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        
        chain.doFilter(request, response);
    }
}