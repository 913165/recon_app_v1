package com.bank.recon.model.dto;

import java.math.BigDecimal;

public record CbsRecord(
    String utr,
    String accountNo,
    String txnDate,
    String txnTime,
    BigDecimal amount,
    String drCr,
    String description,
    String cbsRef,
    String status
) {
}
