package com.bank.recon.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bank.recon.model.dto.ReconResultRecord;

@Service
public class FileWriterService {

    public void writeReconResult(List<ReconResultRecord> results, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("UTR|NPCI_AMOUNT|SWITCH_AMOUNT|NPCI_STATUS|SWITCH_STATUS|RECON_STATUS|REMARKS");
        for (ReconResultRecord row : results) {
            lines.add(String.join("|", safe(row.utr()), amount(row.npciAmount()), amount(row.switchAmount()), safe(row.npciStatus()), safe(row.switchStatus()), safe(row.reconStatus()), safe(row.remarks())));
        }
        Files.write(outputPath, lines, StandardCharsets.UTF_8);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private String amount(BigDecimal value) {
        return value == null ? "--" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
