package com.qualityloop.controller;

import com.qualityloop.dto.EmailRequest;
import com.qualityloop.dto.ReportSummary;
import com.qualityloop.dto.ReportView;
import com.qualityloop.dto.ShareResponse;
import com.qualityloop.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/api/reports/summary")
    public ReportSummary summary() { return service.summary(); }

    @PostMapping("/api/defects/{id}/share")
    public ShareResponse share(@PathVariable Long id) { return service.share(id); }

    @PostMapping("/api/defects/{id}/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void email(@PathVariable Long id, @Valid @RequestBody EmailRequest r) {
        service.emailReport(id, r.to(), r.message());
    }

    // Open to anyone who has the secret link
    @GetMapping("/api/public/reports/{token}")
    public ReportView publicReport(@PathVariable String token) { return service.publicReport(token); }
}
