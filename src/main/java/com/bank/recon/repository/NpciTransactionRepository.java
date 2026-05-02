package com.bank.recon.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bank.recon.model.entity.NpciTransaction;

public interface NpciTransactionRepository extends JpaRepository<NpciTransaction, UUID> {
    List<NpciTransaction> findAllByReconDateOrderByUtrAsc(LocalDate reconDate);
}
