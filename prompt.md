# UPI Recon V1 — Simple File Comparison
### AI Prompt Document — Spring Boot 4.0.6 · Java 25 · PostgreSQL 15

---
---

# SECTION 1 — AI Prompt  (Copy Everything Below)

Paste the entire block below into any AI assistant.

```
You are a senior Java backend engineer.

Build VERSION 1 of a UPI Reconciliation application — a minimal, working
Spring Boot app that reads 2 input files, compares them, and writes 1 output file.

No authentication. No Kafka. No email. No scheduler. No PDF reports.
No exception management UI. Just core file comparison logic — clean and working.


════════════════════════════════════════════════════════════════
TECH STACK

════════════════════════════════════════════════════════════════

Language  : Java 25
Framework : Spring Boot 4.0.6
Database  : PostgreSQL 15  +  Spring Data JPA
Build     : Maven
JSON      : Jackson 3  (tools.jackson — NOT com.fasterxml.jackson)
Use JsonMapper. Dates as ISO-8601 strings automatically.
Logging   : SLF4J + Logback
Container : Tomcat 11+  (do NOT use Undertow)

Spring Boot 4 rules:
- All imports: jakarta.*  (never javax.*)
- Use Records for all DTOs
- Virtual threads enabled: spring.threads.virtual.enabled=true
- Null safety: org.jspecify.annotations


════════════════════════════════════════════════════════════════
WHAT THE APP DOES — ONE SIMPLE FLOW

════════════════════════════════════════════════════════════════

1. User calls POST /api/recon/run?date=20240101
2. App reads FILE 1: NPCI_TXN_20240101.txt          from /data/input/npci/
3. App reads FILE 2: SWITCH_LOG_20240101.txt         from /data/input/switch/
4. App compares both files row by row using UTR as join key
5. App writes FILE 3: RECON_RESULT_20240101.txt      to   /data/output/
6. App saves all results to PostgreSQL
7. User calls GET /api/recon/results?date=20240101   to see results as JSON

That is the entire app. Nothing else.


════════════════════════════════════════════════════════════════
INPUT FILE 1 — NPCI Transaction File

════════════════════════════════════════════════════════════════

Location : /data/input/npci/NPCI_TXN_{date}.txt
Format   : Pipe-delimited ( | ), plain text, UTF-8
Has header row: YES (first row = column names)

Columns:
UTR | RRN | TXN_DATE | TXN_TIME | AMOUNT | PAYER_VPA | PAYEE_VPA | STATUS

Sample data (include this in test files):
UTR|RRN|TXN_DATE|TXN_TIME|AMOUNT|PAYER_VPA|PAYEE_VPA|STATUS
UTR001|RRN001|20240101|100000|500.00|alice@hdfc|shop@sbi|SUCCESS
UTR002|RRN002|20240101|100500|1000.00|bob@hdfc|mart@icici|SUCCESS
UTR003|RRN003|20240101|101000|750.00|carol@hdfc|store@axis|SUCCESS
UTR004|RRN004|20240101|101500|200.00|dave@hdfc|food@sbi|SUCCESS
UTR005|RRN005|20240101|102000|3000.00|eve@hdfc|hotel@icici|SUCCESS
UTR006|RRN006|20240101|102500|150.00|frank@hdfc|cafe@sbi|SUCCESS
UTR007|RRN007|20240101|103000|800.00|grace@hdfc|shop@axis|SUCCESS
UTR008|RRN008|20240101|103500|450.00|henry@hdfc|mart@sbi|SUCCESS
UTR009|RRN009|20240101|104000|250.00|iris@hdfc|store@icici|SUCCESS
UTR010|RRN010|20240101|104500|600.00|jack@hdfc|cafe@axis|SUCCESS


════════════════════════════════════════════════════════════════
INPUT FILE 2 — Switch Log File

════════════════════════════════════════════════════════════════

Location : /data/input/switch/SWITCH_LOG_{date}.txt
Format   : Pipe-delimited ( | ), plain text, UTF-8
Has header row: YES

Columns:
UTR | RRN | TXN_DATE | TXN_TIME | AMOUNT | STATUS | RESPONSE_CODE | SWITCH_REF

Sample data — deliberately introduce mismatches:
UTR|RRN|TXN_DATE|TXN_TIME|AMOUNT|STATUS|RESPONSE_CODE|SWITCH_REF
UTR001|RRN001|20240101|100000|500.00|SUCCESS|00|SW001
UTR002|RRN002|20240101|100500|1000.00|SUCCESS|00|SW002
UTR003|RRN003|20240101|101000|750.00|SUCCESS|00|SW003
UTR004|RRN004|20240101|101500|200.00|SUCCESS|00|SW004
UTR005|RRN005|20240101|102000|3000.00|SUCCESS|00|SW005
UTR006|RRN006|20240101|102500|150.00|SUCCESS|00|SW006
UTR007|RRN007|20240101|103000|999.00|SUCCESS|00|SW007
UTR009|RRN009|20240101|104000|250.00|FAILED|51|SW009
UTR010|RRN010|20240101|104500|600.00|SUCCESS|00|SW010

NOTE: UTR007 has AMOUNT mismatch (NPCI=800.00, Switch=999.00)
NOTE: UTR008 is MISSING from Switch file entirely
NOTE: UTR009 has STATUS mismatch (NPCI=SUCCESS, Switch=FAILED)


════════════════════════════════════════════════════════════════
COMPARISON LOGIC

════════════════════════════════════════════════════════════════

UTR is the join key. For each UTR found across both files:

Rule 1: UTR in NPCI but NOT in Switch?
→ recon_status = SWITCH_MISSING

Rule 2: UTR in Switch but NOT in NPCI?
→ recon_status = NPCI_MISSING

Rule 3: UTR in both, AMOUNT differs?
→ recon_status = AMOUNT_MISMATCH

Rule 4: UTR in both, STATUS differs?
→ recon_status = STATUS_MISMATCH

Rule 5: UTR in both, AMOUNT and STATUS match?
→ recon_status = MATCHED

Apply rules in order 1 → 5. First matching rule wins.
Use Java 25 sealed interface for recon status:

sealed interface ReconStatus
permits Matched, SwitchMissing, NpciMissing,
AmountMismatch, StatusMismatch {}


════════════════════════════════════════════════════════════════
OUTPUT FILE — Recon Result File

════════════════════════════════════════════════════════════════

Location : /data/output/RECON_RESULT_{date}.txt
Format   : Pipe-delimited ( | ), UTF-8, with header row

Columns:
UTR | NPCI_AMOUNT | SWITCH_AMOUNT | NPCI_STATUS | SWITCH_STATUS | RECON_STATUS | REMARKS

Sample output (based on sample input above):
UTR|NPCI_AMOUNT|SWITCH_AMOUNT|NPCI_STATUS|SWITCH_STATUS|RECON_STATUS|REMARKS
UTR001|500.00|500.00|SUCCESS|SUCCESS|MATCHED|All fields match
UTR002|1000.00|1000.00|SUCCESS|SUCCESS|MATCHED|All fields match
UTR003|750.00|750.00|SUCCESS|SUCCESS|MATCHED|All fields match
UTR004|200.00|200.00|SUCCESS|SUCCESS|MATCHED|All fields match
UTR005|3000.00|3000.00|SUCCESS|SUCCESS|MATCHED|All fields match
UTR006|150.00|150.00|SUCCESS|SUCCESS|MATCHED|All fields match
UTR007|800.00|999.00|SUCCESS|SUCCESS|AMOUNT_MISMATCH|Amount differs: NPCI=800.00 Switch=999.00
UTR008|450.00|--|SUCCESS|--|SWITCH_MISSING|UTR present in NPCI but missing from Switch Log
UTR009|250.00|250.00|SUCCESS|FAILED|STATUS_MISMATCH|Status differs: NPCI=SUCCESS Switch=FAILED
UTR010|600.00|600.00|SUCCESS|SUCCESS|MATCHED|All fields match


════════════════════════════════════════════════════════════════
DATABASE — 3 TABLES ONLY

════════════════════════════════════════════════════════════════

Generate schema.sql for these 3 tables only:

TABLE 1: npci_transactions
id          UUID PRIMARY KEY DEFAULT gen_random_uuid()
utr         VARCHAR(50) NOT NULL
rrn         VARCHAR(50)
txn_date    VARCHAR(10)
txn_time    VARCHAR(10)
amount      NUMERIC(15,2)
payer_vpa   VARCHAR(100)
payee_vpa   VARCHAR(100)
status      VARCHAR(20)
recon_date  DATE NOT NULL
created_at  TIMESTAMP DEFAULT now()

TABLE 2: switch_logs
id            UUID PRIMARY KEY DEFAULT gen_random_uuid()
utr           VARCHAR(50) NOT NULL
rrn           VARCHAR(50)
txn_date      VARCHAR(10)
txn_time      VARCHAR(10)
amount        NUMERIC(15,2)
status        VARCHAR(20)
response_code VARCHAR(10)
switch_ref    VARCHAR(50)
recon_date    DATE NOT NULL
created_at    TIMESTAMP DEFAULT now()

TABLE 3: recon_results
id             UUID PRIMARY KEY DEFAULT gen_random_uuid()
utr            VARCHAR(50) NOT NULL
recon_date     DATE NOT NULL
npci_amount    NUMERIC(15,2)
switch_amount  NUMERIC(15,2)
npci_status    VARCHAR(20)
switch_status  VARCHAR(20)
recon_status   VARCHAR(30) NOT NULL
remarks        TEXT
created_at     TIMESTAMP DEFAULT now()

Add index on utr and recon_date for all 3 tables.


════════════════════════════════════════════════════════════════
PROJECT STRUCTURE — MINIMAL

════════════════════════════════════════════════════════════════

src/main/java/com/bank/recon/
controller/
ReconController.java       ← 2 endpoints only
service/
ReconService.java          ← core logic
FileParserService.java     ← reads pipe-delimited files
FileWriterService.java     ← writes output file
repository/
NpciTransactionRepository.java
SwitchLogRepository.java
ReconResultRepository.java
model/
entity/
NpciTransaction.java
SwitchLog.java
ReconResult.java
dto/
NpciRecord.java          ← Record
SwitchRecord.java        ← Record
ReconResultRecord.java   ← Record
ReconSummary.java        ← Record (returned by GET endpoint)
config/
AppConfig.java             ← file path properties only
exception/
GlobalExceptionHandler.java

src/main/resources/
application.yml
schema.sql


════════════════════════════════════════════════════════════════
REST API — 2 ENDPOINTS ONLY

════════════════════════════════════════════════════════════════

ENDPOINT 1 — Run Recon
POST /api/recon/run?date=20240101

What it does:
1. Reads NPCI_TXN_20240101.txt from /data/input/npci/
2. Reads SWITCH_LOG_20240101.txt from /data/input/switch/
3. Saves all rows from both files to PostgreSQL
4. Runs comparison logic
5. Saves recon_results to PostgreSQL
6. Writes RECON_RESULT_20240101.txt to /data/output/
7. Returns summary JSON

Response (200 OK):
{
"date": "2024-01-01",
"totalNpciRows": 10,
"totalSwitchRows": 9,
"matched": 7,
"switchMissing": 1,
"npciMissing": 0,
"amountMismatch": 1,
"statusMismatch": 1,
"outputFile": "/data/output/RECON_RESULT_20240101.txt",
"status": "COMPLETED"
}

Error responses:
404: { "error": "NPCI file not found for date 20240101" }
404: { "error": "Switch log file not found for date 20240101" }
409: { "error": "Recon already run for date 20240101" }

ENDPOINT 2 — Get Results
GET /api/recon/results?date=20240101

What it does:
Returns all recon_results rows for the date as JSON array.

Response (200 OK):
{
"date": "2024-01-01",
"summary": {
"total": 10, "matched": 7, "exceptions": 3
},
"results": [
{
"utr": "UTR001",
"npciAmount": 500.00,
"switchAmount": 500.00,
"npciStatus": "SUCCESS",
"switchStatus": "SUCCESS",
"reconStatus": "MATCHED",
"remarks": "All fields match"
},
...
]
}


════════════════════════════════════════════════════════════════
application.yml

════════════════════════════════════════════════════════════════

spring:
threads:
virtual:
enabled: true
datasource:
url: jdbc:postgresql://localhost:5432/recondb
username: recon_user
password: recon_pass
driver-class-name: org.postgresql.Driver
jpa:
hibernate:
ddl-auto: validate
show-sql: true
sql:
init:
mode: always

recon:
file:
input:
npci-path: /data/input/npci
switch-path: /data/input/switch
output:
path: /data/output


════════════════════════════════════════════════════════════════
WHAT TO GENERATE — IN THIS ORDER

════════════════════════════════════════════════════════════════

1. pom.xml
Parent: spring-boot-starter-parent 4.0.6
Dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa,
postgresql, spring-boot-starter-actuator, lombok (optional)
Java version: 25

2. application.yml  (as above)

3. schema.sql  (3 tables + indexes)

4. AppConfig.java
@ConfigurationProperties("recon.file") to bind file paths

5. NpciRecord.java, SwitchRecord.java, ReconResultRecord.java
Java 25 Records

6. NpciTransaction.java, SwitchLog.java, ReconResult.java
JPA @Entity classes using jakarta.persistence.*

7. All 3 Repository interfaces (JpaRepository)

8. FileParserService.java
- parsNpciFile(Path filePath) → List<NpciRecord>
- parseSwitchFile(Path filePath) → List<SwitchRecord>
- Skips header row, handles missing/blank fields gracefully
- Throws FileNotFoundException with clear message if file missing

9. ReconService.java
- runRecon(LocalDate date) → ReconSummary
- Full comparison logic using sealed interface + pattern matching
- Saves npci rows, switch rows, recon results to DB
- Calls FileWriterService to write output file
- Throws exception if recon already run for date

10. FileWriterService.java
- writeReconResult(List<ReconResultRecord> results, Path outputPath)
- Writes pipe-delimited file with header row
- Creates output directory if not exists

11. ReconController.java
- POST /api/recon/run?date=
- GET  /api/recon/results?date=
- Clean @RestController, delegates fully to ReconService

12. GlobalExceptionHandler.java
- Handles FileNotFoundException → 404
- Handles ReconAlreadyRunException → 409
- Handles generic Exception → 500
- All return: { "error": "message", "date": "...", "timestamp": "..." }

13. docker-compose.yml
- PostgreSQL 15 only
- Mounts ./data/input and ./data/output as volumes
- Creates recondb, recon_user, recon_pass automatically

14. Sample test files
- /data/input/npci/NPCI_TXN_20240101.txt     (10 rows as shown above)
- /data/input/switch/SWITCH_LOG_20240101.txt  (9 rows — with 3 mismatches)

15. README.md with:
- How to start with docker-compose
- How to place input files
- curl commands for both endpoints
- Expected output file sample

Generate complete working code. No placeholders. No TODOs.


```

