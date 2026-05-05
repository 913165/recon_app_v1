package com.bank.recon.generate;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Standalone entry point: writes pipe-delimited {@code .dat} NPCI, Switch, and CBS extract files for load / integration tests.
 * <p>
 * Run (project root):
 * <pre>
 *   {@code java -cp target/classes com.bank.recon.generate.SampleDataGenerator 1000}
 *   {@code mvnw -q compile exec:java "-Dexec.args=1000 --date 20240315"}
 * </pre>
 */
public final class SampleDataGenerator {

    /** Used when no {@code -n} / positional count and no {@code RECON_GEN_COUNT} env. */
    private static final int DEFAULT_ROW_COUNT = 1_00_000;

    private static final String[] PAYER_VPAS = {
        "priya.reddy@ybl", "amit.kumar@oksbi", "sneha.menon@axl", "rohit.sharma@ybl",
        "kavita.nair@paytm", "vikram.patel@oksbi", "ananya.das@paytm", "deepa.joshi@ibl"
    };
    private static final String[] PAYEE_VPAS = {
        "dominos@pine", "bigbasket@hdfcbank", "freshmart@icici", "foodhall@yesbank",
        "hotel.taj@ibl", "cafecoffee@sbi", "retail.store@axisbank", "grocery@ybl"
    };

