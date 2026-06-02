package com.supplychain.model;

public class RiskIntelligenceResult {

    private final String productName;
    private final String categoryName;
    private final double forecastDemand;
    private final double delayRisk;
    private final double profitMargin;
    private final double volatility;
    private final double riskScore;
    private final String riskLevel;
    private final String recommendation;
    private final String stockoutRisk;

    public RiskIntelligenceResult(
            String productName,
            String categoryName,
            double forecastDemand,
            double delayRisk,
            double profitMargin,
            double volatility,
            double riskScore,
            String riskLevel,
            String recommendation,
            String stockoutRisk) {

        this.productName = productName;
        this.categoryName = categoryName;
        this.forecastDemand = forecastDemand;
        this.delayRisk = delayRisk;
        this.profitMargin = profitMargin;
        this.volatility = volatility;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
        this.stockoutRisk = stockoutRisk;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public double getForecastDemand() {
        return forecastDemand;
    }

    public double getDelayRisk() {
        return delayRisk;
    }

    public double getProfitMargin() {
        return profitMargin;
    }

    public double getVolatility() {
        return volatility;
    }

    public double getRiskScore() {
        return riskScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public String getStockoutRisk() {
        return stockoutRisk;
    }
}