---

# SECTION 2 — What V1 Does  (One Simple Flow)


---

# SECTION 3 — The 2 Input Files  →  1 Output File


## 3.1  Input Files


## 3.2  Output File


---

# SECTION 4 — Comparison Logic  (5 Rules)


---

# SECTION 5 — Database  (3 Tables Only)


---

# SECTION 6 — REST API  (2 Endpoints Only)


## Error Responses


---

# SECTION 7 — Project Structure  (Minimal)

```
src/main/java/com/bank/recon/
controller/
ReconController.java          ← POST run + GET results
service/
ReconService.java             ← core comparison logic
FileParserService.java        ← reads pipe-delimited files
FileWriterService.java        ← writes output file
repository/
NpciTransactionRepository.java
SwitchLogRepository.java
ReconResultRepository.java
model/
entity/  NpciTransaction.java, SwitchLog.java, ReconResult.java
dto/     NpciRecord.java (Record), SwitchRecord.java (Record),
ReconResultRecord.java (Record), ReconSummary.java (Record)
config/
AppConfig.java                ← file path @ConfigurationProperties
exception/
GlobalExceptionHandler.java
ReconAlreadyRunException.java

src/main/resources/
application.yml
schema.sql

data/                             ← mounted as Docker volume
input/
npci/    NPCI_TXN_20240101.txt
switch/  SWITCH_LOG_20240101.txt
output/    RECON_RESULT_20240101.txt  ← generated here

docker-compose.yml                ← PostgreSQL 15 only
README.md


```

