package com.medical.qna.medical_qna_system.filter;

import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.security.SessionManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionAuthFilter extends OncePerRequestFilter {
    
    private final SessionManager sessionManager;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain chain) throws IOException, ServletException {
        
        HttpSession session = request.getSession(false);
        
        if (session != null && sessionManager.isSessionValid(session)) {
            User user = sessionManager.getCurrentUser(session);
            
            if (user != null) {
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
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") || 
               path.startsWith("/assets/") || 
               path.equals("/") || 
               path.equals("/index.html");
    }
}