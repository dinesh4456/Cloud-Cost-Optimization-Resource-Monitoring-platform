package com.cloudops.optimizer.web;

import com.cloudops.optimizer.activity.ActivityLogRepository;
import com.cloudops.optimizer.alert.AlertService;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.dashboard.DashboardService;
import com.cloudops.optimizer.recommendation.RecommendationService;
import com.cloudops.optimizer.report.ReportService;
import com.cloudops.optimizer.scan.ScanService;
import com.cloudops.optimizer.snapshot.ResourceService;
import com.cloudops.optimizer.snapshot.ResourceType;
import com.cloudops.optimizer.user.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {

    private final CurrentUserService currentUserService;
    private final DashboardService dashboardService;
    private final ResourceService resourceService;
    private final ScanService scanService;
    private final RecommendationService recommendationService;
    private final AlertService alertService;
    private final ReportService reportService;
    private final ActivityLogRepository activityLogRepository;

    public WebController(
            CurrentUserService currentUserService,
            DashboardService dashboardService,
            ResourceService resourceService,
            ScanService scanService,
            RecommendationService recommendationService,
            AlertService alertService,
            ReportService reportService,
            ActivityLogRepository activityLogRepository) {
        this.currentUserService = currentUserService;
        this.dashboardService = dashboardService;
        this.resourceService = resourceService;
        this.scanService = scanService;
        this.recommendationService = recommendationService;
        this.alertService = alertService;
        this.reportService = reportService;
        this.activityLogRepository = activityLogRepository;
    }

    private void populateCommonAttributes(Model model, String activePage) {
        User user = currentUserService.getCurrentUserOrNull();
        model.addAttribute("currentUser", user);
        model.addAttribute("activePage", activePage);
        model.addAttribute("unreadAlertsCount", alertService.countUnread());
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        populateCommonAttributes(model, "dashboard");
        model.addAttribute("dashboard", dashboardService.getSummary());
        return "dashboard";
    }

    @GetMapping("/resources")
    public String resources(@RequestParam(required = false) ResourceType type, Model model) {
        populateCommonAttributes(model, "resources");
        model.addAttribute("resources", resourceService.findLatestResources(type));
        model.addAttribute("selectedType", type != null ? type.name() : "ALL");
        model.addAttribute("resourceTypes", ResourceType.values());
        return "resources";
    }

    @GetMapping("/scans")
    public String scans(Model model) {
        populateCommonAttributes(model, "scans");
        model.addAttribute("scans", scanService.findAll());
        return "scans";
    }

    @GetMapping("/recommendations")
    public String recommendations(Model model) {
        populateCommonAttributes(model, "recommendations");
        model.addAttribute("recommendations", recommendationService.findAll());
        return "recommendations";
    }

    @GetMapping("/alerts")
    public String alerts(Model model) {
        populateCommonAttributes(model, "alerts");
        model.addAttribute("alerts", alertService.findAll());
        return "alerts";
    }

    @GetMapping("/reports")
    public String reports(Model model) {
        populateCommonAttributes(model, "reports");
        model.addAttribute("reports", reportService.findAll());
        return "reports";
    }

    @GetMapping("/activity")
    public String activity(Model model) {
        populateCommonAttributes(model, "activity");
        model.addAttribute("logs", activityLogRepository.findAllByOrderByCreatedAtDesc());
        return "activity";
    }
}
