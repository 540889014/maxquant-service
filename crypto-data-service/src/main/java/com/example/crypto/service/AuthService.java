package com.example.crypto.service;

import java.util.Map;

/**
 * 認証サービスインターフェース
 * ユーザログインとトークン発行を定義
 */
public interface AuthService {
    Map<String, String> login(String username, String password);

    boolean validateToken(String token);
}