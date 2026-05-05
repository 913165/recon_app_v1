package com.bank.recon.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bank.recon.exception.ReconAlreadyRunException;
import com.bank.recon.model.dto.ReconResultRecord;
import com.bank.recon.model.dto.ReconSummary;
import com.bank.recon.service.ReconService;

@Controller
@RequestMapping("/ui")
public class ReconUiController {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final ReconService reconService;

    public ReconUiController(ReconService reconService) {
        this.reconService = reconService;
    }

    @GetMapping({"", "/", "/home"})
    public String home(Model model, @RequestParam(value = "date", required = false) String dateParam) {
        String date = (dateParam == null || dateParam.isBlank()) ? LocalDate.now().format(FILE_DATE) : dateParam.trim();
        model.addAttribute("date", date);
        return "recon/home";
    }

    @PostMapping("/run")
    public String run(@RequestParam("date") String dateRaw, RedirectAttributes redirectAttributes) {
        String date = dateRaw == null ? "" : dateRaw.trim();
        LocalDate parsed;
        try {
            parsed = LocalDate.parse(date, FILE_DATE);
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid date. Use yyyyMMdd (e.g. 20240101).");
            return "redirect:/ui";
        }
        try {
            ReconSummary outcome = reconService.runRecon(parsed);
            redirectAttributes.addFlashAttribute("successMessage", "Reconciliation completed for " + date + ".");
            if (outcome.reconciliationMillis() != null) {
                redirectAttributes.addFlashAttribute("runReconMillis", outcome.reconciliationMillis());
            }
            if (outcome.durationMillis() != null) {
                redirectAttributes.addFlashAttribute("runTotalMillis", outcome.durationMillis());
            }
        } catch (ReconAlreadyRunException e) {
            redirectAttributes.addFlashAttribute("infoMessage", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                e.getMessage() != null ? e.getMessage() : "I/O error while running reconciliation.");
            return "redirect:/ui?date=" + date;
        }
        return "redirect:/ui/results?date=" + date;
    }

    @GetMapping("/results")
    public String results(@RequestParam(value = "date", required = false) String dateRaw,
                          @RequestParam(value = "page", defaultValue = "1") int page,
                          @RequestParam(value = "size", defaultValue = "100") int size,
                          Model model) {
        if (dateRaw == null || dateRaw.isBlank()) {
            model.addAttribute("errorMessage", "Missing date parameter.");
            model.addAttribute("date", LocalDate.now().format(FILE_DATE));
            return "recon/home";
        }
        String date = dateRaw.trim();
        try {
            LocalDate.parse(date, FILE_DATE);
        } catch (DateTimeParseException e) {
            model.addAttribute("errorMessage", "Invalid date. Use yyyyMMdd.");
            model.addAttribute("date", date);
            return "recon/home";
        }
        LocalDate parsedDate = LocalDate.parse(date, FILE_DATE);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 10), 1000);

        ReconSummary summary = reconService.getResultsOverview(parsedDate);
        Page<ReconResultRecord> resultPage = reconService.getResultsPage(parsedDate, safePage - 1, safeSize);

        model.addAttribute("summary", summary);
        model.addAttribute("date", date);
        model.addAttribute("rows", resultPage.getContent());
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("totalPages", Math.max(resultPage.getTotalPages(), 1));
        model.addAttribute("totalElements", resultPage.getTotalElements());
        model.addAttribute("hasPrev", safePage > 1);
        model.addAttribute("hasNext", safePage < Math.max(resultPage.getTotalPages(), 1));
        model.addAttribute("prevPage", Math.max(safePage - 1, 1));
        model.addAttribute("nextPage", safePage + 1);
        return "recon/results";
    }
}
