package com.bank.recon.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettlementResult(
    LocalDate date,
    BigDecimal npciNet,
    BigDecimal calculatedNet,
    BigDecimal difference,
    String status,
    String rbiRef,
    String remarks
) {
}
