package com.cloudops.optimizer.scan;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scans")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanJob triggerScan(@RequestBody(required = false) Map<String, String> body) {
        String region = body != null ? body.get("region") : null;
        return scanService.runScan(region);
    }

    @GetMapping
    public List<ScanJob> listScans() {
        return scanService.findAll();
    }

    @GetMapping("/{id}")
    public ScanJob getScan(@PathVariable Long id) {
        return scanService.findById(id);
    }

    @GetMapping("/latest")
    public ScanJob getLatestScan() {
        return scanService.findLatestSuccessfulScan();
    }
}
