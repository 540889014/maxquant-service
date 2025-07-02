package com.example.crypto.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "strategy_template", uniqueConstraints = {
    @UniqueConstraint(columnNames = "name")
})
@Data
public class StrategyTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String script;

    private String description;

    @OneToMany(
        mappedBy = "strategyTemplate",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.EAGER
    )
    private List<StrategyParameter> parameters = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void setParameters(List<StrategyParameter> parameters) {
        this.parameters.clear();
        if (parameters != null) {
            for (StrategyParameter parameter : parameters) {
                parameter.setStrategyTemplate(this);
                this.parameters.add(parameter);
            }
        }
    }
} 