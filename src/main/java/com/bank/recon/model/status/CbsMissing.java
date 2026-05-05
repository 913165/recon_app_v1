package com.bank.recon.model.status;

public record CbsMissing() implements ReconStatus {
    @Override
    public String code() {
        return "CBS_MISSING";
    }

    @Override
    public String remarks() {
        return "UTR present in NPCI and Switch but missing from CBS extract";
    }
}
