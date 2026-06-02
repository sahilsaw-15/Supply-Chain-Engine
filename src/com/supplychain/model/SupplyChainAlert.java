package com.supplychain.model;

public class SupplyChainAlert {

    private final String alertType;
    private final String productName;
    private final String categoryName;
    private final double forecastDemand;
    private final double delayRisk;
    private final double profitMargin;
    private final String severity;
    private final String recommendation;

    public SupplyChainAlert(
            String alertType,
            String productName,
            String categoryName,
            double forecastDemand,
            double delayRisk,
            double profitMargin,
            String severity,
            String recommendation) {

        this.alertType = alertType;
        this.productName = productName;
        this.categoryName = categoryName;
        this.forecastDemand = forecastDemand;
        this.delayRisk = delayRisk;
        this.profitMargin = profitMargin;
        this.severity = severity;
        this.recommendation = recommendation;
    }

    public String getAlertType() {
        return alertType;
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

    public String getSeverity() {
        return severity;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
