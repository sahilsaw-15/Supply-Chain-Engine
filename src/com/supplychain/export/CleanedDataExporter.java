package com.supplychain.export;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainAlert;
import com.supplychain.model.SupplyChainRecord;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CleanedDataExporter {

    private static final DateTimeFormatter OUTPUT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void exportToCsv(List<SupplyChainRecord> records, Path outputPath) throws IOException {

        Path parentDirectory = outputPath.getParent();

        if (parentDirectory != null) {

            Files.createDirectories(parentDirectory);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF');
            writer.write(String.join(",",
                    "Order ID",
                    "Order Item ID",
                    "Product Name",
                    "Category Name",
                    "Department Name",
                    "Customer Country",
                    "Customer State",
                    "Customer City",
                    "Order Country",
                    "Order State",
                    "Order City",
                    "Order Region",
                    "Market",
                    "Payment Type",
                    "Customer Segment",
                    "Shipping Mode",
                    "Delivery Status",
                    "Order Status",
                    "Real Shipping Days",
                    "Scheduled Shipping Days",
                    "Late Delivery Risk",
                    "Order Item Quantity",
                    "Product Status",
                    "Sales",
                    "Sales Per Customer",
                    "Profit",
                    "Benefit Per Order",
                    "Order Item Total",
                    "Order Item Discount",
                    "Order Item Discount Rate",
                    "Order Item Profit Ratio",
                    "Product Price",
                    "Latitude",
                    "Longitude",
                    "Order Date",
                    "Shipping Date"
            ));
            writer.newLine();

            for (SupplyChainRecord record : records) {

                writer.write(toCsvLine(record));
                writer.newLine();
            }
        }
    }

    public void exportDemandToCsv(List<DemandRecord> records, Path outputPath) throws IOException {

        Path parentDirectory = outputPath.getParent();

        if (parentDirectory != null) {

            Files.createDirectories(parentDirectory);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF');
            writer.write(String.join(",",
                    "Order Date",
                    "Product Name",
                    "Category Name",
                    "Total Quantity",
                    "Total Sales",
                    "Total Profit",
                    "Order Count"
            ));
            writer.newLine();

            for (DemandRecord record : records) {

                writer.write(String.join(",",
                        escape(record.getDemandDate().toString()),
                        escape(record.getProductName()),
                        escape(record.getCategoryName()),
                        escape(record.getTotalQuantity()),
                        escape(record.getTotalSales()),
                        escape(record.getTotalProfit()),
                        escape(record.getOrderCount())
                ));
                writer.newLine();
            }
        }
    }

    public void exportForecastToCsv(List<ForecastResult> records, Path outputPath) throws IOException {

        Path parentDirectory = outputPath.getParent();

        if (parentDirectory != null) {

            Files.createDirectories(parentDirectory);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF');
            writer.write(String.join(",",
                    "Product Name",
                    "Category Name",
                    "Forecast Date",
                    "Actual Demand",
                    "Moving Average Prediction",
                    "Weighted Moving Average Prediction",
                    "Linear Regression Prediction",
                    "Best Model",
                    "Final Predicted Demand",
                    "MAE",
                    "RMSE",
                    "MAPE",
                    "Product R2 Score",
                    "Demand Trend",
                    "Forecast Risk Level",
                    "Historical Average Demand",
                    "Inventory Recommendation",
                    "Delay Risk Rate",
                    "Profit Margin",
                    "Combined Risk Classification",
                    "Reorder Priority",
                    "Accuracy Note"
            ));
            writer.newLine();

            for (ForecastResult record : records) {

                writer.write(String.join(",",
                        escape(record.getProductName()),
                        escape(record.getCategoryName()),
                        escape(record.getForecastDate().toString()),
                        escape(record.getActualDemand()),
                        escape(record.getMovingAveragePrediction()),
                        escape(record.getWeightedMovingAveragePrediction()),
                        escape(record.getLinearRegressionPrediction()),
                        escape(record.getBestModel()),
                        escape(record.getFinalPredictedDemand()),
                        escape(record.getMae()),
                        escape(record.getRmse()),
                        escape(record.getMape()),
                        escape(record.getR2Score()),
                        escape(record.getDemandTrend()),
                        escape(record.getForecastRiskLevel()),
                        escape(record.getHistoricalAverageDemand()),
                        escape(record.getInventoryRecommendation()),
                        escape(record.getDelayRiskRate()),
                        escape(record.getProfitMargin()),
                        escape(record.getCombinedRiskClassification()),
                        escape(record.getReorderPriority()),
                        escape(record.getAccuracyNote())
                ));
                writer.newLine();
            }
        }
    }

    public void exportRiskIntelligenceToCsv(List<RiskIntelligenceResult> records, Path outputPath) throws IOException {

        Path parentDirectory = outputPath.getParent();

        if (parentDirectory != null) {

            Files.createDirectories(parentDirectory);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF');
            writer.write(String.join(",",
                    "Product Name",
                    "Category Name",
                    "Forecast Demand",
                    "Delay Risk",
                    "Profit Margin",
                    "Volatility",
                    "Risk Score",
                    "Risk Level",
                    "Recommendation",
                    "Stockout Risk"
            ));
            writer.newLine();

            for (RiskIntelligenceResult record : records) {

                writer.write(String.join(",",
                        escape(record.getProductName()),
                        escape(record.getCategoryName()),
                        escape(record.getForecastDemand()),
                        escape(record.getDelayRisk()),
                        escape(record.getProfitMargin()),
                        escape(record.getVolatility()),
                        escape(record.getRiskScore()),
                        escape(record.getRiskLevel()),
                        escape(record.getRecommendation()),
                        escape(record.getStockoutRisk())
                ));
                writer.newLine();
            }
        }
    }

    public void exportAlertsToCsv(List<SupplyChainAlert> records, Path outputPath) throws IOException {

        Path parentDirectory = outputPath.getParent();

        if (parentDirectory != null) {

            Files.createDirectories(parentDirectory);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            writer.write('\uFEFF');
            writer.write(String.join(",",
                    "Alert Type",
                    "Product Name",
                    "Category Name",
                    "Forecast Demand",
                    "Delay Risk",
                    "Profit Margin",
                    "Severity",
                    "Recommendation"
            ));
            writer.newLine();

            for (SupplyChainAlert record : records) {

                writer.write(String.join(",",
                        escape(record.getAlertType()),
                        escape(record.getProductName()),
                        escape(record.getCategoryName()),
                        escape(record.getForecastDemand()),
                        escape(record.getDelayRisk()),
                        escape(record.getProfitMargin()),
                        escape(record.getSeverity()),
                        escape(record.getRecommendation())
                ));
                writer.newLine();
            }
        }
    }

    private String toCsvLine(SupplyChainRecord record) {

        return String.join(",",
                escape(record.getOrderId()),
                escape(record.getOrderItemId()),
                escape(record.getProductName()),
                escape(record.getCategoryName()),
                escape(record.getDepartmentName()),
                escape(record.getCustomerCountry()),
                escape(record.getCustomerState()),
                escape(record.getCustomerCity()),
                escape(record.getOrderCountry()),
                escape(record.getOrderState()),
                escape(record.getOrderCity()),
                escape(record.getOrderRegion()),
                escape(record.getMarket()),
                escape(record.getPaymentType()),
                escape(record.getCustomerSegment()),
                escape(record.getShippingMode()),
                escape(record.getDeliveryStatus()),
                escape(record.getOrderStatus()),
                escape(record.getRealShippingDays()),
                escape(record.getScheduledShippingDays()),
                escape(record.getLateDeliveryRisk()),
                escape(record.getOrderItemQuantity()),
                escape(record.getProductStatus()),
                escape(record.getSales()),
                escape(record.getSalesPerCustomer()),
                escape(record.getProfit()),
                escape(record.getBenefitPerOrder()),
                escape(record.getOrderItemTotal()),
                escape(record.getOrderItemDiscount()),
                escape(record.getOrderItemDiscountRate()),
                escape(record.getOrderItemProfitRatio()),
                escape(record.getProductPrice()),
                escape(record.getLatitude()),
                escape(record.getLongitude()),
                escape(formatDate(record)),
                escape(formatShippingDate(record))
        );
    }

    private String formatDate(SupplyChainRecord record) {

        if (record.getOrderDate() == null) {

            return "";
        }

        return record.getOrderDate().format(OUTPUT_DATE_FORMAT);
    }

    private String formatShippingDate(SupplyChainRecord record) {

        if (record.getShippingDate() == null) {

            return "";
        }

        return record.getShippingDate().format(OUTPUT_DATE_FORMAT);
    }

    private String escape(int value) {

        return String.valueOf(value);
    }

    private String escape(double value) {

        return String.valueOf(value);
    }

    private String escape(String value) {

        if (value == null) {

            return "";
        }

        String cleaned = value.replace("\"", "\"\"");

        if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("\n")) {

            return "\"" + cleaned + "\"";
        }

        return cleaned;
    }
}
