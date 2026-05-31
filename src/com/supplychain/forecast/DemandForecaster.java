package com.supplychain.forecast;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;

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
    private static final int MINIMUM_ACTUAL_FOR_MAPE = 5;

    public List<ForecastResult> forecast(List<DemandRecord> demandRecords) {

        Map<String, List<DemandRecord>> recordsByProductCategory = groupByProductAndCategory(demandRecords);
        List<ForecastResult> results = new ArrayList<>();

        for (List<DemandRecord> records : recordsByProductCategory.values()) {

            records.sort(Comparator.comparing(DemandRecord::getDemandDate));

            if (records.size() < MINIMUM_RECORDS_TO_FORECAST) {

                continue;
            }

            results.addAll(forecastProductCategory(records));
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

            String key = record.getProductName() + "|" + record.getCategoryName();
            recordsByProductCategory.computeIfAbsent(key, value -> new ArrayList<>()).add(record);
        }

        return recordsByProductCategory;
    }

    private List<ForecastResult> forecastProductCategory(List<DemandRecord> records) {

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
        String demandTrend = classifyDemandTrend(records);
        String forecastRiskLevel = classifyForecastRisk(bestMetrics, demandTrend, records);

        List<ForecastResult> results = new ArrayList<>();

        for (PredictionRow predictionRow : predictionRows) {

            results.add(new ForecastResult(
                    predictionRow.getProductName(),
                    predictionRow.getCategoryName(),
                    predictionRow.getForecastDate(),
                    predictionRow.getActualDemand(),
                    predictionRow.getMovingAveragePrediction(),
                    predictionRow.getWeightedMovingAveragePrediction(),
                    predictionRow.getLinearRegressionPrediction(),
                    bestMetrics.getModelName(),
                    predictionRow.predictionFor(bestMetrics.getModelType()),
                    bestMetrics.getMae(),
                    bestMetrics.getRmse(),
                    bestMetrics.getMape(),
                    bestMetrics.getR2Score(),
                    demandTrend,
                    forecastRiskLevel
            ));
        }

        return results;
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

        return new ModelMetrics(modelType, mae, rmse, mape, r2Score);
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
                        .thenComparingDouble(ModelMetrics::getMape))
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
                || bestMetrics.getMape() > 60.0) {

            return "HIGH";
        }

        if ("Declining Demand".equals(demandTrend)
                || bestMetrics.getMape() > 30.0
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

        private ModelMetrics(ModelType modelType, double mae, double rmse, double mape, double r2Score) {

            this.modelType = modelType;
            this.mae = mae;
            this.rmse = rmse;
            this.mape = mape;
            this.r2Score = r2Score;
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
}