---

# SECTION 8 — What V2 Will Add  (Don't Build Yet)

Once V1 is working, V2 will layer these on top:

UPI Recon V1 — Simple File Comparison  |  Spring Boot 4.0.6 + Java 25

---

# Reference Tables

> **Note:**
> **V1 GOAL:** Prove the core recon logic works end-to-end.
>
> - Read 2 files: NPCI Transaction File + Switch Log File
> - Compare row by row using UTR as the key
> - Write 1 output file: Recon Result File (matched + exceptions)
>
> No Kafka. No auth. No email. No PDF. No scheduler.
> Just: **file in → compare → file out.** Get this working first.

| Step | Action | Detail |
| --- | --- | --- |
| 1 | POST /api/recon/run?date=20240101 | User triggers recon for a date |
| 2 | Read NPCI File | NPCI_TXN_20240101.txt from /data/input/npci/ |
| 3 | Read Switch File | SWITCH_LOG_20240101.txt from /data/input/switch/ |
| 4 | Save to DB | Both files loaded into PostgreSQL staging tables |
| 5 | Compare row by row | Using UTR as join key — apply 5 comparison rules |
| 6 | Write output file | RECON_RESULT_20240101.txt to /data/output/ |
| 7 | Save results to DB | recon_results table updated |
| 8 | GET /api/recon/results?date=... | Query results as JSON anytime |

