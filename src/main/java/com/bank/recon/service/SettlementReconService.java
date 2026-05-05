package com.bank.recon.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.recon.config.AppConfig;
import com.bank.recon.model.dto.SettlementFileRecord;
import com.bank.recon.model.dto.SettlementResult;
import com.bank.recon.model.entity.SettlementRecord;
import com.bank.recon.repository.SettlementRecordRepository;

@Service
public class SettlementReconService {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final FileParserService fileParserService;
    private final AppConfig appConfig;
    private final SettlementRecordRepository settlementRecordRepository;

    public SettlementReconService(FileParserService fileParserService, AppConfig appConfig,
                                  SettlementRecordRepository settlementRecordRepository) {
        this.fileParserService = fileParserService;
        this.appConfig = appConfig;
        this.settlementRecordRepository = settlementRecordRepository;
    }

    @Transactional
    public SettlementResult reconcileSettlement(LocalDate date, BigDecimal calculatedNet) {
        String fileDate = date.format(FILE_DATE_FORMAT);
        var settlementFile = appConfig.input().npciDir().resolve("NPCI_SETTLEMENT_" + fileDate + ".txt");

        SettlementRecord entity = settlementRecordRepository.findByReconDate(date).orElseGet(SettlementRecord::new);
        entity.setReconDate(date);
        entity.setCalculatedNet(calculatedNet);
        entity.setCreatedAt(LocalDateTime.now());

        try {
            SettlementFileRecord npci = fileParserService.parseSettlementFile(settlementFile);
            BigDecimal npciNet = defaultZero(npci.netAmount());
            BigDecimal diff = calculatedNet.subtract(npciNet);
            String status = diff.compareTo(BigDecimal.ZERO) == 0 ? "SETTLEMENT_MATCHED" : "SETTLEMENT_MISMATCH";

            entity.setBankCode(npci.bankCode());
            entity.setTotalTxn(npci.totalTxn());
            entity.setNpciTotalDebit(npci.totalDebit());
            entity.setNpciTotalCredit(npci.totalCredit());
            entity.setNpciNetAmount(npciNet);
            entity.setDifference(diff);
            entity.setRbiRef(npci.rbiRef());
            entity.setNpciStatus(npci.status());
            entity.setSettlementStatus(status);
            entity.setRemarks(diff.compareTo(BigDecimal.ZERO) == 0
                ? "Settlement net matched with calculated net"
                : "Settlement mismatch by " + diff.toPlainString());

            settlementRecordRepository.save(entity);
            return new SettlementResult(date, npciNet, calculatedNet, diff, status, npci.rbiRef(), entity.getRemarks());
        } catch (FileNotFoundException ex) {
            entity.setSettlementStatus("SETTLEMENT_PENDING");
            entity.setDifference(null);
            entity.setRemarks("Settlement file not received for date " + fileDate);
            settlementRecordRepository.save(entity);
            return new SettlementResult(date, null, calculatedNet, null, "SETTLEMENT_PENDING", null, entity.getRemarks());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Transactional(readOnly = true)
    public SettlementResult getSettlement(LocalDate date) {
        SettlementRecord row = settlementRecordRepository.findByReconDate(date)
            .orElseThrow(() -> new IllegalArgumentException("Settlement record not found for date " + date.format(FILE_DATE_FORMAT)));
        return new SettlementResult(
            date,
            row.getNpciNetAmount(),
            row.getCalculatedNet(),
            row.getDifference(),
            row.getSettlementStatus(),
            row.getRbiRef(),
            row.getRemarks()
        );
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
