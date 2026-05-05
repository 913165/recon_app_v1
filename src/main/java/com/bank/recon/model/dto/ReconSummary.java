package com.bank.recon.model.dto;

import java.time.LocalDate;
import java.util.List;

public record ReconSummary(
    LocalDate date,
    long totalNpciRows,
    long totalSwitchRows,
    long totalCbsRows,
    long matched,
    long switchMissing,
    long npciMissing,
    long cbsMissing,
    long amountMismatch,
    long statusMismatch,
    String outputFile,
    String status,
    /**
     * Parse input files, match NPCI vs Switch, build recon result rows in memory. Excludes all DB writes, flush, output file, and commit.
     * Null when not measured (e.g. GET results).
     */
    Long reconciliationMillis,
    /**
     * Full server time for the transactional run: persistence, flushes, result file write, and JDBC commit. Excludes HTTP and JSON encoding.
     * Null when not measured (e.g. GET results).
     */
    Long durationMillis,
    SettlementResult settlement,
    Summary summary,
    List<ReconResultRecord> results
) {
    public record Summary(long total, long matched, long exceptions) {}
}
