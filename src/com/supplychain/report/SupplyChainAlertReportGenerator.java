package com.supplychain.report;

import com.supplychain.model.SupplyChainAlert;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SupplyChainAlertReportGenerator {

    private static final int LIMIT = 10;
    private static final int NAME_WIDTH = 40;

    public void printReport(List<SupplyChainAlert> alerts) {

        System.out.println("*** SUPPLY CHAIN ALERT SYSTEM ***");

        if (alerts.isEmpty()) {

            System.out.println("No active supply-chain alerts.");
            return;
        }

        printAlertSummary(alerts);
        printCriticalAlerts(alerts);
        printAlertType(alerts, "HIGH DEMAND DELAY ALERT");
        printAlertType(alerts, "POPULAR BUT UNPROFITABLE ALERT");
    }

    private void printAlertSummary(List<SupplyChainAlert> alerts) {

        Map<String, Long> countsByType = alerts.stream()
                .collect(Collectors.groupingBy(SupplyChainAlert::getAlertType, Collectors.counting()));

        System.out.println("\nAlert Summary");
        System.out.println("Total Alerts: " + alerts.size());
        System.out.println("High Demand Delay Alerts: "
                + countsByType.getOrDefault("HIGH DEMAND DELAY ALERT", 0L));
        System.out.println("Popular but Unprofitable Alerts: "
                + countsByType.getOrDefault("POPULAR BUT UNPROFITABLE ALERT", 0L));
    }

    private void printCriticalAlerts(List<SupplyChainAlert> alerts) {

        List<CriticalAlertSummary> summaries = deduplicateAlerts(alerts);

        System.out.println("\nCritical Alerts");
        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %-32s  %10s  %10s  %13s  %-8s%n",
                "Product",
                "Alert Types",
                "Forecast",
                "Delay",
                "Profit Margin",
                "Severity");
        System.out.println("-".repeat(128));

        summaries.stream()
                .limit(LIMIT)
                .forEach(summary -> System.out.printf(Locale.US,
                        "%-" + NAME_WIDTH + "s  %-32s  %,10.2f  %9.2f%%  %12.2f%%  %-8s%n",
                        fit(summary.productName),
                        fit(summary.alertTypes(), 32),
                        summary.forecastDemand,
                        summary.delayRisk,
                        summary.profitMargin,
                        summary.severity));
    }

    private List<CriticalAlertSummary> deduplicateAlerts(List<SupplyChainAlert> alerts) {

        Map<String, CriticalAlertSummary> summaryByProduct = new LinkedHashMap<>();

        for (SupplyChainAlert alert : alerts) {

            String key = alert.getProductName() + "|" + alert.getCategoryName();
            CriticalAlertSummary summary = summaryByProduct.computeIfAbsent(
                    key,
                    value -> new CriticalAlertSummary(alert));
            summary.add(alert);
        }

        return new ArrayList<>(summaryByProduct.values());
    }

    private void printAlertType(List<SupplyChainAlert> alerts, String alertType) {

        List<SupplyChainAlert> matchingAlerts = alerts.stream()
                .filter(alert -> alertType.equals(alert.getAlertType()))
                .limit(LIMIT)
                .collect(Collectors.toList());

        System.out.println("\n" + alertType);

        if (matchingAlerts.isEmpty()) {

            System.out.println("No alerts found for this type.");
            return;
        }

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %10s  %13s  %-8s%n",
                "Product",
                "Forecast",
                "Delay",
                "Profit Margin",
                "Severity");
        System.out.println("-".repeat(92));

        for (SupplyChainAlert alert : matchingAlerts) {

            System.out.printf(Locale.US,
                    "%-" + NAME_WIDTH + "s  %,10.2f  %9.2f%%  %12.2f%%  %-8s%n",
                    fit(alert.getProductName()),
                    alert.getForecastDemand(),
                    alert.getDelayRisk(),
                    alert.getProfitMargin(),
                    alert.getSeverity());
        }

        SupplyChainAlert firstAlert = matchingAlerts.get(0);

        System.out.println("\nExample Alert:");
        System.out.println("Product: " + firstAlert.getProductName());
        System.out.printf(Locale.US, "Forecast Demand: %.2f%n", firstAlert.getForecastDemand());

        if ("HIGH DEMAND DELAY ALERT".equals(alertType)) {

            System.out.printf(Locale.US, "Delay Risk: %.2f%%%n", firstAlert.getDelayRisk());
        } else {

            System.out.printf(Locale.US, "Profit Margin: %.2f%%%n", firstAlert.getProfitMargin());
        }

        System.out.println("Severity: " + firstAlert.getSeverity());
        System.out.println("Recommendation: " + firstAlert.getRecommendation());
    }

    private String fit(String value) {

        return fit(value, NAME_WIDTH);
    }

    private String fit(String value, int width) {

        if (value == null) {

            return "";
        }

        if (value.length() <= width) {

            return value;
        }

        return value.substring(0, width - 3) + "...";
    }

    private static class CriticalAlertSummary {

        private final String productName;
        private final String categoryName;
        private final double forecastDemand;
        private final double delayRisk;
        private final double profitMargin;
        private final List<String> alertTypes = new ArrayList<>();
        private String severity;

        private CriticalAlertSummary(SupplyChainAlert alert) {

            this.productName = alert.getProductName();
            this.categoryName = alert.getCategoryName();
            this.forecastDemand = alert.getForecastDemand();
            this.delayRisk = alert.getDelayRisk();
            this.profitMargin = alert.getProfitMargin();
            this.severity = alert.getSeverity();
        }

        private void add(SupplyChainAlert alert) {

            if (!alertTypes.contains(alert.getAlertType())) {

                alertTypes.add(alert.getAlertType());
            }

            if ("HIGH".equals(alert.getSeverity())) {

                severity = "HIGH";
            }
        }

        private String alertTypes() {

            return String.join(" + ", alertTypes);
        }
    }
}
