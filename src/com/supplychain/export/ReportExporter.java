package com.supplychain.export;

import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainAlert;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportExporter {

    public Path exportForecastReport(List<ForecastResult> forecasts, Path outputPath) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append("DEMAND FORECAST REPORT\n");
        builder.append("======================\n\n");

        ForecastMetrics metrics = metrics(forecasts);
        builder.append(String.format(Locale.US, "MAE  : %.2f%n", metrics.mae));
        builder.append(String.format(Locale.US, "RMSE : %.2f%n", metrics.rmse));
        builder.append(String.format(Locale.US, "MAPE : %.2f%%%n", metrics.mape));
        builder.append(String.format(Locale.US, "R2   : %.4f%n%n", metrics.r2));

        builder.append("Top 10 Forecasted Products\n");
        builder.append("--------------------------\n");
        latestForecasts(forecasts).stream()
                .sorted(Comparator.comparingDouble(ForecastResult::getFinalPredictedDemand).reversed())
                .limit(10)
                .forEach(result -> builder.append(String.format(Locale.US,
                        "%-45s %.2f%n",
                        result.getProductName(),
                        result.getFinalPredictedDemand())));

        return write(outputPath, builder.toString());
    }

    public Path exportRiskReport(List<RiskIntelligenceResult> risks, Path outputPath) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append("SUPPLY CHAIN RISK REPORT\n");
        builder.append("========================\n\n");

        Map<String, Long> counts = risks.stream()
                .collect(Collectors.groupingBy(RiskIntelligenceResult::getRiskLevel, Collectors.counting()));
        builder.append("Risk Overview\n");
        builder.append("-------------\n");
        counts.forEach((level, count) -> builder.append(level).append(": ").append(count).append('\n'));

        builder.append("\nHighest Risk Products\n");
        builder.append("---------------------\n");
        risks.stream()
                .sorted(Comparator.comparingDouble(RiskIntelligenceResult::getRiskScore).reversed())
                .limit(10)
                .forEach(result -> builder.append(String.format(Locale.US,
                        "%-45s Score %.2f | %s%n",
                        result.getProductName(),
                        result.getRiskScore(),
                        result.getRiskLevel())));

        return write(outputPath, builder.toString());
    }

    public Path exportAlertReport(List<SupplyChainAlert> alerts, Path outputPath) throws IOException {

        StringBuilder builder = new StringBuilder();
        builder.append("SUPPLY CHAIN ALERT REPORT\n");
        builder.append("=========================\n\n");

        long delayAlerts = alerts.stream()
                .filter(alert -> alert.getAlertType().contains("HIGH DEMAND DELAY"))
                .count();
        long profitAlerts = alerts.stream()
                .filter(alert -> alert.getAlertType().contains("UNPROFITABLE"))
                .count();

        builder.append("Alert Distribution\n");
        builder.append("------------------\n");
        builder.append("High Demand Delay Alerts: ").append(delayAlerts).append('\n');
        builder.append("Popular But Unprofitable: ").append(profitAlerts).append('\n');
        builder.append("Total Alerts: ").append(alerts.size()).append("\n\n");

        builder.append("Critical Alerts\n");
        builder.append("---------------\n");
        alerts.stream()
                .sorted(Comparator.comparing(SupplyChainAlert::getSeverity).reversed())
                .limit(20)
                .forEach(alert -> builder.append(String.format(Locale.US,
                        "%-45s %-32s Severity: %-6s Forecast: %.2f%n",
                        alert.getProductName(),
                        alert.getAlertType(),
                        alert.getSeverity(),
                        alert.getForecastDemand())));

        return write(outputPath, builder.toString());
    }

    private Path write(Path outputPath, String content) throws IOException {

        Path parent = outputPath.getParent();

        if (parent != null) {

            Files.createDirectories(parent);
        }

        Files.writeString(outputPath, content, StandardCharsets.UTF_8);
        return outputPath;
    }

    private List<ForecastResult> latestForecasts(List<ForecastResult> forecasts) {

        return forecasts.stream()
                .collect(Collectors.toMap(
                        result -> result.getProductName() + "|" + result.getCategoryName(),
                        result -> result,
                        (left, right) -> right.getForecastDate().isAfter(left.getForecastDate()) ? right : left))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private ForecastMetrics metrics(List<ForecastResult> forecasts) {

        ForecastMetrics metrics = new ForecastMetrics();

        if (forecasts.isEmpty()) {

            return metrics;
        }

        double absoluteErrorTotal = 0.0;
        double squaredErrorTotal = 0.0;
        double percentageErrorTotal = 0.0;
        int percentageCount = 0;
        double actualTotal = forecasts.stream().mapToDouble(ForecastResult::getActualDemand).sum();
        double actualAverage = actualTotal / forecasts.size();
        double residualSquares = 0.0;
        double totalSquares = 0.0;

        for (ForecastResult result : forecasts) {

            double actual = result.getActualDemand();
            double predicted = result.getFinalPredictedDemand();
            double error = actual - predicted;
            absoluteErrorTotal += Math.abs(error);
            squaredErrorTotal += error * error;
            residualSquares += error * error;
            totalSquares += (actual - actualAverage) * (actual - actualAverage);

            if (actual >= 10) {

                percentageErrorTotal += Math.abs(error / actual) * 100.0;
                percentageCount++;
            }
        }

        metrics.mae = absoluteErrorTotal / forecasts.size();
        metrics.rmse = Math.sqrt(squaredErrorTotal / forecasts.size());
        metrics.mape = percentageCount == 0 ? 0.0 : percentageErrorTotal / percentageCount;
        metrics.r2 = totalSquares == 0.0 ? 0.0 : 1.0 - (residualSquares / totalSquares);
        return metrics;
    }

    private static class ForecastMetrics {

        private double mae;
        private double rmse;
        private double mape;
        private double r2;
    }
}
