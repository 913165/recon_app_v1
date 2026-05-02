package com.bank.recon.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.recon.model.entity.ReconResult;

public interface ReconResultRepository extends JpaRepository<ReconResult, UUID> {
    boolean existsByReconDate(LocalDate reconDate);
    List<ReconResult> findAllByReconDateOrderByUtrAsc(LocalDate reconDate);
}
