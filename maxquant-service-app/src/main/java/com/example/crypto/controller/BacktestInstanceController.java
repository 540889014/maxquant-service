package com.example.crypto.controller;

import com.example.crypto.dto.BacktestInstanceDto;
import com.example.crypto.dto.CreateBacktestInstanceRequest;
import com.example.crypto.dto.UpdateBacktestInstanceRequest;
import com.example.crypto.service.BacktestInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backtest-instances")
@RequiredArgsConstructor
public class BacktestInstanceController {

    private final BacktestInstanceService backtestInstanceService;

    @PostMapping
    public ResponseEntity<BacktestInstanceDto> createInstance(@Valid @RequestBody CreateBacktestInstanceRequest request) {
        BacktestInstanceDto createdInstance = backtestInstanceService.createInstance(request);
        return new ResponseEntity<>(createdInstance, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<BacktestInstanceDto>> getInstances(Pageable pageable) {
        Page<BacktestInstanceDto> instances = backtestInstanceService.getInstances(pageable);
        return ResponseEntity.ok(instances);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BacktestInstanceDto> getInstanceById(@PathVariable Long id) {
        BacktestInstanceDto instance = backtestInstanceService.getInstanceById(id);
        return ResponseEntity.ok(instance);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BacktestInstanceDto> updateInstance(@PathVariable Long id, @RequestBody UpdateBacktestInstanceRequest request) {
        BacktestInstanceDto updatedInstance = backtestInstanceService.updateInstance(id, request);
        return ResponseEntity.ok(updatedInstance);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        backtestInstanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<BacktestInstanceDto> runInstance(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        BacktestInstanceDto runningInstance = backtestInstanceService.runInstance(id, authorizationHeader);
        return ResponseEntity.ok(runningInstance);
    }
} 