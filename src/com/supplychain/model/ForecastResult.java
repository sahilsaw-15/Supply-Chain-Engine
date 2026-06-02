package com.supplychain.model;

import java.time.LocalDate;

public class ForecastResult {

    private final String productName;
    private final String categoryName;
    private final LocalDate forecastDate;
    private final int actualDemand;
    private final double movingAveragePrediction;
    private final double weightedMovingAveragePrediction;
    private final double linearRegressionPrediction;
    private final String bestModel;
    private final double finalPredictedDemand;
    private final double mae;
    private final double rmse;
    private final double mape;
    private final double r2Score;
    private final String demandTrend;
    private final String forecastRiskLevel;
    private final double historicalAverageDemand;
    private final String inventoryRecommendation;
    private final double delayRiskRate;
    private final double profitMargin;
    private final String combinedRiskClassification;
    private final String reorderPriority;
    private final String accuracyNote;

    public ForecastResult(
            String productName,
            String categoryName,
            LocalDate forecastDate,
            int actualDemand,
            double movingAveragePrediction,
            double weightedMovingAveragePrediction,
            double linearRegressionPrediction,
            String bestModel,
            double finalPredictedDemand,
            double mae,
            double rmse,
            double mape,
            double r2Score,
            String demandTrend,
            String forecastRiskLevel,
            double historicalAverageDemand,
            String inventoryRecommendation,
            double delayRiskRate,
            double profitMargin,
            String combinedRiskClassification,
            String reorderPriority,
            String accuracyNote) {

        this.productName = productName;
        this.categoryName = categoryName;
        this.forecastDate = forecastDate;
        this.actualDemand = actualDemand;
        this.movingAveragePrediction = movingAveragePrediction;
        this.weightedMovingAveragePrediction = weightedMovingAveragePrediction;
        this.linearRegressionPrediction = linearRegressionPrediction;
        this.bestModel = bestModel;
        this.finalPredictedDemand = finalPredictedDemand;
        this.mae = mae;
        this.rmse = rmse;
        this.mape = mape;
        this.r2Score = r2Score;
        this.demandTrend = demandTrend;
        this.forecastRiskLevel = forecastRiskLevel;
        this.historicalAverageDemand = historicalAverageDemand;
        this.inventoryRecommendation = inventoryRecommendation;
        this.delayRiskRate = delayRiskRate;
        this.profitMargin = profitMargin;
        this.combinedRiskClassification = combinedRiskClassification;
        this.reorderPriority = reorderPriority;
        this.accuracyNote = accuracyNote;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public LocalDate getForecastDate() {
        return forecastDate;
    }

    public int getActualDemand() {
        return actualDemand;
    }

    public double getMovingAveragePrediction() {
        return movingAveragePrediction;
    }

    public double getWeightedMovingAveragePrediction() {
        return weightedMovingAveragePrediction;
    }

    public double getLinearRegressionPrediction() {
        return linearRegressionPrediction;
    }

    public String getBestModel() {
        return bestModel;
    }

    public double getFinalPredictedDemand() {
        return finalPredictedDemand;
    }

    public double getMae() {
        return mae;
    }

    public double getRmse() {
        return rmse;
    }

    public double getMape() {
        return mape;
    }

    public double getR2Score() {
        return r2Score;
    }

    public String getDemandTrend() {
        return demandTrend;
    }

    public String getForecastRiskLevel() {
        return forecastRiskLevel;
    }

    public double getHistoricalAverageDemand() {
        return historicalAverageDemand;
    }

    public String getInventoryRecommendation() {
        return inventoryRecommendation;
    }

    public double getDelayRiskRate() {
        return delayRiskRate;
    }

    public double getProfitMargin() {
        return profitMargin;
    }

    public String getCombinedRiskClassification() {
        return combinedRiskClassification;
    }

    public String getReorderPriority() {
        return reorderPriority;
    }

    public String getAccuracyNote() {
        return accuracyNote;
    }
}
