package com.cloudops.optimizer.alert;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<Alert> listAlerts() {
        return alertService.findAll();
    }

    @PatchMapping("/{id}/read")
    public Alert markAsRead(@PathVariable Long id) {
        return alertService.markAsRead(id);
    }

    @PostMapping("/read-all")
    public void markAllAsRead() {
        alertService.markAllAsRead();
    }
}
