package com.bank.recon.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.bank.recon.config.AppConfig;
import com.bank.recon.exception.ReconAlreadyRunException;
import com.bank.recon.model.StorageBackend;
import com.bank.recon.model.dto.CbsRecord;
import com.bank.recon.model.dto.NpciRecord;
import com.bank.recon.model.dto.ReconResultRecord;
import com.bank.recon.model.dto.ReconSummary;
import com.bank.recon.model.dto.SettlementResult;
import com.bank.recon.model.dto.SwitchRecord;
import com.bank.recon.model.entity.CbsExtract;
import com.bank.recon.model.entity.NpciTransaction;
import com.bank.recon.model.entity.ReconResult;
import com.bank.recon.model.entity.SwitchLog;
import com.bank.recon.model.status.AmountMismatch;
import com.bank.recon.model.status.CbsMissing;
import com.bank.recon.model.status.Matched;
import com.bank.recon.model.status.NpciMissing;
import com.bank.recon.model.status.ReconStatus;
import com.bank.recon.model.status.StatusMismatch;
import com.bank.recon.model.status.SwitchMissing;
import com.bank.recon.repository.CbsExtractRepository;
import com.bank.recon.repository.NpciTransactionRepository;
import com.bank.recon.repository.ReconResultRepository;
import com.bank.recon.repository.SwitchLogRepository;

