package com.example.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

/**
 * アプリケーションのメインクラス
 */
@SpringBootApplication
@EnableScheduling
public class Application {

    @jakarta.annotation.PostConstruct
    public void init() {
        // 設置默認時區為 HKT
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Hong_Kong"));
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}