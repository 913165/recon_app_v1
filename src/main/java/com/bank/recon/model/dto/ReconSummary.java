package com.bank.recon.model.dto;

import java.time.LocalDate;
import java.util.List;

public record ReconSummary(
    LocalDate date,
    long totalNpciRows,
    long totalSwitchRows,
    long matched,
    long switchMissing,
    long npciMissing,
    long amountMismatch,
    long statusMismatch,
    String outputFile,
    String status,
    Summary summary,
    List<ReconResultRecord> results
) {
    public record Summary(long total, long matched, long exceptions) {}
}