@Service
public class ReconService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final FileParserService fileParserService;
    private final FileWriterService fileWriterService;
    private final NpciTransactionRepository npciTransactionRepository;
    private final SwitchLogRepository switchLogRepository;
    private final CbsExtractRepository cbsExtractRepository;
    private final ReconResultRepository reconResultRepository;
    private final SettlementReconService settlementReconService;
    private final RedisReconStore redisReconStore;
    private final AppConfig appConfig;
    private final TransactionTemplate transactionTemplate;

    public ReconService(FileParserService fileParserService, FileWriterService fileWriterService,
                        NpciTransactionRepository npciTransactionRepository, SwitchLogRepository switchLogRepository,
                        CbsExtractRepository cbsExtractRepository, ReconResultRepository reconResultRepository,
                        SettlementReconService settlementReconService, RedisReconStore redisReconStore, AppConfig appConfig,
                        PlatformTransactionManager transactionManager) {
        this.fileParserService = fileParserService;
        this.fileWriterService = fileWriterService;
        this.npciTransactionRepository = npciTransactionRepository;
        this.switchLogRepository = switchLogRepository;
        this.cbsExtractRepository = cbsExtractRepository;
        this.reconResultRepository = reconResultRepository;
        this.settlementReconService = settlementReconService;
        this.redisReconStore = redisReconStore;
        this.appConfig = appConfig;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public ReconSummary runRecon(LocalDate date) throws IOException {
        return runRecon(date, StorageBackend.POSTGRES);
    }

    public ReconSummary runRecon(LocalDate date, StorageBackend backend) throws IOException {
        long startNanos = System.nanoTime();
        try {
            RunOutcome outcome;
            if (backend == StorageBackend.REDIS) {
                outcome = runReconWithRedis(date);
            } else {
                // execute() returns only after PlatformTransactionManager.commit() finishes (JDBC commit included).
                outcome = transactionTemplate.execute(status -> runReconInTransaction(date));
            }
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            return toRunSummary(outcome.date(), outcome.npciCount(), outcome.switchCount(), outcome.cbsCount(), outcome.results(),
                outcome.outputFile(), outcome.reconciliationMillis(), durationMillis, outcome.settlement());
        } catch (UncheckedIOException e) {
            throw (IOException) e.getCause();
        }
    }

    private RunOutcome runReconInTransaction(LocalDate date) {
        if (reconResultRepository.existsByReconDate(date)) {
            throw new ReconAlreadyRunException("Recon already run for date " + date.format(FILE_DATE_FORMAT));
        }
        try {
            String fileDate = date.format(FILE_DATE_FORMAT);
            String ext = appConfig.fileExtension();
            Path npciFile = appConfig.input().npciDir().resolve("NPCI_TXN_" + fileDate + ext);
            Path switchFile = appConfig.input().switchDir().resolve("SWITCH_LOG_" + fileDate + ext);
            Path cbsFile = appConfig.input().cbsDir().resolve("CBS_EXTRACT_" + fileDate + ext);
            Path outputFile = appConfig.output().outputDir().resolve("RECON_RESULT_" + fileDate + ext);

            long reconStartNanos = System.nanoTime();
            List<NpciRecord> npciRows = fileParserService.parseNpciFile(npciFile);
            List<SwitchRecord> switchRows = fileParserService.parseSwitchFile(switchFile);
            List<CbsRecord> cbsRows = fileParserService.parseCbsFile(cbsFile);

            Map<String, NpciRecord> npciByUtr = new LinkedHashMap<>();
            Map<String, SwitchRecord> switchByUtr = new LinkedHashMap<>();
            Map<String, CbsRecord> cbsByUtr = new LinkedHashMap<>();
            for (NpciRecord row : npciRows) npciByUtr.put(row.utr(), row);
            for (SwitchRecord row : switchRows) switchByUtr.put(row.utr(), row);
            for (CbsRecord row : cbsRows) cbsByUtr.put(row.utr(), row);

            List<ReconResultRecord> results = new ArrayList<>();
            TreeSet<String> utrs = new TreeSet<>();
            utrs.addAll(npciByUtr.keySet());
            utrs.addAll(switchByUtr.keySet());
            utrs.addAll(cbsByUtr.keySet());

            for (String utr : utrs) {
                NpciRecord npci = npciByUtr.get(utr);
                SwitchRecord sw = switchByUtr.get(utr);
                CbsRecord cbs = cbsByUtr.get(utr);
                ReconStatus status = resolveStatus(npci, sw, cbs);
                results.add(new ReconResultRecord(
                    utr,
                    npci == null ? null : npci.amount(),
                    sw == null ? null : sw.amount(),
                    cbs == null ? null : cbs.amount(),
                    npci == null ? null : npci.status(),
                    sw == null ? null : sw.status(),
                    cbs == null ? null : cbs.status(),
                    status.code(),
                    status.remarks()
                ));
            }

            long reconciliationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reconStartNanos);

            npciTransactionRepository.saveAll(npciRows.stream().map(row -> toEntity(row, date)).toList());
            switchLogRepository.saveAll(switchRows.stream().map(row -> toEntity(row, date)).toList());
            cbsExtractRepository.saveAll(cbsRows.stream().map(row -> toCbsEntity(row, date)).toList());
            reconResultRepository.saveAll(results.stream().map(row -> toEntity(row, date)).toList());
            npciTransactionRepository.flush();
            switchLogRepository.flush();
            cbsExtractRepository.flush();
            reconResultRepository.flush();
            fileWriterService.writeReconResult(results, outputFile);

            BigDecimal calculatedNet = calculateMatchedNet(results);
            SettlementResult settlement = settlementReconService.reconcileSettlement(date, calculatedNet);

            return new RunOutcome(date, npciRows.size(), switchRows.size(), cbsRows.size(), results, outputFile, reconciliationMillis, settlement);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private RunOutcome runReconWithRedis(LocalDate date) {
        if (redisReconStore.existsByReconDate(date)) {
            throw new ReconAlreadyRunException("Recon already run for date " + date.format(FILE_DATE_FORMAT) + " in REDIS");
        }
        try {
            String fileDate = date.format(FILE_DATE_FORMAT);
            String ext = appConfig.fileExtension();
            Path npciFile = appConfig.input().npciDir().resolve("NPCI_TXN_" + fileDate + ext);
            Path switchFile = appConfig.input().switchDir().resolve("SWITCH_LOG_" + fileDate + ext);
            Path cbsFile = appConfig.input().cbsDir().resolve("CBS_EXTRACT_" + fileDate + ext);
            Path outputFile = appConfig.output().outputDir().resolve("RECON_RESULT_" + fileDate + ext);

            long reconStartNanos = System.nanoTime();
            List<NpciRecord> npciRows = fileParserService.parseNpciFile(npciFile);
            List<SwitchRecord> switchRows = fileParserService.parseSwitchFile(switchFile);
            List<CbsRecord> cbsRows = fileParserService.parseCbsFile(cbsFile);

            Map<String, NpciRecord> npciByUtr = new LinkedHashMap<>();
            Map<String, SwitchRecord> switchByUtr = new LinkedHashMap<>();
            Map<String, CbsRecord> cbsByUtr = new LinkedHashMap<>();
            for (NpciRecord row : npciRows) npciByUtr.put(row.utr(), row);
            for (SwitchRecord row : switchRows) switchByUtr.put(row.utr(), row);
            for (CbsRecord row : cbsRows) cbsByUtr.put(row.utr(), row);

            List<ReconResultRecord> results = new ArrayList<>();
            TreeSet<String> utrs = new TreeSet<>();
            utrs.addAll(npciByUtr.keySet());
            utrs.addAll(switchByUtr.keySet());
            utrs.addAll(cbsByUtr.keySet());

            for (String utr : utrs) {
                NpciRecord npci = npciByUtr.get(utr);
                SwitchRecord sw = switchByUtr.get(utr);
                CbsRecord cbs = cbsByUtr.get(utr);
                ReconStatus status = resolveStatus(npci, sw, cbs);
                results.add(new ReconResultRecord(
                    utr,
                    npci == null ? null : npci.amount(),
                    sw == null ? null : sw.amount(),
                    cbs == null ? null : cbs.amount(),
                    npci == null ? null : npci.status(),
                    sw == null ? null : sw.status(),
                    cbs == null ? null : cbs.status(),
                    status.code(),
                    status.remarks()
                ));
            }

            long reconciliationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - reconStartNanos);
            redisReconStore.save(date, npciRows.size(), switchRows.size(), cbsRows.size(), results);
            fileWriterService.writeReconResult(results, outputFile);
            BigDecimal calculatedNet = calculateMatchedNet(results);
            SettlementResult settlement = settlementReconService.reconcileSettlement(date, calculatedNet);
            return new RunOutcome(date, npciRows.size(), switchRows.size(), cbsRows.size(), results, outputFile, reconciliationMillis, settlement);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record RunOutcome(LocalDate date, int npciCount, int switchCount, int cbsCount, List<ReconResultRecord> results,
                              Path outputFile, long reconciliationMillis, SettlementResult settlement) {}

    @Transactional(readOnly = true)
    public ReconSummary getResults(LocalDate date) {
        List<ReconResultRecord> results = reconResultRepository.findAllByReconDateOrderByUtrAsc(date).stream()
            .map(this::toRecord)
            .toList();

        long matched = results.stream().filter(r -> "MATCHED".equals(r.reconStatus())).count();
        long switchMissing = results.stream().filter(r -> "SWITCH_MISSING".equals(r.reconStatus())).count();
        long npciMissing = results.stream().filter(r -> "NPCI_MISSING".equals(r.reconStatus())).count();
        long cbsMissing = results.stream().filter(r -> "CBS_MISSING".equals(r.reconStatus())).count();
        long amountMismatch = results.stream().filter(r -> "AMOUNT_MISMATCH".equals(r.reconStatus())).count();
        long statusMismatch = results.stream().filter(r -> "STATUS_MISMATCH".equals(r.reconStatus())).count();

        SettlementResult settlement = null;
        try {
            settlement = settlementReconService.getSettlement(date);
        } catch (IllegalArgumentException ignored) {
            // settlement not yet reconciled for this date
        }

        return new ReconSummary(date, 0, 0, 0, matched, switchMissing, npciMissing, cbsMissing, amountMismatch, statusMismatch, null,
            "COMPLETED", null, null, settlement, new ReconSummary.Summary(results.size(), matched, results.size() - matched), results);
    }

    @Transactional(readOnly = true)
    public ReconSummary getResultsOverview(LocalDate date) {
        return getResultsOverview(date, StorageBackend.POSTGRES);
    }

    @Transactional(readOnly = true)
    public ReconSummary getResultsOverview(LocalDate date, StorageBackend backend) {
        if (backend == StorageBackend.REDIS) {
            return redisReconStore.getOverview(date);
        }
        long totalResults = reconResultRepository.countByReconDate(date);
        long matched = reconResultRepository.countByReconDateAndReconStatus(date, "MATCHED");
        long switchMissing = reconResultRepository.countByReconDateAndReconStatus(date, "SWITCH_MISSING");
        long npciMissing = reconResultRepository.countByReconDateAndReconStatus(date, "NPCI_MISSING");
        long cbsMissing = reconResultRepository.countByReconDateAndReconStatus(date, "CBS_MISSING");
        long amountMismatch = reconResultRepository.countByReconDateAndReconStatus(date, "AMOUNT_MISMATCH");
        long statusMismatch = reconResultRepository.countByReconDateAndReconStatus(date, "STATUS_MISMATCH");
        long npciRows = npciTransactionRepository.countByReconDate(date);
        long switchRows = switchLogRepository.countByReconDate(date);
        long cbsRows = cbsExtractRepository.countByReconDate(date);

        return new ReconSummary(
            date,
            npciRows,
            switchRows,
            cbsRows,
            matched,
            switchMissing,
            npciMissing,
            cbsMissing,
            amountMismatch,
            statusMismatch,
            null,
            "COMPLETED",
            null,
            null,
            null,
            new ReconSummary.Summary(totalResults, matched, totalResults - matched),
            List.of()
        );
    }

    @Transactional(readOnly = true)
    public Page<ReconResultRecord> getResultsPage(LocalDate date, int page, int size, @Nullable String filter, StorageBackend backend) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 1000);
        String normalized = filter == null ? "ALL" : filter.trim().toUpperCase();
        if (backend == StorageBackend.REDIS) {
            return redisReconStore.getPage(date, safePage, safeSize, normalized);
        }
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "utr"));
        Page<ReconResult> pageResult = switch (normalized) {
            case "MATCHED" -> reconResultRepository.findByReconDateAndReconStatus(date, "MATCHED", pageable);
            case "MISMATCHED" -> reconResultRepository.findByReconDateAndReconStatusNot(date, "MATCHED", pageable);
            default -> reconResultRepository.findByReconDate(date, pageable);
        };
        return pageResult.map(this::toRecord);
    }

    private ReconStatus resolveStatus(@Nullable NpciRecord npci, @Nullable SwitchRecord sw, @Nullable CbsRecord cbs) {
        if (npci != null && sw == null) return new SwitchMissing();
        if (npci == null) return new NpciMissing();
        if (cbs == null) return new CbsMissing();
        if (!tripleAmountMatches(npci.amount(), sw.amount(), cbs.amount())) {
            return new AmountMismatch("Amount differs: NPCI=" + fmt(npci.amount()) + " Switch=" + fmt(sw.amount()) + " CBS=" + fmt(cbs.amount()));
        }
        if (!tripleStringMatches(npci.status(), sw.status(), cbs.status())) {
            return new StatusMismatch("Status differs: NPCI=" + str(npci.status()) + " Switch=" + str(sw.status()) + " CBS=" + str(cbs.status()));
        }
        return new Matched();
    }

    private boolean tripleAmountMatches(@Nullable BigDecimal a, @Nullable BigDecimal b, @Nullable BigDecimal c) {
        return amountMatches(a, b) && amountMatches(a, c);
    }

    private boolean tripleStringMatches(@Nullable String a, @Nullable String b, @Nullable String c) {
        return stringMatches(a, b) && stringMatches(a, c);
    }

    private String str(@Nullable String s) {
        return s == null ? "--" : s;
    }

    private boolean amountMatches(@Nullable BigDecimal a, @Nullable BigDecimal b) {
        return a != null && b != null && a.compareTo(b) == 0;
    }

    private boolean stringMatches(@Nullable String a, @Nullable String b) {
        return a != null && a.equals(b);
    }

    private String fmt(@Nullable BigDecimal value) {
        return value == null ? "--" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private NpciTransaction toEntity(NpciRecord row, LocalDate date) {
        NpciTransaction e = new NpciTransaction();
        e.setUtr(row.utr());
        e.setRrn(row.rrn());
        e.setTxnDate(row.txnDate());
        e.setTxnTime(row.txnTime());
        e.setAmount(row.amount());
        e.setPayerVpa(row.payerVpa());
        e.setPayeeVpa(row.payeeVpa());
        e.setStatus(row.status());
        e.setReconDate(date);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private SwitchLog toEntity(SwitchRecord row, LocalDate date) {
        SwitchLog e = new SwitchLog();
        e.setUtr(row.utr());
        e.setRrn(row.rrn());
        e.setTxnDate(row.txnDate());
        e.setTxnTime(row.txnTime());
        e.setAmount(row.amount());
        e.setStatus(row.status());
        e.setResponseCode(row.responseCode());
        e.setSwitchRef(row.switchRef());
        e.setReconDate(date);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private ReconResult toEntity(ReconResultRecord row, LocalDate date) {
        ReconResult e = new ReconResult();
        e.setUtr(row.utr());
        e.setReconDate(date);
        e.setNpciAmount(row.npciAmount());
        e.setSwitchAmount(row.switchAmount());
        e.setCbsAmount(row.cbsAmount());
        e.setNpciStatus(row.npciStatus());
        e.setSwitchStatus(row.switchStatus());
        e.setCbsStatus(row.cbsStatus());
        e.setReconStatus(row.reconStatus());
        e.setRemarks(row.remarks());
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private ReconResultRecord toRecord(ReconResult row) {
        return new ReconResultRecord(
            row.getUtr(),
            row.getNpciAmount(),
            row.getSwitchAmount(),
            row.getCbsAmount(),
            row.getNpciStatus(),
            row.getSwitchStatus(),
            row.getCbsStatus(),
            row.getReconStatus(),
            row.getRemarks()
        );
    }

    private CbsExtract toCbsEntity(CbsRecord row, LocalDate date) {
        CbsExtract e = new CbsExtract();
        e.setUtr(row.utr());
        e.setAccountNo(row.accountNo());
        e.setTxnDate(row.txnDate());
        e.setTxnTime(row.txnTime());
        e.setAmount(row.amount());
        e.setDrCr(row.drCr());
        e.setDescription(row.description());
        e.setCbsRef(row.cbsRef());
        e.setStatus(row.status());
        e.setReconDate(date);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private ReconSummary toRunSummary(LocalDate date, int npciCount, int switchCount, int cbsCount, List<ReconResultRecord> results,
                                      Path outputFile, long reconciliationMillis, long durationMillis, SettlementResult settlement) {
        long matched = results.stream().filter(r -> "MATCHED".equals(r.reconStatus())).count();
        long switchMissing = results.stream().filter(r -> "SWITCH_MISSING".equals(r.reconStatus())).count();
        long npciMissing = results.stream().filter(r -> "NPCI_MISSING".equals(r.reconStatus())).count();
        long cbsMissing = results.stream().filter(r -> "CBS_MISSING".equals(r.reconStatus())).count();
        long amountMismatch = results.stream().filter(r -> "AMOUNT_MISMATCH".equals(r.reconStatus())).count();
        long statusMismatch = results.stream().filter(r -> "STATUS_MISMATCH".equals(r.reconStatus())).count();
        return new ReconSummary(date, npciCount, switchCount, cbsCount, matched, switchMissing, npciMissing, cbsMissing, amountMismatch,
            statusMismatch, outputFile.toString().replace('\\', '/'), "COMPLETED", reconciliationMillis, durationMillis, settlement, null, null);
    }

    private BigDecimal calculateMatchedNet(List<ReconResultRecord> results) {
        return results.stream()
            .filter(r -> "MATCHED".equals(r.reconStatus()))
            .map(ReconResultRecord::npciAmount)
            .filter(v -> v != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
