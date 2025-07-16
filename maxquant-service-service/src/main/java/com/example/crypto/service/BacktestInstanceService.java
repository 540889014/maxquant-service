package com.example.crypto.service;

import com.example.crypto.dto.BacktestInstanceDto;
import com.example.crypto.dto.CreateBacktestInstanceRequest;
import com.example.crypto.dto.UpdateBacktestInstanceRequest;
import com.example.crypto.entity.BacktestInstance;
import com.example.crypto.entity.StrategyTemplate;
import com.example.crypto.enums.BacktestStatus;
import com.example.crypto.events.BacktestCompletionEvent;
import com.example.crypto.repository.BacktestInstanceRepository;
import com.example.crypto.dao.StrategyTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BacktestInstanceService {

    private final BacktestInstanceRepository backtestInstanceRepository;
    private final BacktestService backtestService;
    private final BacktestReportService backtestReportService;
    private final StrategyTemplateRepository strategyTemplateRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BacktestInstanceDto createInstance(CreateBacktestInstanceRequest request) {
        // TODO: Replace with actual user from SecurityContext
        Long currentUserId = 1L;

        BacktestInstance instance = new BacktestInstance();
        instance.setName(request.getName());
        instance.setStrategyTemplateId(request.getStrategyTemplateId());
        instance.setParams(request.getParams());
        instance.setUserId(currentUserId);
        instance.setStatus(BacktestStatus.NOT_RUN);

        BacktestInstance savedInstance = backtestInstanceRepository.save(instance);
        return toDto(savedInstance);
    }

    @Transactional(readOnly = true)
    public Page<BacktestInstanceDto> getInstances(Pageable pageable) {
        // TODO: Replace with actual user and roles from SecurityContext
        Long currentUserId = 1L;
        boolean isAdmin = false; // or true for admin user

        if (isAdmin) {
            return backtestInstanceRepository.findAll(pageable).map(this::toDto);
        } else {
            return backtestInstanceRepository.findByUserId(currentUserId, pageable).map(this::toDto);
        }
    }

    @Transactional(readOnly = true)
    public BacktestInstanceDto getInstanceById(Long id) {
        BacktestInstance instance = findInstanceById(id);
        checkAccess(instance);
        return toDto(instance);
    }

    @Transactional
    public BacktestInstanceDto updateInstance(Long id, UpdateBacktestInstanceRequest request) {
        BacktestInstance instance = findInstanceById(id);
        checkAccess(instance);

        if (request.getName() != null) {
            instance.setName(request.getName());
        }
        if (request.getStrategyTemplateId() != null) {
            instance.setStrategyTemplateId(request.getStrategyTemplateId());
        }
        if (request.getParams() != null) {
            instance.setParams(request.getParams());
        }

        BacktestInstance updatedInstance = backtestInstanceRepository.save(instance);
        return toDto(updatedInstance);
    }

    @Transactional
    public void deleteInstance(Long id) throws IOException {
        BacktestInstance instance = findInstanceById(id);
        checkAccess(instance);

        // First, delete the associated report files.
        // If this fails, it will throw an IOException and the transaction will be rolled back.
        //backtestInstance_{id} 是固定的写法
        backtestReportService.deleteBacktestReportsByStrategyName("backtestInstance_"+id);

        // If report deletion is successful, delete the instance from the database.
        backtestInstanceRepository.delete(instance);
    }

    @Transactional
    public BacktestInstanceDto runInstance(Long id, String authorizationHeader) {
        BacktestInstance instance = findInstanceById(id);
        checkAccess(instance);
        instance.setStatus(BacktestStatus.RUNNING);

        String token = authorizationHeader;

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Authorization token is missing or empty.");
        }

        // The backtest service expects the raw token without "Bearer "
        if (token.toLowerCase().startsWith("bearer ")) {
            token = token.substring(7);
        }

        // Fetch the strategy code from the template and inject it into the params
        String finalParamsJson = prepareParamsWithStrategyCode(instance);
        
        backtestService.runBacktest(id.toString(), finalParamsJson, token);
        
        BacktestInstance runningInstance = backtestInstanceRepository.save(instance);
        return toDto(runningInstance);
    }
    
    @EventListener
    @Transactional
    public void handleBacktestCompletion(BacktestCompletionEvent event) {
        Long instanceId = Long.parseLong(event.getBacktestInstanceId());
        BacktestInstance instance = backtestInstanceRepository.findById(instanceId)
                .orElse(null);

        if (instance != null) {
            instance.setStatus(event.getFinalStatus());
            backtestInstanceRepository.save(instance);
        } else {
            // Log if the instance is not found, which might indicate a problem
        }
    }

    private String prepareParamsWithStrategyCode(BacktestInstance instance) {
        if (instance.getStrategyTemplateId() == null) {
            return instance.getParams();
        }

        StrategyTemplate template = strategyTemplateRepository.findById(instance.getStrategyTemplateId())
                .orElseThrow(() -> new EntityNotFoundException("StrategyTemplate not found with id: " + instance.getStrategyTemplateId()));

        String strategyCode = template.getScript();

        try {
            Map<String, Object> paramsMap = objectMapper.readValue(instance.getParams(), new TypeReference<>() {});
            paramsMap.put("STRATEGY_CODE", strategyCode);
            return objectMapper.writeValueAsString(paramsMap);
        } catch (JsonProcessingException e) {
            // Or handle this more gracefully, e.g., by logging and returning original params
            throw new IllegalStateException("Failed to process params JSON for backtest instance: " + instance.getId(), e);
        }
    }

    private BacktestInstance findInstanceById(Long id) {
        return backtestInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("BacktestInstance not found with id: " + id));
    }
    
    private void checkAccess(BacktestInstance instance) {
        // TODO: Replace with actual user and roles from SecurityContext
        Long currentUserId = 1L;
        boolean isAdmin = false;

        if (isAdmin) {
            return;
        }
        if (!Objects.equals(instance.getUserId(), currentUserId)) {
            throw new AccessDeniedException("You do not have permission to access this resource.");
        }
    }

    // A simple mapper method (in a real app, consider using MapStruct)
    private BacktestInstanceDto toDto(BacktestInstance entity) {
        BacktestInstanceDto dto = new BacktestInstanceDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setUserId(entity.getUserId());
        dto.setStrategyTemplateId(entity.getStrategyTemplateId());
        dto.setParams(entity.getParams());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
} 