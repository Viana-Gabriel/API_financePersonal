package com.example.financePersonal.reports.controller;

import com.example.financePersonal.reports.dto.ComparativeReportResponse;
import com.example.financePersonal.reports.dto.ReportMetric;
import com.example.financePersonal.reports.service.ComparativeReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ComparativeReportController {

    private final ComparativeReportService service;

    @GetMapping("/comparative")
    public ResponseEntity<ComparativeReportResponse> comparative(
            @RequestParam int yearA,
            @RequestParam int yearB,
            @RequestParam(defaultValue = "INCOME") ReportMetric metric
    ) {
        return ResponseEntity.ok(service.build(yearA, yearB, metric));
    }

}
