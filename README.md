# UPI Recon V1

Minimal Spring Boot 4 application for UPI reconciliation using two pipe-delimited input files and one output file.

## Prerequisites
- Java 25
- Docker Desktop
- (Optional) Maven 3.9+ — if you do not use the included Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Start PostgreSQL
```bash
docker compose up -d
```

This starts PostgreSQL 15 with:
- DB: `recondb`
- User: `recon_user`
- Password: `recon_pass`

## Input File Placement
Put files in (pipe-delimited UTF-8; extension controlled by `recon.file.extension`, default **`dat`**):
- `data/input/npci/NPCI_TXN_YYYYMMDD.dat`
- `data/input/switch/SWITCH_LOG_YYYYMMDD.dat`

To use `.txt` instead, set environment variable `RECON_FILE_EXT=txt` or change `recon.file.extension` in `application.yml`.

Sample files for `20240101` are already included.

## Generate test `.dat` files
Create matching NPCI and Switch files with any row count (e.g. 100, 1000, 10000):

**Windows (PowerShell / cmd):**
```bat
.\mvnw.cmd -q compile exec:java "-Dexec.args=1000 --date 20240315"
```

**macOS / Linux:**
```bash
./mvnw -q compile exec:java -Dexec.args="1000 --date 20240315"
```

This writes `data/input/npci/NPCI_TXN_20240315.dat` and `data/input/switch/SWITCH_LOG_20240315.dat` (UTF-8, same column layout as the app). With **no arguments**, it generates **100** rows for **today’s** date (override with `-n`, `-d`, or env vars — see `SampleDataGenerator --help`).

## Run Application
With the Maven Wrapper (no global `mvn` required):

**Windows (PowerShell / cmd):**
```bat
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**
```bash
./mvnw spring-boot:run
```

If Maven is installed globally, you can use `mvn spring-boot:run` instead.

## API Calls
Run recon:
```bash
curl -X POST "http://localhost:8080/api/recon/run?date=20240101"
```

Get results:
```bash
curl "http://localhost:8080/api/recon/results?date=20240101"
```

## Output File
Generated at (same extension as input/output config, default `.dat`):
- `data/output/RECON_RESULT_20240101.dat`
#
