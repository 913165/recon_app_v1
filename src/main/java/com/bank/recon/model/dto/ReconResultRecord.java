package com.bank.recon.model.dto;

import java.math.BigDecimal;

public record ReconResultRecord(
    String utr,
    BigDecimal npciAmount,
    BigDecimal switchAmount,
    BigDecimal cbsAmount,
    String npciStatus,
    String switchStatus,
    String cbsStatus,
    String reconStatus,
    String remarks
) {
}
