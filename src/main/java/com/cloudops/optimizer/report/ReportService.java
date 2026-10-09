package com.cloudops.optimizer.report;

import com.cloudops.optimizer.activity.ActivityLogService;
import com.cloudops.optimizer.common.CurrentUserService;
import com.cloudops.optimizer.common.ResourceNotFoundException;
import com.cloudops.optimizer.config.OptimizerProperties;
import com.cloudops.optimizer.recommendation.Recommendation;
import com.cloudops.optimizer.recommendation.RecommendationRepository;
import com.cloudops.optimizer.scan.ScanJob;
import com.cloudops.optimizer.scan.ScanService;
import com.cloudops.optimizer.snapshot.ResourceSnapshot;
import com.cloudops.optimizer.snapshot.ResourceSnapshotRepository;
import com.cloudops.optimizer.user.User;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final ReportRepository reportRepository;
    private final ScanService scanService;
    private final ResourceSnapshotRepository snapshotRepository;
    private final RecommendationRepository recommendationRepository;
    private final ActivityLogService activityLogService;
    private final CurrentUserService currentUserService;
    private final OptimizerProperties properties;

    public ReportService(
            ReportRepository reportRepository,
            ScanService scanService,
            ResourceSnapshotRepository snapshotRepository,
            RecommendationRepository recommendationRepository,
            ActivityLogService activityLogService,
            CurrentUserService currentUserService,
            OptimizerProperties properties) {
        this.reportRepository = reportRepository;
        this.scanService = scanService;
        this.snapshotRepository = snapshotRepository;
        this.recommendationRepository = recommendationRepository;
        this.activityLogService = activityLogService;
        this.currentUserService = currentUserService;
        this.properties = properties;
    }

    public List<Report> findAll() {
        return reportRepository.findAllByOrderByCreatedAtDesc();
    }

    public Report findById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found: " + id));
    }

    @Transactional
    public Report generateReport(ReportType type) {
        User user = currentUserService.getCurrentUserOrNull();
        ScanJob latest = scanService.findLatestSuccessfulScan();

        Instant end = Instant.now();
        Instant start = type == ReportType.WEEKLY
                ? end.minus(7, ChronoUnit.DAYS)
                : end.minus(30, ChronoUnit.DAYS);

        List<ResourceSnapshot> snapshots = latest != null
                ? snapshotRepository.findByScanJob(latest)
                : List.of();
        List<Recommendation> recommendations = latest != null
                ? recommendationRepository.findByScanJobOrderByEstimatedMonthlySavingsDesc(latest)
                : List.of();

        byte[] pdfBytes = buildPdf(type, latest, snapshots, recommendations, user);

        String outputDir = properties.getReports().getOutputDir();
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = String.format("cloudops_%s_%d.pdf", type.name().toLowerCase(), System.currentTimeMillis());
        Path filePath = Paths.get(outputDir, fileName);

        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(pdfBytes);
        } catch (IOException e) {
            log.error("Failed to write PDF report to disk: {}", e.getMessage(), e);
        }

        String summaryJson = String.format(
                "{\"healthScore\":%d,\"resources\":%d,\"findings\":%d,\"savings\":\"%s\"}",
                latest != null && latest.getHealthScore() != null ? latest.getHealthScore() : 100,
                snapshots.size(),
                recommendations.size(),
                latest != null && latest.getEstimatedMonthlySavings() != null ? latest.getEstimatedMonthlySavings() : "0.00"
        );

        Report report = new Report(user, type, start, end, filePath.toAbsolutePath().toString(), summaryJson);
        Report saved = reportRepository.save(report);

        activityLogService.record(
                user,
                "REPORT_EXPORTED",
                "REPORT",
                String.valueOf(saved.getId()),
                "Generated " + type.name() + " PDF report #" + saved.getId()
        );

        return saved;
    }

    public byte[] getReportPdfBytes(Long reportId) {
        Report report = findById(reportId);
        try {
            Path path = Paths.get(report.getFilePath());
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (Exception ex) {
            log.warn("Could not read PDF from disk: {}. Regenerating on the fly.", ex.getMessage());
        }

        ScanJob latest = scanService.findLatestSuccessfulScan();
        List<ResourceSnapshot> snapshots = latest != null ? snapshotRepository.findByScanJob(latest) : List.of();
        List<Recommendation> recommendations = latest != null ? recommendationRepository.findByScanJobOrderByEstimatedMonthlySavingsDesc(latest) : List.of();
        return buildPdf(report.getReportType(), latest, snapshots, recommendations, report.getGeneratedBy());
    }

    private byte[] buildPdf(
            ReportType type,
            ScanJob scan,
            List<ResourceSnapshot> snapshots,
            List<Recommendation> recommendations,
            User user) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Colors
            Color primaryColor = new Color(15, 23, 42);   // Slate 900
            Color accentColor = new Color(14, 165, 233);  // Sky 500
            Color greenColor = new Color(16, 185, 129);   // Emerald 500
            Color darkCardBg = new Color(241, 245, 249);  // Slate 100

            // Title Banner
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, primaryColor);
            Paragraph title = new Paragraph("CLOUDOPS OPTIMIZER", titleFont);
            title.setAlignment(Element.ALIGN_LEFT);
            document.add(title);

            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, accentColor);
            Paragraph subTitle = new Paragraph("Cloud Cost Optimization & Executive Resource Audit Report — " + type.name(), subTitleFont);
            document.add(subTitle);

            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Paragraph meta = new Paragraph(
                    "Generated on: " + DATE_FMT.format(Instant.now()) + " UTC | Generated by: " + (user != null ? user.getEmail() : "System") +
                            " | Target Region: " + (scan != null ? scan.getRegion() : properties.getAws().getRegion()),
                    metaFont
            );
            meta.setSpacingAfter(15);
            document.add(meta);

            // Executive Summary KPIs Table
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingBefore(10);
            kpiTable.setSpacingAfter(20);

            int healthScore = scan != null && scan.getHealthScore() != null ? scan.getHealthScore() : 100;
            String monthlySpend = scan != null && scan.getEstimatedMonthlyCost() != null ? "$" + scan.getEstimatedMonthlyCost() : "$0.00";
            String monthlySavings = scan != null && scan.getEstimatedMonthlySavings() != null ? "$" + scan.getEstimatedMonthlySavings() : "$0.00";

            addKpiCell(kpiTable, "Optimization Score", healthScore + "/100", healthScore >= 80 ? greenColor : new Color(245, 158, 11));
            addKpiCell(kpiTable, "Live AWS Resources", String.valueOf(snapshots.size()), primaryColor);
            addKpiCell(kpiTable, "Estimated Monthly Spend", monthlySpend, primaryColor);
            addKpiCell(kpiTable, "Potential Monthly Savings", monthlySavings, greenColor);

            document.add(kpiTable);

            // Section: Recommendations
            Font secHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, primaryColor);
            Paragraph recoHeader = new Paragraph("Prioritized Cost & Waste Recommendations", secHeaderFont);
            recoHeader.setSpacingBefore(10);
            recoHeader.setSpacingAfter(8);
            document.add(recoHeader);

            if (recommendations.isEmpty()) {
                Paragraph emptyMsg = new Paragraph("No open cost optimizations detected. Infrastructure is operating efficiently!",
                        FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.DARK_GRAY));
                emptyMsg.setSpacingAfter(15);
                document.add(emptyMsg);
            } else {
                PdfPTable recoTable = new PdfPTable(new float[]{1.8f, 1.2f, 4.0f, 1.5f, 1.5f});
                recoTable.setWidthPercentage(100);
                recoTable.setSpacingAfter(20);

                addTableHeader(recoTable, "Rule", "Severity", "Finding Description", "Est. Savings", "Status");

                for (Recommendation r : recommendations) {
                    addTableCell(recoTable, r.getRuleCode(), false);
                    addTableCell(recoTable, r.getSeverity().name(), false);
                    addTableCell(recoTable, r.getTitle() + "\n" + r.getDescription(), false);
                    addTableCell(recoTable, "$" + r.getEstimatedMonthlySavings() + "/mo", false);
                    addTableCell(recoTable, r.getStatus().name(), false);
                }
                document.add(recoTable);
            }

            // Section: Resource Inventory Breakdown
            Paragraph invHeader = new Paragraph("Discovered Cloud Resource Inventory", secHeaderFont);
            invHeader.setSpacingBefore(10);
            invHeader.setSpacingAfter(8);
            document.add(invHeader);

            PdfPTable invTable = new PdfPTable(new float[]{1.5f, 2.5f, 2.5f, 1.5f, 2.0f});
            invTable.setWidthPercentage(100);
            invTable.setSpacingAfter(20);

            addTableHeader(invTable, "Type", "Resource ID", "Name / Tag", "State", "Key Metric / Spec");

            for (ResourceSnapshot s : snapshots) {
                String keyMetric = "";
                if (s.getCpuAvg7d() != null) {
                    keyMetric = "CPU: " + s.getCpuAvg7d() + "%";
                } else if (s.attribute("sizeGb") != null) {
                    keyMetric = s.attribute("sizeGb") + " GB (" + s.attribute("volumeType") + ")";
                } else if (s.attribute("estimatedSizeGb") != null) {
                    keyMetric = s.attribute("estimatedSizeGb") + " GB";
                } else if (s.attribute("publicIp") != null) {
                    keyMetric = "IP: " + s.attribute("publicIp");
                }

                addTableCell(invTable, s.getResourceType().name(), false);
                addTableCell(invTable, s.getResourceId(), false);
                addTableCell(invTable, s.getResourceName() != null ? s.getResourceName() : "-", false);
                addTableCell(invTable, s.getState() != null ? s.getState() : "ACTIVE", false);
                addTableCell(invTable, keyMetric, false);
            }
            document.add(invTable);

            // Footer note
            Paragraph footer = new Paragraph(
                    "CloudCostOptimizer v1.0 — Confidential Cloud Governance Document — Automated FinOps Scan",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, Color.GRAY)
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception ex) {
            log.error("PDF generation failed: {}", ex.getMessage(), ex);
        }

        return baos.toByteArray();
    }

    private void addKpiCell(PdfPTable table, String label, String value, Color valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(248, 250, 252));
        cell.setPadding(10);
        cell.setBorderColor(new Color(226, 232, 240));

        Paragraph pLabel = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(100, 116, 139)));
        Paragraph pValue = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, valueColor));
        cell.addElement(pLabel);
        cell.addElement(pValue);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE)));
            cell.setBackgroundColor(new Color(30, 41, 59));
            cell.setPadding(6);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, boolean bold) {
        Font font = bold
                ? FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.DARK_GRAY)
                : FontFactory.getFont(FontFactory.HELVETICA, 8, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        cell.setBorderColor(new Color(226, 232, 240));
        table.addCell(cell);
    }
}
