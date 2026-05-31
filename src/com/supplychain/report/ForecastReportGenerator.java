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

    private static final int MINIMUM_ACTUAL_FOR_MAPE = 5;

    public void printReport(List<ForecastResult> forecastResults) {

        
        System.out.println("*** DEMAND FORECASTING REPORT ***");
        

        if (forecastResults.isEmpty()) {

            System.out.println("No forecast results available.");
            return;
        }

        List<ForecastResult> productResults = uniqueProductCategoryResults(forecastResults);
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

        printOverallMetrics(forecastResults);
        printHighRiskForecasts(productResults);
    }

    private List<ForecastResult> uniqueProductCategoryResults(List<ForecastResult> forecastResults) {

        Map<String, ForecastResult> firstResultByProductCategory = new LinkedHashMap<>();

        for (ForecastResult result : forecastResults) {

            String key = result.getProductName() + "|" + result.getCategoryName();
            firstResultByProductCategory.putIfAbsent(key, result);
        }

        return new ArrayList<>(firstResultByProductCategory.values());
    }

    private Map<String, Integer> countBestModels(List<ForecastResult> productResults) {

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (ForecastResult result : productResults) {

            counts.put(result.getBestModel(), counts.getOrDefault(result.getBestModel(), 0) + 1);
        }

        return counts;
    }

    private void printOverallMetrics(List<ForecastResult> forecastResults) {

        double absoluteErrorTotal = 0.0;
        double squaredErrorTotal = 0.0;
        double percentageErrorTotal = 0.0;
        int percentageCount = 0;
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
            }
        }

        double mae = absoluteErrorTotal / forecastResults.size();
        double rmse = Math.sqrt(squaredErrorTotal / forecastResults.size());
        double mape = percentageCount == 0 ? 0.0 : percentageErrorTotal / percentageCount;
        double r2Score = totalSquares == 0 ? 0.0 : 1.0 - (residualSquares / totalSquares);

        System.out.println("\nOverall Metrics:");
        System.out.printf(Locale.US, "MAE: %.2f%n", mae);
        System.out.printf(Locale.US, "RMSE: %.2f%n", rmse);
        System.out.printf(Locale.US, "MAPE: %.2f%%%n", mape);
        System.out.printf(Locale.US, "R2: %.4f%n", r2Score);

        if (r2Score < 0) {

            System.out.println("R2 Note: Negative R2 means the selected forecasts performed worse than");
            System.out.println("a simple average-demand baseline for some product demand patterns.");
        }
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

        if ("Increasing Demand".equals(result.getDemandTrend())) {

            return "Predicted Demand Spike";
        }

        if ("Declining Demand".equals(result.getDemandTrend())) {

            return "Declining Demand";
        }

        return "High Forecast Error";
    }
}
