package com.bank.recon.model.status;

public sealed interface ReconStatus permits Matched, SwitchMissing, NpciMissing, AmountMismatch, StatusMismatch {
    String code();
    String remarks();
}
