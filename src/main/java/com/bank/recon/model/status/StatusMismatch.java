package com.bank.recon.model.status;

public record StatusMismatch(String remarks) implements ReconStatus {
    @Override
    public String code() { return "STATUS_MISMATCH"; }
}
