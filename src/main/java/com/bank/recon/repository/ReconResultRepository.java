package com.bank.recon.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.recon.model.entity.ReconResult;

public interface ReconResultRepository extends JpaRepository<ReconResult, UUID> {
    boolean existsByReconDate(LocalDate reconDate);
    List<ReconResult> findAllByReconDateOrderByUtrAsc(LocalDate reconDate);
    Page<ReconResult> findByReconDate(LocalDate reconDate, Pageable pageable);
    Page<ReconResult> findByReconDateAndReconStatus(LocalDate reconDate, String reconStatus, Pageable pageable);
    Page<ReconResult> findByReconDateAndReconStatusNot(LocalDate reconDate, String reconStatus, Pageable pageable);
    long countByReconDate(LocalDate reconDate);
    long countByReconDateAndReconStatus(LocalDate reconDate, String reconStatus);
}
