package com.bank.recon.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.recon.model.entity.SettlementRecord;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, UUID> {
    Optional<SettlementRecord> findByReconDate(LocalDate date);
}