| File | Location | Columns | Rows |
| --- | --- | --- | --- |
| NPCI_TXN_{date}.txt | /data/input/npci/ | UTR|RRN|TXN_DATE|TXN_TIME|AMOUNT|PAYER_VPA|PAYEE_VPA|STATUS | 10 |
| SWITCH_LOG_{date}.txt | /data/input/switch/ | UTR|RRN|TXN_DATE|TXN_TIME|AMOUNT|STATUS|RESPONSE_CODE|SWITCH_REF | 9 (1 missing) |

| File | Location | Columns |
| --- | --- | --- |
| RECON_RESULT_{date}.txt | /data/output/ | UTR|NPCI_AMOUNT|SWITCH_AMOUNT|NPCI_STATUS|SWITCH_STATUS|RECON_STATUS|REMARKS |

> **Note:**
> ► UTR is the only join key across both files. ► Files are pipe-delimited ( | ) plain text with a header row. ► If a file is missing, the API returns a clear 404 error message.

| Rule | Condition | Result | Example |
| --- | --- | --- | --- |
| 1 | UTR in NPCI — missing from Switch | SWITCH_MISSING | UTR008 in NPCI, not in Switch |
| 2 | UTR in Switch — missing from NPCI | NPCI_MISSING | Extra UTR in Switch only |
| 3 | UTR in both — amount differs | AMOUNT_MISMATCH | NPCI=800 Switch=999 |
| 4 | UTR in both — status differs | STATUS_MISMATCH | NPCI=SUCCESS Switch=FAILED |
| 5 | UTR in both — amount + status both match | MATCHED | UTR001 all good |

