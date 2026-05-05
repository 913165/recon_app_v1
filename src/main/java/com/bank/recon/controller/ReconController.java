package com.bank.recon.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.bank.recon.model.StorageBackend;
import com.bank.recon.model.dto.ReconSummary;
import com.bank.recon.model.dto.SettlementResult;
import com.bank.recon.service.ReconService;
import com.bank.recon.service.SettlementReconService;

@RestController
@RequestMapping("/api/recon")
public class ReconController {

    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final ReconService reconService;
    private final SettlementReconService settlementReconService;

    public ReconController(ReconService reconService, SettlementReconService settlementReconService) {
        this.reconService = reconService;
        this.settlementReconService = settlementReconService;
    }

    @PostMapping("/run")
    public ReconSummary runRecon(@RequestParam("date") String date,
                                 @RequestParam(value = "db", defaultValue = "POSTGRES") String db) throws IOException {
        return reconService.runRecon(LocalDate.parse(date, API_DATE_FORMAT), StorageBackend.parse(db));
    }

    @GetMapping("/results")
    public ReconSummary getResults(@RequestParam("date") String date,
                                   @RequestParam(value = "db", defaultValue = "POSTGRES") String db) {
        StorageBackend backend = StorageBackend.parse(db);
        if (backend == StorageBackend.REDIS) {
            return reconService.getResultsOverview(LocalDate.parse(date, API_DATE_FORMAT), backend);
        }
        return reconService.getResults(LocalDate.parse(date, API_DATE_FORMAT));
    }

    @GetMapping("/settlement")
    public SettlementResult getSettlement(@RequestParam("date") String date) {
        try {
            return settlementReconService.getSettlement(LocalDate.parse(date, API_DATE_FORMAT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }
}
