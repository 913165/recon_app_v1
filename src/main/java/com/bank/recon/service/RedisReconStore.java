package com.bank.recon.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.bank.recon.model.dto.ReconResultRecord;
import com.bank.recon.model.dto.ReconSummary;

import org.springframework.data.redis.core.StringRedisTemplate;

@Service
public class RedisReconStore {
    private final StringRedisTemplate redisTemplate;

    public RedisReconStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean existsByReconDate(LocalDate date) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(summaryKey(date)));
    }

    public void save(LocalDate date, int npciCount, int switchCount, int cbsCount, List<ReconResultRecord> rows) {
        String summaryKey = summaryKey(date);
        String rowsKey = rowsKey(date);
        redisTemplate.delete(summaryKey);
        redisTemplate.delete(rowsKey);

        long matched = rows.stream().filter(r -> "MATCHED".equals(r.reconStatus())).count();
        long switchMissing = rows.stream().filter(r -> "SWITCH_MISSING".equals(r.reconStatus())).count();
        long npciMissing = rows.stream().filter(r -> "NPCI_MISSING".equals(r.reconStatus())).count();
        long cbsMissing = rows.stream().filter(r -> "CBS_MISSING".equals(r.reconStatus())).count();
        long amountMismatch = rows.stream().filter(r -> "AMOUNT_MISMATCH".equals(r.reconStatus())).count();
        long statusMismatch = rows.stream().filter(r -> "STATUS_MISMATCH".equals(r.reconStatus())).count();

        redisTemplate.opsForHash().put(summaryKey, "totalNpciRows", String.valueOf(npciCount));
        redisTemplate.opsForHash().put(summaryKey, "totalSwitchRows", String.valueOf(switchCount));
        redisTemplate.opsForHash().put(summaryKey, "totalCbsRows", String.valueOf(cbsCount));
        redisTemplate.opsForHash().put(summaryKey, "matched", String.valueOf(matched));
        redisTemplate.opsForHash().put(summaryKey, "switchMissing", String.valueOf(switchMissing));
        redisTemplate.opsForHash().put(summaryKey, "npciMissing", String.valueOf(npciMissing));
        redisTemplate.opsForHash().put(summaryKey, "cbsMissing", String.valueOf(cbsMissing));
        redisTemplate.opsForHash().put(summaryKey, "amountMismatch", String.valueOf(amountMismatch));
        redisTemplate.opsForHash().put(summaryKey, "statusMismatch", String.valueOf(statusMismatch));

        List<String> payloads = new ArrayList<>(rows.size());
        for (ReconResultRecord row : rows) {
            payloads.add(encodeRow(row));
        }
        if (!payloads.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(rowsKey, payloads);
        }
    }

    public ReconSummary getOverview(LocalDate date) {
        String key = summaryKey(date);
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return new ReconSummary(
                date, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                null, "COMPLETED", null, null, null, new ReconSummary.Summary(0, 0, 0), List.of()
            );
        }

        long npciRows = longField(key, "totalNpciRows");
        long switchRows = longField(key, "totalSwitchRows");
        long cbsRows = longField(key, "totalCbsRows");
        long matched = longField(key, "matched");
        long switchMissing = longField(key, "switchMissing");
        long npciMissing = longField(key, "npciMissing");
        long cbsMissing = longField(key, "cbsMissing");
        long amountMismatch = longField(key, "amountMismatch");
        long statusMismatch = longField(key, "statusMismatch");
        long total = listSize(rowsKey(date));

        return new ReconSummary(
            date, npciRows, switchRows, cbsRows, matched, switchMissing, npciMissing, cbsMissing, amountMismatch, statusMismatch,
            null, "COMPLETED", null, null, null, new ReconSummary.Summary(total, matched, total - matched), List.of()
        );
    }

    public Page<ReconResultRecord> getPage(LocalDate date, int page, int size, String filter) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 1000);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        List<ReconResultRecord> allRows = readAllRows(date);
        List<ReconResultRecord> filtered = switch (filter) {
            case "MATCHED" -> allRows.stream().filter(r -> "MATCHED".equals(r.reconStatus())).toList();
            case "MISMATCHED" -> allRows.stream().filter(r -> !"MATCHED".equals(r.reconStatus())).toList();
            default -> allRows;
        };

        int from = Math.min(safePage * safeSize, filtered.size());
        int to = Math.min(from + safeSize, filtered.size());
        List<ReconResultRecord> content = filtered.subList(from, to);
        return new PageImpl<>(content, pageable, filtered.size());
    }

    private List<ReconResultRecord> readAllRows(LocalDate date) {
        List<String> payloads = redisTemplate.opsForList().range(rowsKey(date), 0, -1);
        if (payloads == null || payloads.isEmpty()) {
            return List.of();
        }
        List<ReconResultRecord> rows = new ArrayList<>(payloads.size());
        for (String payload : payloads) {
            rows.add(decodeRow(payload));
        }
        rows.sort((a, b) -> a.utr().compareTo(b.utr()));
        return rows;
    }

    private static String encodeRow(ReconResultRecord row) {
        return String.join("\t",
            str(row.utr()),
            str(row.npciAmount()),
            str(row.switchAmount()),
            str(row.cbsAmount()),
            str(row.npciStatus()),
            str(row.switchStatus()),
            str(row.cbsStatus()),
            str(row.reconStatus()),
            str(row.remarks())
        );
    }

    private static ReconResultRecord decodeRow(String payload) {
        String[] p = payload.split("\t", -1);
        return new ReconResultRecord(
            nullable(p, 0),
            decimal(nullable(p, 1)),
            decimal(nullable(p, 2)),
            decimal(nullable(p, 3)),
            nullable(p, 4),
            nullable(p, 5),
            nullable(p, 6),
            nullable(p, 7),
            nullable(p, 8)
        );
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }

    private static String nullable(String[] arr, int i) {
        if (i >= arr.length) {
            return null;
        }
        String v = arr[i];
        return v == null || v.isEmpty() ? null : v;
    }

    private static BigDecimal decimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    private long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0 : size;
    }

    private long longField(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        if (value == null) {
            return 0;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private static String summaryKey(LocalDate date) {
        return "recon:summary:" + date;
    }

    private static String rowsKey(LocalDate date) {
        return "recon:rows:" + date;
    }
}
