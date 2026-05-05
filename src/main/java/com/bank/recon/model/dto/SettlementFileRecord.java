package com.bank.recon.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementFileRecord(
    LocalDate settlementDate,
    String bankCode,
    int totalTxn,
    BigDecimal totalDebit,
    BigDecimal totalCredit,
    BigDecimal netAmount,
    String rbiRef,
    String status
) {
}
