package com.example.crypto.mapper;

import com.example.crypto.dto.StrategyParameterDto;
import com.example.crypto.dto.StrategyTemplateDetailDto;
import com.example.crypto.dto.StrategyTemplateDto;
import com.example.crypto.entity.StrategyParameter;
import com.example.crypto.entity.StrategyTemplate;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StrategyMapper {

    public StrategyParameterDto toDto(StrategyParameter entity) {
        if (entity == null) {
            return null;
        }
        StrategyParameterDto dto = new StrategyParameterDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDataType(entity.getDataType());
        dto.setDirection(entity.getDirection());
        dto.setDefaultValue(entity.getDefaultValue());
        return dto;
    }

    public StrategyParameter toEntity(StrategyParameterDto dto) {
        if (dto == null) {
            return null;
        }
        StrategyParameter entity = new StrategyParameter();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setDataType(dto.getDataType());
        entity.setDirection(dto.getDirection());
        entity.setDefaultValue(dto.getDefaultValue());
        return entity;
    }

    public StrategyTemplateDto toDto(StrategyTemplate entity) {
        if (entity == null) {
            return null;
        }
        StrategyTemplateDto dto = new StrategyTemplateDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setParameters(entity.getParameters().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        return dto;
    }
    
    public StrategyTemplateDetailDto toDetailDto(StrategyTemplate entity) {
        if (entity == null) {
            return null;
        }
        StrategyTemplateDetailDto dto = new StrategyTemplateDetailDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setScript(entity.getScript());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setParameters(entity.getParameters().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        return dto;
    }
} 