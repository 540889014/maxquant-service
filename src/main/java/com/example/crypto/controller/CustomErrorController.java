package com.example.crypto.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * カスタムエラーコントローラ
 * エラーハンドリングをカスタマイズ
 */
@RestController
public class CustomErrorController implements ErrorController {
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> handleError(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        String requestUri = (String) request.getAttribute("javax.servlet.error.request_uri");
        String message = (String) request.getAttribute("javax.servlet.error.message");
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorDetails.put("path", requestUri != null ? requestUri : "不明");
        errorDetails.put("message", message != null ? message : "不明なエラー");

        if (throwable != null) {
            errorDetails.put("exception", throwable.getClass().getName());
            errorDetails.put("exceptionMessage", throwable.getMessage());
            logger.error("エラー発生: statusCode={}, requestUri={}, message={}, exception={}", statusCode, requestUri, message, throwable.getMessage(), throwable);
        } else {
            logger.error("エラー発生: statusCode={}, requestUri={}, message={}", statusCode, requestUri, message);
        }

        if (statusCode != null) {
            if (statusCode == 403) {
                errorDetails.put("error", "アクセスが拒否されました");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorDetails);
            } else if (statusCode == 401) {
                try {
                    jakarta.servlet.http.HttpServletResponse httpResponse = (jakarta.servlet.http.HttpServletResponse) request;
                    httpResponse.sendRedirect("/index.html");
                    return null;
                } catch (Exception e) {
                    logger.error("リダイレクトエラー: {}", e.getMessage(), e);
                    errorDetails.put("error", "認証が必要です");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorDetails);
                }
            } else if (statusCode == 500) {
                errorDetails.put("error", "サーバーエラー");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
            }
        }
        errorDetails.put("error", "不明なエラー");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDetails);
    }
}