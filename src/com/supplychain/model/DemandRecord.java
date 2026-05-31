package com.supplychain.model;

import java.time.LocalDate;

public class DemandRecord {

    private LocalDate demandDate;
    private String productName;
    private String categoryName;
    private int totalQuantity;
    private double totalSales;
    private double totalProfit;
    private int orderCount;

    public DemandRecord(LocalDate demandDate, String productName, String categoryName) {

        this.demandDate = demandDate;
        this.productName = productName;
        this.categoryName = categoryName;
    }

    public void addOrder(int quantity, double sales, double profit) {

        totalQuantity += quantity;
        totalSales += sales;
        totalProfit += profit;
        orderCount++;
    }

    public LocalDate getDemandDate() {
        return demandDate;
    }

    public String getProductName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public double getTotalSales() {
        return totalSales;
    }

    public double getTotalProfit() {
        return totalProfit;
    }

    public int getOrderCount() {
        return orderCount;
    }
}
