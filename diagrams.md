# Reconciliation Diagrams

This document provides visual diagrams for the end-to-end reconciliation process.

## End-to-End Flow

```mermaid
flowchart TD
    A["User or Scheduler triggers run: POST /api/recon/run with date"] --> B["ReconController"]
    B --> C["ReconService runRecon"]
    C --> D{"Results already exist for date?"}
    D -- Yes --> E["Throw ReconAlreadyRunException (HTTP 409)"]
    D -- No --> F["Resolve file paths from AppConfig"]
    F --> G[Read NPCI input file]
    F --> H[Read Switch input file]
    G --> I[FileParserService.parseNpciFile]
    H --> J[FileParserService.parseSwitchFile]
    I --> K[Persist NPCI staging rows]
    J --> L[Persist Switch staging rows]
    K --> M[Build UTR maps + union keys]
    L --> M
    M --> N["Resolve status per UTR: MATCHED / MISMATCH / MISSING"]
    N --> O[Persist recon_result rows]
    O --> P[Write RECON_RESULT output file]
    P --> Q[Return ReconSummary response]
```

## Sequence Diagram (Run + Read Results)

```mermaid
sequenceDiagram
    actor U as User
    participant RC as ReconController
    participant RS as ReconService
    participant FP as FileParserService
    participant DB as Repositories
    participant FW as FileWriterService
    participant FS as File System

    U->>RC: POST /api/recon/run with date
    RC->>RS: runRecon(date)
    RS->>DB: existsByReconDate(date)
    alt Already run
        RS-->>RC: ReconAlreadyRunException
        RC-->>U: 409 Conflict
    else First run for date
        RS->>FS: Resolve input/output paths (AppConfig)
        RS->>FP: parseNpciFile(npciFile)
        FP->>FS: Read NPCI lines
        FP-->>RS: List of NpciRecord
        RS->>FP: parseSwitchFile(switchFile)
        FP->>FS: Read Switch lines
        FP-->>RS: List of SwitchRecord
        RS->>DB: saveAll NPCI + Switch staging entities
        RS->>RS: Compare by UTR and resolve statuses
        RS->>DB: saveAll recon_result rows
        RS->>FW: writeReconResult(results, outputFile)
        FW->>FS: Write pipe-delimited output file
        RS-->>RC: ReconSummary
        RC-->>U: 200 OK + summary JSON
    end

    U->>RC: GET /api/recon/results with date
    RC->>RS: getResults(date)
    RS->>DB: findByReconDate(date)
    RS-->>RC: ReconSummary (from DB results)
    RC-->>U: 200 OK + summary JSON
```

