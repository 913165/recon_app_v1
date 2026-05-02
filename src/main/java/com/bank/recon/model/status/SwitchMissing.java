package com.bank.recon.model.status;

public record SwitchMissing() implements ReconStatus {
    @Override
    public String code() { return "SWITCH_MISSING"; }

    @Override
    public String remarks() { return "UTR present in NPCI but missing from Switch Log"; }
}
