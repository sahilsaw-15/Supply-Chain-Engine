package com.supplychain.dashboard;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainAlert;
import com.supplychain.model.SupplyChainRecord;
import com.supplychain.export.ReportExporter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class SupplyChainDashboard extends JFrame {

    private static final int TABLE_LIMIT = 10;
    private static final int MINIMUM_ACTUAL_FOR_MAPE = 10;
    private static final Color BACKGROUND_TOP = new Color(255, 255, 255);
    private static final Color BACKGROUND_BOTTOM = new Color(232, 235, 241);
    private static final Color CARD_BACKGROUND = Color.WHITE;
    private static final Color BORDER = new Color(215, 220, 228);
    private static final Color HEADER = new Color(24, 24, 27);
    private static final Color MUTED = new Color(92, 99, 112);
    private static final Color TABLE_HEADER = new Color(34, 34, 37);
    private static final Color TABLE_TEXT = new Color(31, 41, 55);
    private static final Color TABLE_BACKGROUND = new Color(255, 255, 255);
    private static final Color TABLE_SELECTION = new Color(222, 229, 239);

    private final List<SupplyChainRecord> records;
    private final List<ForecastResult> forecastResults;
    private final List<RiskIntelligenceResult> riskResults;
    private final List<SupplyChainAlert> alerts;
    private final NumberFormat integerFormat = NumberFormat.getIntegerInstance(Locale.US);
    private JPanel rootPanel;
    private JScrollPane dashboardContent;
    private JComboBox<String> productFilter;
    private JComboBox<String> categoryFilter;
    private JComboBox<String> countryFilter;

    public SupplyChainDashboard(
            List<SupplyChainRecord> records,
            List<DemandRecord> demandRecords,
            List<ForecastResult> forecastResults,
            List<RiskIntelligenceResult> riskResults,
            List<SupplyChainAlert> alerts) {

        this.records = records;
        this.forecastResults = forecastResults;
        this.riskResults = riskResults;
        this.alerts = alerts;

        setTitle("Supply Chain Analytics Dashboard");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 760));
        setLocationRelativeTo(null);
        buildDashboard();
    }

    private void buildDashboard() {

        rootPanel = new GradientPanel();
        rootPanel.setLayout(new BorderLayout(14, 14));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        rootPanel.add(titlePanel(), BorderLayout.NORTH);
        dashboardContent = contentPanel();
        rootPanel.add(dashboardContent, BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    private JPanel titlePanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 4));
        titleBlock.setOpaque(false);

        JLabel title = new JLabel("SUPPLY CHAIN ANALYTICS DASHBOARD");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(HEADER);
        titleBlock.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Forecasting | Alerts | Risk Intelligence");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        titleBlock.add(subtitle, BorderLayout.SOUTH);

        panel.add(titleBlock, BorderLayout.NORTH);
        panel.add(searchAndExportPanel(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel searchAndExportPanel() {

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchPanel.setOpaque(false);
        productFilter = dropdown(productOptions());
        categoryFilter = dropdown(categoryOptions());
        countryFilter = dropdown(countryOptions());
        searchPanel.add(new JLabel("Product"));
        searchPanel.add(productFilter);
        searchPanel.add(new JLabel("Category"));
        searchPanel.add(categoryFilter);
        searchPanel.add(new JLabel("Country"));
        searchPanel.add(countryFilter);

        JButton applyButton = button("Apply");
        applyButton.addActionListener(event -> refreshDashboard());
        JButton clearButton = button("Clear");
        clearButton.addActionListener(event -> clearSearch());
        searchPanel.add(applyButton);
        searchPanel.add(clearButton);

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        exportPanel.setOpaque(false);
        JButton forecastButton = button("Export Forecast Report");
        forecastButton.addActionListener(event -> exportForecastReport());
        JButton riskButton = button("Export Risk Report");
        riskButton.addActionListener(event -> exportRiskReport());
        JButton alertButton = button("Export Alert Report");
        alertButton.addActionListener(event -> exportAlertReport());
        exportPanel.add(forecastButton);
        exportPanel.add(riskButton);
        exportPanel.add(alertButton);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(exportPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JComboBox<String> dropdown(String[] options) {

        JComboBox<String> comboBox = new JComboBox<>(options);
        comboBox.setPreferredSize(new Dimension(150, 28));
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return comboBox;
    }

    private JButton button(String text) {

        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        return button;
    }

    private String[] productOptions() {

        Set<String> options = new LinkedHashSet<>();
        options.add("All");
        forecastResults.stream()
                .map(ForecastResult::getProductName)
                .filter(value -> value != null && !value.isBlank())
                .sorted()
                .forEach(options::add);
        return options.toArray(new String[0]);
    }

    private String[] categoryOptions() {

        Set<String> options = new LinkedHashSet<>();
        options.add("All");
        forecastResults.stream()
                .map(ForecastResult::getCategoryName)
                .filter(value -> value != null && !value.isBlank())
                .sorted()
                .forEach(options::add);
        return options.toArray(new String[0]);
    }

    private String[] countryOptions() {

        Set<String> options = new LinkedHashSet<>();
        options.add("All");
        records.stream()
                .map(SupplyChainRecord::getOrderCountry)
                .filter(value -> value != null && !value.isBlank())
                .sorted()
                .forEach(options::add);
        return options.toArray(new String[0]);
    }

    private void refreshDashboard() {

        rootPanel.remove(dashboardContent);
        dashboardContent = contentPanel();
        rootPanel.add(dashboardContent, BorderLayout.CENTER);
        rootPanel.revalidate();
        rootPanel.repaint();
    }

    private void clearSearch() {

        productFilter.setSelectedItem("All");
        categoryFilter.setSelectedItem("All");
        countryFilter.setSelectedItem("All");
        refreshDashboard();
    }

    private JScrollPane contentPanel() {

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        List<ForecastResult> filteredForecasts = filteredForecastResults();
        List<RiskIntelligenceResult> filteredRisks = filteredRiskResults();
        List<SupplyChainAlert> filteredAlerts = filteredAlerts();
        List<ForecastResult> latestForecasts = latestForecasts(filteredForecasts);
        ForecastMetrics metrics = calculateForecastMetrics(filteredForecasts);
        List<CriticalAlertSummary> criticalAlerts = criticalAlerts(filteredAlerts);
        RiskCounts riskCounts = riskCounts(filteredRisks);

        addSection(content, kpiPanel(latestForecasts.size(), criticalAlerts.size(), riskCounts.highRiskProducts(), metrics.r2));
        addSection(content, forecastTrendSection(filteredForecasts));
        addSection(content, section("Forecast Performance", metricsRows(metrics), 190, true));
        addSection(content, section("Top 10 Forecasted Products", topForecastRows(latestForecasts), 330, true));
        addSection(content, summaryGridSection("Alert Distribution", alertDistributionRows(filteredAlerts)));
        addSection(content, section("Critical Alerts", criticalAlertRows(criticalAlerts), 330));
        addSection(content, summaryGridSection(
                "Demand Trend Overview",
                new String[][] {
                        {"Increasing Products", integerFormat.format(trendCounts(latestForecasts).increasing)},
                        {"Stable Products", integerFormat.format(trendCounts(latestForecasts).stable)},
                        {"Declining Products", integerFormat.format(trendCounts(latestForecasts).declining)}
                }));
        addSection(content, summaryGridSection(
                "Risk Overview",
                new String[][] {
                        {"High Risk Products", integerFormat.format(riskCounts.high)},
                        {"Medium Risk Products", integerFormat.format(riskCounts.medium)},
                        {"Low Risk Products", integerFormat.format(riskCounts.low)}
                }));

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private void addSection(JPanel content, JPanel section) {

        section.setAlignmentX(LEFT_ALIGNMENT);
        content.add(section);
        content.add(Box.createVerticalStrut(14));
    }

    private JPanel kpiPanel(int productsForecasted, int criticalAlertCount, int highRiskProducts, double r2) {

        JPanel panel = new JPanel(new GridLayout(1, 4, 12, 12));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));
        panel.setPreferredSize(new Dimension(1000, 116));

        panel.add(kpiCard("Products Forecasted", integerFormat.format(productsForecasted)));
        panel.add(kpiCard("Critical Alerts", integerFormat.format(criticalAlertCount)));
        panel.add(kpiCard("High Risk Products", integerFormat.format(highRiskProducts)));
        panel.add(kpiCard("Forecast Accuracy", "R2 " + String.format(Locale.US, "%.4f", r2)));

        return panel;
    }

    private JPanel kpiCard(String label, String value) {

        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(CARD_BACKGROUND);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(HEADER);

        JLabel labelLabel = new JLabel(label, SwingConstants.CENTER);
        labelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        labelLabel.setForeground(MUTED);

        card.add(valueLabel, BorderLayout.CENTER);
        card.add(labelLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel section(String title, String[][] rows, int height) {

        return section(title, rows, height, false);
    }

    private JPanel section(String title, String[][] rows, int height, boolean centerColumns) {

        JPanel panel = baseCard(title);
        JTable table = table(rows, centerColumns);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(232, 235, 240)));
        scrollPane.getViewport().setBackground(TABLE_BACKGROUND);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        panel.setPreferredSize(new Dimension(1000, height));
        return panel;
    }

    private JPanel summaryGridSection(String title, String[][] values) {

        JPanel panel = baseCard(title);
        JPanel grid = new JPanel(new GridLayout(1, values.length, 12, 12));
        grid.setOpaque(false);

        for (String[] value : values) {

            JPanel summary = new JPanel(new BorderLayout(4, 4));
            summary.setBackground(new Color(247, 248, 250));
            summary.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(228, 232, 238)),
                    BorderFactory.createEmptyBorder(14, 14, 14, 14)));

            JLabel number = new JLabel(value[1], SwingConstants.CENTER);
            number.setFont(new Font("Segoe UI", Font.BOLD, 26));
            number.setForeground(HEADER);

            JLabel label = new JLabel(value[0], SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            label.setForeground(MUTED);

            summary.add(number, BorderLayout.CENTER);
            summary.add(label, BorderLayout.SOUTH);
            grid.add(summary);
        }

        panel.add(grid, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        panel.setPreferredSize(new Dimension(1000, 170));
        return panel;
    }

    private JPanel forecastTrendSection(List<ForecastResult> filteredForecasts) {

        JPanel panel = baseCard("Forecast Trend");
        panel.add(new ForecastTrendChart(monthlyForecastTrend(filteredForecasts)), BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));
        panel.setPreferredSize(new Dimension(1000, 340));
        return panel;
    }

    private JPanel baseCard(String title) {

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 17));
        label.setForeground(HEADER);
        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private JTable table(String[][] rows, boolean centerColumns) {

        String[] headers = rows[0];
        String[][] body = new String[rows.length - 1][headers.length];
        System.arraycopy(rows, 1, body, 0, rows.length - 1);

        DefaultTableModel model = new DefaultTableModel(body, headers) {

            @Override
            public boolean isCellEditable(int row, int column) {

                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TABLE_TEXT);
        table.setBackground(TABLE_BACKGROUND);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(new Color(229, 232, 238));
        table.setSelectionBackground(TABLE_SELECTION);
        table.setSelectionForeground(HEADER);
        table.setShowVerticalLines(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setReorderingAllowed(false);

        applyColumnWidths(table);
        applyRenderers(table, centerColumns);
        return table;
    }

    private void applyColumnWidths(JTable table) {

        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {

            String header = table.getColumnName(columnIndex);
            TableColumn column = table.getColumnModel().getColumn(columnIndex);
            int width = preferredColumnWidth(header);
            column.setPreferredWidth(width);
            column.setMinWidth(Math.min(width, 90));
        }
    }

    private int preferredColumnWidth(String header) {

        return switch (header) {
            case "Product" -> 300;
            case "Reason" -> 430;
            case "Alert Type" -> 230;
            case "Severity" -> 110;
            case "Metric" -> 360;
            case "Value" -> 220;
            case "Forecast" -> 220;
            default -> 130;
        };
    }

    private void applyRenderers(JTable table, boolean centerColumns) {

        for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {

            String header = table.getColumnName(columnIndex);
            int alignment = centerColumns ? SwingConstants.CENTER : cellAlignment(header);

            DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
            cellRenderer.setHorizontalAlignment(alignment);
            cellRenderer.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            cellRenderer.setForeground(TABLE_TEXT);
            cellRenderer.setBackground(TABLE_BACKGROUND);
            table.getColumnModel().getColumn(columnIndex).setCellRenderer(cellRenderer);

            DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
            headerRenderer.setOpaque(true);
            headerRenderer.setBackground(TABLE_HEADER);
            headerRenderer.setForeground(Color.WHITE);
            headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
            headerRenderer.setHorizontalAlignment(alignment);
            headerRenderer.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            table.getColumnModel().getColumn(columnIndex).setHeaderRenderer(headerRenderer);
        }
    }

    private int cellAlignment(String header) {

        if ("Alert Type".equals(header) || "Severity".equals(header)) {

            return SwingConstants.CENTER;
        }

        boolean numericColumn = "Forecast".equals(header)
                || "Delay Risk".equals(header)
                || "Profit Margin".equals(header)
                || "Value".equals(header);

        return numericColumn ? SwingConstants.RIGHT : SwingConstants.LEFT;
    }

    private String[][] metricsRows(ForecastMetrics metrics) {

        return new String[][] {
                {"Metric", "Value"},
                {"MAE", String.format(Locale.US, "%.2f", metrics.mae)},
                {"RMSE", String.format(Locale.US, "%.2f", metrics.rmse)},
                {"MAPE", String.format(Locale.US, "%.2f%%", metrics.mape)},
                {"R2", String.format(Locale.US, "%.4f", metrics.r2)}
        };
    }

    private String[][] topForecastRows(List<ForecastResult> latestForecasts) {

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Product", "Forecast"});

        latestForecasts.stream()
                .sorted(Comparator
                        .comparingDouble(ForecastResult::getFinalPredictedDemand)
                        .reversed()
                        .thenComparing(ForecastResult::getProductName))
                .limit(TABLE_LIMIT)
                .forEach(result -> rows.add(new String[] {
                        result.getProductName(),
                        String.format(Locale.US, "%.2f", result.getFinalPredictedDemand())
                }));

        return rows.toArray(new String[0][]);
    }

    private String[][] criticalAlertRows(List<CriticalAlertSummary> summaries) {

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"Product", "Alert Type", "Severity", "Reason"});

        summaries.stream()
                .limit(TABLE_LIMIT)
                .forEach(summary -> rows.add(new String[] {
                        summary.productName,
                        summary.alertTypeLabel(),
                        summary.severity,
                        summary.reason()
                }));

        return rows.toArray(new String[0][]);
    }

    private String[][] alertDistributionRows(List<SupplyChainAlert> filteredAlerts) {

        int highDemandDelayAlerts = 0;
        int popularButUnprofitableAlerts = 0;

        for (SupplyChainAlert alert : filteredAlerts) {

            if (alert.getAlertType().contains("HIGH DEMAND DELAY")) {

                highDemandDelayAlerts++;
            }

            if (alert.getAlertType().contains("UNPROFITABLE")) {

                popularButUnprofitableAlerts++;
            }
        }

        return new String[][] {
                {"High Demand Delay Alerts", integerFormat.format(highDemandDelayAlerts)},
                {"Popular But Unprofitable", integerFormat.format(popularButUnprofitableAlerts)},
                {"Total Alerts", integerFormat.format(filteredAlerts.size())}
        };
    }

    private void exportForecastReport() {

        exportReport("Forecast report exported", exporter -> exporter.exportForecastReport(
                filteredForecastResults(),
                Path.of("Reports", "forecast_report.txt")));
    }

    private void exportRiskReport() {

        exportReport("Risk report exported", exporter -> exporter.exportRiskReport(
                filteredRiskResults(),
                Path.of("Reports", "risk_report.txt")));
    }

    private void exportAlertReport() {

        exportReport("Alert report exported", exporter -> exporter.exportAlertReport(
                filteredAlerts(),
                Path.of("Reports", "alert_report.txt")));
    }

    private void exportReport(String message, ReportExportAction action) {

        try {

            Path path = action.export(new ReportExporter());
            JOptionPane.showMessageDialog(
                    this,
                    message + ":\n" + path.toAbsolutePath(),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Report export failed:\n" + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<ForecastResult> filteredForecastResults() {

        return forecastResults.stream()
                .filter(result -> matchesProduct(result.getProductName()))
                .filter(result -> matchesCategory(result.getCategoryName()))
                .filter(result -> matchesCountry(result.getProductName(), result.getCategoryName()))
                .collect(Collectors.toList());
    }

    private List<RiskIntelligenceResult> filteredRiskResults() {

        return riskResults.stream()
                .filter(result -> matchesProduct(result.getProductName()))
                .filter(result -> matchesCategory(result.getCategoryName()))
                .filter(result -> matchesCountry(result.getProductName(), result.getCategoryName()))
                .collect(Collectors.toList());
    }

    private List<SupplyChainAlert> filteredAlerts() {

        return alerts.stream()
                .filter(alert -> matchesProduct(alert.getProductName()))
                .filter(alert -> matchesCategory(alert.getCategoryName()))
                .filter(alert -> matchesCountry(alert.getProductName(), alert.getCategoryName()))
                .collect(Collectors.toList());
    }

    private boolean matchesProduct(String productName) {

        return matchesSelection(productName, productFilter);
    }

    private boolean matchesCategory(String categoryName) {

        return matchesSelection(categoryName, categoryFilter);
    }

    private boolean matchesCountry(String productName, String categoryName) {

        String query = selectedValue(countryFilter);

        if ("All".equals(query)) {

            return true;
        }

        for (SupplyChainRecord record : records) {

            if (!safeEquals(productName, record.getProductName())
                    || !safeEquals(categoryName, record.getCategoryName())) {

                continue;
            }

            if (query.equals(record.getOrderCountry())
                    || query.equals(record.getCustomerCountry())) {

                return true;
            }
        }

        return false;
    }

    private boolean matchesSelection(String value, JComboBox<String> comboBox) {

        String selected = selectedValue(comboBox);

        return "All".equals(selected) || safeEquals(value, selected);
    }

    private String selectedValue(JComboBox<String> comboBox) {

        Object value = comboBox.getSelectedItem();
        return value == null ? "All" : value.toString();
    }

    private boolean safeEquals(String left, String right) {

        return left == null ? right == null : left.equals(right);
    }

    private List<MonthlyForecastPoint> monthlyForecastTrend(List<ForecastResult> filteredForecasts) {

        Map<YearMonth, MonthlyForecastPoint> byMonth = new LinkedHashMap<>();

        filteredForecasts.stream()
                .sorted(Comparator.comparing(ForecastResult::getForecastDate))
                .forEach(result -> {
                    YearMonth month = YearMonth.from(result.getForecastDate());
                    MonthlyForecastPoint point = byMonth.computeIfAbsent(
                            month,
                            value -> new MonthlyForecastPoint(value));
                    point.actualDemand += result.getActualDemand();
                    point.forecastDemand += result.getFinalPredictedDemand();
                    point.count++;
                });

        return byMonth.values().stream()
                .skip(Math.max(0, byMonth.size() - 12))
                .collect(Collectors.toList());
    }

    private List<ForecastResult> latestForecasts(List<ForecastResult> sourceForecasts) {

        Map<String, ForecastResult> latest = new LinkedHashMap<>();

        for (ForecastResult result : sourceForecasts) {

            String key = result.getProductName() + "|" + result.getCategoryName();
            ForecastResult existing = latest.get(key);

            if (existing == null || result.getForecastDate().isAfter(existing.getForecastDate())) {

                latest.put(key, result);
            }
        }

        return new ArrayList<>(latest.values());
    }

    private ForecastMetrics calculateForecastMetrics(List<ForecastResult> sourceForecasts) {

        if (sourceForecasts.isEmpty()) {

            return new ForecastMetrics();
        }

        double absoluteErrorTotal = 0.0;
        double squaredErrorTotal = 0.0;
        double percentageErrorTotal = 0.0;
        int percentageCount = 0;
        double actualTotal = 0.0;

        for (ForecastResult result : sourceForecasts) {

            actualTotal += result.getActualDemand();
        }

        double actualAverage = actualTotal / sourceForecasts.size();
        double residualSquares = 0.0;
        double totalSquares = 0.0;

        for (ForecastResult result : sourceForecasts) {

            double actual = result.getActualDemand();
            double predicted = result.getFinalPredictedDemand();
            double error = actual - predicted;

            absoluteErrorTotal += Math.abs(error);
            squaredErrorTotal += error * error;
            residualSquares += error * error;
            totalSquares += (actual - actualAverage) * (actual - actualAverage);

            if (actual >= MINIMUM_ACTUAL_FOR_MAPE) {

                percentageErrorTotal += Math.abs(error / actual) * 100.0;
                percentageCount++;
            }
        }

        ForecastMetrics metrics = new ForecastMetrics();
        metrics.mae = absoluteErrorTotal / sourceForecasts.size();
        metrics.rmse = Math.sqrt(squaredErrorTotal / sourceForecasts.size());
        metrics.mape = percentageCount == 0 ? 0.0 : percentageErrorTotal / percentageCount;
        metrics.r2 = totalSquares == 0.0 ? 0.0 : 1.0 - (residualSquares / totalSquares);
        return metrics;
    }

    private List<CriticalAlertSummary> criticalAlerts(List<SupplyChainAlert> sourceAlerts) {

        Map<String, CriticalAlertSummary> summaries = new LinkedHashMap<>();

        for (SupplyChainAlert alert : sourceAlerts) {

            if (!isCriticalAlertType(alert.getAlertType())) {

                continue;
            }

            String key = alert.getProductName() + "|" + alert.getCategoryName();
            CriticalAlertSummary summary = summaries.computeIfAbsent(
                    key,
                    value -> new CriticalAlertSummary(alert.getProductName()));
            summary.add(alert);
        }

        return summaries.values().stream()
                .sorted(Comparator
                        .comparingInt((CriticalAlertSummary summary) -> severityRank(summary.severity)).reversed()
                        .thenComparing(Comparator.comparingDouble(CriticalAlertSummary::forecastDemand).reversed()))
                .collect(Collectors.toList());
    }

    private boolean isCriticalAlertType(String alertType) {

        return alertType.contains("HIGH DEMAND DELAY")
                || alertType.contains("UNPROFITABLE");
    }

    private TrendCounts trendCounts(List<ForecastResult> latestForecasts) {

        TrendCounts counts = new TrendCounts();

        for (ForecastResult result : latestForecasts) {

            if ("Growing Demand".equals(result.getDemandTrend())) {

                counts.increasing++;
            } else if ("Declining Demand".equals(result.getDemandTrend())) {

                counts.declining++;
            } else {

                counts.stable++;
            }
        }

        return counts;
    }

    private RiskCounts riskCounts(List<RiskIntelligenceResult> sourceRisks) {

        RiskCounts counts = new RiskCounts();

        for (RiskIntelligenceResult result : sourceRisks) {

            if (result.getRiskLevel().contains("CRITICAL") || result.getRiskLevel().contains("HIGH")) {

                counts.high++;
            } else if (result.getRiskLevel().contains("MEDIUM")) {

                counts.medium++;
            } else {

                counts.low++;
            }
        }

        return counts;
    }

    private int severityRank(String severity) {

        if ("HIGH".equals(severity)) {

            return 3;
        }

        if ("MEDIUM".equals(severity)) {

            return 2;
        }

        return 1;
    }

    private static class ForecastMetrics {

        private double mae;
        private double rmse;
        private double mape;
        private double r2;
    }

    @FunctionalInterface
    private interface ReportExportAction {

        Path export(ReportExporter exporter) throws IOException;
    }

    private static class TrendCounts {

        private int increasing;
        private int stable;
        private int declining;
    }

    private static class RiskCounts {

        private int high;
        private int medium;
        private int low;

        private int highRiskProducts() {

            return high;
        }
    }

    private static class CriticalAlertSummary {

        private final String productName;
        private final Set<String> alertTypes = new LinkedHashSet<>();
        private double forecastDemand;
        private double delayRisk;
        private double profitMargin;
        private String severity = "LOW";

        private CriticalAlertSummary(String productName) {

            this.productName = productName;
        }

        private void add(SupplyChainAlert alert) {

            alertTypes.add(alert.getAlertType());
            forecastDemand = Math.max(forecastDemand, alert.getForecastDemand());
            delayRisk = Math.max(delayRisk, alert.getDelayRisk());
            profitMargin = alert.getProfitMargin();

            if (severityRankValue(alert.getSeverity()) > severityRankValue(severity)) {

                severity = alert.getSeverity();
            }
        }

        private double forecastDemand() {

            return forecastDemand;
        }

        private String reason() {

            List<String> reasons = new ArrayList<>();

            if (alertTypes.stream().anyMatch(type -> type.contains("HIGH DEMAND DELAY"))) {

                reasons.add("High demand forecast with high delay risk");
            }

            if (alertTypes.stream().anyMatch(type -> type.contains("UNPROFITABLE"))) {

                reasons.add("High demand with low profit margin");
            }

            return String.join("; ", reasons);
        }

        private String alertTypeLabel() {

            boolean delayAlert = alertTypes.stream().anyMatch(type -> type.contains("HIGH DEMAND DELAY"));
            boolean profitAlert = alertTypes.stream().anyMatch(type -> type.contains("UNPROFITABLE"));

            if (delayAlert && profitAlert) {

                return "Delay + Profit";
            }

            if (delayAlert) {

                return "High Demand Delay";
            }

            if (profitAlert) {

                return "Popular Unprofitable";
            }

            return String.join(" + ", alertTypes);
        }

        private static int severityRankValue(String severity) {

            if ("HIGH".equals(severity)) {

                return 3;
            }

            if ("MEDIUM".equals(severity)) {

                return 2;
            }

            return 1;
        }
    }

    private static class MonthlyForecastPoint {

        private final YearMonth month;
        private double actualDemand;
        private double forecastDemand;
        private int count;

        private MonthlyForecastPoint(YearMonth month) {

            this.month = month;
        }
    }

    private static class ForecastTrendChart extends JPanel {

        private static final Color ACTUAL_COLOR = new Color(17, 24, 39);
        private static final Color FORECAST_COLOR = new Color(37, 99, 235);
        private static final Color GRID_COLOR = new Color(226, 232, 240);
        private static final Color AXIS_COLOR = new Color(107, 114, 128);

        private final List<MonthlyForecastPoint> points;

        private ForecastTrendChart(List<MonthlyForecastPoint> points) {

            this.points = points;
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(900, 260));
        }

        @Override
        protected void paintComponent(Graphics graphics) {

            super.paintComponent(graphics);
            Graphics2D graphics2d = (Graphics2D) graphics.create();
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int left = 62;
            int right = 28;
            int top = 28;
            int bottom = 48;
            int width = getWidth() - left - right;
            int height = getHeight() - top - bottom;

            graphics2d.setColor(GRID_COLOR);
            for (int index = 0; index <= 4; index++) {

                int y = top + (height * index / 4);
                graphics2d.drawLine(left, y, left + width, y);
            }

            graphics2d.setColor(AXIS_COLOR);
            graphics2d.drawLine(left, top, left, top + height);
            graphics2d.drawLine(left, top + height, left + width, top + height);

            if (points.isEmpty()) {

                graphics2d.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                graphics2d.drawString("No forecast trend data available", left + 20, top + 40);
                graphics2d.dispose();
                return;
            }

            double maxDemand = points.stream()
                    .mapToDouble(point -> Math.max(point.actualDemand, point.forecastDemand))
                    .max()
                    .orElse(1.0);

            drawLine(graphics2d, left, top, width, height, maxDemand, true);
            drawLine(graphics2d, left, top, width, height, maxDemand, false);
            drawLabels(graphics2d, left, top, width, height, maxDemand);
            drawLegend(graphics2d, left, top);
            graphics2d.dispose();
        }

        private void drawLine(
                Graphics2D graphics2d,
                int left,
                int top,
                int width,
                int height,
                double maxDemand,
                boolean actualLine) {

            graphics2d.setColor(actualLine ? ACTUAL_COLOR : FORECAST_COLOR);

            int previousX = 0;
            int previousY = 0;

            for (int index = 0; index < points.size(); index++) {

                MonthlyForecastPoint point = points.get(index);
                double value = actualLine ? point.actualDemand : point.forecastDemand;
                int x = left + (points.size() == 1 ? width / 2 : width * index / (points.size() - 1));
                int y = top + height - (int) Math.round((value / maxDemand) * height);

                graphics2d.fillOval(x - 4, y - 4, 8, 8);

                if (index > 0) {

                    graphics2d.drawLine(previousX, previousY, x, y);
                }

                previousX = x;
                previousY = y;
            }
        }

        private void drawLabels(Graphics2D graphics2d, int left, int top, int width, int height, double maxDemand) {

            graphics2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            graphics2d.setColor(AXIS_COLOR);

            for (int index = 0; index <= 4; index++) {

                double value = maxDemand - (maxDemand * index / 4);
                int y = top + (height * index / 4) + 4;
                graphics2d.drawString(String.format(Locale.US, "%.0f", value), 8, y);
            }

            for (int index = 0; index < points.size(); index++) {

                MonthlyForecastPoint point = points.get(index);
                int x = left + (points.size() == 1 ? width / 2 : width * index / (points.size() - 1));
                graphics2d.drawString(point.month.toString(), x - 22, top + height + 24);
            }
        }

        private void drawLegend(Graphics2D graphics2d, int left, int top) {

            graphics2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
            graphics2d.setColor(ACTUAL_COLOR);
            graphics2d.fillRect(left, top - 18, 12, 12);
            graphics2d.drawString("Actual Demand", left + 18, top - 8);
            graphics2d.setColor(FORECAST_COLOR);
            graphics2d.fillRect(left + 130, top - 18, 12, 12);
            graphics2d.drawString("Forecast Demand", left + 148, top - 8);
        }
    }

    private static class GradientPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics graphics) {

            Graphics2D graphics2d = (Graphics2D) graphics.create();
            GradientPaint gradient = new GradientPaint(
                    0,
                    0,
                    BACKGROUND_TOP,
                    0,
                    getHeight(),
                    BACKGROUND_BOTTOM);
            graphics2d.setPaint(gradient);
            graphics2d.fillRect(0, 0, getWidth(), getHeight());
            graphics2d.dispose();
            super.paintComponent(graphics);
        }
    }
}
