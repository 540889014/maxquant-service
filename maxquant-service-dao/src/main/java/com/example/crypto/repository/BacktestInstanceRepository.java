package com.example.crypto.repository;

import com.example.crypto.entity.BacktestInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BacktestInstanceRepository extends JpaRepository<BacktestInstance, Long> {

    Page<BacktestInstance> findByUserId(Long userId, Pageable pageable);

} 