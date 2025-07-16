package com.example.crypto.controller;

import com.example.crypto.dto.BacktestInstanceDto;
import com.example.crypto.dto.CreateBacktestInstanceRequest;
import com.example.crypto.dto.UpdateBacktestInstanceRequest;
import com.example.crypto.models.ApiResponse;
import com.example.crypto.service.BacktestInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backtest-instances")
@RequiredArgsConstructor
public class BacktestInstanceController {

    private final BacktestInstanceService backtestInstanceService;

    @PostMapping
    public ApiResponse<BacktestInstanceDto> createInstance(@Valid @RequestBody CreateBacktestInstanceRequest request) {
        BacktestInstanceDto createdInstance = backtestInstanceService.createInstance(request);
        return ApiResponse.ok(createdInstance);
    }

    @GetMapping
    public ApiResponse<Page<BacktestInstanceDto>> getInstances(Pageable pageable) {
        Page<BacktestInstanceDto> instances = backtestInstanceService.getInstances(pageable);
        return ApiResponse.ok(instances);
    }

    @GetMapping("/{id}")
    public ApiResponse<BacktestInstanceDto> getInstanceById(@PathVariable Long id) {
        BacktestInstanceDto instance = backtestInstanceService.getInstanceById(id);
        return ApiResponse.ok(instance);
    }

    @PutMapping("/{id}")
    public ApiResponse<BacktestInstanceDto> updateInstance(@PathVariable Long id, @RequestBody UpdateBacktestInstanceRequest request) {
        BacktestInstanceDto updatedInstance = backtestInstanceService.updateInstance(id, request);
        return ApiResponse.ok(updatedInstance);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInstance(@PathVariable Long id) {
        try {
            backtestInstanceService.deleteInstance(id);
            return ApiResponse.ok();
        } catch (java.io.IOException e) {
            // Log the exception (optional, but recommended)
            System.err.println("Failed to delete backtest reports for instance " + id + ": " + e.getMessage());
            return ApiResponse.fail(500, "Failed to delete instance and its reports: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/run")
    public ApiResponse<BacktestInstanceDto> runInstance(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        BacktestInstanceDto runningInstance = backtestInstanceService.runInstance(id, authorizationHeader);
        return ApiResponse.ok(runningInstance);
    }
} 