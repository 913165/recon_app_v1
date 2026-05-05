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
@Table(name = "recon_results")
public class ReconResult {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String utr;
    @Column(name = "recon_date", nullable = false)
    private LocalDate reconDate;
    @Column(name = "npci_amount", precision = 15, scale = 2)
    private BigDecimal npciAmount;
    @Column(name = "switch_amount", precision = 15, scale = 2)
    private BigDecimal switchAmount;
    @Column(name = "npci_status", length = 20)
    private String npciStatus;
    @Column(name = "switch_status", length = 20)
    private String switchStatus;
    @Column(name = "cbs_amount", precision = 15, scale = 2)
    private BigDecimal cbsAmount;
    @Column(name = "cbs_status", length = 20)
    private String cbsStatus;
    @Column(name = "recon_status", nullable = false, length = 30)
    private String reconStatus;
    @Column(columnDefinition = "TEXT")
    private String remarks;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUtr() { return utr; }
    public void setUtr(String utr) { this.utr = utr; }
    public LocalDate getReconDate() { return reconDate; }
    public void setReconDate(LocalDate reconDate) { this.reconDate = reconDate; }
    public BigDecimal getNpciAmount() { return npciAmount; }
    public void setNpciAmount(BigDecimal npciAmount) { this.npciAmount = npciAmount; }
    public BigDecimal getSwitchAmount() { return switchAmount; }
    public void setSwitchAmount(BigDecimal switchAmount) { this.switchAmount = switchAmount; }
    public String getNpciStatus() { return npciStatus; }
    public void setNpciStatus(String npciStatus) { this.npciStatus = npciStatus; }
    public String getSwitchStatus() { return switchStatus; }
    public void setSwitchStatus(String switchStatus) { this.switchStatus = switchStatus; }
    public BigDecimal getCbsAmount() { return cbsAmount; }
    public void setCbsAmount(BigDecimal cbsAmount) { this.cbsAmount = cbsAmount; }
    public String getCbsStatus() { return cbsStatus; }
    public void setCbsStatus(String cbsStatus) { this.cbsStatus = cbsStatus; }
    public String getReconStatus() { return reconStatus; }
    public void setReconStatus(String reconStatus) { this.reconStatus = reconStatus; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
