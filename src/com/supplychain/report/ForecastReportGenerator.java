package com.supplychain.report;

import com.supplychain.model.ForecastResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ForecastReportGenerator {

    private static final int MINIMUM_ACTUAL_FOR_MAPE = 10;

    public void printReport(List<ForecastResult> forecastResults) {

        
        System.out.println("*** DEMAND FORECASTING REPORT ***");
        

        if (forecastResults.isEmpty()) {

            System.out.println("No forecast results available.");
            return;
        }

        List<ForecastResult> productResults = latestProductCategoryResults(forecastResults);
        Map<String, Integer> bestModelCounts = countBestModels(productResults);

        System.out.println("\nProducts Forecasted: " + productResults.size());
        System.out.println("Training Split: 80%");
        System.out.println("Testing Split: 20%");

        System.out.println("\nBest Model Summary:");
        System.out.println("Moving Average Best For: " + bestModelCounts.getOrDefault("Moving Average", 0) + " products");
        System.out.println("Weighted Moving Average Best For: "
                + bestModelCounts.getOrDefault("Weighted Moving Average", 0) + " products");
        System.out.println("Linear Regression Best For: "
                + bestModelCounts.getOrDefault("Linear Regression", 0) + " products");

        printOverallMetrics(forecastResults, productResults);
        printForecastRankings(productResults);
        printInventoryRecommendations(productResults);
        printCombinedRiskClassifications(productResults);
        printReorderPriorities(productResults);
        printHighRiskForecasts(productResults);
    }

    private List<ForecastResult> latestProductCategoryResults(List<ForecastResult> forecastResults) {

        Map<String, ForecastResult> latestResultByProductCategory = new LinkedHashMap<>();

        for (ForecastResult result : forecastResults) {

            String key = result.getProductName() + "|" + result.getCategoryName();
            ForecastResult current = latestResultByProductCategory.get(key);

            if (current == null || result.getForecastDate().isAfter(current.getForecastDate())) {

                latestResultByProductCategory.put(key, result);
            }
        }

        return new ArrayList<>(latestResultByProductCategory.values());
    }

    private Map<String, Integer> countBestModels(List<ForecastResult> productResults) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (ForecastResult result : productResults) {

            counts.put(result.getBestModel(), counts.getOrDefault(result.getBestModel(), 0) + 1);
        }

        return counts;
    }

    private void printForecastRankings(List<ForecastResult> productResults) {

        System.out.println("\nProduct Forecast Ranking");

        List<ForecastResult> topForecasts = productResults.stream()
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getFinalPredictedDemand)
                        .reversed()
                        .thenComparing(ForecastResult::getProductName))
                .limit(10)
                .collect(Collectors.toList());

        System.out.println("\nTop 10 Forecasted Products");
        printForecastRankingTable(topForecasts);

        List<ForecastResult> lowestForecasts = productResults.stream()
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getFinalPredictedDemand)
                        .thenComparing(ForecastResult::getProductName))
                .limit(10)
                .collect(Collectors.toList());

        System.out.println("\nLowest Forecasted Products");
        printForecastRankingTable(lowestForecasts);
    }

    private void printInventoryRecommendations(List<ForecastResult> productResults) {

        System.out.println("\nInventory Recommendation");
        System.out.printf(Locale.US,
                "%-36s  %10s  %10s  %-20s%n",
                "Product",
                "Forecast",
                "Hist Avg",
                "Recommendation");
        System.out.println("-".repeat(82));

        productResults.stream()
                .sorted(Comparator
                        .comparingInt(this::riskSortOrder)
                        .thenComparing(Comparator.comparingDouble(ForecastResult::getFinalPredictedDemand).reversed()))
                .limit(10)
                .forEach(result -> System.out.printf(Locale.US,
                        "%-36s  %,10.2f  %,10.2f  %-20s%n",
                        fit(result.getProductName(), 36),
                        result.getFinalPredictedDemand(),
                        result.getHistoricalAverageDemand(),
                        result.getInventoryRecommendation()));
    }

    private void printCombinedRiskClassifications(List<ForecastResult> productResults) {

        System.out.println("\nForecast Risk Classification");
        System.out.printf(Locale.US,
                "%-36s  %10s  %10s  %10s  %-12s%n",
                "Product",
                "Forecast",
                "Hist Avg",
                "Delay Risk",
                "Classification");
        System.out.println("-".repeat(88));

        productResults.stream()
                .sorted(Comparator
                        .comparingInt(this::riskSortOrder)
                        .thenComparing(Comparator.comparingDouble(ForecastResult::getFinalPredictedDemand).reversed()))
                .limit(10)
                .forEach(result -> System.out.printf(Locale.US,
                        "%-36s  %,10.2f  %,10.2f  %9.2f%%  %-12s%n",
                        fit(result.getProductName(), 36),
                        result.getFinalPredictedDemand(),
                        result.getHistoricalAverageDemand(),
                        result.getDelayRiskRate(),
                        result.getCombinedRiskClassification()));
    }

    private void printReorderPriorities(List<ForecastResult> productResults) {

        System.out.println("\nReorder Priority");
        System.out.printf(Locale.US,
                "%-36s  %10s  %10s  %12s  %-16s%n",
                "Product",
                "Forecast",
                "Delay Risk",
                "Profit Margin",
                "Priority");
        System.out.println("-".repeat(94));

        productResults.stream()
                .sorted(Comparator
                        .comparingInt(this::prioritySortOrder)
                        .thenComparing(Comparator.comparingDouble(ForecastResult::getFinalPredictedDemand).reversed())
                        .thenComparing(Comparator.comparingDouble(ForecastResult::getDelayRiskRate).reversed())
                        .thenComparing(Comparator.comparingDouble(ForecastResult::getProfitMargin).reversed()))
                .limit(10)
                .forEach(result -> System.out.printf(Locale.US,
                        "%-36s  %,10.2f  %9.2f%%  %11.2f%%  %-16s%n",
                        fit(result.getProductName(), 36),
                        result.getFinalPredictedDemand(),
                        result.getDelayRiskRate(),
                        result.getProfitMargin(),
                        result.getReorderPriority()));
    }

    private void printForecastRankingTable(List<ForecastResult> results) {

        System.out.printf(Locale.US,
                "%-36s  %10s  %-20s  %-12s%n",
                "Product",
                "Forecast",
                "Recommendation",
                "Risk");
        System.out.println("-".repeat(84));

        for (ForecastResult result : results) {

            System.out.printf(Locale.US,
                    "%-36s  %,10.2f  %-20s  %-12s%n",
                    fit(result.getProductName(), 36),
                    result.getFinalPredictedDemand(),
                    result.getInventoryRecommendation(),
                    result.getCombinedRiskClassification());
        }
    }

    private int riskSortOrder(ForecastResult result) {

        if ("HIGH RISK".equals(result.getCombinedRiskClassification())) {

            return 0;
        }

        if ("MEDIUM RISK".equals(result.getCombinedRiskClassification())) {

            return 1;
        }

        return 2;
    }

    private int prioritySortOrder(ForecastResult result) {

        if ("HIGH PRIORITY".equals(result.getReorderPriority())) {

            return 0;
        }

        if ("MEDIUM PRIORITY".equals(result.getReorderPriority())) {

            return 1;
        }

        return 2;
    }

    private void printOverallMetrics(List<ForecastResult> forecastResults, List<ForecastResult> productResults) {

        double absoluteErrorTotal = 0.0;
        double squaredErrorTotal = 0.0;
        double percentageErrorTotal = 0.0;
        int percentageCount = 0;
        int lowDemandMapeExcludedCount = 0;
        double actualTotal = 0.0;

        for (ForecastResult result : forecastResults) {

            actualTotal += result.getActualDemand();
        }

        double actualAverage = actualTotal / forecastResults.size();
        double residualSquares = 0.0;
        double totalSquares = 0.0;

        for (ForecastResult result : forecastResults) {

            double actual = result.getActualDemand();
            double predicted = result.getFinalPredictedDemand();
            double error = actual - predicted;

            absoluteErrorTotal += Math.abs(error);
            squaredErrorTotal += error * error;
            residualSquares += error * error;
            totalSquares += (actual - actualAverage) * (actual - actualAverage);

            if (actual >= MINIMUM_ACTUAL_FOR_MAPE) {

                percentageErrorTotal += Math.abs(error / actual) * 100.0;
                percentageCount++;
            } else {

                lowDemandMapeExcludedCount++;
            }
        }

        double mae = absoluteErrorTotal / forecastResults.size();
        double rmse = Math.sqrt(squaredErrorTotal / forecastResults.size());
        double mape = percentageCount == 0 ? 0.0 : percentageErrorTotal / percentageCount;
        double r2Score = totalSquares == 0 ? 0.0 : 1.0 - (residualSquares / totalSquares);

        System.out.println("\nOverall Metrics:");
        System.out.printf(Locale.US, "MAE: %.2f%n", mae);
        System.out.printf(Locale.US, "RMSE: %.2f%n", rmse);
        System.out.printf(Locale.US, "MAPE (actual demand >= %d only): %.2f%%%n", MINIMUM_ACTUAL_FOR_MAPE, mape);
        System.out.printf(Locale.US, "Low-demand rows excluded from MAPE: %,d%n", lowDemandMapeExcludedCount);
        System.out.printf(Locale.US, "Overall Row-Weighted R2: %.4f%n", r2Score);
        printProductR2Summary(productResults);

        if (r2Score < 0) {

            System.out.println("R2 Note: Negative R2 means the selected forecasts performed worse than");
            System.out.println("a simple average-demand baseline for some product demand patterns.");
        }
    }

    private void printProductR2Summary(List<ForecastResult> productResults) {

        List<Double> productR2Scores = productResults.stream()
                .map(ForecastResult::getR2Score)
                .sorted()
                .collect(Collectors.toList());

        if (productR2Scores.isEmpty()) {

            return;
        }

        double averageProductR2 = productR2Scores.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        double medianProductR2 = median(productR2Scores);
        long negativeProductR2Count = productR2Scores.stream()
                .filter(score -> score < 0.0)
                .count();

        System.out.printf(Locale.US, "Average Product-Level R2: %.4f%n", averageProductR2);
        System.out.printf(Locale.US, "Median Product-Level R2: %.4f%n", medianProductR2);
        System.out.printf(Locale.US,
                "Products With Negative Product-Level R2: %,d of %,d%n",
                negativeProductR2Count,
                productR2Scores.size());
        System.out.println("R2 Note: Overall Row-Weighted R2 is pooled across forecast rows.");
        System.out.println("Product-Level R2 is calculated per product and can be weak or negative for individual products.");
    }

    private double median(List<Double> values) {

        int midpoint = values.size() / 2;

        if (values.size() % 2 == 1) {

            return values.get(midpoint);
        }

        return (values.get(midpoint - 1) + values.get(midpoint)) / 2.0;
    }

    private void printHighRiskForecasts(List<ForecastResult> productResults) {

        List<ForecastResult> highRiskResults = productResults.stream()
                .filter(result -> "HIGH".equals(result.getForecastRiskLevel()))
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getFinalPredictedDemand)
                        .reversed())
                .limit(3)
                .collect(Collectors.toList());

        System.out.println("\nHigh Risk Forecasts:");

        if (highRiskResults.isEmpty()) {

            System.out.println("No high risk forecasts found.");
            return;
        }

        for (int i = 0; i < highRiskResults.size(); i++) {

            ForecastResult result = highRiskResults.get(i);
            System.out.printf(Locale.US,
                    "%d. %s -> %s, predicted %.2f units%n",
                    i + 1,
                    result.getProductName(),
                    riskReason(result),
                    result.getFinalPredictedDemand());
        }
    }

    private String riskReason(ForecastResult result) {

        if ("Volatile Demand".equals(result.getDemandTrend())) {

            return "Volatile Demand";
        }

        if ("Growing Demand".equals(result.getDemandTrend())) {

            return "Predicted Demand Spike";
        }

        if ("Declining Demand".equals(result.getDemandTrend())) {

            return "Declining Demand";
        }

        return "High Forecast Error";
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
}
