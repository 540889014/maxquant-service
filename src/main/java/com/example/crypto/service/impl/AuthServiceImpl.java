package com.example.crypto.service.impl;

import com.example.crypto.dao.UserRepository;
import com.example.crypto.entity.User;
import com.example.crypto.service.AuthService;
import com.example.crypto.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * 認証サービスの実施クラス
 * ユーザログインとJWTトークン発行を処理
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthServiceImpl(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, String> login(String username, String password) {
        logger.info("ユーザログイン試行: username={}", username);
        try {
            return userRepository.findByUsername(username)
                .map(user -> {
                    if (user.getPassword().equals(password)) {
                        String token = jwtService.generateToken(username);
                        logger.info("ログイン成功: username={}", username);
                        return Map.of("token", token);
                    } else {
                        logger.warn("ログイン失敗: username={}", username);
                        throw new RuntimeException("Invalid credentials");
                    }
                })
                .orElseThrow(() -> {
                    logger.warn("ログイン失敗: username={}", username);
                    return new RuntimeException("Invalid credentials");
                });
        } catch (Exception e) {
            logger.error("ログイン処理に失敗: username={}, error={}", username, e.getMessage(), e);
            throw new RuntimeException("Failed to login", e);
        }
    }

    @Override
    public boolean validateToken(String token) {
        try {
            String username = jwtService.getUsernameFromToken(token);
            return username != null && !username.isEmpty();
        } catch (Exception e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}