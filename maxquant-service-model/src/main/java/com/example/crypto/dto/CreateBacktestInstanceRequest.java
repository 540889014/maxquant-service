package com.example.crypto.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBacktestInstanceRequest {

    @NotEmpty(message = "Name cannot be empty")
    private String name;

    @NotNull(message = "Strategy template ID cannot be null")
    private Long strategyTemplateId;

    @NotEmpty(message = "Params cannot be empty")
    private String params; // JSON string
} 