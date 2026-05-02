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
@Table(name = "npci_transactions")
public class NpciTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String utr;
    @Column(length = 50)
    private String rrn;
    @Column(name = "txn_date", length = 10)
    private String txnDate;
    @Column(name = "txn_time", length = 10)
    private String txnTime;
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    @Column(name = "payer_vpa", length = 100)
    private String payerVpa;
    @Column(name = "payee_vpa", length = 100)
    private String payeeVpa;
    @Column(length = 20)
    private String status;
    @Column(name = "recon_date", nullable = false)
    private LocalDate reconDate;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }
    public String getRrn() { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }
    public String getTxnDate() { return txnDate; }
    public void setTxnDate(String txnDate) { this.txnDate = txnDate; }
    public String getTxnTime() { return txnTime; }
    public void setTxnTime(String txnTime) { this.txnTime = txnTime; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPayerVpa() { return payerVpa; }
    public void setPayerVpa(String payerVpa) { this.payerVpa = payerVpa; }
    public String getPayeeVpa() { return payeeVpa; }
    public void setPayeeVpa(String payeeVpa) { this.payeeVpa = payeeVpa; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getReconDate() { return reconDate; }
    public void setReconDate(LocalDate reconDate) { this.reconDate = reconDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
