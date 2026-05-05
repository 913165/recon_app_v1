package com.bank.recon.repository;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.recon.model.entity.CbsExtract;

public interface CbsExtractRepository extends JpaRepository<CbsExtract, UUID> {
    long countByReconDate(LocalDate reconDate);
}
