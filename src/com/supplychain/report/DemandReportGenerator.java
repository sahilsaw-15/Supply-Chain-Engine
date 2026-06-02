package com.supplychain.report;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.SupplyChainRecord;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DemandReportGenerator {

    private static final int LIMIT = 10;
    private static final int NAME_WIDTH = 46;
    private static final int UNITS_WIDTH = 12;
    private static final int MONEY_WIDTH = 16;
    private static final int ORDERS_WIDTH = 10;
    private static final int PERCENT_WIDTH = 10;
    private static final int RANK_WIDTH = 4;

    public void printReport(List<DemandRecord> demandRecords, List<SupplyChainRecord> supplyChainRecords) {

        System.out.println("*** DEMAND ANALYZER REPORT ***");
    
        if (demandRecords.isEmpty() && supplyChainRecords.isEmpty()) {

            System.out.println("No records available for demand analysis.");
            return;
        }

        printDemandSummary(demandRecords);
        printProductPerformanceAnalysis(demandRecords);
        printCategoryPerformanceAnalysis(demandRecords);
        printRegionalPerformanceAnalysis(supplyChainRecords);
        printCountryPerformanceAnalysis(supplyChainRecords);
        printMonthlyDemandTrend(demandRecords);
        printDemandVolatility(demandRecords);
        printSupplyChainRiskInsights(supplyChainRecords);
    }

    private void printDemandSummary(List<DemandRecord> demandRecords) {

        int totalQuantity = demandRecords.stream()
                .mapToInt(DemandRecord::getTotalQuantity)
                .sum();
        double totalSales = demandRecords.stream()
                .mapToDouble(DemandRecord::getTotalSales)
                .sum();
        double totalProfit = demandRecords.stream()
                .mapToDouble(DemandRecord::getTotalProfit)
                .sum();
        int totalOrders = demandRecords.stream()
                .mapToInt(DemandRecord::getOrderCount)
                .sum();

        System.out.println("\n1. Demand Summary");
        System.out.printf(Locale.US, "%-24s %,d%n", "Total Quantity Sold:", totalQuantity);
        System.out.printf(Locale.US, "%-24s $%,.2f%n", "Total Sales:", totalSales);
        System.out.printf(Locale.US, "%-24s $%,.2f%n", "Total Profit:", totalProfit);
        System.out.printf(Locale.US, "%-24s %,d%n", "Total Orders:", totalOrders);
    }

    private void printProductPerformanceAnalysis(List<DemandRecord> demandRecords) {

        if (demandRecords.isEmpty()) {

            System.out.println("\n2. Product Performance Analysis");
            System.out.println("No demand records available for product analysis.");
            return;
        }

        Map<String, MetricSummary> productSummary = summarizeDemandByProduct(demandRecords);

        System.out.println("\n2. Product Performance Analysis");
        System.out.println("\nBest Performing Products");
        System.out.println("Top 10 Products by Demand");
        printMetricTable(sortByDemand(productSummary, true), "Product", LIMIT);

        System.out.println("\nTop 10 Products by Sales");
        printMetricTable(sortBySales(productSummary, true), "Product", LIMIT);

        System.out.println("\nTop 10 Products by Profit");
        printMetricTable(sortByProfit(productSummary, true), "Product", LIMIT);

        System.out.println("\nWorst Performing Products");
        System.out.println("Bottom 10 Products by Demand");
        printMetricTable(sortByDemand(productSummary, false), "Product", LIMIT);

        System.out.println("\nBottom 10 Products by Sales");
        printMetricTable(sortBySales(productSummary, false), "Product", LIMIT);

        System.out.println("\nBottom 10 Products by Profit");
        printMetricTable(sortByProfit(productSummary, false), "Product", LIMIT);

        System.out.println("\nBusiness Insight: Products with low demand may be candidates for inventory reduction or discontinuation.");
    }

    private void printCategoryPerformanceAnalysis(List<DemandRecord> demandRecords) {

        if (demandRecords.isEmpty()) {

            System.out.println("\n3. Category Performance Analysis");
            System.out.println("No demand records available for category analysis.");
            return;
        }

        Map<String, MetricSummary> categorySummary = summarizeDemandByCategory(demandRecords);

        System.out.println("\n3. Category Performance Analysis");
        System.out.println("\nBest Categories");
        System.out.println("Top Categories by Demand");
        printMetricTable(sortByDemand(categorySummary, true), "Category", LIMIT);

        System.out.println("\nTop Categories by Sales");
        printMetricTable(sortBySales(categorySummary, true), "Category", LIMIT);

        System.out.println("\nWorst Categories");
        System.out.println("Lowest Demand Categories");
        printMetricTable(sortByDemand(categorySummary, false), "Category", LIMIT);

        System.out.println("\nLowest Revenue Categories");
        printMetricTable(sortBySales(categorySummary, false), "Category", LIMIT);
    }

    private void printRegionalPerformanceAnalysis(List<SupplyChainRecord> supplyChainRecords) {

        System.out.println("\n4. Regional Performance Analysis");

        if (supplyChainRecords.isEmpty()) {

            System.out.println("No supply-chain records available for regional analysis.");
            return;
        }

        Map<String, MetricSummary> regionSummary = summarizeSupplyRecords(supplyChainRecords, GroupType.REGION);

        System.out.println("\nBest Regions");
        System.out.println("Top Regions by Demand");
        printMetricTable(sortByDemand(regionSummary, true), "Region", LIMIT);

        System.out.println("\nTop Regions by Revenue");
        printMetricTable(sortBySales(regionSummary, true), "Region", LIMIT);

        System.out.println("\nWorst Regions");
        System.out.println("Lowest Demand Regions");
        printMetricTable(sortByDemand(regionSummary, false), "Region", LIMIT);

        System.out.println("\nLowest Revenue Regions");
        printMetricTable(sortBySales(regionSummary, false), "Region", LIMIT);
    }

    private void printCountryPerformanceAnalysis(List<SupplyChainRecord> supplyChainRecords) {

        System.out.println("\n5. Country Performance Analysis");

        if (supplyChainRecords.isEmpty()) {

            System.out.println("No supply-chain records available for country analysis.");
            return;
        }

        Map<String, MetricSummary> countrySummary = summarizeSupplyRecords(supplyChainRecords, GroupType.COUNTRY);

        System.out.println("\nBest Countries");
        System.out.println("Top 10 Countries by Demand");
        printMetricTable(sortByDemand(countrySummary, true), "Country", LIMIT);

        System.out.println("\nTop 10 Countries by Revenue");
        printMetricTable(sortBySales(countrySummary, true), "Country", LIMIT);

        System.out.println("\nWorst Countries");
        System.out.println("Bottom 10 Countries by Demand");
        printMetricTable(sortByDemand(countrySummary, false), "Country", LIMIT);

        System.out.println("\nBottom 10 Countries by Revenue");
        printMetricTable(sortBySales(countrySummary, false), "Country", LIMIT);
    }

    private void printMonthlyDemandTrend(List<DemandRecord> demandRecords) {

        if (demandRecords.isEmpty()) {

            System.out.println("\n6. Monthly Demand Trend");
            System.out.println("No demand records available for monthly trend analysis.");
            return;
        }

        Map<YearMonth, MetricSummary> monthlySummary = new TreeMap<>();

        for (DemandRecord record : demandRecords) {

            YearMonth month = YearMonth.from(record.getDemandDate());
            MetricSummary summary = monthlySummary.computeIfAbsent(month, value -> new MetricSummary(value.toString()));
            summary.add(record.getTotalQuantity(), record.getTotalSales(), record.getTotalProfit(), record.getOrderCount());
        }

        System.out.println("\n6. Monthly Demand Trend");
        System.out.printf(Locale.US,
                "%-10s  %" + UNITS_WIDTH + "s  %" + MONEY_WIDTH + "s  %" + MONEY_WIDTH + "s  %18s%n",
                "Month",
                "Units Sold",
                "Sales",
                "Profit",
                "Units Change");
        System.out.println(repeat("-", 10 + UNITS_WIDTH + MONEY_WIDTH + MONEY_WIDTH + 26));

        MetricSummary previous = null;

        for (Map.Entry<YearMonth, MetricSummary> entry : monthlySummary.entrySet()) {

            MetricSummary current = entry.getValue();
            String change = "N/A";

            if (previous != null && previous.getQuantity() > 0) {

                double changePercent = ((current.getQuantity() - previous.getQuantity()) * 100.0)
                        / previous.getQuantity();
                change = String.format(Locale.US, "%+.2f%%", changePercent);
            }

            System.out.printf(Locale.US,
                    "%-10s  %," + UNITS_WIDTH + "d  $%," + (MONEY_WIDTH - 1) + ".2f  $%,"
                            + (MONEY_WIDTH - 1) + ".2f  %18s%n",
                    entry.getKey(),
                    current.getQuantity(),
                    current.getSales(),
                    current.getProfit(),
                    change);

            previous = current;
        }

        System.out.println("\nNote: A very large drop near the final months may indicate that the dataset is incomplete");
        System.out.println("near the end date, so late-period drops should be reviewed before making business decisions.");
    }

    private void printDemandVolatility(List<DemandRecord> demandRecords) {

        if (demandRecords.isEmpty()) {

            System.out.println("\n7. Demand Volatility");
            System.out.println("No demand records available for volatility analysis.");
            return;
        }

        Map<String, List<DemandRecord>> recordsByProduct = new HashMap<>();

        for (DemandRecord record : demandRecords) {

            recordsByProduct.computeIfAbsent(record.getProductName(), key -> new ArrayList<>()).add(record);
        }

        List<MetricSummary> topProducts = sortByDemand(summarizeDemandByProduct(demandRecords), true);

        System.out.println("\n7. Demand Volatility");
        System.out.println("For top-demand products:");
        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %12s  %12s  %12s  %10s  %-18s%n",
                "Product",
                "Avg Units",
                "Max Units",
                "Min Units",
                "CV",
                "Classification");
        System.out.println(repeat("-", NAME_WIDTH + 12 + 12 + 12 + 10 + 30));

        topProducts.stream()
                .limit(LIMIT)
                .forEach(product -> printVolatilityRow(product.getName(), recordsByProduct.get(product.getName())));
    }

    private void printSupplyChainRiskInsights(List<SupplyChainRecord> supplyChainRecords) {

        System.out.println("\n8. Supply Chain Risk Alerts");

        if (supplyChainRecords.isEmpty()) {

            System.out.println("No supply-chain records available for risk analysis.");
            return;
        }

        Map<String, MetricSummary> productSummary = summarizeSupplyRecords(supplyChainRecords, GroupType.PRODUCT);
        List<MetricSummary> topDemandProducts = sortByDemand(productSummary, true);

        System.out.println("\nHigh Demand + High Delay Alert");
        System.out.println("Potential Supply Bottleneck Alert");
        printRiskTable(sortByDelayRisk(topDemandProducts), "Delay Risk");

        System.out.println("\nPopular but Unprofitable Alert");
        System.out.println("High Demand + Low Profit");
        printRiskTable(sortByProfitMarginLow(topDemandProducts), "Profit Margin");
    }

    private void printVolatilityRow(String productName, List<DemandRecord> records) {

        if (records == null || records.isEmpty()) {

            return;
        }

        double averageDemand = records.stream()
                .mapToInt(DemandRecord::getTotalQuantity)
                .average()
                .orElse(0.0);
        int maximumDemand = records.stream()
                .mapToInt(DemandRecord::getTotalQuantity)
                .max()
                .orElse(0);
        int minimumDemand = records.stream()
                .mapToInt(DemandRecord::getTotalQuantity)
                .min()
                .orElse(0);

        double coefficientOfVariation = coefficientOfVariation(records, averageDemand);
        String classification = classifyVolatility(coefficientOfVariation);

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %,12.2f  %,12d  %,12d  %,9.2f%%  %-18s%n",
                fit(productName, NAME_WIDTH),
                averageDemand,
                maximumDemand,
                minimumDemand,
                coefficientOfVariation * 100.0,
                classification);
    }

    private String classifyVolatility(double coefficientOfVariation) {

        if (coefficientOfVariation <= 0.25) {

            return "Low Volatility";
        }

        if (coefficientOfVariation <= 0.75) {

            return "Medium Volatility";
        }

        return "High Volatility";
    }

    private double coefficientOfVariation(List<DemandRecord> records, double averageDemand) {

        if (records.isEmpty() || averageDemand == 0) {

            return 0.0;
        }

        double variance = 0.0;

        for (DemandRecord record : records) {

            double difference = record.getTotalQuantity() - averageDemand;
            variance += difference * difference;
        }

        double standardDeviation = Math.sqrt(variance / records.size());
        return standardDeviation / averageDemand;
    }

    private Map<String, MetricSummary> summarizeDemandByProduct(List<DemandRecord> demandRecords) {

        Map<String, MetricSummary> summaryMap = new LinkedHashMap<>();

        for (DemandRecord record : demandRecords) {

            MetricSummary summary = summaryMap.computeIfAbsent(record.getProductName(), MetricSummary::new);
            summary.add(record.getTotalQuantity(), record.getTotalSales(), record.getTotalProfit(), record.getOrderCount());
        }

        return summaryMap;
    }

    private Map<String, MetricSummary> summarizeDemandByCategory(List<DemandRecord> demandRecords) {

        Map<String, MetricSummary> summaryMap = new LinkedHashMap<>();

        for (DemandRecord record : demandRecords) {

            MetricSummary summary = summaryMap.computeIfAbsent(record.getCategoryName(), MetricSummary::new);
            summary.add(record.getTotalQuantity(), record.getTotalSales(), record.getTotalProfit(), record.getOrderCount());
        }

        return summaryMap;
    }

    private Map<String, MetricSummary> summarizeSupplyRecords(
            List<SupplyChainRecord> supplyChainRecords,
            GroupType groupType) {

        Map<String, MetricSummary> summaryMap = new LinkedHashMap<>();

        for (SupplyChainRecord record : supplyChainRecords) {

            String key = groupKey(record, groupType);
            MetricSummary summary = summaryMap.computeIfAbsent(key, MetricSummary::new);
            summary.add(record.getOrderItemQuantity(), record.getSales(), record.getProfit(), 1);

            if (record.getLateDeliveryRisk() == 1) {

                summary.incrementDelayedOrders();
            }
        }

        return summaryMap;
    }

    private String groupKey(SupplyChainRecord record, GroupType groupType) {

        if (groupType == GroupType.PRODUCT) {

            return record.getProductName();
        }

        if (groupType == GroupType.REGION) {

            return record.getOrderRegion();
        }

        return record.getOrderCountry();
    }

    private List<MetricSummary> sortByDemand(Map<String, MetricSummary> summaryMap, boolean descending) {

        Comparator<MetricSummary> comparator = Comparator
                .comparingInt(MetricSummary::getQuantity)
                .thenComparingDouble(MetricSummary::getSales);

        return sort(summaryMap, comparator, descending);
    }

    private List<MetricSummary> sortBySales(Map<String, MetricSummary> summaryMap, boolean descending) {

        Comparator<MetricSummary> comparator = Comparator
                .comparingDouble(MetricSummary::getSales)
                .thenComparingInt(MetricSummary::getQuantity);

        return sort(summaryMap, comparator, descending);
    }

    private List<MetricSummary> sortByProfit(Map<String, MetricSummary> summaryMap, boolean descending) {

        Comparator<MetricSummary> comparator = Comparator
                .comparingDouble(MetricSummary::getProfit)
                .thenComparingDouble(MetricSummary::getSales);

        return sort(summaryMap, comparator, descending);
    }

    private List<MetricSummary> sort(Map<String, MetricSummary> summaryMap, Comparator<MetricSummary> comparator, boolean descending) {

        List<MetricSummary> rows = new ArrayList<>(summaryMap.values());

        if (descending) {

            rows.sort(comparator.reversed());
        } else {

            rows.sort(comparator);
        }

        return rows;
    }

    private List<MetricSummary> sortByDelayRisk(List<MetricSummary> topDemandProducts) {

        List<MetricSummary> rows = new ArrayList<>(topDemandProducts.subList(0, Math.min(LIMIT, topDemandProducts.size())));
        rows.sort(Comparator
                .comparingDouble(MetricSummary::getDelayRiskRate)
                .reversed()
                .thenComparing(Comparator.comparingInt(MetricSummary::getQuantity).reversed()));

        return rows;
    }

    private List<MetricSummary> sortByProfitMarginLow(List<MetricSummary> topDemandProducts) {

        List<MetricSummary> rows = new ArrayList<>(topDemandProducts.subList(0, Math.min(LIMIT, topDemandProducts.size())));
        rows.sort(Comparator
                .comparingDouble(MetricSummary::getProfitMargin)
                .thenComparing(Comparator.comparingInt(MetricSummary::getQuantity).reversed()));

        return rows;
    }

    private void printMetricTable(List<MetricSummary> rows, String label, int limit) {

        System.out.printf(Locale.US,
                "%" + RANK_WIDTH + "s  %-" + NAME_WIDTH + "s  %" + UNITS_WIDTH + "s  %" + MONEY_WIDTH + "s  %"
                        + MONEY_WIDTH + "s  %" + ORDERS_WIDTH + "s%n",
                "No.",
                label,
                "Units Sold",
                "Sales",
                "Profit",
                "Orders");
        System.out.println(repeat("-", RANK_WIDTH + NAME_WIDTH + UNITS_WIDTH + MONEY_WIDTH + MONEY_WIDTH + ORDERS_WIDTH + 10));

        for (int i = 0; i < rows.size() && i < limit; i++) {

            MetricSummary row = rows.get(i);

            System.out.printf(Locale.US,
                    "%" + RANK_WIDTH + "d  %-" + NAME_WIDTH + "s  %," + UNITS_WIDTH + "d  $%,"
                            + (MONEY_WIDTH - 1) + ".2f  $%," + (MONEY_WIDTH - 1) + ".2f  %,"
                            + ORDERS_WIDTH + "d%n",
                    i + 1,
                    fit(row.getName(), NAME_WIDTH),
                    row.getQuantity(),
                    row.getSales(),
                    row.getProfit(),
                    row.getOrders());
        }
    }

    private void printRiskTable(List<MetricSummary> rows, String riskColumnName) {

        System.out.printf(Locale.US,
                "%-" + NAME_WIDTH + "s  %" + UNITS_WIDTH + "s  %" + MONEY_WIDTH + "s  %"
                        + MONEY_WIDTH + "s  %" + PERCENT_WIDTH + "s%n",
                "Product",
                "Units Sold",
                "Sales",
                "Profit",
                riskColumnName);
        System.out.println(repeat("-", NAME_WIDTH + UNITS_WIDTH + MONEY_WIDTH + MONEY_WIDTH + PERCENT_WIDTH + 8));

        rows.stream()
                .limit(LIMIT)
                .forEach(row -> {

                    double percentage = "Delay Risk".equals(riskColumnName)
                            ? row.getDelayRiskRate()
                            : row.getProfitMargin();

                    System.out.printf(Locale.US,
                            "%-" + NAME_WIDTH + "s  %," + UNITS_WIDTH + "d  $%,"
                                    + (MONEY_WIDTH - 1) + ".2f  $%," + (MONEY_WIDTH - 1)
                                    + ".2f  %" + (PERCENT_WIDTH - 1) + ".2f%%%n",
                            fit(row.getName(), NAME_WIDTH),
                            row.getQuantity(),
                            row.getSales(),
                            row.getProfit(),
                            percentage);
                });
    }

    private String fit(String value, int width) {

        if (value == null) {

            return "";
        }

        if (value.length() <= width) {

            return value;
        }

        return value.substring(0, width - 3) + "...";
    }

    private String repeat(String value, int count) {

        return value.repeat(count);
    }

    private enum GroupType {

        PRODUCT,
        REGION,
        COUNTRY
    }

    private static class MetricSummary {

        private final String name;
        private int quantity;
        private double sales;
        private double profit;
        private int orders;
        private int delayedOrders;

        private MetricSummary(String name) {

            this.name = name;
        }

        private void add(int quantity, double sales, double profit, int orders) {

            this.quantity += quantity;
            this.sales += sales;
            this.profit += profit;
            this.orders += orders;
        }

        private void incrementDelayedOrders() {

            delayedOrders++;
        }

        private String getName() {

            return name;
        }

        private int getQuantity() {

            return quantity;
        }

        private double getSales() {

            return sales;
        }

        private double getProfit() {

            return profit;
        }

        private int getOrders() {

            return orders;
        }

        private double getDelayRiskRate() {

            if (orders == 0) {

                return 0.0;
            }

            return delayedOrders * 100.0 / orders;
        }

        private double getProfitMargin() {

            if (sales == 0) {

                return 0.0;
            }

            return profit * 100.0 / sales;
        }
    }
}
