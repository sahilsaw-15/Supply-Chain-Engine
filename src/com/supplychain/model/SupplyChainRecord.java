package com.supplychain.model;

import java.time.LocalDateTime;

public class SupplyChainRecord {

    private String categoryName;
    private String departmentName;
    private String productName;
    private String customerCountry;
    private String customerState;
    private String customerCity;
    private String orderCountry;
    private String orderState;
    private String orderCity;
    private String orderRegion;
    private String market;
    private String paymentType;
    private String customerSegment;
    private String shippingMode;
    private String deliveryStatus;
    private String orderStatus;

    private int orderId;
    private int orderItemId;
    private int realShippingDays;
    private int scheduledShippingDays;
    private int lateDeliveryRisk;
    private int orderItemQuantity;
    private int productStatus;

    private double sales;
    private double salesPerCustomer;
    private double profit;
    private double benefitPerOrder;
    private double orderItemTotal;
    private double orderItemDiscount;
    private double orderItemDiscountRate;
    private double orderItemProfitRatio;
    private double productPrice;
    private double latitude;
    private double longitude;

    private LocalDateTime orderDate;
    private LocalDateTime shippingDate;

    public SupplyChainRecord(
            String categoryName,
            String departmentName,
            String productName,
            String customerCountry,
            String customerState,
            String customerCity,
            String orderCountry,
            String orderState,
            String orderCity,
            String orderRegion,
            String market,
            String paymentType,
            String customerSegment,
            String shippingMode,
            String deliveryStatus,
            String orderStatus,
            int orderId,
            int orderItemId,
            int realShippingDays,
            int scheduledShippingDays,
            int lateDeliveryRisk,
            int orderItemQuantity,
            int productStatus,
            double sales,
            double salesPerCustomer,
            double profit,
            double benefitPerOrder,
            double orderItemTotal,
            double orderItemDiscount,
            double orderItemDiscountRate,
            double orderItemProfitRatio,
            double productPrice,
            double latitude,
            double longitude,
            LocalDateTime orderDate,
            LocalDateTime shippingDate) {

        this.categoryName = categoryName;
        this.departmentName = departmentName;
        this.productName = productName;
        this.customerCountry = customerCountry;
        this.customerState = customerState;
        this.customerCity = customerCity;
        this.orderCountry = orderCountry;
        this.orderState = orderState;
        this.orderCity = orderCity;
        this.orderRegion = orderRegion;
        this.market = market;
        this.paymentType = paymentType;
        this.customerSegment = customerSegment;
        this.shippingMode = shippingMode;
        this.deliveryStatus = deliveryStatus;
        this.orderStatus = orderStatus;
        this.orderId = orderId;
        this.orderItemId = orderItemId;
        this.realShippingDays = realShippingDays;
        this.scheduledShippingDays = scheduledShippingDays;
        this.lateDeliveryRisk = lateDeliveryRisk;
        this.orderItemQuantity = orderItemQuantity;
        this.productStatus = productStatus;
        this.sales = sales;
        this.salesPerCustomer = salesPerCustomer;
        this.profit = profit;
        this.benefitPerOrder = benefitPerOrder;
        this.orderItemTotal = orderItemTotal;
        this.orderItemDiscount = orderItemDiscount;
        this.orderItemDiscountRate = orderItemDiscountRate;
        this.orderItemProfitRatio = orderItemProfitRatio;
        this.productPrice = productPrice;
        this.latitude = latitude;
        this.longitude = longitude;
        this.orderDate = orderDate;
        this.shippingDate = shippingDate;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getProductName() {
        return productName;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public String getCustomerState() {
        return customerState;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public String getOrderCountry() {
        return orderCountry;
    }

    public String getOrderState() {
        return orderState;
    }

    public String getOrderCity() {
        return orderCity;
    }

    public String getOrderRegion() {
        return orderRegion;
    }

    public String getMarket() {
        return market;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public String getCustomerSegment() {
        return customerSegment;
    }

    public String getShippingMode() {
        return shippingMode;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public int getOrderId() {
        return orderId;
    }

    public int getOrderItemId() {
        return orderItemId;
    }

    public int getRealShippingDays() {
        return realShippingDays;
    }

    public int getScheduledShippingDays() {
        return scheduledShippingDays;
    }

    public int getLateDeliveryRisk() {
        return lateDeliveryRisk;
    }

    public int getOrderItemQuantity() {
        return orderItemQuantity;
    }

    public int getProductStatus() {
        return productStatus;
    }

    public double getSales() {
        return sales;
    }

    public double getSalesPerCustomer() {
        return salesPerCustomer;
    }

    public double getProfit() {
        return profit;
    }

    public double getBenefitPerOrder() {
        return benefitPerOrder;
    }

    public double getOrderItemTotal() {
        return orderItemTotal;
    }

    public double getOrderItemDiscount() {
        return orderItemDiscount;
    }

    public double getOrderItemDiscountRate() {
        return orderItemDiscountRate;
    }

    public double getOrderItemProfitRatio() {
        return orderItemProfitRatio;
    }

    public double getProductPrice() {
        return productPrice;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public LocalDateTime getShippingDate() {
        return shippingDate;
    }

    @Override
    public String toString() {
        return "Order ID: " + orderId +
                " | Product: " + productName +
                " | Category: " + categoryName +
                " | Order Country: " + orderCountry +
                " | Status: " + orderStatus +
                " | Sales: " + sales +
                " | Profit: " + profit;
    }
}
