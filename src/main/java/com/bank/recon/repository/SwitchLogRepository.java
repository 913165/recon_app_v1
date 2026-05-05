package com.bank.recon.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.recon.model.entity.SwitchLog;

public interface SwitchLogRepository extends JpaRepository<SwitchLog, UUID> {
    List<SwitchLog> findAllByReconDateOrderByUtrAsc(LocalDate reconDate);
    long countByReconDate(LocalDate reconDate);
}
