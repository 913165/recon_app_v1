package com.bank.recon.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "settlement_records")
public class SettlementRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recon_date", nullable = false, unique = true)
    private LocalDate reconDate;
    @Column(name = "bank_code", length = 20)
    private String bankCode;
    @Column(name = "total_txn")
    private Integer totalTxn;
    @Column(name = "npci_total_debit", precision = 15, scale = 2)
    private BigDecimal npciTotalDebit;
    @Column(name = "npci_total_credit", precision = 15, scale = 2)
    private BigDecimal npciTotalCredit;
    @Column(name = "npci_net_amount", precision = 15, scale = 2)
    private BigDecimal npciNetAmount;
    @Column(name = "calculated_net", precision = 15, scale = 2)
    private BigDecimal calculatedNet;
    @Column(name = "difference", precision = 15, scale = 2)
    private BigDecimal difference;
    @Column(name = "rbi_ref", length = 50)
    private String rbiRef;
    @Column(name = "npci_status", length = 20)
    private String npciStatus;
    @Column(name = "settlement_status", nullable = false, length = 30)
    private String settlementStatus;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getReconDate() { return reconDate; }
    public void setReconDate(LocalDate reconDate) { this.reconDate = reconDate; }
    public String getBankCode() { return bankCode; }
    public void setBankCode(String bankCode) { this.bankCode = bankCode; }
    public Integer getTotalTxn() { return totalTxn; }
    public void setTotalTxn(Integer totalTxn) { this.totalTxn = totalTxn; }
    public BigDecimal getNpciTotalDebit() { return npciTotalDebit; }
    public void setNpciTotalDebit(BigDecimal npciTotalDebit) { this.npciTotalDebit = npciTotalDebit; }
    public BigDecimal getNpciTotalCredit() { return npciTotalCredit; }
    public void setNpciTotalCredit(BigDecimal npciTotalCredit) { this.npciTotalCredit = npciTotalCredit; }
    public BigDecimal getNpciNetAmount() { return npciNetAmount; }
    public void setNpciNetAmount(BigDecimal npciNetAmount) { this.npciNetAmount = npciNetAmount; }
    public BigDecimal getCalculatedNet() { return calculatedNet; }
    public void setCalculatedNet(BigDecimal calculatedNet) { this.calculatedNet = calculatedNet; }
    public BigDecimal getDifference() { return difference; }
    public void setDifference(BigDecimal difference) { this.difference = difference; }
    public String getRbiRef() { return rbiRef; }
    public void setRbiRef(String rbiRef) { this.rbiRef = rbiRef; }
    public String getNpciStatus() { return npciStatus; }
    public void setNpciStatus(String npciStatus) { this.npciStatus = npciStatus; }
    public String getSettlementStatus() { return settlementStatus; }
    public void setSettlementStatus(String settlementStatus) { this.settlementStatus = settlementStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
