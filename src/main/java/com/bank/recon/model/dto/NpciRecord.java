package com.bank.recon.model.dto;

import java.math.BigDecimal;

public record NpciRecord(
    String utr,
    String rrn,
    String txnDate,
    String txnTime,
    BigDecimal amount,
    String payerVpa,
    String payeeVpa,
    String status
) {
}
