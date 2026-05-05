package com.bank.recon.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.bank.recon.model.dto.CbsRecord;
import com.bank.recon.model.dto.NpciRecord;
import com.bank.recon.model.dto.SettlementFileRecord;
import com.bank.recon.model.dto.SwitchRecord;

@Service
public class FileParserService {
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public List<NpciRecord> parseNpciFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("NPCI file not found for date " + extractDate(filePath.getFileName().toString()));
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<NpciRecord> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\\|", -1);
            records.add(new NpciRecord(value(p, 0), value(p, 1), value(p, 2), value(p, 3), decimal(value(p, 4)), value(p, 5), value(p, 6), value(p, 7)));
        }
        return records;
    }

    public List<SwitchRecord> parseSwitchFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Switch log file not found for date " + extractDate(filePath.getFileName().toString()));
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<SwitchRecord> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\\|", -1);
            records.add(new SwitchRecord(value(p, 0), value(p, 1), value(p, 2), value(p, 3), decimal(value(p, 4)), value(p, 5), value(p, 6), value(p, 7)));
        }
        return records;
    }

    public List<CbsRecord> parseCbsFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("CBS extract file not found for date " + extractDate(filePath.getFileName().toString()));
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        List<CbsRecord> records = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\\|", -1);
            records.add(new CbsRecord(
                value(p, 0),
                value(p, 1),
                value(p, 2),
                value(p, 3),
                decimal(value(p, 4)),
                value(p, 5),
                value(p, 6),
                value(p, 7),
                value(p, 8)));
        }
        return records;
    }

    public SettlementFileRecord parseSettlementFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException("Settlement file not found for date " + extractDate(filePath.getFileName().toString()));
        }
        List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        if (lines.size() < 2) {
            throw new IOException("Settlement file has no data row for date " + extractDate(filePath.getFileName().toString()));
        }
        String[] p = lines.get(1).split("\\|", -1);
        return new SettlementFileRecord(
            LocalDate.parse(value(p, 0), FILE_DATE_FORMAT),
            value(p, 1),
            integer(value(p, 2)),
            decimal(value(p, 3)),
            decimal(value(p, 4)),
            decimal(value(p, 5)),
            value(p, 6),
            value(p, 7)
        );
    }

    private String value(String[] parts, int index) {
        if (index >= parts.length) return null;
        String v = parts[index] == null ? "" : parts[index].trim();
        return v.isBlank() ? null : v;
    }

    private BigDecimal decimal(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return new BigDecimal(raw);
    }

    private int integer(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        return Integer.parseInt(raw);
    }

    private String extractDate(String fileName) {
        String digits = fileName.replaceAll("\\D", "");
        return digits.length() >= 8 ? digits.substring(0, 8) : "UNKNOWN";
    }
}
