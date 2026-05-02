package com.bank.recon.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.recon.config.AppConfig;
import com.bank.recon.exception.ReconAlreadyRunException;
import com.bank.recon.model.dto.NpciRecord;
import com.bank.recon.model.dto.ReconResultRecord;
import com.bank.recon.model.dto.ReconSummary;
import com.bank.recon.model.dto.SwitchRecord;
import com.bank.recon.model.entity.NpciTransaction;
import com.bank.recon.model.entity.ReconResult;
import com.bank.recon.model.entity.SwitchLog;
import com.bank.recon.model.status.AmountMismatch;
import com.bank.recon.model.status.Matched;
import com.bank.recon.model.status.NpciMissing;
import com.bank.recon.model.status.ReconStatus;
import com.bank.recon.model.status.StatusMismatch;
import com.bank.recon.model.status.SwitchMissing;
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
    private final ReconResultRepository reconResultRepository;
    private final AppConfig appConfig;

    public ReconService(FileParserService fileParserService, FileWriterService fileWriterService,
                        NpciTransactionRepository npciTransactionRepository, SwitchLogRepository switchLogRepository,
                        ReconResultRepository reconResultRepository, AppConfig appConfig) {
        this.fileParserService = fileParserService;
        this.fileWriterService = fileWriterService;
        this.npciTransactionRepository = npciTransactionRepository;
        this.switchLogRepository = switchLogRepository;
        this.reconResultRepository = reconResultRepository;
        this.appConfig = appConfig;
    }

    @Transactional
    public ReconSummary runRecon(LocalDate date) throws IOException {
        if (reconResultRepository.existsByReconDate(date)) {
            throw new ReconAlreadyRunException("Recon already run for date " + date.format(FILE_DATE_FORMAT));
        }

        String fileDate = date.format(FILE_DATE_FORMAT);
        Path npciFile = appConfig.input().npciDir().resolve("NPCI_TXN_" + fileDate + ".txt");
        Path switchFile = appConfig.input().switchDir().resolve("SWITCH_LOG_" + fileDate + ".txt");
        Path outputFile = appConfig.output().outputDir().resolve("RECON_RESULT_" + fileDate + ".txt");

        List<NpciRecord> npciRows = fileParserService.parseNpciFile(npciFile);
        List<SwitchRecord> switchRows = fileParserService.parseSwitchFile(switchFile);

        npciTransactionRepository.saveAll(npciRows.stream().map(row -> toEntity(row, date)).toList());
        switchLogRepository.saveAll(switchRows.stream().map(row -> toEntity(row, date)).toList());

        Map<String, NpciRecord> npciByUtr = new LinkedHashMap<>();
        Map<String, SwitchRecord> switchByUtr = new LinkedHashMap<>();
        for (NpciRecord row : npciRows) npciByUtr.put(row.utr(), row);
        for (SwitchRecord row : switchRows) switchByUtr.put(row.utr(), row);

        List<ReconResultRecord> results = new ArrayList<>();
        TreeSet<String> utrs = new TreeSet<>();
        utrs.addAll(npciByUtr.keySet());
        utrs.addAll(switchByUtr.keySet());

        for (String utr : utrs) {
            NpciRecord npci = npciByUtr.get(utr);
            SwitchRecord sw = switchByUtr.get(utr);
            ReconStatus status = resolveStatus(npci, sw);
            results.add(new ReconResultRecord(
                utr,
                npci == null ? null : npci.amount(),
                sw == null ? null : sw.amount(),
                npci == null ? null : npci.status(),
                sw == null ? null : sw.status(),
                status.code(),
                status.remarks()
            ));
        }

        reconResultRepository.saveAll(results.stream().map(row -> toEntity(row, date)).toList());
        fileWriterService.writeReconResult(results, outputFile);

        return toRunSummary(date, npciRows.size(), switchRows.size(), results, outputFile);
    }

    @Transactional(readOnly = true)
    public ReconSummary getResults(LocalDate date) {
        List<ReconResultRecord> results = reconResultRepository.findAllByReconDateOrderByUtrAsc(date).stream()
            .map(row -> new ReconResultRecord(row.getUtr(), row.getNpciAmount(), row.getSwitchAmount(), row.getNpciStatus(), row.getSwitchStatus(), row.getReconStatus(), row.getRemarks()))
            .toList();

        long matched = results.stream().filter(r -> "MATCHED".equals(r.reconStatus())).count();
        long switchMissing = results.stream().filter(r -> "SWITCH_MISSING".equals(r.reconStatus())).count();
        long npciMissing = results.stream().filter(r -> "NPCI_MISSING".equals(r.reconStatus())).count();
        long amountMismatch = results.stream().filter(r -> "AMOUNT_MISMATCH".equals(r.reconStatus())).count();
        long statusMismatch = results.stream().filter(r -> "STATUS_MISMATCH".equals(r.reconStatus())).count();

        return new ReconSummary(date, 0, 0, matched, switchMissing, npciMissing, amountMismatch, statusMismatch, null,
            "COMPLETED", new ReconSummary.Summary(results.size(), matched, results.size() - matched), results);
    }

    private ReconStatus resolveStatus(@Nullable NpciRecord npci, @Nullable SwitchRecord sw) {
        if (npci != null && sw == null) return new SwitchMissing();
        if (npci == null) return new NpciMissing();
        if (!amountMatches(npci.amount(), sw.amount())) return new AmountMismatch("Amount differs: NPCI=" + fmt(npci.amount()) + " Switch=" + fmt(sw.amount()));
        if (!stringMatches(npci.status(), sw.status())) return new StatusMismatch("Status differs: NPCI=" + npci.status() + " Switch=" + sw.status());
        return new Matched();
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
        e.setNpciStatus(row.npciStatus());
        e.setSwitchStatus(row.switchStatus());
        e.setReconStatus(row.reconStatus());
        e.setRemarks(row.remarks());
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private ReconSummary toRunSummary(LocalDate date, int npciCount, int switchCount, List<ReconResultRecord> results, Path outputFile) {
        long matched = results.stream().filter(r -> "MATCHED".equals(r.reconStatus())).count();
        long switchMissing = results.stream().filter(r -> "SWITCH_MISSING".equals(r.reconStatus())).count();
        long npciMissing = results.stream().filter(r -> "NPCI_MISSING".equals(r.reconStatus())).count();
        long amountMismatch = results.stream().filter(r -> "AMOUNT_MISMATCH".equals(r.reconStatus())).count();
        long statusMismatch = results.stream().filter(r -> "STATUS_MISMATCH".equals(r.reconStatus())).count();
        return new ReconSummary(date, npciCount, switchCount, matched, switchMissing, npciMissing, amountMismatch, statusMismatch,
            outputFile.toString().replace('\\', '/'), "COMPLETED", null, null);
    }
}
