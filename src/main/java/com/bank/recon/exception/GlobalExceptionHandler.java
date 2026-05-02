package com.bank.recon.exception;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFileNotFound(FileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
    }

    @ExceptionHandler(ReconAlreadyRunException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyRun(ReconAlreadyRunException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error(ex.getMessage() == null ? "Internal error - check logs" : ex.getMessage()));
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        body.put("date", LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
