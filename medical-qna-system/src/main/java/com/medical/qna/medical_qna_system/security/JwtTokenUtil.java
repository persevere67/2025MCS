package com.medical.qna.medical_qna_system.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "userId";
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expirationTime;
    
    private SecretKey key;
    
    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(getKeyBytesWithPadding(secret));
    }
    
    private byte[] getKeyBytesWithPadding(String base64Key) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Key);
        
        if (keyBytes.length < 64) {
            byte[] paddedKey = new byte[64];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 64));
            return paddedKey;
        }
        return keyBytes;
    }
    
    /**
     * 生成JWT Token (包含用户名、角色和用户ID)
     */
    public String generateToken(String username, String role, Long userId) {
        return Jwts.builder()
                   .setSubject(username)
                   .claim(CLAIM_ROLE, role)
                   .claim(CLAIM_USER_ID, userId)
                   .setIssuedAt(new Date())
                   .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                   .signWith(key)
                   .compact();
    }
    
    /**
     * 验证Token有效性
     */

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                                    .setSigningKey(key) // 确保密钥正确设置在这里
                                    .build()
                                    .parseClaimsJws(token); // 解析JWT
            System.out.println("收到的声明信息: " + claims.getBody());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }


    
    /**
     * 从Token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseTokenClaims(token).getSubject();
    }
    
    /**
     * 从Token中获取用户角色
     */
    public String getRoleFromToken(String token) {
        return parseTokenClaims(token).get(CLAIM_ROLE, String.class);
    }
    
    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        return parseTokenClaims(token).get(CLAIM_USER_ID, Long.class);
    }
    
    /**
     * 解析Token声明
     */
    private Claims parseTokenClaims(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(key) // 确保密钥正确设置在这里
                   .build()
                   .parseClaimsJws(token) // 解析JWT
                   .getBody();
    }
}