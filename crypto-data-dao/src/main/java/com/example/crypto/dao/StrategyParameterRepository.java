package com.example.crypto.dao;

import com.example.crypto.entity.StrategyParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StrategyParameterRepository extends JpaRepository<StrategyParameter, Long> {
} 