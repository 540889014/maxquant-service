package com.example.crypto.service;

import com.example.crypto.dto.StrategyTemplateDetailDto;
import com.example.crypto.dto.StrategyTemplateDto;
import com.example.crypto.dto.StrategyParameterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StrategyTemplateService {

    Page<StrategyTemplateDto> getAllStrategyTemplates(Pageable pageable);

    StrategyTemplateDetailDto getStrategyTemplateById(Long id);

    StrategyTemplateDetailDto createStrategyTemplate(String name, String description, MultipartFile scriptFile, List<StrategyParameterDto> parameters) throws IOException;

    StrategyTemplateDetailDto updateStrategyTemplate(Long id, String name, String description, MultipartFile scriptFile, List<StrategyParameterDto> parameters) throws IOException;

    void deleteStrategyTemplate(Long id);
} 