package com.bank.recon.model.status;

public record NpciMissing() implements ReconStatus {
    @Override
    public String code() { return "NPCI_MISSING"; }

    @Override
    public String remarks() { return "UTR present in Switch Log but missing from NPCI"; }
}
