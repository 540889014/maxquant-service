package com.example.crypto.service.impl;

import com.example.crypto.dto.StrategyParameterDto;
import com.example.crypto.dto.StrategyTemplateDetailDto;
import com.example.crypto.dto.StrategyTemplateDto;
import com.example.crypto.exception.DuplicateResourceException;
import com.example.crypto.exception.ResourceNotFoundException;
import com.example.crypto.mapper.StrategyMapper;
import com.example.crypto.entity.StrategyParameter;
import com.example.crypto.entity.StrategyTemplate;
import com.example.crypto.dao.StrategyTemplateRepository;
import com.example.crypto.service.StrategyTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StrategyTemplateServiceImpl implements StrategyTemplateService {

    private final StrategyTemplateRepository strategyTemplateRepository;
    private final StrategyMapper strategyMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<StrategyTemplateDto> getAllStrategyTemplates(Pageable pageable) {
        return strategyTemplateRepository.findAll(pageable)
                .map(strategyMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public StrategyTemplateDetailDto getStrategyTemplateById(Long id) {
        StrategyTemplate template = strategyTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StrategyTemplate not found with id: " + id));
        return strategyMapper.toDetailDto(template);
    }

    @Override
    @Transactional
    public StrategyTemplateDetailDto createStrategyTemplate(String name, String description, MultipartFile scriptFile, List<StrategyParameterDto> parameters) throws IOException {
        strategyTemplateRepository.findByName(name).ifPresent(s -> {
            throw new DuplicateResourceException("StrategyTemplate with name '" + name + "' already exists.");
        });

        StrategyTemplate template = new StrategyTemplate();
        template.setName(name);
        template.setDescription(description);
        template.setScript(new String(scriptFile.getBytes(), StandardCharsets.UTF_8));
        
        if (parameters != null) {
            List<StrategyParameter> parameterEntities = parameters.stream()
                    .map(strategyMapper::toEntity)
                    .collect(Collectors.toList());
            template.setParameters(parameterEntities);
        }

        StrategyTemplate savedTemplate = strategyTemplateRepository.save(template);
        return strategyMapper.toDetailDto(savedTemplate);
    }

    @Override
    @Transactional
    public StrategyTemplateDetailDto updateStrategyTemplate(Long id, String name, String description, MultipartFile scriptFile, List<StrategyParameterDto> parameters) throws IOException {
        StrategyTemplate template = strategyTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StrategyTemplate not found with id: " + id));

        strategyTemplateRepository.findByName(name).ifPresent(s -> {
            if (!s.getId().equals(id)) {
                throw new DuplicateResourceException("StrategyTemplate with name '" + name + "' already exists.");
            }
        });

        template.setName(name);
        template.setDescription(description);

        if (scriptFile != null && !scriptFile.isEmpty()) {
            template.setScript(new String(scriptFile.getBytes(), StandardCharsets.UTF_8));
        }

        if (parameters != null) {
            List<StrategyParameter> parameterEntities = parameters.stream()
                    .map(strategyMapper::toEntity)
                    .collect(Collectors.toList());
            template.setParameters(parameterEntities);
        }

        StrategyTemplate updatedTemplate = strategyTemplateRepository.save(template);
        return strategyMapper.toDetailDto(updatedTemplate);
    }

    @Override
    @Transactional
    public void deleteStrategyTemplate(Long id) {
        if (!strategyTemplateRepository.existsById(id)) {
            throw new ResourceNotFoundException("StrategyTemplate not found with id: " + id);
        }
        strategyTemplateRepository.deleteById(id);
    }
} 