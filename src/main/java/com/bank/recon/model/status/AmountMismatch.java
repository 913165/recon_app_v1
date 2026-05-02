package com.bank.recon.model.status;

public record AmountMismatch(String remarks) implements ReconStatus {
    @Override
    public String code() { return "AMOUNT_MISMATCH"; }
}
