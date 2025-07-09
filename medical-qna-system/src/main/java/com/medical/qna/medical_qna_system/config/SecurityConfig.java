package com.medical.qna.medical_qna_system.config;

import com.medical.qna.medical_qna_system.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // 禁用CSRF
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // 配置CORS
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 无状态会话
            
            .authorizeHttpRequests(auth -> auth
                // 1. 公开访问的API接口 (最具体，最优先匹配)
                .requestMatchers(
                    "/api/auth/**",            // 认证相关接口 (登录、注册)
                    "/health",          // 后端统一的健康检查接口
                    "/api/qa/spring-health",   // Spring Boot 服务健康检查接口
                    "/api/qa/ask",             // 问答API (流式和非流式)
                    "/api/qa/test-python",     // 测试Python服务的API
                    "/swagger-ui/**",          // Swagger UI 路径
                    "/v3/api-docs/**"          // OpenAPI 文档路径
                ).permitAll()
                
                // 2. 管理员API接口 (次优先匹配)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 3. 其他所有 /api/** 路径需要认证 (通用API保护规则)
                .requestMatchers("/api/**").authenticated()

                // 4. 允许所有前端路由和静态资源访问 (最不具体，最后处理)
                // 这确保了即使没有认证，前端页面和其资源也能被加载
                .requestMatchers(
                    "/", "/index.html", "/favicon.ico", "/vite.svg", "/error", // 根路径和基本文件
                    "/qna", "/admin", "/auth", "/home", // Vue 路由页面路径
                    "/static/**", "/assets/**", "/css/**", "/js/**", "/img/**", "/fonts/**", // 静态资源目录
                    "/*.css", "/*.js", "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", "/*.ico", // 常见静态文件扩展名
                    "/*.woff", "/*.woff2", "/*.ttf", "/*.eot", "/*.html" // 字体和HTML文件
                ).permitAll()
                
                // 5. 任何其他未明确匹配的请求，默认需要认证 (兜底规则)
                // 确保没有被前面规则匹配到的请求都需要认证
                .anyRequest().authenticated()
            )
            
            // 添加JWT过滤器，在UsernamePasswordAuthenticationFilter之前执行
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源，生产环境应限制为特定域名，例如：Arrays.asList("http://localhost:5173", "https://your-frontend-domain.com")
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); 
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 允许的HTTP方法
        configuration.setAllowedHeaders(Arrays.asList("*")); // 允许所有请求头
        configuration.setAllowCredentials(true); // 允许发送凭证（如Cookie, Authorization头）
        configuration.setMaxAge(3600L); // 预检请求的缓存时间

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 对所有路径应用CORS配置
        return source;
    }
}
