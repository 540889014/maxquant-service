package com.example.crypto.controller;

import com.example.crypto.models.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * カスタムエラーコントローラ
 * エラーハンドリングをカスタマイズ
 */
@RestController
public class CustomErrorController implements ErrorController {
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public ApiResponse<Void> handleError(HttpServletRequest request, HttpServletResponse response) {
        Integer statusCode = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        String requestUri = (String) request.getAttribute("jakarta.servlet.error.request_uri");
        Throwable throwable = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        String message = (throwable != null) ? throwable.getMessage() : (String) request.getAttribute("jakarta.servlet.error.message");

        if (statusCode == null) {
            statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        }

        response.setStatus(statusCode);

        // Log the error
        if (throwable != null) {
            logger.error("Error occurred: status={}, uri={}, message={}", statusCode, requestUri, message, throwable);
        } else {
            logger.error("Error occurred: status={}, uri={}, message={}", statusCode, requestUri, message);
        }
        
        // Don't redirect for API calls, just return the structured error
        if (requestUri != null && requestUri.startsWith("/api/")) {
             return ApiResponse.fail(statusCode, message);
        }
        
        // Redirect for 401 on non-api routes
        if (statusCode == HttpStatus.UNAUTHORIZED.value()) {
            try {
                response.sendRedirect("/index.html");
                // Returning null because the response is already handled by the redirect.
                return null; 
            } catch (Exception e) {
                logger.error("Redirect failed for 401 error: {}", e.getMessage(), e);
                // Fallback to returning an error response
                return ApiResponse.fail(statusCode, "Authentication required, redirect failed.");
            }
        }
        
        // For other errors on non-api routes, you might still want to return a JSON error
        // or redirect to a generic error page. Returning JSON here for consistency.
        return ApiResponse.fail(statusCode, message != null ? message : "An unexpected error occurred.");
    }
}