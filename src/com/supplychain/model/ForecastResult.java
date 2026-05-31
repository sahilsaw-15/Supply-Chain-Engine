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
            String forecastRiskLevel) {

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
}
