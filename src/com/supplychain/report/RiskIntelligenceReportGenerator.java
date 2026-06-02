package com.supplychain.report;

import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainRecord;
import com.supplychain.risk.RiskIntelligenceGenerator;
import com.supplychain.risk.RiskIntelligenceGenerator.GroupRiskSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class RiskIntelligenceReportGenerator {

    private static final int LIMIT = 10;
    private static final int NAME_WIDTH = 40;
    private static final double HIGH_ABSOLUTE_FORECAST = 50.0;
    private static final double HIGH_DELAY_ALERT_MINIMUM_FORECAST = 20.0;

    public void printReport(
            List<RiskIntelligenceResult> riskResults,
            List<ForecastResult> forecastResults,
            List<SupplyChainRecord> supplyChainRecords) {

        System.out.println("*** SUPPLY CHAIN RISK INTELLIGENCE REPORT ***");

        if (riskResults.isEmpty()) {

            System.out.println("No risk intelligence results available.");
            return;
        }

        RiskIntelligenceGenerator generator = new RiskIntelligenceGenerator();

        printHighDemandHighDelayRisk(forecastResults);
        printHighDemandLowProfitRisk(forecastResults);
        printDecliningDemandRisk(forecastResults);
        printInventoryRiskClassification(riskResults);
        printSupplierCarrierRisk(generator.summarizeSupplierCarrierRisk(supplyChainRecords));
        printCountryRisk(generator.summarizeCountryRisk(supplyChainRecords));
        printCategoryRisk(generator.summarizeCategoryRisk(supplyChainRecords));
        printStockoutRisk(riskResults);
    }

    private void printHighDemandHighDelayRisk(List<ForecastResult> forecastResults) {

        System.out.println("\n1. High Demand + High Delay Risk");
        List<ForecastResult> rows = latestForecasts(forecastResults).stream()
                .filter(result -> isHighDemandDelayRisk(result) && result.getDelayRiskRate() >= 50.0)
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getDelayRiskRate)
                        .reversed()
                        .thenComparing(Comparator.comparingDouble(ForecastResult::getFinalPredictedDemand).reversed()))
                .limit(LIMIT)
                .collect(Collectors.toList());

        if (rows.isEmpty()) {

            System.out.println("No high demand products with high delay risk found.");
            return;
        }

        printForecastHeader("Risk Level");

        for (ForecastResult result : rows) {

            System.out.printf(Locale.US,
                    "%-" + NAME_WIDTH + "s  %,10.2f  %9.2f%%  %-12s%n",
                    fit(result.getProductName()),
                    result.getFinalPredictedDemand(),
                    result.getDelayRiskRate(),
                    "HIGH");
        }

        System.out.println("Recommendation: Increase safety stock. Review supplier lead times.");
    }

    private void printHighDemandLowProfitRisk(List<ForecastResult> forecastResults) {

        System.out.println("\n2. High Demand + Low Profit");
        List<ForecastResult> rows = latestForecasts(forecastResults).stream()
                .filter(result -> isHighForecast(result) && result.getProfitMargin() < 10.0)
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getFinalPredictedDemand)
                        .reversed())
                .limit(LIMIT)
                .collect(Collectors.toList());

        if (rows.isEmpty()) {

            System.out.println("No popular but unprofitable forecasted products found.");
            return;
        }

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %13s  %-30s%n",
                "Product",
                "Forecast",
                "Profit Margin",
                "Risk");
        System.out.println("-".repeat(98));

        for (ForecastResult result : rows) {

            System.out.printf(Locale.US,
                    "%-" + NAME_WIDTH + "s  %,10.2f  %12.2f%%  %-30s%n",
                    fit(result.getProductName()),
                    result.getFinalPredictedDemand(),
                    result.getProfitMargin(),
                    "Popular but Unprofitable Product");
        }

        System.out.println("Recommendation: Review pricing strategy. Negotiate supplier costs.");
    }

    private void printDecliningDemandRisk(List<ForecastResult> forecastResults) {

        System.out.println("\n3. Declining Demand Risk");
        List<ForecastResult> rows = latestForecasts(forecastResults).stream()
                .filter(result -> "Declining Demand".equals(result.getDemandTrend()))
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getFinalPredictedDemand)
                        .reversed())
                .limit(LIMIT)
                .collect(Collectors.toList());

        if (rows.isEmpty()) {

            System.out.println("No declining demand products found.");
            return;
        }

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %-18s%n",
                "Product",
                "Forecast",
                "Risk");
        System.out.println("-".repeat(74));

        for (ForecastResult result : rows) {

            System.out.printf(Locale.US,
                    "%-" + NAME_WIDTH + "s  %,10.2f  %-18s%n",
                    fit(result.getProductName()),
                    result.getFinalPredictedDemand(),
                    "Demand decreasing");
        }

        System.out.println("Recommendation: Reduce future inventory purchases.");
    }

    private void printInventoryRiskClassification(List<RiskIntelligenceResult> riskResults) {

        System.out.println("\n4. Inventory Risk Classification");
        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %10s  %10s  %10s  %-14s%n",
                "Product",
                "Forecast",
                "Delay",
                "Volatility",
                "Score",
                "Risk Level");
        System.out.println("-".repeat(102));

        riskResults.stream()
                .limit(LIMIT)
                .forEach(result -> System.out.printf(Locale.US,
                        "%-" + NAME_WIDTH + "s  %,10.2f  %9.2f%%  %9.2f%%  %10.2f  %-14s%n",
                        fit(result.getProductName()),
                        result.getForecastDemand(),
                        result.getDelayRisk(),
                        result.getVolatility(),
                        result.getRiskScore(),
                        result.getRiskLevel()));
    }

    private void printSupplierCarrierRisk(List<GroupRiskSummary> rows) {

        System.out.println("\n5. Supplier Risk Analysis");
        System.out.println("Supplier proxy: Shipping Mode, because the dataset does not expose a supplier ID.");
        printGroupRiskTable(rows, "Supplier/Carrier");
    }

    private void printCountryRisk(List<GroupRiskSummary> rows) {

        System.out.println("\n6. Country Risk Analysis");
        printGroupRiskTable(rows, "Country");
        System.out.println("Insight: High-demand countries with high delay rates are delivery-risk hot spots.");
    }

    private void printCategoryRisk(List<GroupRiskSummary> rows) {

        System.out.println("\n7. Category Risk Analysis");
        printGroupRiskTable(rows, "Category");
        System.out.println("Insight: Categories with high demand, weak margin, and high delay risk may indicate a supply bottleneck.");
    }

    private void printStockoutRisk(List<RiskIntelligenceResult> riskResults) {

        System.out.println("\n8. Stockout Risk Prediction");
        List<RiskIntelligenceResult> rows = riskResults.stream()
                .filter(result -> !"LOW".equals(result.getStockoutRisk()))
                .sorted(Comparator
                        .comparingInt(this::stockoutSortOrder)
                        .thenComparing(Comparator.comparingDouble(RiskIntelligenceResult::getForecastDemand).reversed()))
                .limit(LIMIT)
                .collect(Collectors.toList());

        if (rows.isEmpty()) {

            System.out.println("No elevated stockout risk found using the current inventory proxy.");
            return;
        }

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %-14s  %-28s%n",
                "Product",
                "Forecast",
                "Stockout Risk",
                "Output");
        System.out.println("-".repeat(92));

        for (RiskIntelligenceResult result : rows) {

            System.out.printf(Locale.US,
                    "%-" + NAME_WIDTH + "s  %,10.2f  %-14s  %-28s%n",
                    fit(result.getProductName()),
                    result.getForecastDemand(),
                    result.getStockoutRisk(),
                    "Inventory Shortage Expected");
        }
    }

    private void printGroupRiskTable(List<GroupRiskSummary> rows, String label) {

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %13s  %10s  %10s  %-14s%n",
                label,
                "Demand",
                "Avg Lead Time",
                "Delay",
                "Score",
                "Risk Level");
        System.out.println("-".repeat(104));

        rows.stream()
                .limit(LIMIT)
                .forEach(row -> System.out.printf(Locale.US,
                        "%-" + NAME_WIDTH + "s  %,10d  %12.2f  %9.2f%%  %10.2f  %-14s%n",
                        fit(row.getName()),
                        row.getDemand(),
                        row.getAverageLeadTime(),
                        row.getDelayRate(),
                        row.getRiskScore(),
                        row.getRiskLevel()));
    }

    private List<ForecastResult> latestForecasts(List<ForecastResult> forecastResults) {

        return forecastResults.stream()
                .collect(Collectors.toMap(
                        result -> result.getProductName() + "|" + result.getCategoryName(),
                        result -> result,
                        (left, right) -> right.getForecastDate().isAfter(left.getForecastDate()) ? right : left))
                .values()
                .stream()
                .collect(Collectors.toList());
    }

    private boolean isHighForecast(ForecastResult result) {

        return result.getFinalPredictedDemand() >= HIGH_ABSOLUTE_FORECAST;
    }

    private boolean isHighDemandDelayRisk(ForecastResult result) {

        return result.getFinalPredictedDemand() >= result.getHistoricalAverageDemand()
                && result.getFinalPredictedDemand() >= HIGH_DELAY_ALERT_MINIMUM_FORECAST;
    }

    private int stockoutSortOrder(RiskIntelligenceResult result) {

        if ("HIGH".equals(result.getStockoutRisk())) {

            return 0;
        }

        return 1;
    }

    private void printForecastHeader(String finalColumnName) {

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %10s  %10s  %-12s%n",
                "Product",
                "Forecast",
                "Delay",
                finalColumnName);
        System.out.println("-".repeat(78));
    }

    private String fit(String value) {

        if (value == null) {

            return "";
        }

        if (value.length() <= NAME_WIDTH) {

            return value;
        }

        return value.substring(0, NAME_WIDTH - 3) + "...";
    }
}
