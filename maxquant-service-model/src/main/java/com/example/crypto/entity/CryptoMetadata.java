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
    private String settleCcy;
    private String state;
    private String exchange;
    private String ctVal;
    private String ctMult;
    private String ctValCcy;

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
    public String getSettleCcy() { return settleCcy; }
    public void setSettleCcy(String settleCcy) { this.settleCcy = settleCcy; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public String getCtVal() { return ctVal; }
    public void setCtVal(String ctVal) { this.ctVal = ctVal; }
    public String getCtMult() { return ctMult; }
    public void setCtMult(String ctMult) { this.ctMult = ctMult; }
    public String getCtValCcy() { return ctValCcy; }
    public void setCtValCcy(String ctValCcy) { this.ctValCcy = ctValCcy; }
}