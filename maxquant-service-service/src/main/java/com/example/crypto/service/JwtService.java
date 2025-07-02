package com.example.crypto.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Date;

/**
 * JWTトークン生成サービス
 * ユーザ認証用のトークンを生成および検証
 */
@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String generateToken(String username) {
        logger.debug("生成 JWT: username={}", username);
        try {
            return Jwts.builder()
                    .claim("sub", username)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expiration))
                    .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS512)
                    .compact();
        } catch (Exception e) {
            logger.error("JWT 生成失敗: username={}, error={}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to generate token", e);
        }
    }

    public String getUsernameFromToken(String token) {
        logger.debug("驗證 JWT: token={}", token);
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String username = claims.get("sub", String.class);
            logger.debug("JWT 驗證成功: username={}", username);
            return username;
        } catch (Exception e) {
            logger.error("JWT 驗證失敗: token={}, error={}", token, e.getMessage(), e);
            throw new RuntimeException("Invalid token", e);
        }
    }
}