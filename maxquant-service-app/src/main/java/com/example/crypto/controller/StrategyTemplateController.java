package com.example.crypto.controller;

import com.example.crypto.dto.StrategyParameterDto;
import com.example.crypto.dto.StrategyTemplateDetailDto;
import com.example.crypto.dto.StrategyTemplateDto;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.StrategyTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/strategy-templates")
@RequiredArgsConstructor
public class StrategyTemplateController {

    private final StrategyTemplateService strategyTemplateService;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = {"multipart/form-data"})
    public ApiResponse<StrategyTemplateDetailDto> createStrategyTemplate(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("scriptFile") MultipartFile scriptFile,
            @RequestParam("parameters") String parametersJson
    ) {
        try {
            List<StrategyParameterDto> parameters = parseParameters(parametersJson);
            StrategyTemplateDetailDto createdTemplate = strategyTemplateService.createStrategyTemplate(name, description, scriptFile, parameters);
            return ApiResponse.ok(createdTemplate);
        } catch (IOException e) {
            return ApiResponse.fail(500, "Failed to create strategy template: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<Page<StrategyTemplateDto>> getAllStrategyTemplates(Pageable pageable) {
        Page<StrategyTemplateDto> templates = strategyTemplateService.getAllStrategyTemplates(pageable);
        return ApiResponse.ok(templates);
    }

    @GetMapping("/{id}")
    public ApiResponse<StrategyTemplateDetailDto> getStrategyTemplateById(@PathVariable Long id) {
        StrategyTemplateDetailDto template = strategyTemplateService.getStrategyTemplateById(id);
        return ApiResponse.ok(template);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ApiResponse<StrategyTemplateDetailDto> updateStrategyTemplate(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "scriptFile", required = false) MultipartFile scriptFile,
            @RequestParam("parameters") String parametersJson
    ) {
        try {
            List<StrategyParameterDto> parameters = parseParameters(parametersJson);
            StrategyTemplateDetailDto updatedTemplate = strategyTemplateService.updateStrategyTemplate(id, name, description, scriptFile, parameters);
            return ApiResponse.ok(updatedTemplate);
        } catch (IOException e) {
            return ApiResponse.fail(500, "Failed to update strategy template: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteStrategyTemplate(@PathVariable Long id) {
        strategyTemplateService.deleteStrategyTemplate(id);
        return ApiResponse.ok();
    }

    private List<StrategyParameterDto> parseParameters(String parametersJson) throws JsonProcessingException {
        if (parametersJson == null || parametersJson.isEmpty()) {
            return List.of();
        }
        return objectMapper.readValue(parametersJson, new TypeReference<List<StrategyParameterDto>>() {});
    }
} 