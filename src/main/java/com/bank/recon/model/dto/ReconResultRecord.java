package com.bank.recon.model.dto;

import java.math.BigDecimal;

public record ReconResultRecord(
    String utr,
    BigDecimal npciAmount,
    BigDecimal switchAmount,
    String npciStatus,
    String switchStatus,
    String reconStatus,
    String remarks
) {
}
