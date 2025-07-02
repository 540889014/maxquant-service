package com.example.crypto.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FcsApiKlineDto {
    @JsonProperty("o")
    private String open;
    @JsonProperty("h")
    private String high;
    @JsonProperty("l")
    private String low;
    @JsonProperty("c")
    private String close;
    @JsonProperty("t")
    private long timestamp;
    @JsonProperty("tm")
    private String time;
} 