> **Note:**
> NOTE: Rules apply in order 1 → 5. First matching rule wins. NOTE: Use Java 25 sealed interface for ReconStatus — no string comparison. NOTE: Amount comparison must use BigDecimal.compareTo() — never == or .equals() on double.

| Table | Purpose | Key Columns |
| --- | --- | --- |
| npci_transactions | Stores raw NPCI file rows | id(UUID), utr, amount, status, recon_date |
| switch_logs | Stores raw Switch Log rows | id(UUID), utr, amount, status, response_code, recon_date |
| recon_results | Stores comparison output per UTR | id(UUID), utr, recon_status, remarks, recon_date |

| Method | Endpoint | Description | Response |
| --- | --- | --- | --- |
| POST | POST /api/recon/run?date=20240101 | Run recon for a date | JSON summary — totals by status |
| GET | GET  /api/recon/results?date=20240101 | Fetch all results for date | JSON array of all recon rows |

| HTTP | Error | When |
| --- | --- | --- |
| 404 | NPCI file not found for date YYYYMMDD | NPCI input file missing from folder |
| 404 | Switch log file not found for date YYYYMMDD | Switch input file missing from folder |
| 409 | Recon already run for date YYYYMMDD | POST called twice for same date |
| 500 | Internal error — check logs | Unexpected parsing or DB error |

| Feature | V1 | V2 |
| --- | --- | --- |
| Files compared | 2 (NPCI + Switch) | 4 (+ CBS + Settlement) |
| Messaging | None — direct call | Apache Kafka 4.0 pipeline |
| Scheduling | Manual API trigger | @Scheduled EOD cron |
| Output | 1 flat file | Excel + PDF + Archive + Email |
| Auth | None | JWT — Spring Security 7 |
| Exception management | File only | DB + resolve API |
| Kafka topics | None | 6 topics + DLQ |
| docker-compose services | PostgreSQL only | PostgreSQL + Kafka (KRaft) |

> **Note:**
> ► Build V1 first. Make POST /api/recon/run work end-to-end. ► Verify the output file is correct for all 5 exception types. ► Then use the V2 prompt document to upgrade to the full system.