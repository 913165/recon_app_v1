package com.bank.recon.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.recon.model.dto.ReconSummary;
import com.bank.recon.service.ReconService;

@RestController
@RequestMapping("/api/recon")
public class ReconController {

    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final ReconService reconService;

    public ReconController(ReconService reconService) {
        this.reconService = reconService;
    }

    @PostMapping("/run")
    public ReconSummary runRecon(@RequestParam("date") String date) throws IOException {
        return reconService.runRecon(LocalDate.parse(date, API_DATE_FORMAT));
    }

    @GetMapping("/results")
    public ReconSummary getResults(@RequestParam("date") String date) {
        return reconService.getResults(LocalDate.parse(date, API_DATE_FORMAT));
    }
}
