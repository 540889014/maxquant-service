package com.example.crypto.controller;

import com.example.crypto.dto.StrategyParameterDto;
import com.example.crypto.dto.StrategyTemplateDetailDto;
import com.example.crypto.dto.StrategyTemplateDto;
import com.example.crypto.service.StrategyTemplateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<StrategyTemplateDetailDto> createStrategyTemplate(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("scriptFile") MultipartFile scriptFile,
            @RequestParam("parameters") String parametersJson
    ) throws IOException {
        List<StrategyParameterDto> parameters = parseParameters(parametersJson);
        StrategyTemplateDetailDto createdTemplate = strategyTemplateService.createStrategyTemplate(name, description, scriptFile, parameters);
        return new ResponseEntity<>(createdTemplate, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<StrategyTemplateDto>> getAllStrategyTemplates(Pageable pageable) {
        Page<StrategyTemplateDto> templates = strategyTemplateService.getAllStrategyTemplates(pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StrategyTemplateDetailDto> getStrategyTemplateById(@PathVariable Long id) {
        StrategyTemplateDetailDto template = strategyTemplateService.getStrategyTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<StrategyTemplateDetailDto> updateStrategyTemplate(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "scriptFile", required = false) MultipartFile scriptFile,
            @RequestParam("parameters") String parametersJson
    ) throws IOException {
        List<StrategyParameterDto> parameters = parseParameters(parametersJson);
        StrategyTemplateDetailDto updatedTemplate = strategyTemplateService.updateStrategyTemplate(id, name, description, scriptFile, parameters);
        return ResponseEntity.ok(updatedTemplate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStrategyTemplate(@PathVariable Long id) {
        strategyTemplateService.deleteStrategyTemplate(id);
        return ResponseEntity.noContent().build();
    }

    private List<StrategyParameterDto> parseParameters(String parametersJson) throws JsonProcessingException {
        if (parametersJson == null || parametersJson.isEmpty()) {
            return List.of();
        }
        return objectMapper.readValue(parametersJson, new TypeReference<List<StrategyParameterDto>>() {});
    }
} 