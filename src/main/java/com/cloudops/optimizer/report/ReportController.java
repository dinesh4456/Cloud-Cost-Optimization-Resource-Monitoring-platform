package com.cloudops.optimizer.report;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public List<Report> listReports() {
        return reportService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Report generateReport(@RequestBody(required = false) Map<String, String> body) {
        String typeStr = body != null && body.containsKey("type") ? body.get("type") : "OPTIMIZATION";
        ReportType type = ReportType.valueOf(typeStr.toUpperCase());
        return reportService.generateReport(type);
    }

    @GetMapping("/{id}")
    public Report getReport(@PathVariable Long id) {
        return reportService.findById(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id) {
        Report report = reportService.findById(id);
        byte[] pdfBytes = reportService.getReportPdfBytes(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "cloudops_report_" + report.getReportType().name().toLowerCase() + "_" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
