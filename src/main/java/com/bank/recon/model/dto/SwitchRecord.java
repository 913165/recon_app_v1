package com.bank.recon.model.dto;

import java.math.BigDecimal;

public record SwitchRecord(
    String utr,
    String rrn,
    String txnDate,
    String txnTime,
    BigDecimal amount,
    String status,
    String responseCode,
    String switchRef
) {
}
