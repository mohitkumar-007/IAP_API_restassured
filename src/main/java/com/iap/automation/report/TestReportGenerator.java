package com.iap.automation.report;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates an HTML execution report and console summary for the E2E test run.
 */
public class TestReportGenerator {

    private static final Logger logger = LogManager.getLogger(TestReportGenerator.class);

    private final String reportTitle;
    private final List<StepResult> steps = new ArrayList<>();
    private final Map<String, String> summaryData = new LinkedHashMap<>();
    private final LocalDateTime startTime = LocalDateTime.now();

    public TestReportGenerator(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public void addStep(String stepId, String description, String status, String details) {
        steps.add(new StepResult(stepId, description, status, details, LocalDateTime.now()));
    }

    public void addSummary(String key, String value) {
        summaryData.put(key, value);
    }

    public String generateHtmlReport() {
        LocalDateTime endTime = LocalDateTime.now();
        String timestamp = endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String sanitized = reportTitle.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String fileName = sanitized + "_" + timestamp + ".html";

        Path reportDir = Paths.get("reports");
        try {
            Files.createDirectories(reportDir);
        } catch (IOException e) {
            logger.error("Failed to create reports directory", e);
        }
        Path reportFile = reportDir.resolve(fileName);

        long passCount = steps.stream().filter(s -> "PASS".equals(s.status)).count();
        long failCount = steps.stream().filter(s -> "FAIL".equals(s.status)).count();
        long skipCount = steps.stream().filter(s -> "SKIP".equals(s.status)).count();
        long infoCount = steps.stream().filter(s -> "INFO".equals(s.status)).count();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(reportTitle).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Tahoma, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }\n");
        html.append("h1 { color: #2c3e50; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n");
        html.append("h2 { color: #34495e; margin-top: 30px; }\n");
        html.append(".summary-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 12px; margin: 15px 0; }\n");
        html.append(".summary-card { background: white; padding: 12px 16px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        html.append(".summary-card .label { font-size: 12px; color: #7f8c8d; text-transform: uppercase; }\n");
        html.append(".summary-card .value { font-size: 16px; color: #2c3e50; font-weight: 600; margin-top: 4px; word-break: break-all; }\n");
        html.append(".stats { display: flex; gap: 20px; margin: 20px 0; }\n");
        html.append(".stat-box { padding: 15px 25px; border-radius: 8px; color: white; font-size: 18px; font-weight: bold; min-width: 80px; text-align: center; }\n");
        html.append(".stat-box .count { font-size: 28px; display: block; }\n");
        html.append(".stat-pass { background: #27ae60; }\n");
        html.append(".stat-fail { background: #e74c3c; }\n");
        html.append(".stat-skip { background: #f39c12; }\n");
        html.append(".stat-info { background: #3498db; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 15px; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }\n");
        html.append("th { background: #2c3e50; color: white; padding: 12px 15px; text-align: left; font-weight: 600; }\n");
        html.append("td { padding: 10px 15px; border-bottom: 1px solid #ecf0f1; }\n");
        html.append("tr:hover { background: #f8f9fa; }\n");
        html.append(".status-pass { color: #27ae60; font-weight: bold; }\n");
        html.append(".status-fail { color: #e74c3c; font-weight: bold; }\n");
        html.append(".status-skip { color: #f39c12; font-weight: bold; }\n");
        html.append(".status-info { color: #3498db; font-weight: bold; }\n");
        html.append(".footer { margin-top: 30px; padding: 15px; color: #7f8c8d; font-size: 12px; text-align: center; border-top: 1px solid #ddd; }\n");
        html.append(".detail-table { width: 100%; border-collapse: collapse; margin: 6px 0 2px 0; font-size: 13px; }\n");
        html.append(".detail-table th { background: #34495e; color: white; padding: 6px 10px; text-align: left; font-size: 12px; }\n");
        html.append(".detail-table td { padding: 5px 10px; border-bottom: 1px solid #ddd; font-size: 13px; }\n");
        html.append(".detail-table tr:nth-child(even) { background: #f9f9f9; }\n");
        html.append(".detail-label { font-weight: 600; color: #2c3e50; }\n");
        html.append("</style>\n</head>\n<body>\n");

        // Header
        html.append("<h1>").append(reportTitle).append("</h1>\n");
        html.append("<p>Executed: ").append(startTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")));
        html.append(" — Completed: ").append(endTime.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss")));
        html.append("</p>\n");

        // Stats
        html.append("<div class=\"stats\">\n");
        html.append("<div class=\"stat-box stat-pass\"><span class=\"count\">").append(passCount).append("</span>PASS</div>\n");
        html.append("<div class=\"stat-box stat-fail\"><span class=\"count\">").append(failCount).append("</span>FAIL</div>\n");
        html.append("<div class=\"stat-box stat-skip\"><span class=\"count\">").append(skipCount).append("</span>SKIP</div>\n");
        html.append("<div class=\"stat-box stat-info\"><span class=\"count\">").append(infoCount).append("</span>INFO</div>\n");
        html.append("</div>\n");

        // Summary
        html.append("<h2>Execution Summary</h2>\n");
        html.append("<div class=\"summary-grid\">\n");
        for (Map.Entry<String, String> entry : summaryData.entrySet()) {
            html.append("<div class=\"summary-card\"><div class=\"label\">").append(entry.getKey());
            html.append("</div><div class=\"value\">").append(entry.getValue()).append("</div></div>\n");
        }
        html.append("</div>\n");

        // Steps table
        html.append("<h2>Test Steps Detail</h2>\n");
        html.append("<table>\n<thead><tr><th>#</th><th>Step ID</th><th>Description</th><th>Status</th><th>Details</th><th>Timestamp</th></tr></thead>\n<tbody>\n");

        for (int i = 0; i < steps.size(); i++) {
            StepResult s = steps.get(i);
            String cssClass = "status-" + s.status.toLowerCase();
            html.append("<tr>");
            html.append("<td>").append(i + 1).append("</td>");
            html.append("<td><strong>").append(s.stepId).append("</strong></td>");
            html.append("<td>").append(s.description).append("</td>");
            html.append("<td class=\"").append(cssClass).append("\">").append(s.status).append("</td>");
            html.append("<td>").append(s.details).append("</td>");
            html.append("<td>").append(s.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))).append("</td>");
            html.append("</tr>\n");
        }

        html.append("</tbody>\n</table>\n");

        // Footer
        html.append("<div class=\"footer\">IAP API Automation — ").append(reportTitle).append(" — Generated automatically</div>\n");
        html.append("</body>\n</html>");

        try {
            Files.writeString(reportFile, html.toString());
            logger.info("HTML report written to: {}", reportFile.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to write HTML report", e);
        }

        return reportFile.toAbsolutePath().toString();
    }

    public void printConsoleSummary() {
        long passCount = steps.stream().filter(s -> "PASS".equals(s.status)).count();
        long failCount = steps.stream().filter(s -> "FAIL".equals(s.status)).count();
        long skipCount = steps.stream().filter(s -> "SKIP".equals(s.status)).count();

        String upperTitle = reportTitle.toUpperCase();
        String headerLine = "  " + upperTitle + " — EXECUTION SUMMARY  ";
        int boxWidth = Math.max(58, headerLine.length() + 2);
        String border = "═".repeat(boxWidth);
        logger.info("╔{}╗", border);
        logger.info("║{}║", String.format("%-" + boxWidth + "s", headerLine));
        logger.info("╠{}╣", border);

        for (Map.Entry<String, String> entry : summaryData.entrySet()) {
            logger.info("║  {}: {}", String.format("%-20s", entry.getKey()), entry.getValue());
        }

        logger.info("╠{}╣", border);
        logger.info("║  PASS: {}  |  FAIL: {}  |  SKIP: {}  |  TOTAL: {}",
                passCount, failCount, skipCount, steps.size());
        logger.info("╠{}╣", border);

        for (StepResult s : steps) {
            String icon = switch (s.status) {
                case "PASS" -> "✅";
                case "FAIL" -> "❌";
                case "SKIP" -> "⏭️";
                case "INFO" -> "ℹ️";
                default -> "  ";
            };
            logger.info("║  {} [{}] {} — {}", icon, s.stepId, s.description, s.details);
        }

        logger.info("╚{}╝", border);
    }

    private record StepResult(String stepId, String description, String status, String details,
                               LocalDateTime timestamp) {
    }
}
