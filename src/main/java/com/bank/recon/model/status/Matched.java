package com.bank.recon.model.status;

public record Matched() implements ReconStatus {
    @Override
    public String code() { return "MATCHED"; }

    @Override
    public String remarks() { return "All fields match"; }
}
