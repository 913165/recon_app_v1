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