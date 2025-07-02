package com.example.crypto.dao;

import com.example.crypto.entity.StrategyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StrategyTemplateRepository extends JpaRepository<StrategyTemplate, Long> {
    Optional<StrategyTemplate> findByName(String name);
} 