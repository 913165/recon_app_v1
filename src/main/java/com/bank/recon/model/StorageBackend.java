package com.bank.recon.model;

public enum StorageBackend {
    POSTGRES,
    REDIS;

    public static StorageBackend parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return POSTGRES;
        }
        try {
            return StorageBackend.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return POSTGRES;
        }
    }
}
