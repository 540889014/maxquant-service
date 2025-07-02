package com.example.crypto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FcsApiResponseDto {
    private boolean status;
    private int code;
    private String msg;
    private Map<String, FcsApiKlineDto> response;
} 