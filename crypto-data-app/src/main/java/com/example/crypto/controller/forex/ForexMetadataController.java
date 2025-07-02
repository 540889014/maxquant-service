package com.example.crypto.controller.forex;

import com.example.crypto.entity.ForexMetadata;
import com.example.crypto.service.forex.ForexMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forex/metadata")
public class ForexMetadataController {

    @Autowired
    private ForexMetadataService forexMetadataService;

    @GetMapping("/all")
    public List<ForexMetadata> getAllForexMetadata() {
        return forexMetadataService.getAllForexMetadata();
    }
} 