    public static void main(String[] args) {
        Args opt = parseArgs(args);
        if (opt.showHelp) {
            printHelp();
            return;
        }
        if (opt.n < 1) {
            System.err.println("n must be >= 1");
            System.exit(1);
            return;
        }
        if (opt.mismatchPercent < 0 || opt.mismatchPercent > 100) {
            System.err.println("--mismatch-percent must be between 0 and 100");
            System.exit(1);
            return;
        }
        if (opt.demoAnomalies && opt.n < 3) {
            System.err.println("--demo-anomalies requires n >= 3");
            System.exit(1);
            return;
        }
        Random random = opt.seed == null ? new Random(ThreadLocalRandom.current().nextLong()) : new Random(opt.seed);
        try {
            writePair(opt, random);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        int switchRows = opt.demoAnomalies ? opt.n - 1 : opt.n;
        System.out.printf(Locale.ROOT, "Wrote %d NPCI rows, %d Switch rows, %d CBS rows, and settlement file:%n  %s%n  %s%n  %s%n  %s%n",
            opt.n, switchRows, opt.n, opt.npciFile, opt.switchFile, opt.cbsFile, opt.settlementFile);
    }

    private static void writePair(Args opt, Random random) throws IOException {
        Files.createDirectories(opt.npciDir);
        Files.createDirectories(opt.switchDir);
        Files.createDirectories(opt.cbsDir);
        String date = opt.fileDate;
        StringBuilder npci = new StringBuilder();
        StringBuilder sw = new StringBuilder();
        StringBuilder cbs = new StringBuilder();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int mismatchRows = 0;
        int amountMismatchRows = 0;
        int statusMismatchRows = 0;
        if (opt.mismatchPercent > 0) {
            mismatchRows = (int) Math.round((opt.n * opt.mismatchPercent) / 100.0d);
            mismatchRows = Math.max(1, mismatchRows);
            mismatchRows = Math.min(opt.n, mismatchRows);
            if (mismatchRows == 1) {
                amountMismatchRows = 1;
                statusMismatchRows = 0;
            } else {
                // Keep split uneven and realistic (not fixed 50/50).
                double amountShare = 0.55d + (random.nextDouble() * 0.30d); // 55%..85%
                amountMismatchRows = (int) Math.round(mismatchRows * amountShare);
                amountMismatchRows = Math.max(1, Math.min(mismatchRows - 1, amountMismatchRows));
                statusMismatchRows = mismatchRows - amountMismatchRows;
                if (amountMismatchRows == statusMismatchRows) {
                    amountMismatchRows = Math.min(mismatchRows - 1, amountMismatchRows + 1);
                    statusMismatchRows = mismatchRows - amountMismatchRows;
                }
            }
        }
        npci.append("UTR|RRN|TXN_DATE|TXN_TIME|AMOUNT|PAYER_VPA|PAYEE_VPA|STATUS\n");
        sw.append("UTR|RRN|TXN_DATE|TXN_TIME|AMOUNT|STATUS|RESPONSE_CODE|SWITCH_REF\n");
        cbs.append("UTR|ACCOUNT_NO|TXN_DATE|TXN_TIME|AMOUNT|DR_CR|DESCRIPTION|CBS_REF|STATUS\n");

        for (int i = 1; i <= opt.n; i++) {
            String utr = buildUtr(date, i);
            String rrn = buildRrn(i);
            String time = buildTime(i, random);
            BigDecimal amount = randomAmount(random);
            String payer = PAYER_VPAS[(i - 1) % PAYER_VPAS.length];
            String payee = PAYEE_VPAS[(i - 1) % PAYEE_VPAS.length];
            String npciStatus = "SUCCESS";

            npci.append(String.join("|", utr, rrn, date, time, amount.toPlainString(), payer, payee, npciStatus));
            npci.append('\n');
            totalDebit = totalDebit.add(amount);

            String accountNo = buildCbsAccountNo(i);
            String cbsRef = i <= 999
                ? "CBS" + String.format(Locale.ROOT, "%03d", i)
                : "CBS" + String.format(Locale.ROOT, "%08d", i);
            String cbsStatus = "SUCCESS";
            if (opt.demoAnomalies && i == opt.n) {
                cbsStatus = "FAILED";
            }
            String description = "UPI/" + utr + "/" + payee;
            cbs.append(String.join("|",
                utr,
                accountNo,
                date,
                time,
                amount.toPlainString(),
                "DR",
                description,
                cbsRef,
                cbsStatus));
            cbs.append('\n');

            if (opt.demoAnomalies && i == opt.n - 2) {
                continue;
            }

            BigDecimal swAmount = amount;
            String swStatus = "SUCCESS";
            String response = "00";
            boolean mismatchRow = mismatchRows > 0 && i > (opt.n - mismatchRows);
            if (mismatchRow) {
                // Randomized assignment with quotas so split is uneven but controlled.
                int remainingRows = amountMismatchRows + statusMismatchRows;
                boolean makeAmountMismatch;
                if (amountMismatchRows == 0) {
                    makeAmountMismatch = false;
                } else if (statusMismatchRows == 0) {
                    makeAmountMismatch = true;
                } else {
                    makeAmountMismatch = random.nextInt(remainingRows) < amountMismatchRows;
                }
                if (makeAmountMismatch) {
                    swAmount = amount.add(new BigDecimal("0.01"));
                    amountMismatchRows--;
                } else {
                    swStatus = "FAILED";
                    response = "51";
                    statusMismatchRows--;
                }
            }
            if (opt.demoAnomalies && i == opt.n - 1) {
                swAmount = amount.add(new BigDecimal("0.01"));
            }
            if (opt.demoAnomalies && i == opt.n) {
                swStatus = "FAILED";
                response = "51";
            }
            String switchRef = "923" + date + String.format(Locale.ROOT, "%08d", i);
            sw.append(String.join("|", utr, rrn, date, time, swAmount.toPlainString(), swStatus, response, switchRef));
            sw.append('\n');
        }

        Files.writeString(opt.npciFile, npci.toString(), StandardCharsets.UTF_8);
        Files.writeString(opt.switchFile, sw.toString(), StandardCharsets.UTF_8);
        Files.writeString(opt.cbsFile, cbs.toString(), StandardCharsets.UTF_8);

        BigDecimal settlementNet = totalDebit.subtract(totalCredit);
        if (opt.settlementMismatch) {
            settlementNet = settlementNet.add(new BigDecimal("50.00"));
        }
        StringBuilder settlement = new StringBuilder();
        settlement.append("SETTLEMENT_DATE|BANK_CODE|TOTAL_TXN|TOTAL_DEBIT|TOTAL_CREDIT|NET_AMOUNT|RBI_REF|STATUS\n");
        settlement.append(String.join("|",
            date,
            "HDFC",
            String.valueOf(opt.n),
            totalDebit.toPlainString(),
            totalCredit.toPlainString(),
            settlementNet.toPlainString(),
            "RBI" + date + "0001",
            "SETTLED"));
        settlement.append('\n');
        Files.writeString(opt.settlementFile, settlement.toString(), StandardCharsets.UTF_8);
    }

    /** Matches sample pattern {@code HDFC0001001} for index 1. */
    private static String buildCbsAccountNo(int i) {
        int suffix = 1000 + i;
        return "HDFC" + String.format(Locale.ROOT, "%07d", suffix);
    }

    private static Args parseArgs(String[] raw) {
        Args a = new Args();
        List<String> positional = new ArrayList<>();
        for (int i = 0; i < raw.length; i++) {
            String s = raw[i];
            switch (s) {
                case "-h", "--help" -> a.showHelp = true;
                case "-n", "--count" -> a.n = Integer.parseInt(require(++i, raw, "count"));
                case "-d", "--date" -> a.fileDate = require(++i, raw, "date");
                case "--npci-dir" -> a.npciDir = Path.of(require(++i, raw, "npci-dir"));
                case "--switch-dir" -> a.switchDir = Path.of(require(++i, raw, "switch-dir"));
                case "--cbs-dir" -> a.cbsDir = Path.of(require(++i, raw, "cbs-dir"));
                case "--settlement-mismatch" -> a.settlementMismatch = true;
                case "--seed" -> a.seed = Long.parseLong(require(++i, raw, "seed"));
                case "--mismatch-percent" -> a.mismatchPercent = Double.parseDouble(require(++i, raw, "mismatch-percent"));
                case "--demo-anomalies" -> a.demoAnomalies = true;
                default -> {
                    if (s.startsWith("-")) {
                        throw new IllegalArgumentException("Unknown option: " + s);
                    }
                    positional.add(s);
                }
            }
        }
        if (!positional.isEmpty() && a.n == 0) {
            a.n = Integer.parseInt(positional.getFirst());
        }
        applyEnvAndDefaults(a);
        String ext = normalizeExtension(System.getenv("RECON_FILE_EXT"));
        a.npciFile = a.npciDir.resolve("NPCI_TXN_" + a.fileDate + "." + ext);
        a.switchFile = a.switchDir.resolve("SWITCH_LOG_" + a.fileDate + "." + ext);
        a.cbsFile = a.cbsDir.resolve("CBS_EXTRACT_" + a.fileDate + "." + ext);
        a.settlementFile = a.npciDir.resolve("NPCI_SETTLEMENT_" + a.fileDate + ".txt");
        return a;
    }

    private static void applyEnvAndDefaults(Args a) {
        var isoDate = java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
        if (a.fileDate == null) {
            String env = getenvOrBlank("RECON_GEN_DATE");
            a.fileDate = env != null ? env : java.time.LocalDate.now().format(isoDate);
        }
        if (a.npciDir == null) {
            String env = getenvOrBlank("RECON_NPCI_PATH");
            a.npciDir = env != null ? Path.of(env) : Path.of("data/input/npci");
        }
        if (a.switchDir == null) {
            String env = getenvOrBlank("RECON_SWITCH_PATH");
            a.switchDir = env != null ? Path.of(env) : Path.of("data/input/switch");
        }
        if (a.cbsDir == null) {
            String env = getenvOrBlank("RECON_CBS_PATH");
            a.cbsDir = env != null ? Path.of(env) : Path.of("data/input/cbs");
        }
        if (a.seed == null) {
            String env = getenvOrBlank("RECON_GEN_SEED");
            if (env != null) {
                a.seed = Long.parseLong(env);
            }
        }
        if (a.n == 0) {
            String env = getenvOrBlank("RECON_GEN_COUNT");
            a.n = env != null ? Integer.parseInt(env) : DEFAULT_ROW_COUNT;
        }
    }

    private static String getenvOrBlank(String name) {
        String v = System.getenv(name);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static String normalizeExtension(String raw) {
        if (raw == null || raw.isBlank()) {
            return "dat";
        }
        String e = raw.strip();
        if (e.startsWith(".")) {
            e = e.substring(1);
        }
        return e.isEmpty() ? "dat" : e;
    }

    private static String require(int i, String[] raw, String name) {
        if (i >= raw.length) {
            throw new IllegalArgumentException("Missing value for " + name);
        }
        return raw[i];
    }

    private static String buildUtr(String yyyymmdd, int i) {
        return "5" + yyyymmdd + String.format(Locale.ROOT, "%08d", i);
    }

    private static String buildRrn(int i) {
        long base = 800_000_000_000L + (long) i;
        if (base > 999_999_999_999L) {
            base = 100_000_000_000L + (i % 900_000_000_000L);
        }
        return String.format(Locale.ROOT, "%012d", base);
    }

    private static String buildTime(int i, Random random) {
        int secondOfDay = (i * 17_311 + random.nextInt(60)) % 86_400;
        int h = secondOfDay / 3600;
        int m = (secondOfDay % 3600) / 60;
        int sec = secondOfDay % 60;
        return String.format(Locale.ROOT, "%02d%02d%02d", h, m, sec);
    }

    private static BigDecimal randomAmount(Random random) {
        int cents = 100 + random.nextInt(5_000_000);
        return BigDecimal.valueOf(cents, 2);
    }

    private static void printHelp() {
        System.out.printf(Locale.ROOT, """
            Generate matching NPCI, Switch, CBS extract, and settlement files (pipe-delimited, UTF-8).

            Usage:
              SampleDataGenerator [n] [options]
              SampleDataGenerator -n <n> [options]

            Defaults (when omitted):
              Row count n     → %d, or env RECON_GEN_COUNT
              Date            → today (yyyyMMdd), or env RECON_GEN_DATE
              --npci-dir      → env RECON_NPCI_PATH, else data/input/npci
              --switch-dir    → env RECON_SWITCH_PATH, else data/input/switch
              --cbs-dir       → env RECON_CBS_PATH, else data/input/cbs
              File extension  → env RECON_FILE_EXT (same as Spring app), else dat
              --seed          → env RECON_GEN_SEED if set

            Options:
              -n, --count <n>     Number of records (overrides default / RECON_GEN_COUNT)
              -d, --date <yyyyMMdd>  File date in filenames and TXN_DATE column
              --npci-dir <path>
              --switch-dir <path>
              --cbs-dir <path>
              --settlement-mismatch  Add +50.00 to settlement NET_AMOUNT to force mismatch
              --seed <long>
              --mismatch-percent <0..100>
                                 Creates approx this %% of switch-side mismatches (default 1.0%%).
              --demo-anomalies    Row (n-2) only in NPCI; (n-1) amount +0.01 on switch;
                                  row n FAILED on switch. Requires n >= 3.
              -h, --help

            Output:
              <npci-dir>/NPCI_TXN_<date>.<ext>
              <npci-dir>/NPCI_SETTLEMENT_<date>.txt
              <switch-dir>/SWITCH_LOG_<date>.<ext>
              <cbs-dir>/CBS_EXTRACT_<date>.<ext>
            """, DEFAULT_ROW_COUNT);
    }

    private static final class Args {
        int n;
        String fileDate;
        Path npciDir;
        Path switchDir;
        Path cbsDir;
        Path npciFile;
        Path settlementFile;
        Path switchFile;
        Path cbsFile;
        Long seed;
        double mismatchPercent = 1.0d;
        boolean showHelp;
        boolean demoAnomalies;
        boolean settlementMismatch;
    }
}
