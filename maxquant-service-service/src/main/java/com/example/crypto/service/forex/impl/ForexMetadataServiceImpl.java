package com.example.crypto.service.forex.impl;

import com.example.crypto.entity.ForexMetadata;
import com.example.crypto.repository.ForexMetadataRepository;
import com.example.crypto.service.forex.ForexMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForexMetadataServiceImpl implements ForexMetadataService {

    @Autowired
    private ForexMetadataRepository forexMetadataRepository;

    @Override
    public List<ForexMetadata> getAllForexMetadata() {
        return forexMetadataRepository.findAll();
    }
} 