package com.example.crypto.config;

import com.example.crypto.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * セキュリティ設定
 * JWT認証とエンドポイントのアクセス制御を定義
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("初始化 Spring Security 配置");
        try {
            http
                    .csrf().disable()
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/", "/index.html", "/static/**", "/js/**", "/css/**", "/images/**", "/fonts/**", "/**.js", "/**.css", "/**.png", "/**.jpg", "/**.jpeg", "/**.gif", "/**.svg", "/favicon.ico", "/error",
                                    "/api/v1/auth/**", "/api/v5/public/**", "/api/v1/public/**", "/ws/**", "/subscriptions-list.html",
                                    "/spread.html", "/spread.js",
                                    "/api/v1/data-correction/**"
                            )
                            .permitAll()
                            .anyRequest().authenticated()
                    )
                    .formLogin(form -> form
                            .loginPage("/index.html") // 指定登录页面为 index.html
                            .permitAll()
                    )
                    .exceptionHandling(eh -> eh
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                logger.error("アクセス拒否: URI={}, error={}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("アクセスが拒否されました: " + accessDeniedException.getMessage());
                            })
                            .authenticationEntryPoint((request, response, authException) -> {
                                logger.error("認証エラー: URI={}, error={}", request.getRequestURI(), authException.getMessage(), authException);
                                response.sendRedirect("/index.html");
                            })
                    )
                    .addFilterBefore(jwtFilter(), UsernamePasswordAuthenticationFilter.class);
            return http.build();
        } catch (Exception e) {
            logger.error("Security 配置初始化失敗: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure security", e);
        }
    }

    @Bean
    public Filter jwtFilter() {
        return new JwtFilter(jwtService);
    }

    class JwtFilter implements Filter {
        private final JwtService jwtService;

        public JwtFilter(JwtService jwtService) {
            this.jwtService = jwtService;
        }

        @Override
        public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response,
                             jakarta.servlet.FilterChain chain) throws IOException, jakarta.servlet.ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String requestURI = httpRequest.getRequestURI();

            // WebSocket路径的认证由ChannelInterceptor在STOMP CONNECT帧处理，这里直接放行HTTP请求以避免不必要的警告
            if (requestURI.startsWith("/ws/")) {
                chain.doFilter(request, response);
                return;
            }

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String authHeader = httpRequest.getHeader("Authorization");
            logger.debug("驗證請求: URI={}, Authorization={}", requestURI, authHeader != null ? "存在" : "無");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    String username = jwtService.getUsernameFromToken(token);
                    logger.info("Token 驗證成功: username={}", username);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username, null, Collections.emptyList());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    chain.doFilter(request, response);
                    return;
                } catch (Exception e) {
                    logger.error("Token 驗證失敗: token={}, error={}", token, e.getMessage(), e);
                    httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
                    return;
                }
            } else {
                logger.warn("請求缺少 Authorization 頭部: URI={}", requestURI);
            }
            chain.doFilter(request, response);
        }
    }
}