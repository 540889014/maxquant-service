package com.example.crypto.service;

import com.example.crypto.dto.FcsApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class FcsApiService {

    private final RestTemplate restTemplate;

    @Value("${fcsapi.base-url}")
    private String baseUrl;

    @Value("${fcsapi.access-key}")
    private String accessKey;

    public FcsApiService() {
        this.restTemplate = new RestTemplate();
    }

    public FcsApiResponseDto fetchHistory(String symbol, String period) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("symbol", symbol)
                .queryParam("period", period)
                .queryParam("access_key", accessKey)
                .toUriString();

        return restTemplate.getForObject(url, FcsApiResponseDto.class);
    }
} 