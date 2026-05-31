package com.supplychain.model;

public class CleaningConfig {

    private double maximumSales;
    private double maximumProfitLoss;
    private int maximumShippingDays;
    private double statisticalOutlierZScore;

    public CleaningConfig() {

        this.maximumSales = 10000.0;
        this.maximumProfitLoss = 5000.0;
        this.maximumShippingDays = 60;
        this.statisticalOutlierZScore = 3.0;
    }

    public double getMaximumSales() {
        return maximumSales;
    }

    public double getMaximumProfitLoss() {
        return maximumProfitLoss;
    }

    public int getMaximumShippingDays() {
        return maximumShippingDays;
    }

    public double getStatisticalOutlierZScore() {
        return statisticalOutlierZScore;
    }
}
