package com.supplychain.loader;

import com.supplychain.model.SupplyChainRecord;
import com.supplychain.model.CleaningConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CSVDataLoader {

    private static final String UNKNOWN_VALUE = "UNKNOWN";
    private static final Set<String> VALID_DELIVERY_STATUSES =
            Set.of("Advance shipping", "Late delivery", "Shipping canceled", "Shipping on time");
    private static final Set<String> VALID_SHIPPING_MODES =
            Set.of("First Class", "Same Day", "Second Class", "Standard Class");
    private static final Set<String> VALID_PAYMENT_TYPES =
            Set.of("CASH", "DEBIT", "PAYMENT", "TRANSFER");
    private static final Set<String> VALID_CUSTOMER_SEGMENTS =
            Set.of("Consumer", "Corporate", "Home Office");
    private static final Set<String> VALID_ORDER_STATUSES =
            Set.of("CANCELED", "CLOSED", "COMPLETE", "ON_HOLD", "PAYMENT_REVIEW",
                    "PENDING", "PENDING_PAYMENT", "PROCESSING", "SUSPECTED_FRAUD");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("M/d/yyyy H:mm");
    private static final Map<String, String> COUNTRY_TRANSLATIONS = buildCountryTranslations();
    private final CleaningConfig cleaningConfig;

    public CSVDataLoader() {

        this(new CleaningConfig());
    }

    public CSVDataLoader(CleaningConfig cleaningConfig) {

        this.cleaningConfig = cleaningConfig;
    }

    public List<String[]> loadCSV(String filePath) {

        List<String[]> rows = new ArrayList<>();
        Path path = Path.of(filePath);

        try (BufferedReader reader =
                     Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {

            rows = parseCsv(reader);

        } catch (IOException e) {

            System.out.println("Error loading CSV file: " + e.getMessage());
        }

        return rows;
    }

    public void printDatasetSummary(List<String[]> rows) {

        if (rows.isEmpty()) {

            System.out.println("No data found.");
            return;
        }

        System.out.println("Total rows loaded: " + (rows.size() - 1));
        System.out.println("Total columns: " + rows.get(0).length);

        System.out.println("\nColumn Names:");

        for (String column : rows.get(0)) {

            System.out.println("- " + readableColumnName(column));
        }

        System.out.println("\nFirst 3 Data Rows:");

        for (int i = 1; i <= 3 && i < rows.size(); i++) {

            System.out.println(String.join(" | ", rows.get(i)));
        }
    }

    public DataCleaningResult convertToRecords(List<String[]> rows) {

        DataCleaningReport report = new DataCleaningReport();
        List<SupplyChainRecord> records = new ArrayList<>();

        if (rows.isEmpty()) {

            report.addWarning("No rows found in the CSV file.");
            return new DataCleaningResult(records, report);
        }

        Map<String, Integer> columnIndexes = buildColumnIndexes(rows.get(0), report);
        RequiredColumns columns = resolveRequiredColumns(columnIndexes);

        report.setSourceRows(Math.max(0, rows.size() - 1));
        report.setSourceColumns(rows.get(0).length);
        report.markSensitiveColumnIgnored("Customer Email Address");
        report.markSensitiveColumnIgnored("Customer Password");
        report.markSensitiveColumnIgnored("Customer First Name");
        report.markSensitiveColumnIgnored("Customer Last Name");
        report.markSensitiveColumnIgnored("Customer Street");
        report.markSensitiveColumnIgnored("Customer Zip Code");

        Set<String> uniqueOrderItems = new HashSet<>();
        Set<String> likelyDuplicateOrders = new HashSet<>();

        for (int i = 1; i < rows.size(); i++) {

            String[] cleanedRow = normalizeRowLength(rows.get(i), rows.get(0).length, i + 1, report);
            RowCleaningTracker tracker = new RowCleaningTracker();

            String categoryName = cleanText(
                    valueAt(cleanedRow, columns.categoryName), "Category Name", i + 1, tracker, report);
            String departmentName = cleanText(
                    valueAt(cleanedRow, columns.departmentName), "Department Name", i + 1, tracker, report);
            String productName = cleanText(
                    valueAt(cleanedRow, columns.productName), "Product Name", i + 1, tracker, report);
            String customerCountry = cleanCountry(
                    valueAt(cleanedRow, columns.customerCountry), "Customer Country", i + 1, tracker, report);
            String customerState = cleanText(
                    valueAt(cleanedRow, columns.customerState), "Customer State", i + 1, tracker, report);
            String customerCity = cleanText(
                    valueAt(cleanedRow, columns.customerCity), "Customer City", i + 1, tracker, report);
            String orderCountry = cleanCountry(
                    valueAt(cleanedRow, columns.orderCountry), "Order Country", i + 1, tracker, report);
            String orderState = cleanText(
                    valueAt(cleanedRow, columns.orderState), "Order State", i + 1, tracker, report);
            String orderCity = cleanText(
                    valueAt(cleanedRow, columns.orderCity), "Order City", i + 1, tracker, report);
            String orderRegion = cleanText(
                    valueAt(cleanedRow, columns.orderRegion), "Order Region", i + 1, tracker, report);
            String market = cleanText(
                    valueAt(cleanedRow, columns.market), "Market", i + 1, tracker, report);
            String paymentType = cleanAllowedText(
                    valueAt(cleanedRow, columns.paymentType), "Payment Type", VALID_PAYMENT_TYPES,
                    i + 1, tracker, report);
            String customerSegment = cleanAllowedText(
                    valueAt(cleanedRow, columns.customerSegment), "Customer Segment", VALID_CUSTOMER_SEGMENTS,
                    i + 1, tracker, report);
            String shippingMode = cleanAllowedText(
                    valueAt(cleanedRow, columns.shippingMode), "Shipping Mode", VALID_SHIPPING_MODES,
                    i + 1, tracker, report);
            String deliveryStatus = cleanAllowedText(
                    valueAt(cleanedRow, columns.deliveryStatus), "Delivery Status", VALID_DELIVERY_STATUSES,
                    i + 1, tracker, report);
            String orderStatus = cleanAllowedText(
                    valueAt(cleanedRow, columns.orderStatus), "Order Status", VALID_ORDER_STATUSES,
                    i + 1, tracker, report);

            int orderId = cleanNonNegativeInt(
                    valueAt(cleanedRow, columns.orderId), "Order ID", i + 1, tracker, report);
            int orderItemId = cleanNonNegativeInt(
                    valueAt(cleanedRow, columns.orderItemId), "Order Item ID", i + 1, tracker, report);
            int realShippingDays = cleanNonNegativeInt(
                    valueAt(cleanedRow, columns.realShippingDays), "Days for shipping (real)",
                    i + 1, tracker, report);
            int scheduledShippingDays = cleanNonNegativeInt(
                    valueAt(cleanedRow, columns.scheduledShippingDays), "Days for shipment (scheduled)",
                    i + 1, tracker, report);
            int lateDeliveryRisk = cleanRiskFlag(
                    valueAt(cleanedRow, columns.lateDeliveryRisk), i + 1, tracker, report);
            int orderItemQuantity = cleanNonNegativeInt(
                    valueAt(cleanedRow, columns.orderItemQuantity), "Order Item Quantity",
                    i + 1, tracker, report);
            int productStatus = cleanProductStatus(
                    valueAt(cleanedRow, columns.productStatus), i + 1, tracker, report);

            double sales = cleanDouble(
                    valueAt(cleanedRow, columns.sales), "Sales", i + 1, tracker, report);
            double salesPerCustomer = cleanDouble(
                    valueAt(cleanedRow, columns.salesPerCustomer), "Sales per customer", i + 1, tracker, report);
            double profit = cleanDouble(
                    valueAt(cleanedRow, columns.profit), "Order Profit Per Order", i + 1, tracker, report);
            double benefitPerOrder = cleanDouble(
                    valueAt(cleanedRow, columns.benefitPerOrder), "Benefit per order", i + 1, tracker, report);
            double orderItemTotal = cleanDouble(
                    valueAt(cleanedRow, columns.orderItemTotal), "Order Item Total", i + 1, tracker, report);
            double orderItemDiscount = cleanDouble(
                    valueAt(cleanedRow, columns.orderItemDiscount), "Order Item Discount", i + 1, tracker, report);
            double orderItemDiscountRate = cleanRate(
                    valueAt(cleanedRow, columns.orderItemDiscountRate), "Order Item Discount Rate",
                    i + 1, tracker, report);
            double orderItemProfitRatio = cleanDouble(
                    valueAt(cleanedRow, columns.orderItemProfitRatio), "Order Item Profit Ratio",
                    i + 1, tracker, report);
            double productPrice = cleanDouble(
                    valueAt(cleanedRow, columns.productPrice), "Product Price", i + 1, tracker, report);
            double latitude = cleanLatitude(
                    valueAt(cleanedRow, columns.latitude), i + 1, tracker, report);
            double longitude = cleanLongitude(
                    valueAt(cleanedRow, columns.longitude), i + 1, tracker, report);

            LocalDateTime orderDate = cleanDate(
                    valueAt(cleanedRow, columns.orderDate), "Order Date", i + 1, tracker, report);
            LocalDateTime shippingDate = cleanDate(
                    valueAt(cleanedRow, columns.shippingDate), "Shipping Date", i + 1, tracker, report);

            checkDuplicateOrderItem(orderId, orderItemId, uniqueOrderItems, i + 1, tracker, report);
            checkLikelyDuplicateOrder(
                    productName, customerCountry, customerCity, orderDate, shippingDate, sales,
                    likelyDuplicateOrders, i + 1, tracker, report);
            validateBusinessRules(
                    orderDate, shippingDate, realShippingDays, scheduledShippingDays, lateDeliveryRisk,
                    deliveryStatus, orderStatus, sales, salesPerCustomer, profit, benefitPerOrder,
                    orderItemTotal, orderItemDiscount, orderItemDiscountRate, orderItemProfitRatio,
                    productPrice, orderItemQuantity,
                    latitude, longitude, i + 1, tracker, report);

            records.add(new SupplyChainRecord(
                    categoryName,
                    departmentName,
                    productName,
                    customerCountry,
                    customerState,
                    customerCity,
                    orderCountry,
                    orderState,
                    orderCity,
                    orderRegion,
                    market,
                    paymentType,
                    customerSegment,
                    shippingMode,
                    deliveryStatus,
                    orderStatus,
                    orderId,
                    orderItemId,
                    realShippingDays,
                    scheduledShippingDays,
                    lateDeliveryRisk,
                    orderItemQuantity,
                    productStatus,
                    sales,
                    salesPerCustomer,
                    profit,
                    benefitPerOrder,
                    orderItemTotal,
                    orderItemDiscount,
                    orderItemDiscountRate,
                    orderItemProfitRatio,
                    productPrice,
                    latitude,
                    longitude,
                    orderDate,
                    shippingDate
            ));

            if (tracker.wasCleaned()) {

                report.incrementRowsCleaned();
            }

            if (tracker.wasFlagged()) {

                report.incrementRowsFlagged();
            }
        }

        report.setConvertedRecords(records.size());
        detectStatisticalOutliers(records, report);
        return new DataCleaningResult(records, report);
    }

    private List<String[]> parseCsv(BufferedReader reader) throws IOException {

        List<String[]> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean insideQuotes = false;
        int nextChar;

        while ((nextChar = reader.read()) != -1) {

            char current = (char) nextChar;

            if (current == '"') {

                reader.mark(1);
                int following = reader.read();

                if (insideQuotes && following == '"') {

                    currentField.append('"');
                } else {

                    insideQuotes = !insideQuotes;

                    if (following != -1) {

                        reader.reset();
                    }
                }
            } else if (current == ',' && !insideQuotes) {

                currentRecord.add(currentField.toString());
                currentField.setLength(0);
            } else if ((current == '\n' || current == '\r') && !insideQuotes) {

                if (current == '\r') {

                    reader.mark(1);
                    int following = reader.read();

                    if (following != '\n' && following != -1) {

                        reader.reset();
                    }
                }

                currentRecord.add(currentField.toString());
                records.add(currentRecord.toArray(new String[0]));
                currentRecord = new ArrayList<>();
                currentField.setLength(0);
            } else {

                currentField.append(current);
            }
        }

        if (insideQuotes) {

            currentField.append('"');
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {

            currentRecord.add(currentField.toString());
            records.add(currentRecord.toArray(new String[0]));
        }

        return records;
    }

    private Map<String, Integer> buildColumnIndexes(String[] headers, DataCleaningReport report) {

        Map<String, Integer> columnIndexes = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {

            String cleanedHeader = cleanHeader(headers[i]);
            String normalizedHeader = normalizeName(cleanedHeader);

            if (normalizedHeader.isEmpty()) {

                report.incrementBlankHeaders();
            } else if (columnIndexes.containsKey(normalizedHeader)) {

                report.incrementDuplicateHeaders();
                report.addWarning("Duplicate column name found and ignored: " + cleanedHeader);
            } else {

                columnIndexes.put(normalizedHeader, i);
            }
        }

        return columnIndexes;
    }

    private RequiredColumns resolveRequiredColumns(Map<String, Integer> columnIndexes) {

        return new RequiredColumns(
                requiredIndex(columnIndexes, "Category Name"),
                requiredIndex(columnIndexes, "Department Name"),
                requiredIndex(columnIndexes, "Product Name"),
                requiredIndex(columnIndexes, "Customer Country"),
                requiredIndex(columnIndexes, "Customer State"),
                requiredIndex(columnIndexes, "Customer City"),
                requiredIndex(columnIndexes, "Order Country"),
                requiredIndex(columnIndexes, "Order State"),
                requiredIndex(columnIndexes, "Order City"),
                requiredIndex(columnIndexes, "Order Region"),
                requiredIndex(columnIndexes, "Market"),
                requiredIndex(columnIndexes, "Type"),
                requiredIndex(columnIndexes, "Customer Segment"),
                requiredIndex(columnIndexes, "Shipping Mode"),
                requiredIndex(columnIndexes, "Delivery Status"),
                requiredIndex(columnIndexes, "Order Status"),
                requiredIndex(columnIndexes, "Order Id"),
                requiredIndex(columnIndexes, "Order Item Id"),
                requiredIndex(columnIndexes, "Days for shipping (real)"),
                requiredIndex(columnIndexes, "Days for shipment (scheduled)"),
                requiredIndex(columnIndexes, "Late_delivery_risk"),
                requiredIndex(columnIndexes, "Order Item Quantity"),
                requiredIndex(columnIndexes, "Product Status"),
                requiredIndex(columnIndexes, "Sales"),
                requiredIndex(columnIndexes, "Sales per customer"),
                requiredIndex(columnIndexes, "Order Profit Per Order"),
                requiredIndex(columnIndexes, "Benefit per order"),
                requiredIndex(columnIndexes, "Order Item Total"),
                requiredIndex(columnIndexes, "Order Item Discount"),
                requiredIndex(columnIndexes, "Order Item Discount Rate"),
                requiredIndex(columnIndexes, "Order Item Profit Ratio"),
                requiredIndex(columnIndexes, "Product Price"),
                requiredIndex(columnIndexes, "Latitude"),
                requiredIndex(columnIndexes, "Longitude"),
                requiredIndex(columnIndexes, "order date (DateOrders)"),
                requiredIndex(columnIndexes, "shipping date (DateOrders)")
        );
    }

    private int requiredIndex(Map<String, Integer> columnIndexes, String columnName) {

        Integer index = columnIndexes.get(normalizeName(columnName));

        if (index == null) {

            throw new IllegalArgumentException("Required column missing from dataset: " + columnName);
        }

        return index;
    }

    private String[] normalizeRowLength(
            String[] row,
            int expectedLength,
            int rowNumber,
            DataCleaningReport report) {

        if (row.length == expectedLength) {

            return row;
        }

        report.incrementRowsWithColumnMismatch();
        report.incrementFieldCorrections("Column count");

        String[] normalized = Arrays.copyOf(row, expectedLength);

        if (row.length < expectedLength) {

            Arrays.fill(normalized, row.length, expectedLength, "");
            report.addSampleIssue("Row " + rowNumber + " had missing trailing columns and was padded.");
        } else {

            report.addSampleIssue("Row " + rowNumber + " had extra columns and was trimmed.");
        }

        return normalized;
    }

    private String valueAt(String[] row, int index) {

        if (index < 0 || index >= row.length) {

            return "";
        }

        return row[index];
    }

    private String cleanText(
            String value,
            String fieldName,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        String cleaned = value == null ? "" : value.trim().replaceAll("\\s+", " ");

        if (cleaned.isEmpty()) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had blank " + fieldName + "; set to UNKNOWN.");
            return UNKNOWN_VALUE;
        }

        if (!cleaned.equals(value)) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
        }

        return cleaned;
    }

    private String cleanCountry(
            String value,
            String fieldName,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        String country = cleanText(value, fieldName, rowNumber, tracker, report);

        if ("EE. UU.".equalsIgnoreCase(country) || "USA".equalsIgnoreCase(country)
                || "US".equalsIgnoreCase(country) || "United States of America".equalsIgnoreCase(country)) {

            if (!"United States".equals(country)) {

                tracker.markCleaned();
                report.incrementFieldCorrections(fieldName);
                report.addSampleIssue("Row " + rowNumber + " normalized " + fieldName
                        + " from '" + country + "' to 'United States'.");
            }

            return "United States";
        }

        String translatedCountry = COUNTRY_TRANSLATIONS.get(normalizeCountryKey(country));

        if (translatedCountry == null) {

            return country;
        }

        if (!translatedCountry.equals(country)) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " translated " + fieldName
                    + " from '" + country + "' to '" + translatedCountry + "'.");
        }

        return translatedCountry;
    }

    private String cleanAllowedText(
            String value,
            String fieldName,
            Set<String> allowedValues,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        String cleaned = cleanText(value, fieldName, rowNumber, tracker, report);

        for (String allowedValue : allowedValues) {

            if (allowedValue.equalsIgnoreCase(cleaned)) {

                return allowedValue;
            }
        }

        tracker.markCleaned();
        report.incrementFieldCorrections(fieldName);
        report.addSampleIssue("Row " + rowNumber + " had invalid " + fieldName + "; set to UNKNOWN.");
        return UNKNOWN_VALUE;
    }

    private int cleanNonNegativeInt(
            String value,
            String fieldName,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        Integer parsed = parseInteger(value);

        if (parsed == null) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had invalid " + fieldName + "; set to 0.");
            return 0;
        }

        if (parsed < 0) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had negative " + fieldName + "; set to 0.");
            return 0;
        }

        return parsed;
    }

    private int cleanRiskFlag(
            String value,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        Integer parsed = parseInteger(value);

        if (parsed == null) {

            tracker.markCleaned();
            report.incrementFieldCorrections("Late Delivery Risk");
            report.addSampleIssue("Row " + rowNumber + " had invalid Late Delivery Risk; set to 0.");
            return 0;
        }

        if (parsed == 0 || parsed == 1) {

            return parsed;
        }

        tracker.markCleaned();
        report.incrementFieldCorrections("Late Delivery Risk");
        report.addSampleIssue("Row " + rowNumber + " had Late Delivery Risk outside 0/1; normalized.");
        return parsed > 0 ? 1 : 0;
    }

    private int cleanProductStatus(
            String value,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        Integer parsed = parseInteger(value);

        if (parsed == null) {

            tracker.markCleaned();
            report.incrementFieldCorrections("Product Status");
            report.addSampleIssue("Row " + rowNumber + " had invalid Product Status; set to 0.");
            return 0;
        }

        if (parsed == 0 || parsed == 1) {

            return parsed;
        }

        tracker.markCleaned();
        report.incrementFieldCorrections("Product Status");
        report.addSampleIssue("Row " + rowNumber + " had Product Status outside 0/1; normalized.");
        return parsed > 0 ? 1 : 0;
    }

    private double cleanDouble(
            String value,
            String fieldName,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        Double parsed = parseDouble(value);

        if (parsed == null || parsed.isNaN() || parsed.isInfinite()) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had invalid " + fieldName + "; set to 0.0.");
            return 0.0;
        }

        return parsed;
    }

    private double cleanRate(
            String value,
            String fieldName,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        double rate = cleanDouble(value, fieldName, rowNumber, tracker, report);

        if (rate < 0.0) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had negative " + fieldName + "; set to 0.0.");
            return 0.0;
        }

        if (rate > 1.0) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had " + fieldName + " above 1; set to 1.0.");
            return 1.0;
        }

        return rate;
    }

    private double cleanLatitude(
            String value,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        double latitude = cleanDouble(value, "Latitude", rowNumber, tracker, report);

        if (latitude < -90 || latitude > 90) {

            tracker.markCleaned();
            report.incrementFieldCorrections("Latitude");
            report.addSampleIssue("Row " + rowNumber + " had Latitude outside -90 to 90; set to 0.0.");
            return 0.0;
        }

        return latitude;
    }

    private double cleanLongitude(
            String value,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        double longitude = cleanDouble(value, "Longitude", rowNumber, tracker, report);

        if (longitude < -180 || longitude > 180) {

            tracker.markCleaned();
            report.incrementFieldCorrections("Longitude");
            report.addSampleIssue("Row " + rowNumber + " had Longitude outside -180 to 180; set to 0.0.");
            return 0.0;
        }

        return longitude;
    }

    private LocalDateTime cleanDate(
            String value,
            String fieldName,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        String cleaned = value == null ? "" : value.trim();

        if (cleaned.isEmpty()) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had blank " + fieldName + "; set to null.");
            return null;
        }

        try {

            return LocalDateTime.parse(cleaned, DATE_FORMATTER);
        } catch (DateTimeParseException e) {

            tracker.markCleaned();
            report.incrementFieldCorrections(fieldName);
            report.addSampleIssue("Row " + rowNumber + " had invalid " + fieldName + "; set to null.");
            return null;
        }
    }

    private void checkDuplicateOrderItem(
            int orderId,
            int orderItemId,
            Set<String> uniqueOrderItems,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        String key = orderId + "-" + orderItemId;

        if (!uniqueOrderItems.add(key)) {

            tracker.markFlagged();
            report.incrementDuplicateOrderItems();
            report.addSampleIssue("Row " + rowNumber
                    + " duplicated Order ID + Order Item ID: " + key + ".");
        }
    }

    private void checkLikelyDuplicateOrder(
            String productName,
            String customerCountry,
            String customerCity,
            LocalDateTime orderDate,
            LocalDateTime shippingDate,
            double sales,
            Set<String> likelyDuplicateOrders,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        String key = productName + "|" + customerCountry + "|" + customerCity + "|"
                + orderDate + "|" + shippingDate + "|" + sales;

        if (!likelyDuplicateOrders.add(key)) {

            tracker.markFlagged();
            report.incrementLikelyDuplicateOrders();
            report.addSampleIssue("Row " + rowNumber + " looks like a duplicate based on product, customer, dates, and sales.");
        }
    }

    private void validateBusinessRules(
            LocalDateTime orderDate,
            LocalDateTime shippingDate,
            int realShippingDays,
            int scheduledShippingDays,
            int lateDeliveryRisk,
            String deliveryStatus,
            String orderStatus,
            double sales,
            double salesPerCustomer,
            double profit,
            double benefitPerOrder,
            double orderItemTotal,
            double orderItemDiscount,
            double orderItemDiscountRate,
            double orderItemProfitRatio,
            double productPrice,
            int orderItemQuantity,
            double latitude,
            double longitude,
            int rowNumber,
            RowCleaningTracker tracker,
            DataCleaningReport report) {

        if (orderDate != null && shippingDate != null) {

            if (shippingDate.isBefore(orderDate)) {

                tracker.markFlagged();
                report.incrementBusinessRuleWarnings();
                report.addSampleIssue("Row " + rowNumber + " has shipping date before order date.");
            }

            long dateDifference = ChronoUnit.DAYS.between(orderDate.toLocalDate(), shippingDate.toLocalDate());

            if (dateDifference != realShippingDays) {

                tracker.markFlagged();
                report.incrementBusinessRuleWarnings();
                report.addSampleIssue("Row " + rowNumber + " shipping days does not match order/shipping dates.");
            }
        }

        if (realShippingDays > scheduledShippingDays && lateDeliveryRisk == 0) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " looks late but Late Delivery Risk is 0.");
        }

        if (realShippingDays <= scheduledShippingDays && lateDeliveryRisk == 1
                && !"Late delivery".equals(deliveryStatus)) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " has late risk 1 but shipping days do not look late.");
        }

        if ("Shipping canceled".equals(deliveryStatus) && !"CANCELED".equals(orderStatus)) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " has canceled delivery but Order Status is not CANCELED.");
        }

        if (sales < 0 || orderItemTotal < 0 || productPrice < 0) {

            tracker.markFlagged();
            report.incrementOutlierWarnings();
            report.addSampleIssue("Row " + rowNumber + " has a negative amount where a positive amount is expected.");
        }

        if (orderItemQuantity == 0 && sales > 0) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " has sales but zero quantity.");
        }

        if (sales > 0 && Math.abs(profit) > sales) {

            tracker.markFlagged();
            report.incrementOutlierWarnings();
            report.addSampleIssue("Row " + rowNumber + " has profit/loss larger than sales.");
        }

        if (sales > cleaningConfig.getMaximumSales()
                || Math.abs(profit) > cleaningConfig.getMaximumProfitLoss()
                || realShippingDays > cleaningConfig.getMaximumShippingDays()) {

            tracker.markFlagged();
            report.incrementOutlierWarnings();
            report.addSampleIssue("Row " + rowNumber + " has an unusually large sales/profit/shipping value.");
        }

        if (latitude == 0.0 && longitude == 0.0) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " has empty-looking coordinates.");
        }

        if (Math.abs(profit - benefitPerOrder) > 0.01) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " profit does not match Benefit per order.");
        }

        if (Math.abs(orderItemTotal - salesPerCustomer) > 0.01) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " Order Item Total does not match Sales per customer.");
        }

        if (sales > 0 && Math.abs((sales - orderItemDiscount) - orderItemTotal) > 0.05) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " discount math does not match Order Item Total.");
        }

        if (orderItemDiscountRate > 0 && Math.abs((sales * orderItemDiscountRate) - orderItemDiscount) > 0.05) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " discount amount does not match discount rate.");
        }

        if (orderItemTotal != 0.0 && Math.abs((profit / orderItemTotal) - orderItemProfitRatio) > 0.05) {

            tracker.markFlagged();
            report.incrementBusinessRuleWarnings();
            report.addSampleIssue("Row " + rowNumber + " profit ratio does not match profit and total.");
        }
    }

    private void detectStatisticalOutliers(List<SupplyChainRecord> records, DataCleaningReport report) {

        detectOutliersForField(records, report, "Sales");
        detectOutliersForField(records, report, "Profit");
        detectOutliersForField(records, report, "Real Shipping Days");
    }

    private void detectOutliersForField(
            List<SupplyChainRecord> records,
            DataCleaningReport report,
            String fieldName) {

        if (records.isEmpty()) {

            return;
        }

        double sum = 0.0;

        for (SupplyChainRecord record : records) {

            sum += getNumericValue(record, fieldName);
        }

        double average = sum / records.size();
        double varianceSum = 0.0;

        for (SupplyChainRecord record : records) {

            double difference = getNumericValue(record, fieldName) - average;
            varianceSum += difference * difference;
        }

        double standardDeviation = Math.sqrt(varianceSum / records.size());

        if (standardDeviation == 0.0) {

            return;
        }

        int outlierCount = 0;

        for (SupplyChainRecord record : records) {

            double zScore = Math.abs((getNumericValue(record, fieldName) - average) / standardDeviation);

            if (zScore > cleaningConfig.getStatisticalOutlierZScore()) {

                outlierCount++;
            }
        }

        if (outlierCount > 0) {

            report.addStatisticalOutliers(fieldName, outlierCount);
        }
    }

    private double getNumericValue(SupplyChainRecord record, String fieldName) {

        if ("Sales".equals(fieldName)) {

            return record.getSales();
        }

        if ("Profit".equals(fieldName)) {

            return record.getProfit();
        }

        if ("Real Shipping Days".equals(fieldName)) {

            return record.getRealShippingDays();
        }

        return 0.0;
    }

    private Integer parseInteger(String value) {

        if (value == null) {

            return null;
        }

        String cleaned = value.trim().replace(",", "");

        if (cleaned.isEmpty()) {

            return null;
        }

        try {

            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {

            try {

                return (int) Math.round(Double.parseDouble(cleaned));
            } catch (NumberFormatException ignored) {

                return null;
            }
        }
    }

    private Double parseDouble(String value) {

        if (value == null) {

            return null;
        }

        String cleaned = value.trim()
                .replace("$", "")
                .replace("%", "")
                .replace(",", "");

        if (cleaned.isEmpty()) {

            return null;
        }

        try {

            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {

            return null;
        }
    }

    private String cleanHeader(String header) {

        if (header == null) {

            return "";
        }

        return header.trim().replaceAll("\\s+", " ");
    }

    private String readableColumnName(String columnName) {

        Map<String, String> readableNames = new HashMap<>();
        readableNames.put("Customer Fname", "Customer First Name");
        readableNames.put("Customer Lname", "Customer Last Name");
        readableNames.put("Customer Email", "Customer Email Address");
        readableNames.put("Customer Zipcode", "Customer Zip Code");
        readableNames.put("Order Zipcode", "Order Zip Code");
        readableNames.put("Order Id", "Order ID");
        readableNames.put("Order Item Id", "Order Item ID");
        readableNames.put("Customer Id", "Customer ID");
        readableNames.put("Order Customer Id", "Order Customer ID");
        readableNames.put("Category Id", "Category ID");
        readableNames.put("Department Id", "Department ID");
        readableNames.put("Product Card Id", "Product Card ID");
        readableNames.put("Product Category Id", "Product Category ID");
        readableNames.put("Order Item Cardprod Id", "Order Item Card Product ID");
        readableNames.put("Late_delivery_risk", "Late Delivery Risk");
        readableNames.put("Type", "Payment Type");
        readableNames.put("order date (DateOrders)", "Order Date");
        readableNames.put("shipping date (DateOrders)", "Shipping Date");

        return readableNames.getOrDefault(columnName, columnName);
    }

    private String normalizeName(String value) {

        return cleanHeader(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    private static String normalizeCountryKey(String value) {

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static Map<String, String> buildCountryTranslations() {

        Map<String, String> countries = new HashMap<>();

        countries.put("afganistan", "Afghanistan");
        countries.put("alemania", "Germany");
        countries.put("arabia saudi", "Saudi Arabia");
        countries.put("argelia", "Algeria");
        countries.put("azerbaiyan", "Azerbaijan");
        countries.put("banglades", "Bangladesh");
        countries.put("barein", "Bahrain");
        countries.put("belgica", "Belgium");
        countries.put("belice", "Belize");
        countries.put("benin", "Benin");
        countries.put("bielorrusia", "Belarus");
        countries.put("bosnia y herzegovina", "Bosnia and Herzegovina");
        countries.put("botsuana", "Botswana");
        countries.put("brasil", "Brazil");
        countries.put("butan", "Bhutan");
        countries.put("camboya", "Cambodia");
        countries.put("camerun", "Cameroon");
        countries.put("chipre", "Cyprus");
        countries.put("corea del sur", "South Korea");
        countries.put("costa de marfil", "Ivory Coast");
        countries.put("croacia", "Croatia");
        countries.put("dinamarca", "Denmark");
        countries.put("ee uu", "United States");
        countries.put("emiratos arabes unidos", "United Arab Emirates");
        countries.put("egipto", "Egypt");
        countries.put("eslovaquia", "Slovakia");
        countries.put("eslovenia", "Slovenia");
        countries.put("espana", "Spain");
        countries.put("estados unidos", "United States");
        countries.put("etiopia", "Ethiopia");
        countries.put("filipinas", "Philippines");
        countries.put("finlandia", "Finland");
        countries.put("francia", "France");
        countries.put("gabon", "Gabon");
        countries.put("grecia", "Greece");
        countries.put("guadalupe", "Guadeloupe");
        countries.put("guayana francesa", "French Guiana");
        countries.put("guinea ecuatorial", "Equatorial Guinea");
        countries.put("haiti", "Haiti");
        countries.put("hungria", "Hungary");
        countries.put("irak", "Iraq");
        countries.put("iran", "Iran");
        countries.put("irlanda", "Ireland");
        countries.put("italia", "Italy");
        countries.put("japon", "Japan");
        countries.put("jordania", "Jordan");
        countries.put("kazajistan", "Kazakhstan");
        countries.put("kenia", "Kenya");
        countries.put("kirguistan", "Kyrgyzstan");
        countries.put("lesoto", "Lesotho");
        countries.put("libano", "Lebanon");
        countries.put("libia", "Libya");
        countries.put("lituania", "Lithuania");
        countries.put("luxemburgo", "Luxembourg");
        countries.put("macedonia", "North Macedonia");
        countries.put("malasia", "Malaysia");
        countries.put("marruecos", "Morocco");
        countries.put("martinica", "Martinique");
        countries.put("mexico", "Mexico");
        countries.put("moldavia", "Moldova");
        countries.put("myanmar birmania", "Myanmar");
        countries.put("niger", "Niger");
        countries.put("noruega", "Norway");
        countries.put("nueva zelanda", "New Zealand");
        countries.put("oman", "Oman");
        countries.put("paises bajos", "Netherlands");
        countries.put("pakistan", "Pakistan");
        countries.put("panama", "Panama");
        countries.put("papua nueva guinea", "Papua New Guinea");
        countries.put("peru", "Peru");
        countries.put("polonia", "Poland");
        countries.put("reino unido", "United Kingdom");
        countries.put("republica centroafricana", "Central African Republic");
        countries.put("republica checa", "Czech Republic");
        countries.put("republica de gambia", "Gambia");
        countries.put("republica del congo", "Republic of the Congo");
        countries.put("republica democratica del congo", "Democratic Republic of the Congo");
        countries.put("republica dominicana", "Dominican Republic");
        countries.put("ruanda", "Rwanda");
        countries.put("rumania", "Romania");
        countries.put("rusia", "Russia");
        countries.put("sahara occidental", "Western Sahara");
        countries.put("sierra leona", "Sierra Leone");
        countries.put("singapur", "Singapore");
        countries.put("siria", "Syria");
        countries.put("suazilandia", "Eswatini");
        countries.put("sudafrica", "South Africa");
        countries.put("sudan", "Sudan");
        countries.put("sudan del sur", "South Sudan");
        countries.put("suecia", "Sweden");
        countries.put("suiza", "Switzerland");
        countries.put("surinam", "Suriname");
        countries.put("tailandia", "Thailand");
        countries.put("taiwan", "Taiwan");
        countries.put("tayikistan", "Tajikistan");
        countries.put("trinidad y tobago", "Trinidad and Tobago");
        countries.put("tunez", "Tunisia");
        countries.put("turkmenistan", "Turkmenistan");
        countries.put("turquia", "Turkey");
        countries.put("ucrania", "Ukraine");
        countries.put("uzbekistan", "Uzbekistan");
        countries.put("yibuti", "Djibouti");
        countries.put("zimbabue", "Zimbabwe");

        return countries;
    }

    private static class RequiredColumns {

        private final int categoryName;
        private final int departmentName;
        private final int productName;
        private final int customerCountry;
        private final int customerState;
        private final int customerCity;
        private final int orderCountry;
        private final int orderState;
        private final int orderCity;
        private final int orderRegion;
        private final int market;
        private final int paymentType;
        private final int customerSegment;
        private final int shippingMode;
        private final int deliveryStatus;
        private final int orderStatus;
        private final int orderId;
        private final int orderItemId;
        private final int realShippingDays;
        private final int scheduledShippingDays;
        private final int lateDeliveryRisk;
        private final int orderItemQuantity;
        private final int productStatus;
        private final int sales;
        private final int salesPerCustomer;
        private final int profit;
        private final int benefitPerOrder;
        private final int orderItemTotal;
        private final int orderItemDiscount;
        private final int orderItemDiscountRate;
        private final int orderItemProfitRatio;
        private final int productPrice;
        private final int latitude;
        private final int longitude;
        private final int orderDate;
        private final int shippingDate;

        private RequiredColumns(
                int categoryName,
                int departmentName,
                int productName,
                int customerCountry,
                int customerState,
                int customerCity,
                int orderCountry,
                int orderState,
                int orderCity,
                int orderRegion,
                int market,
                int paymentType,
                int customerSegment,
                int shippingMode,
                int deliveryStatus,
                int orderStatus,
                int orderId,
                int orderItemId,
                int realShippingDays,
                int scheduledShippingDays,
                int lateDeliveryRisk,
                int orderItemQuantity,
                int productStatus,
                int sales,
                int salesPerCustomer,
                int profit,
                int benefitPerOrder,
                int orderItemTotal,
                int orderItemDiscount,
                int orderItemDiscountRate,
                int orderItemProfitRatio,
                int productPrice,
                int latitude,
                int longitude,
                int orderDate,
                int shippingDate) {

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
    }

    private static class RowCleaningTracker {

        private boolean cleaned;
        private boolean flagged;

        private void markCleaned() {

            cleaned = true;
        }

        private boolean wasCleaned() {

            return cleaned;
        }

        private void markFlagged() {

            flagged = true;
        }

        private boolean wasFlagged() {

            return flagged;
        }
    }

    public static class DataCleaningResult {

        private final List<SupplyChainRecord> records;
        private final DataCleaningReport report;

        public DataCleaningResult(List<SupplyChainRecord> records, DataCleaningReport report) {

            this.records = records;
            this.report = report;
        }

        public List<SupplyChainRecord> getRecords() {

            return records;
        }

        public DataCleaningReport getReport() {

            return report;
        }
    }

    public static class DataCleaningReport {

        private int sourceRows;
        private int sourceColumns;
        private int convertedRecords;
        private int rowsCleaned;
        private int rowsFlagged;
        private int rowsWithColumnMismatch;
        private int blankHeaders;
        private int duplicateHeaders;
        private int duplicateOrderItems;
        private int likelyDuplicateOrders;
        private int businessRuleWarnings;
        private int outlierWarnings;
        private final Map<String, Integer> fieldCorrections = new HashMap<>();
        private final Map<String, Integer> statisticalOutliers = new HashMap<>();
        private final List<String> sampleIssues = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final List<String> ignoredSensitiveColumns = new ArrayList<>();

        private void setSourceRows(int sourceRows) {

            this.sourceRows = sourceRows;
        }

        private void setSourceColumns(int sourceColumns) {

            this.sourceColumns = sourceColumns;
        }

        private void setConvertedRecords(int convertedRecords) {

            this.convertedRecords = convertedRecords;
        }

        private void incrementRowsCleaned() {

            rowsCleaned++;
        }

        private void incrementRowsFlagged() {

            rowsFlagged++;
        }

        private void incrementRowsWithColumnMismatch() {

            rowsWithColumnMismatch++;
        }

        private void incrementBlankHeaders() {

            blankHeaders++;
        }

        private void incrementDuplicateHeaders() {

            duplicateHeaders++;
        }

        private void incrementDuplicateOrderItems() {

            duplicateOrderItems++;
        }

        private void incrementLikelyDuplicateOrders() {

            likelyDuplicateOrders++;
        }

        private void incrementBusinessRuleWarnings() {

            businessRuleWarnings++;
        }

        private void incrementOutlierWarnings() {

            outlierWarnings++;
        }

        private void incrementFieldCorrections(String fieldName) {

            fieldCorrections.put(fieldName, fieldCorrections.getOrDefault(fieldName, 0) + 1);
        }

        private void markSensitiveColumnIgnored(String columnName) {

            ignoredSensitiveColumns.add(columnName);
        }

        private void addStatisticalOutliers(String fieldName, int count) {

            statisticalOutliers.put(fieldName, count);
            outlierWarnings += count;
        }

        private void addSampleIssue(String issue) {

            if (sampleIssues.size() < 10) {

                sampleIssues.add(issue);
            }
        }

        private void addWarning(String warning) {

            warnings.add(warning);
        }

        public int getRowsCleaned() {

            return rowsCleaned;
        }

        public void printReport() {

            System.out.println("\nData Cleaning Report:");
            System.out.println("Source rows: " + sourceRows);
            System.out.println("Source columns: " + sourceColumns);
            System.out.println("Converted records: " + convertedRecords);
            System.out.println("Rows corrected/standardized: " + rowsCleaned);
            System.out.println("Rows flagged for review: " + rowsFlagged);
            System.out.println("Rows with column-count fixes: " + rowsWithColumnMismatch);
            System.out.println("Blank headers found: " + blankHeaders);
            System.out.println("Duplicate headers ignored: " + duplicateHeaders);
            System.out.println("Duplicate order items found: " + duplicateOrderItems);
            System.out.println("Likely duplicate orders found: " + likelyDuplicateOrders);
            System.out.println("Business rule warnings: " + businessRuleWarnings);
            System.out.println("Outlier warnings: " + outlierWarnings);

            if (!ignoredSensitiveColumns.isEmpty()) {

                System.out.println("Sensitive columns ignored: " + String.join(", ", ignoredSensitiveColumns));
            }

            if (fieldCorrections.isEmpty()) {

                System.out.println("Field corrections: none");
            } else {

                System.out.println("Field corrections:");

                fieldCorrections.keySet().stream().sorted().forEach(field -> {

                    System.out.println("- " + field + ": " + fieldCorrections.get(field));
                });
            }

            if (!sampleIssues.isEmpty()) {

                System.out.println("Sample cleaning actions:");

                for (String issue : sampleIssues) {

                    System.out.println("- " + issue);
                }
            }

            if (!statisticalOutliers.isEmpty()) {

                System.out.println("Statistical outliers:");

                statisticalOutliers.keySet().stream().sorted().forEach(field -> {

                    System.out.println("- " + field + ": " + statisticalOutliers.get(field));
                });
            }

            if (!warnings.isEmpty()) {

                System.out.println("Warnings:");

                for (String warning : warnings) {

                    System.out.println("- " + warning);
                }
            }
        }
    }
}
