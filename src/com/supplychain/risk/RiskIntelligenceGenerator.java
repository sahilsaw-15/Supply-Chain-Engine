package com.supplychain.risk;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainRecord;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RiskIntelligenceGenerator {

    private static final double HIGH_ABSOLUTE_FORECAST = 50.0;
    private static final int MINIMUM_COUNTRY_RISK_DEMAND = 50;

    public List<RiskIntelligenceResult> generate(
            List<ForecastResult> forecastResults,
            List<DemandRecord> demandRecords,
            List<SupplyChainRecord> supplyChainRecords) {

        Map<String, ForecastResult> latestForecasts = latestForecasts(forecastResults);
        Map<String, Double> volatilityByProductCategory = calculateVolatility(demandRecords);
        List<RiskIntelligenceResult> results = new ArrayList<>();

        for (ForecastResult forecast : latestForecasts.values()) {

            String key = productCategoryKey(forecast.getProductName(), forecast.getCategoryName());
            double volatility = volatilityByProductCategory.getOrDefault(key, 0.0);
            double profitRisk = profitRisk(forecast.getProfitMargin());
            double riskScore = (0.4 * forecast.getDelayRiskRate())
                    + (0.3 * volatility)
                    + (0.3 * profitRisk);
            String riskLevel = classifyRiskLevel(riskScore);
            String recommendation = recommendation(forecast, riskLevel);
            String stockoutRisk = stockoutRisk(forecast);

            results.add(new RiskIntelligenceResult(
                    forecast.getProductName(),
                    forecast.getCategoryName(),
                    forecast.getFinalPredictedDemand(),
                    forecast.getDelayRiskRate(),
                    forecast.getProfitMargin(),
                    volatility,
                    riskScore,
                    riskLevel,
                    recommendation,
                    stockoutRisk));
        }

        results.sort(Comparator
                .comparingDouble(RiskIntelligenceResult::getRiskScore)
                .reversed()
                .thenComparing(RiskIntelligenceResult::getProductName));

        return results;
    }

    public List<GroupRiskSummary> summarizeSupplierCarrierRisk(List<SupplyChainRecord> records) {

        Map<String, GroupRiskSummary> summaries = new LinkedHashMap<>();

        for (SupplyChainRecord record : records) {

            GroupRiskSummary summary = summaries.computeIfAbsent(
                    record.getShippingMode(),
                    value -> new GroupRiskSummary(value));
            summary.add(record.getOrderItemQuantity(), record.getSales(), record.getProfit(),
                    record.getLateDeliveryRisk(), record.getRealShippingDays());
        }

        return sortGroupSummaries(summaries);
    }

    public List<GroupRiskSummary> summarizeCountryRisk(List<SupplyChainRecord> records) {

        Map<String, GroupRiskSummary> summaries = new LinkedHashMap<>();

        for (SupplyChainRecord record : records) {

            GroupRiskSummary summary = summaries.computeIfAbsent(
                    record.getOrderCountry(),
                    value -> new GroupRiskSummary(value));
            summary.add(record.getOrderItemQuantity(), record.getSales(), record.getProfit(),
                    record.getLateDeliveryRisk(), record.getRealShippingDays());
        }

        return sortGroupSummaries(summaries, MINIMUM_COUNTRY_RISK_DEMAND);
    }

    public List<GroupRiskSummary> summarizeCategoryRisk(List<SupplyChainRecord> records) {

        Map<String, GroupRiskSummary> summaries = new LinkedHashMap<>();

        for (SupplyChainRecord record : records) {

            GroupRiskSummary summary = summaries.computeIfAbsent(
                    record.getCategoryName(),
                    value -> new GroupRiskSummary(value));
            summary.add(record.getOrderItemQuantity(), record.getSales(), record.getProfit(),
                    record.getLateDeliveryRisk(), record.getRealShippingDays());
        }

        return sortGroupSummaries(summaries);
    }

    private Map<String, ForecastResult> latestForecasts(List<ForecastResult> forecastResults) {

        Map<String, ForecastResult> latestByProductCategory = new LinkedHashMap<>();

        for (ForecastResult result : forecastResults) {

            String key = productCategoryKey(result.getProductName(), result.getCategoryName());
            ForecastResult current = latestByProductCategory.get(key);

            if (current == null || result.getForecastDate().isAfter(current.getForecastDate())) {

                latestByProductCategory.put(key, result);
            }
        }

        return latestByProductCategory;
    }

    private Map<String, Double> calculateVolatility(List<DemandRecord> demandRecords) {

        Map<String, List<Integer>> quantitiesByProductCategory = new LinkedHashMap<>();

        for (DemandRecord record : demandRecords) {

            String key = productCategoryKey(record.getProductName(), record.getCategoryName());
            quantitiesByProductCategory.computeIfAbsent(key, value -> new ArrayList<>())
                    .add(record.getTotalQuantity());
        }

        Map<String, Double> volatilityByProductCategory = new LinkedHashMap<>();

        for (Map.Entry<String, List<Integer>> entry : quantitiesByProductCategory.entrySet()) {

            volatilityByProductCategory.put(entry.getKey(), volatilityScore(entry.getValue()));
        }

        return volatilityByProductCategory;
    }

    private double volatilityScore(List<Integer> quantities) {

        if (quantities.isEmpty()) {

            return 0.0;
        }

        double average = quantities.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        if (average == 0) {

            return 0.0;
        }

        double variance = 0.0;

        for (int quantity : quantities) {

            double difference = quantity - average;
            variance += difference * difference;
        }

        double standardDeviation = Math.sqrt(variance / quantities.size());
        double coefficientOfVariation = standardDeviation / average;
        return Math.min(100.0, coefficientOfVariation * 100.0);
    }

    private double profitRisk(double profitMargin) {

        if (profitMargin < 0.0) {

            return 100.0;
        }

        if (profitMargin < 5.0) {

            return 75.0;
        }

        if (profitMargin < 10.0) {

            return 50.0;
        }

        if (profitMargin < 15.0) {

            return 25.0;
        }

        return 10.0;
    }

    private String classifyRiskLevel(double riskScore) {

        if (riskScore >= 75.0) {

            return "CRITICAL RISK";
        }

        if (riskScore >= 50.0) {

            return "HIGH RISK";
        }

        if (riskScore >= 25.0) {

            return "MEDIUM RISK";
        }

        return "LOW RISK";
    }

    private String recommendation(ForecastResult forecast, String riskLevel) {

        if ("CRITICAL RISK".equals(riskLevel)) {

            return "Increase safety stock; review supplier lead times immediately.";
        }

        if (forecast.getDelayRiskRate() >= 50.0 && isHighForecast(forecast)) {

            return "Increase safety stock; review supplier lead times.";
        }

        if (forecast.getProfitMargin() < 10.0 && isHighForecast(forecast)) {

            return "Review pricing strategy; negotiate supplier costs.";
        }

        if ("Declining Demand".equals(forecast.getDemandTrend())) {

            return "Reduce future inventory purchases.";
        }

        if ("HIGH RISK".equals(riskLevel)) {

            return "Monitor supply risk; rebalance inventory buffers.";
        }

        if ("MEDIUM RISK".equals(riskLevel)) {

            return "Maintain inventory controls and monitor delay risk.";
        }

        return "Maintain standard replenishment plan.";
    }

    private String stockoutRisk(ForecastResult forecast) {

        double currentInventoryProxy = forecast.getHistoricalAverageDemand() * 1.10;

        if (forecast.getFinalPredictedDemand() > currentInventoryProxy * 1.25
                && forecast.getDelayRiskRate() >= 50.0) {

            return "HIGH";
        }

        if (forecast.getFinalPredictedDemand() > currentInventoryProxy) {

            return "MEDIUM";
        }

        return "LOW";
    }

    private boolean isHighForecast(ForecastResult forecast) {

        double historicalAverage = forecast.getHistoricalAverageDemand();

        return historicalAverage == 0
                ? forecast.getFinalPredictedDemand() > 0
                : forecast.getFinalPredictedDemand() >= HIGH_ABSOLUTE_FORECAST
                        || forecast.getFinalPredictedDemand() >= historicalAverage * 1.20;
    }

    private List<GroupRiskSummary> sortGroupSummaries(Map<String, GroupRiskSummary> summaries) {

        return sortGroupSummaries(summaries, 0);
    }

    private List<GroupRiskSummary> sortGroupSummaries(Map<String, GroupRiskSummary> summaries, int minimumDemand) {

        return summaries.values().stream()
                .filter(summary -> summary.getDemand() >= minimumDemand)
                .sorted(Comparator
                        .comparingDouble(GroupRiskSummary::getRiskScore)
                        .reversed()
                        .thenComparing(GroupRiskSummary::getName))
                .collect(Collectors.toList());
    }

    private String productCategoryKey(String productName, String categoryName) {

        return productName + "|" + categoryName;
    }

    public static class GroupRiskSummary {

        private final String name;
        private int demand;
        private int orders;
        private int delayedOrders;
        private double sales;
        private double profit;
        private double leadTimeTotal;

        private GroupRiskSummary(String name) {

            this.name = name;
        }

        private void add(int demand, double sales, double profit, int lateDeliveryRisk, int leadTime) {

            this.demand += demand;
            this.sales += sales;
            this.profit += profit;
            this.leadTimeTotal += leadTime;
            orders++;

            if (lateDeliveryRisk == 1) {

                delayedOrders++;
            }
        }

        public String getName() {
            return name;
        }

        public int getDemand() {
            return demand;
        }

        public double getDelayRate() {

            if (orders == 0) {

                return 0.0;
            }

            return delayedOrders * 100.0 / orders;
        }

        public double getAverageLeadTime() {

            if (orders == 0) {

                return 0.0;
            }

            return leadTimeTotal / orders;
        }

        public double getProfitMargin() {

            if (sales == 0) {

                return 0.0;
            }

            return profit * 100.0 / sales;
        }

        public double getRiskScore() {

            double leadTimeRisk = Math.min(100.0, getAverageLeadTime() * 8.0);
            double profitRisk = getProfitMargin() < 10.0 ? 50.0 : 15.0;
            return (0.5 * getDelayRate()) + (0.3 * leadTimeRisk) + (0.2 * profitRisk);
        }

        public String getRiskLevel() {

            double score = getRiskScore();

            if (score >= 75.0) {

                return "CRITICAL RISK";
            }

            if (score >= 50.0) {

                return "HIGH RISK";
            }

            if (score >= 25.0) {

                return "MEDIUM RISK";
            }

            return "LOW RISK";
        }
    }
}
