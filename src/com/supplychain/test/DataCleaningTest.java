package com.supplychain.test;

import com.supplychain.export.CleanedDataExporter;
import com.supplychain.forecast.DemandDatasetBuilder;
import com.supplychain.loader.CSVDataLoader;
import com.supplychain.loader.CSVDataLoader.DataCleaningResult;
import com.supplychain.model.DemandRecord;
import com.supplychain.model.SupplyChainRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DataCleaningTest {

    public static void main(String[] args) throws IOException {

        testCountryCleaning();
        testColumnNameMapping();
        testExportCreatesCsvFile();
        testDemandDatasetGrouping();
        System.out.println("All data cleaning tests passed.");
    }

    private static void testCountryCleaning() {

        DataCleaningResult result = new CSVDataLoader().convertToRecords(sampleRows());
        SupplyChainRecord record = result.getRecords().get(0);

        assertEquals("United States", record.getCustomerCountry(), "Customer country should be standardized.");
        assertEquals("Japan", record.getOrderCountry(), "Order country should be translated to English.");
    }

    private static void testColumnNameMapping() {

        DataCleaningResult result = new CSVDataLoader().convertToRecords(sampleRows());
        SupplyChainRecord record = result.getRecords().get(0);

        assertEquals("Smart watch", record.getProductName(), "Product name should be read by column name.");
        assertEquals(77202, record.getOrderId(), "Order ID should be read by column name.");
    }

    private static void testExportCreatesCsvFile() throws IOException {

        DataCleaningResult result = new CSVDataLoader().convertToRecords(sampleRows());
        Path outputPath = Path.of("Data", "test_cleaned_output.csv");

        new CleanedDataExporter().exportToCsv(result.getRecords(), outputPath);

        if (!Files.exists(outputPath)) {

            throw new AssertionError("Cleaned CSV file was not created.");
        }

        String fileText = Files.readString(outputPath);

        if (!fileText.contains("Order ID") || !fileText.contains("Smart watch")) {

            throw new AssertionError("Cleaned CSV file does not contain expected data.");
        }
    }

    private static void testDemandDatasetGrouping() {

        DataCleaningResult result = new CSVDataLoader().convertToRecords(sampleRows());
        List<DemandRecord> demandRecords = new DemandDatasetBuilder().buildDailyProductDemand(result.getRecords());
        DemandRecord demandRecord = demandRecords.get(0);

        assertEquals(1, demandRecords.size(), "One daily demand row should be created.");
        assertEquals(1, demandRecord.getTotalQuantity(), "Total demand quantity should be summed.");
        assertEquals("Smart watch", demandRecord.getProductName(), "Demand row should keep product name.");
    }

    private static List<String[]> sampleRows() {

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{
                "Type",
                "Days for shipping (real)",
                "Days for shipment (scheduled)",
                "Benefit per order",
                "Sales per customer",
                "Delivery Status",
                "Late_delivery_risk",
                "Category Name",
                "Customer City",
                "Customer Country",
                "Customer Segment",
                "Customer State",
                "Department Name",
                "Latitude",
                "Longitude",
                "Market",
                "Order City",
                "Order Country",
                "order date (DateOrders)",
                "Order Id",
                "Order Item Discount",
                "Order Item Discount Rate",
                "Order Item Id",
                "Order Item Profit Ratio",
                "Order Item Quantity",
                "Sales",
                "Order Item Total",
                "Order Profit Per Order",
                "Order Region",
                "Order State",
                "Order Status",
                "Product Name",
                "Product Price",
                "Product Status",
                "shipping date (DateOrders)",
                "Shipping Mode"
        });
        rows.add(new String[]{
                "DEBIT",
                "3",
                "4",
                "91.25",
                "314.64",
                "Advance shipping",
                "0",
                "Sporting Goods",
                "San Jose",
                "EE. UU.",
                "Consumer",
                "CA",
                "Fitness",
                "37.29",
                "-121.88",
                "Pacific Asia",
                "Tokyo",
                "Japon",
                "1/31/2018 22:56",
                "77202",
                "13.11",
                "0.04",
                "180517",
                "0.29",
                "1",
                "327.75",
                "314.64",
                "91.25",
                "East Asia",
                "Tokyo",
                "COMPLETE",
                "Smart watch",
                "327.75",
                "0",
                "2/3/2018 22:56",
                "Standard Class"
        });

        return rows;
    }

    private static void assertEquals(String expected, String actual, String message) {

        if (!expected.equals(actual)) {

            throw new AssertionError(message + " Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {

        if (expected != actual) {

            throw new AssertionError(message + " Expected: " + expected + ", Actual: " + actual);
        }
    }
}
