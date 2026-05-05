-- UPI Recon V1: three tables. gen_random_uuid() is built into PostgreSQL 13+ (no pgcrypto).

CREATE TABLE IF NOT EXISTS npci_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utr VARCHAR(50) NOT NULL,
    rrn VARCHAR(50),
    txn_date VARCHAR(10),
    txn_time VARCHAR(10),
    amount NUMERIC(15, 2),
    payer_vpa VARCHAR(100),
    payee_vpa VARCHAR(100),
    status VARCHAR(20),
    recon_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_npci_transactions_utr ON npci_transactions(utr);
CREATE INDEX IF NOT EXISTS idx_npci_transactions_recon_date ON npci_transactions(recon_date);

CREATE TABLE IF NOT EXISTS switch_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utr VARCHAR(50) NOT NULL,
    rrn VARCHAR(50),
    txn_date VARCHAR(10),
    txn_time VARCHAR(10),
    amount NUMERIC(15, 2),
    status VARCHAR(20),
    response_code VARCHAR(10),
    switch_ref VARCHAR(50),
    recon_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_switch_logs_utr ON switch_logs(utr);
CREATE INDEX IF NOT EXISTS idx_switch_logs_recon_date ON switch_logs(recon_date);

CREATE TABLE IF NOT EXISTS cbs_extracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utr VARCHAR(50) NOT NULL,
    account_no VARCHAR(50),
    txn_date VARCHAR(10),
    txn_time VARCHAR(10),
    amount NUMERIC(15, 2),
    dr_cr VARCHAR(5),
    description VARCHAR(500),
    cbs_ref VARCHAR(50),
    status VARCHAR(20),
    recon_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cbs_extracts_utr ON cbs_extracts(utr);
CREATE INDEX IF NOT EXISTS idx_cbs_extracts_recon_date ON cbs_extracts(recon_date);

CREATE TABLE IF NOT EXISTS recon_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utr VARCHAR(50) NOT NULL,
    recon_date DATE NOT NULL,
    npci_amount NUMERIC(15, 2),
    switch_amount NUMERIC(15, 2),
    npci_status VARCHAR(20),
    switch_status VARCHAR(20),
    recon_status VARCHAR(30) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_recon_results_utr ON recon_results(utr);
CREATE INDEX IF NOT EXISTS idx_recon_results_recon_date ON recon_results(recon_date);

ALTER TABLE recon_results ADD COLUMN IF NOT EXISTS cbs_amount NUMERIC(15, 2);
ALTER TABLE recon_results ADD COLUMN IF NOT EXISTS cbs_status VARCHAR(20);

CREATE TABLE IF NOT EXISTS settlement_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recon_date DATE NOT NULL UNIQUE,
    bank_code VARCHAR(20),
    total_txn INTEGER,
    npci_total_debit NUMERIC(15, 2),
    npci_total_credit NUMERIC(15, 2),
    npci_net_amount NUMERIC(15, 2),
    calculated_net NUMERIC(15, 2),
    difference NUMERIC(15, 2),
    rbi_ref VARCHAR(50),
    npci_status VARCHAR(20),
    settlement_status VARCHAR(30) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_settlement_records_recon_date ON settlement_records(recon_date);