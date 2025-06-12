package com.example.crypto.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 暗号資産メタデータエンティティ
 * 取引ペア情報を格納
 */
@Entity
@Table(name = "crypto_metadata")
public class CryptoMetadata {
    @Id
    private String id;
    private String instType;
    private String instId;
    private String baseCcy;
    private String quoteCcy;
    private String state;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getInstType() { return instType; }
    public void setInstType(String instType) { this.instType = instType; }
    public String getInstId() { return instId; }
    public void setInstId(String instId) { this.instId = instId; }
    public String getBaseCcy() { return baseCcy; }
    public void setBaseCcy(String baseCcy) { this.baseCcy = baseCcy; }
    public String getQuoteCcy() { return quoteCcy; }
    public void setQuoteCcy(String quoteCcy) { this.quoteCcy = quoteCcy; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
}