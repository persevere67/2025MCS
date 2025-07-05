package com.medical.qna.medical_qna_system.filter;

import com.medical.qna.medical_qna_system.entity.mysql.User;
import com.medical.qna.medical_qna_system.security.JwtTokenUtil;
import com.medical.qna.medical_qna_system.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserService userService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 不需要JWT认证的路径列表 - 使用通配符匹配
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
        "/api/auth/**",              // 所有认证相关接口
        "/api/question/spring-health", // Spring健康检查
        "/",                         // 根路径
        "/index.html",               // 首页
        "/favicon.ico",              // 图标
        "/vite.svg",                 // Vite图标
        "/static/**",                // 静态资源
        "/assets/**",                // 前端资源
        "/css/**",                   // CSS资源
        "/js/**",                    // JS资源
        "/img/**",                   // 图片资源
        "/fonts/**",                 // 字体资源
        "/error"                     // 错误页面
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 对于OPTIONS预检请求跳过认证
        if ("OPTIONS".equals(method)) {
            log.debug("跳过JWT认证 - OPTIONS预检请求: {}", path);
            return true;
        }
        
        // 检查是否在排除路径中 - 使用通配符匹配
        boolean shouldExclude = EXCLUDE_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        if (shouldExclude) {
            log.debug("跳过JWT认证 - 匹配排除路径: {} -> {}", path, method);
        } else {
            log.debug("需要JWT认证 - 路径: {} -> {}", path, method);
        }
        
        return shouldExclude;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        log.debug("JWT Filter 处理需要认证的请求: {}", path);
        
        final String requestTokenHeader = request.getHeader("Authorization");
        
        String username = null;
        String jwtToken = null;
        
        // JWT Token在格式为 "Bearer token"
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                username = jwtTokenUtil.getUsernameFromToken(jwtToken);
                log.debug("从JWT中解析到用户名: {}", username);
            } catch (Exception e) {
                log.error("无法从token中获取用户名: {}", e.getMessage());
            }
        } else {
            log.debug("请求头中没有找到Bearer token - Path: {}", path);
        }
        
        // 得到token并验证它
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            
            try {
                // 验证token
                if (jwtTokenUtil.validateToken(jwtToken)) {
                    
                    // 从数据库获取用户信息
                    User user = userService.findByUsername(username);
                    if (user != null) {
                        
                        // 将用户信息存储到request中，供Controller使用
                        request.setAttribute("currentUser", user);
                        
                        // 从token中获取用户角色
                        String role = jwtTokenUtil.getRoleFromToken(jwtToken);
                        
                        // 创建认证对象
                        UsernamePasswordAuthenticationToken authenticationToken = 
                            new UsernamePasswordAuthenticationToken(
                                username, 
                                null, 
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                        
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // 设置认证信息到SecurityContext
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                        
                        log.debug("用户 {} 认证成功，角色: {}", username, role);
                    } else {
                        log.warn("JWT中的用户 {} 在数据库中不存在", username);
                    }
                } else {
                    log.warn("JWT Token 验证失败，路径: {}", path);
                }
            } catch (Exception e) {
                log.error("JWT认证处理异常: {}", e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}