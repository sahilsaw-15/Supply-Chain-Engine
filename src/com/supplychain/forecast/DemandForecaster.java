package com.supplychain.forecast;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;
import com.supplychain.model.SupplyChainRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DemandForecaster {

    private static final int MOVING_AVERAGE_WINDOW = 7;
    private static final double TRAINING_RATIO = 0.80;
    private static final int MINIMUM_RECORDS_TO_FORECAST = 5;
    private static final int MINIMUM_ACTUAL_FOR_MAPE = 10;

    public List<ForecastResult> forecast(List<DemandRecord> demandRecords) {

        return forecast(demandRecords, Collections.emptyList());
    }

    public List<ForecastResult> forecast(List<DemandRecord> demandRecords, List<SupplyChainRecord> supplyChainRecords) {

        Map<String, List<DemandRecord>> recordsByProductCategory = groupByProductAndCategory(demandRecords);
        Map<String, ProductDecisionSummary> decisionSummaryByProductCategory =
                summarizeProductDecisionData(supplyChainRecords);
        List<ForecastResult> results = new ArrayList<>();

        for (List<DemandRecord> records : recordsByProductCategory.values()) {

            records.sort(Comparator.comparing(DemandRecord::getDemandDate));

            if (records.size() < MINIMUM_RECORDS_TO_FORECAST) {

                continue;
            }

            String key = productCategoryKey(records.get(0).getProductName(), records.get(0).getCategoryName());
            ProductDecisionSummary decisionSummary = decisionSummaryByProductCategory
                    .getOrDefault(key, new ProductDecisionSummary());

            results.addAll(forecastProductCategory(records, decisionSummary));
        }

        results.sort(Comparator
                .comparing(ForecastResult::getProductName)
                .thenComparing(ForecastResult::getCategoryName)
                .thenComparing(ForecastResult::getForecastDate));

        return results;
    }

    private Map<String, List<DemandRecord>> groupByProductAndCategory(List<DemandRecord> demandRecords) {

        Map<String, List<DemandRecord>> recordsByProductCategory = new LinkedHashMap<>();

        for (DemandRecord record : demandRecords) {

            String key = productCategoryKey(record.getProductName(), record.getCategoryName());
            recordsByProductCategory.computeIfAbsent(key, value -> new ArrayList<>()).add(record);
        }

        return recordsByProductCategory;
    }

    private Map<String, ProductDecisionSummary> summarizeProductDecisionData(List<SupplyChainRecord> records) {

        Map<String, ProductDecisionSummary> decisionSummaryByProductCategory = new LinkedHashMap<>();

        for (SupplyChainRecord record : records) {

            String key = productCategoryKey(record.getProductName(), record.getCategoryName());
            ProductDecisionSummary summary =
                    decisionSummaryByProductCategory.computeIfAbsent(key, value -> new ProductDecisionSummary());
            summary.add(record);
        }

        return decisionSummaryByProductCategory;
    }

    private String productCategoryKey(String productName, String categoryName) {

        return productName + "|" + categoryName;
    }

    private List<ForecastResult> forecastProductCategory(
            List<DemandRecord> records,
            ProductDecisionSummary decisionSummary) {

        int splitIndex = (int) Math.floor(records.size() * TRAINING_RATIO);

        if (splitIndex < 2 || splitIndex >= records.size()) {

            return Collections.emptyList();
        }

        List<Integer> history = new ArrayList<>();

        for (int i = 0; i < splitIndex; i++) {

            history.add(records.get(i).getTotalQuantity());
        }

        List<PredictionRow> predictionRows = new ArrayList<>();

        for (int i = splitIndex; i < records.size(); i++) {

            DemandRecord actual = records.get(i);
            double movingAveragePrediction = movingAverage(history);
            double weightedMovingAveragePrediction = weightedMovingAverage(history);
            double linearRegressionPrediction = linearRegression(history);

            predictionRows.add(new PredictionRow(
                    actual,
                    movingAveragePrediction,
                    weightedMovingAveragePrediction,
                    linearRegressionPrediction
            ));

            history.add(actual.getTotalQuantity());
        }

        ModelMetrics movingAverageMetrics = evaluate(predictionRows, ModelType.MOVING_AVERAGE);
        ModelMetrics weightedMovingAverageMetrics = evaluate(predictionRows, ModelType.WEIGHTED_MOVING_AVERAGE);
        ModelMetrics linearRegressionMetrics = evaluate(predictionRows, ModelType.LINEAR_REGRESSION);
        ModelMetrics bestMetrics = bestMetrics(movingAverageMetrics, weightedMovingAverageMetrics, linearRegressionMetrics);
        double historicalAverageDemand = averageDemand(records);
        double delayRiskRate = decisionSummary.getDelayRiskRate();
        double profitMargin = decisionSummary.getProfitMargin();

        List<ForecastResult> results = new ArrayList<>();

        for (PredictionRow predictionRow : predictionRows) {

            double finalPredictedDemand = predictionRow.predictionFor(bestMetrics.getModelType());
            String demandTrend = classifyForecastDemandTrend(finalPredictedDemand, historicalAverageDemand);
            String forecastRiskLevel = classifyForecastRisk(bestMetrics, demandTrend, records);
            String inventoryRecommendation = recommendInventory(finalPredictedDemand, historicalAverageDemand);
            String combinedRiskClassification = classifyCombinedRisk(
                    finalPredictedDemand,
                    historicalAverageDemand,
                    delayRiskRate,
                    forecastRiskLevel);
            String reorderPriority = classifyReorderPriority(
                    finalPredictedDemand,
                    historicalAverageDemand,
                    delayRiskRate,
                    profitMargin);
            String accuracyNote = accuracyNote(predictionRow.getActualDemand(), bestMetrics);

            results.add(new ForecastResult(
                    predictionRow.getProductName(),
                    predictionRow.getCategoryName(),
                    predictionRow.getForecastDate(),
                    predictionRow.getActualDemand(),
                    predictionRow.getMovingAveragePrediction(),
                    predictionRow.getWeightedMovingAveragePrediction(),
                    predictionRow.getLinearRegressionPrediction(),
                    bestMetrics.getModelName(),
                    finalPredictedDemand,
                    bestMetrics.getMae(),
                    bestMetrics.getRmse(),
                    bestMetrics.getMape(),
                    bestMetrics.getR2Score(),
                    demandTrend,
                    forecastRiskLevel,
                    historicalAverageDemand,
                    inventoryRecommendation,
                    delayRiskRate,
                    profitMargin,
                    combinedRiskClassification,
                    reorderPriority,
                    accuracyNote
            ));
        }

        return results;
    }

    private double averageDemand(List<DemandRecord> records) {

        return records.stream()
                .mapToInt(DemandRecord::getTotalQuantity)
                .average()
                .orElse(0.0);
    }

    private String recommendInventory(double forecast, double historicalAverage) {

        if (historicalAverage == 0) {

            return forecast > 0 ? "Increase Inventory" : "Maintain Inventory";
        }

        double differenceRatio = (forecast - historicalAverage) / historicalAverage;

        if (differenceRatio > 0.05) {

            return "Increase Inventory";
        }

        if (differenceRatio < -0.05) {

            return "Reduce Inventory";
        }

        return "Maintain Inventory";
    }

    private String classifyForecastDemandTrend(double forecast, double historicalAverage) {

        if (forecast > historicalAverage) {

            return "Growing Demand";
        }

        return "Declining Demand";
    }

    private String classifyCombinedRisk(
            double forecast,
            double historicalAverage,
            double delayRiskRate,
            String forecastRiskLevel) {

        boolean highForecast = historicalAverage == 0
                ? forecast > 0
                : forecast >= historicalAverage * 1.20;
        boolean mediumForecast = historicalAverage > 0 && forecast >= historicalAverage;
        boolean highDelayRisk = delayRiskRate >= 50.0;
        boolean mediumDelayRisk = delayRiskRate >= 25.0;

        if ((highForecast && highDelayRisk)
                || ("HIGH".equals(forecastRiskLevel) && mediumDelayRisk)) {

            return "HIGH RISK";
        }

        if ((highForecast && mediumDelayRisk)
                || (mediumForecast && highDelayRisk)
                || "MEDIUM".equals(forecastRiskLevel)) {

            return "MEDIUM RISK";
        }

        return "LOW RISK";
    }

    private String classifyReorderPriority(
            double forecast,
            double historicalAverage,
            double delayRiskRate,
            double profitMargin) {

        if (forecast < 10.0) {

            return "LOW PRIORITY";
        }

        int score = 0;

        if (isHighForecast(forecast, historicalAverage)) {

            score += 2;
        } else if (isMediumForecast(forecast, historicalAverage)) {

            score += 1;
        }

        if (delayRiskRate >= 50.0) {

            score += 2;
        } else if (delayRiskRate >= 25.0) {

            score += 1;
        }

        if (profitMargin >= 12.0) {

            score += 2;
        } else if (profitMargin >= 5.0) {

            score += 1;
        } else if (profitMargin < 0.0) {

            score -= 1;
        }

        if (score >= 5) {

            return "HIGH PRIORITY";
        }

        if (score >= 3) {

            return "MEDIUM PRIORITY";
        }

        return "LOW PRIORITY";
    }

    private boolean isHighForecast(double forecast, double historicalAverage) {

        return historicalAverage == 0
                ? forecast > 0
                : forecast >= historicalAverage * 1.20;
    }

    private boolean isMediumForecast(double forecast, double historicalAverage) {

        return historicalAverage > 0 && forecast >= historicalAverage;
    }

    private String accuracyNote(int actualDemand, ModelMetrics bestMetrics) {

        if (actualDemand < MINIMUM_ACTUAL_FOR_MAPE) {

            return "Low-demand product: MAPE may be unstable";
        }

        if (!bestMetrics.isMapeReliable()) {

            return "MAPE excluded from low-demand test rows";
        }

        return "";
    }

    private double movingAverage(List<Integer> history) {

        int startIndex = Math.max(0, history.size() - MOVING_AVERAGE_WINDOW);
        double total = 0.0;

        for (int i = startIndex; i < history.size(); i++) {

            total += history.get(i);
        }

        return total / (history.size() - startIndex);
    }

    private double weightedMovingAverage(List<Integer> history) {

        int startIndex = Math.max(0, history.size() - MOVING_AVERAGE_WINDOW);
        double weightedTotal = 0.0;
        int weightTotal = 0;
        int weight = 1;

        for (int i = startIndex; i < history.size(); i++) {

            weightedTotal += history.get(i) * weight;
            weightTotal += weight;
            weight++;
        }

        return weightedTotal / weightTotal;
    }

    private double linearRegression(List<Integer> history) {

        int size = history.size();

        if (size == 1) {

            return history.get(0);
        }

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXSquare = 0.0;

        for (int i = 0; i < size; i++) {

            double x = i;
            double y = history.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXSquare += x * x;
        }

        double denominator = (size * sumXSquare) - (sumX * sumX);

        if (denominator == 0) {

            return sumY / size;
        }

        double slope = ((size * sumXY) - (sumX * sumY)) / denominator;
        double intercept = (sumY - (slope * sumX)) / size;

        return Math.max(0.0, intercept + (slope * size));
    }

    private ModelMetrics evaluate(List<PredictionRow> predictionRows, ModelType modelType) {

        double absoluteErrorTotal = 0.0;
        double squaredErrorTotal = 0.0;
        double percentageErrorTotal = 0.0;
        int percentageCount = 0;
        double actualTotal = 0.0;

        for (PredictionRow row : predictionRows) {

            actualTotal += row.getActualDemand();
        }

        double actualAverage = actualTotal / predictionRows.size();
        double residualSquares = 0.0;
        double totalSquares = 0.0;

        for (PredictionRow row : predictionRows) {

            double actual = row.getActualDemand();
            double predicted = row.predictionFor(modelType);
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

        double mae = absoluteErrorTotal / predictionRows.size();
        double rmse = Math.sqrt(squaredErrorTotal / predictionRows.size());
        double mape = percentageCount == 0 ? 0.0 : percentageErrorTotal / percentageCount;
        double r2Score = totalSquares == 0 ? 0.0 : 1.0 - (residualSquares / totalSquares);

        return new ModelMetrics(modelType, mae, rmse, mape, r2Score, percentageCount);
    }

    private ModelMetrics bestMetrics(
            ModelMetrics movingAverageMetrics,
            ModelMetrics weightedMovingAverageMetrics,
            ModelMetrics linearRegressionMetrics) {

        List<ModelMetrics> metrics = Arrays.asList(
                movingAverageMetrics,
                weightedMovingAverageMetrics,
                linearRegressionMetrics
        );

        return metrics.stream()
                .min(Comparator
                        .comparingDouble(ModelMetrics::getMae)
                        .thenComparingDouble(ModelMetrics::getRmse)
                        .thenComparingDouble(ModelMetrics::getReliableMapeForTieBreak))
                .orElse(movingAverageMetrics);
    }

    private String classifyDemandTrend(List<DemandRecord> records) {

        List<Integer> quantities = records.stream()
                .map(DemandRecord::getTotalQuantity)
                .collect(Collectors.toList());

        double average = quantities.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        double coefficientOfVariation = coefficientOfVariation(quantities, average);
        double slope = slope(quantities);
        double recentChange = recentChange(quantities);

        if (coefficientOfVariation > 0.75) {

            return "Volatile Demand";
        }

        if (slope > 0.05 || recentChange > 0.20) {

            return "Increasing Demand";
        }

        if (slope < -0.05 || recentChange < -0.20) {

            return "Declining Demand";
        }

        return "Stable Demand";
    }

    private String classifyForecastRisk(ModelMetrics bestMetrics, String demandTrend, List<DemandRecord> records) {

        List<Integer> quantities = records.stream()
                .map(DemandRecord::getTotalQuantity)
                .collect(Collectors.toList());
        double average = quantities.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        double coefficientOfVariation = coefficientOfVariation(quantities, average);

        if ("Volatile Demand".equals(demandTrend)
                || (average >= 20 && coefficientOfVariation > 0.75)
                || (bestMetrics.isMapeReliable() && bestMetrics.getMape() > 60.0)) {

            return "HIGH";
        }

        if ("Declining Demand".equals(demandTrend)
                || (bestMetrics.isMapeReliable() && bestMetrics.getMape() > 30.0)
                || coefficientOfVariation > 0.50) {

            return "MEDIUM";
        }

        return "LOW";
    }

    private double coefficientOfVariation(List<Integer> values, double average) {

        if (values.isEmpty() || average == 0) {

            return 0.0;
        }

        double variance = 0.0;

        for (int value : values) {

            variance += (value - average) * (value - average);
        }

        double standardDeviation = Math.sqrt(variance / values.size());
        return standardDeviation / average;
    }

    private double slope(List<Integer> values) {

        int size = values.size();

        if (size < 2) {

            return 0.0;
        }

        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumXSquare = 0.0;

        for (int i = 0; i < size; i++) {

            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXSquare += x * x;
        }

        double denominator = (size * sumXSquare) - (sumX * sumX);

        if (denominator == 0) {

            return 0.0;
        }

        return ((size * sumXY) - (sumX * sumY)) / denominator;
    }

    private double recentChange(List<Integer> quantities) {

        if (quantities.size() < 4) {

            return 0.0;
        }

        int midpoint = quantities.size() / 2;
        double earlyAverage = quantities.subList(0, midpoint).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        double recentAverage = quantities.subList(midpoint, quantities.size()).stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        if (earlyAverage == 0) {

            return 0.0;
        }

        return (recentAverage - earlyAverage) / earlyAverage;
    }

    private enum ModelType {

        MOVING_AVERAGE("Moving Average"),
        WEIGHTED_MOVING_AVERAGE("Weighted Moving Average"),
        LINEAR_REGRESSION("Linear Regression");

        private final String modelName;

        ModelType(String modelName) {

            this.modelName = modelName;
        }
    }

    private static class ModelMetrics {

        private final ModelType modelType;
        private final double mae;
        private final double rmse;
        private final double mape;
        private final double r2Score;
        private final int mapeObservationCount;

        private ModelMetrics(
                ModelType modelType,
                double mae,
                double rmse,
                double mape,
                double r2Score,
                int mapeObservationCount) {

            this.modelType = modelType;
            this.mae = mae;
            this.rmse = rmse;
            this.mape = mape;
            this.r2Score = r2Score;
            this.mapeObservationCount = mapeObservationCount;
        }

        private ModelType getModelType() {

            return modelType;
        }

        private String getModelName() {

            return modelType.modelName;
        }

        private double getMae() {

            return mae;
        }

        private double getRmse() {

            return rmse;
        }

        private double getMape() {

            return mape;
        }

        private double getReliableMapeForTieBreak() {

            return isMapeReliable() ? mape : Double.MAX_VALUE;
        }

        private boolean isMapeReliable() {

            return mapeObservationCount > 0;
        }

        private double getR2Score() {

            return r2Score;
        }
    }

    private static class PredictionRow {

        private final DemandRecord actualRecord;
        private final double movingAveragePrediction;
        private final double weightedMovingAveragePrediction;
        private final double linearRegressionPrediction;

        private PredictionRow(
                DemandRecord actualRecord,
                double movingAveragePrediction,
                double weightedMovingAveragePrediction,
                double linearRegressionPrediction) {

            this.actualRecord = actualRecord;
            this.movingAveragePrediction = movingAveragePrediction;
            this.weightedMovingAveragePrediction = weightedMovingAveragePrediction;
            this.linearRegressionPrediction = linearRegressionPrediction;
        }

        private String getProductName() {

            return actualRecord.getProductName();
        }

        private String getCategoryName() {

            return actualRecord.getCategoryName();
        }

        private java.time.LocalDate getForecastDate() {

            return actualRecord.getDemandDate();
        }

        private int getActualDemand() {

            return actualRecord.getTotalQuantity();
        }

        private double getMovingAveragePrediction() {

            return movingAveragePrediction;
        }

        private double getWeightedMovingAveragePrediction() {

            return weightedMovingAveragePrediction;
        }

        private double getLinearRegressionPrediction() {

            return linearRegressionPrediction;
        }

        private double predictionFor(ModelType modelType) {

            if (modelType == ModelType.WEIGHTED_MOVING_AVERAGE) {

                return weightedMovingAveragePrediction;
            }

            if (modelType == ModelType.LINEAR_REGRESSION) {

                return linearRegressionPrediction;
            }

            return movingAveragePrediction;
        }
    }

    private static class ProductDecisionSummary {

        private int totalOrders;
        private int delayedOrders;
        private double sales;
        private double profit;

        private void add(SupplyChainRecord record) {

            totalOrders++;
            sales += record.getSales();
            profit += record.getProfit();

            if (record.getLateDeliveryRisk() == 1) {

                delayedOrders++;
            }
        }

        private double getDelayRiskRate() {

            if (totalOrders == 0) {

                return 0.0;
            }

            return delayedOrders * 100.0 / totalOrders;
        }

        private double getProfitMargin() {

            if (sales == 0) {

                return 0.0;
            }

            return profit * 100.0 / sales;
        }
    }
}
