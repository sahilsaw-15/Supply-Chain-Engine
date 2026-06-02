package com.supplychain;

import com.supplychain.alert.SupplyChainAlertGenerator;
import com.supplychain.dashboard.SupplyChainDashboard;
import com.supplychain.export.CleanedDataExporter;
import com.supplychain.forecast.DemandDatasetBuilder;
import com.supplychain.forecast.DemandForecaster;
import com.supplychain.loader.CSVDataLoader;
import com.supplychain.loader.CSVDataLoader.DataCleaningResult;
import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainAlert;
import com.supplychain.model.SupplyChainRecord;
import com.supplychain.report.DemandReportGenerator;
import com.supplychain.report.ForecastReportGenerator;
import com.supplychain.report.RiskIntelligenceReportGenerator;
import com.supplychain.report.SupplyChainAlertReportGenerator;
import com.supplychain.risk.RiskIntelligenceGenerator;
import com.supplychain.storage.LocalDatabaseStorage;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

       
        clearConsole();
        System.out.println(" ***SUPPLY CHAIN RISK ENGINE*** ");
        

        CSVDataLoader loader = new CSVDataLoader();

        String filePath = "Data/DataCoSupplyChainDataset.csv";

        List<String[]> rows = loader.loadCSV(filePath);

        loader.printDatasetSummary(rows);

        DataCleaningResult cleaningResult =
                loader.convertToRecords(rows);

        cleaningResult.getReport().printReport();

        List<SupplyChainRecord> records =
                cleaningResult.getRecords();

        System.out.println("\nConverted Records: "
                + records.size());

        if (records.isEmpty()) {

            System.out.println("\nNo converted records available to display.");
        } else {

            System.out.println("\nSample Converted Record:");
            System.out.println(records.get(0));
        }

        Path cleanedOutputPath = saveCleanedDataset(records);
        ForecastingOutput forecastingOutput = saveForecastingDataset(records);
        ForecastResultOutput forecastResultOutput = saveForecastResults(forecastingOutput.getDemandRecords(), records);
        RiskIntelligenceOutput riskIntelligenceOutput = saveRiskIntelligenceResults(
                forecastResultOutput.getForecastResults(),
                forecastingOutput.getDemandRecords(),
                records);
        AlertOutput alertOutput = saveSupplyChainAlerts(forecastResultOutput.getForecastResults());
        Path databasePath = saveLocalDatabaseSnapshot(
                records,
                forecastResultOutput.getForecastResults(),
                riskIntelligenceOutput.getRiskResults(),
                alertOutput.getAlerts());

        printGeneratedCsvMessage(
                cleanedOutputPath,
                forecastingOutput.getOutputPath(),
                forecastResultOutput.getOutputPath(),
                riskIntelligenceOutput.getOutputPath(),
                alertOutput.getOutputPath());
        System.out.println("Local database snapshot saved at: " + databasePath.toAbsolutePath());

        printAlertReport(alertOutput.getAlerts());
        printForecastReport(forecastResultOutput.getForecastResults());
        printRiskIntelligenceReport(
                riskIntelligenceOutput.getRiskResults(),
                forecastResultOutput.getForecastResults(),
                records);
        printDemandReport(forecastingOutput.getDemandRecords(), records);

        launchDashboardIfEnabled(
                args,
                records,
                forecastingOutput.getDemandRecords(),
                forecastResultOutput.getForecastResults(),
                riskIntelligenceOutput.getRiskResults(),
                alertOutput.getAlerts());
    }

    private static void clearConsole() {

        try {

            String operatingSystem = System.getProperty("os.name").toLowerCase();

            if (operatingSystem.contains("win")) {

                new ProcessBuilder("cmd", "/c", "cls")
                        .inheritIO()
                        .start()
                        .waitFor();
            } else {

                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {

            System.out.print("\033[H\033[2J");
            System.out.println("\n".repeat(50));
            System.out.flush();

            if (e instanceof InterruptedException) {

                Thread.currentThread().interrupt();
            }
        }
    }

    private static Path saveCleanedDataset(List<SupplyChainRecord> records) {

        Path outputPath = Path.of("Data", "cleaned_supply_chain_data.csv");

        if (records.isEmpty()) {

            System.out.println("\nCleaned CSV was not created because there are no records to export.");
            return outputPath;
        }

        CleanedDataExporter exporter = new CleanedDataExporter();

        try {

            exporter.exportToCsv(records, outputPath);
            System.out.println("\nCleaned Excel-friendly CSV saved at: " + outputPath.toAbsolutePath());
        } catch (IOException e) {

            System.out.println("\nError saving cleaned CSV file: " + e.getMessage());
        }

        return outputPath;
    }

    private static ForecastingOutput saveForecastingDataset(List<SupplyChainRecord> records) {

        Path outputPath = Path.of("Data", "forecasting_demand_dataset.csv");

        if (records.isEmpty()) {

            System.out.println("\nForecasting CSV was not created because there are no records to export.");
            return new ForecastingOutput(List.of(), outputPath);
        }

        DemandDatasetBuilder demandDatasetBuilder = new DemandDatasetBuilder();
        List<DemandRecord> demandRecords = demandDatasetBuilder.buildDailyProductDemand(records);

        CleanedDataExporter exporter = new CleanedDataExporter();

        try {

            exporter.exportDemandToCsv(demandRecords, outputPath);
            System.out.println("Forecasting-ready demand CSV saved at: " + outputPath.toAbsolutePath());
            System.out.println("Forecasting demand rows created: " + demandRecords.size());
        } catch (IOException e) {

            System.out.println("\nError saving forecasting CSV file: " + e.getMessage());
        }

        return new ForecastingOutput(demandRecords, outputPath);
    }

    private static ForecastResultOutput saveForecastResults(
            List<DemandRecord> demandRecords,
            List<SupplyChainRecord> records) {

        Path outputPath = Path.of("Data", "demand_forecast_results.csv");

        if (demandRecords.isEmpty()) {

            System.out.println("\nForecast result CSV was not created because there are no demand records.");
            return new ForecastResultOutput(List.of(), outputPath);
        }

        DemandForecaster forecaster = new DemandForecaster();
        List<ForecastResult> forecastResults = forecaster.forecast(demandRecords, records);
        CleanedDataExporter exporter = new CleanedDataExporter();

        try {

            exporter.exportForecastToCsv(forecastResults, outputPath);
            System.out.println("Demand forecast results CSV saved at: " + outputPath.toAbsolutePath());
            System.out.println("Forecast result rows created: " + forecastResults.size());
        } catch (IOException e) {

            System.out.println("\nError saving demand forecast results CSV file: " + e.getMessage());
        }

        return new ForecastResultOutput(forecastResults, outputPath);
    }

    private static RiskIntelligenceOutput saveRiskIntelligenceResults(
            List<ForecastResult> forecastResults,
            List<DemandRecord> demandRecords,
            List<SupplyChainRecord> records) {

        Path outputPath = Path.of("Data", "risk_intelligence_results.csv");

        if (forecastResults.isEmpty()) {

            System.out.println("\nRisk intelligence CSV was not created because there are no forecast results.");
            return new RiskIntelligenceOutput(List.of(), outputPath);
        }

        RiskIntelligenceGenerator generator = new RiskIntelligenceGenerator();
        List<RiskIntelligenceResult> riskResults = generator.generate(forecastResults, demandRecords, records);
        CleanedDataExporter exporter = new CleanedDataExporter();

        try {

            exporter.exportRiskIntelligenceToCsv(riskResults, outputPath);
            System.out.println("Risk intelligence results CSV saved at: " + outputPath.toAbsolutePath());
            System.out.println("Risk intelligence rows created: " + riskResults.size());
        } catch (IOException e) {

            System.out.println("\nError saving risk intelligence results CSV file: " + e.getMessage());
        }

        return new RiskIntelligenceOutput(riskResults, outputPath);
    }

    private static AlertOutput saveSupplyChainAlerts(List<ForecastResult> forecastResults) {

        Path outputPath = Path.of("Data", "supply_chain_alerts.csv");

        if (forecastResults.isEmpty()) {

            System.out.println("\nAlert CSV was not created because there are no forecast results.");
            return new AlertOutput(List.of(), outputPath);
        }

        SupplyChainAlertGenerator generator = new SupplyChainAlertGenerator();
        List<SupplyChainAlert> alerts = generator.generate(forecastResults);
        CleanedDataExporter exporter = new CleanedDataExporter();

        try {

            exporter.exportAlertsToCsv(alerts, outputPath);
            System.out.println("Supply chain alerts CSV saved at: " + outputPath.toAbsolutePath());
            System.out.println("Supply chain alert rows created: " + alerts.size());
        } catch (IOException e) {

            System.out.println("\nError saving supply chain alerts CSV file: " + e.getMessage());
        }

        return new AlertOutput(alerts, outputPath);
    }

    private static void printGeneratedCsvMessage(
            Path cleanedOutputPath,
            Path forecastingOutputPath,
            Path forecastResultsOutputPath,
            Path riskIntelligenceOutputPath,
            Path alertOutputPath) {

        System.out.println("\nAll CSV files are generated:");
        System.out.println("1. " + cleanedOutputPath.getFileName());
        System.out.println("2. " + forecastingOutputPath.getFileName());
        System.out.println("3. " + forecastResultsOutputPath.getFileName());
        System.out.println("4. " + riskIntelligenceOutputPath.getFileName());
        System.out.println("5. " + alertOutputPath.getFileName());
    }

    private static void printDemandReport(
            List<DemandRecord> demandRecords,
            List<SupplyChainRecord> records) {

        DemandReportGenerator reportGenerator = new DemandReportGenerator();
        reportGenerator.printReport(demandRecords, records);
    }

    private static void printForecastReport(List<ForecastResult> forecastResults) {

        ForecastReportGenerator reportGenerator = new ForecastReportGenerator();
        reportGenerator.printReport(forecastResults);
    }

    private static void printRiskIntelligenceReport(
            List<RiskIntelligenceResult> riskResults,
            List<ForecastResult> forecastResults,
            List<SupplyChainRecord> records) {

        RiskIntelligenceReportGenerator reportGenerator = new RiskIntelligenceReportGenerator();
        reportGenerator.printReport(riskResults, forecastResults, records);
    }

    private static void printAlertReport(List<SupplyChainAlert> alerts) {

        SupplyChainAlertReportGenerator reportGenerator = new SupplyChainAlertReportGenerator();
        reportGenerator.printReport(alerts);
    }

    private static Path saveLocalDatabaseSnapshot(
            List<SupplyChainRecord> records,
            List<ForecastResult> forecastResults,
            List<RiskIntelligenceResult> riskResults,
            List<SupplyChainAlert> alerts) {

        Path outputPath = Path.of("Data", "supply_chain_engine_store.db");
        LocalDatabaseStorage storage = new LocalDatabaseStorage();

        try {

            storage.saveSnapshot(records, forecastResults, riskResults, alerts, outputPath);
        } catch (IOException e) {

            System.out.println("\nError saving local database snapshot: " + e.getMessage());
        }

        return outputPath;
    }

    private static void launchDashboardIfEnabled(
            String[] args,
            List<SupplyChainRecord> records,
            List<DemandRecord> demandRecords,
            List<ForecastResult> forecastResults,
            List<RiskIntelligenceResult> riskResults,
            List<SupplyChainAlert> alerts) {

        if (isNoDashboardRequested(args)) {

            System.out.println("\nDashboard skipped because --no-dashboard was provided.");
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {

            System.out.println("\nDashboard skipped because no graphical display is available.");
            return;
        }

        SwingUtilities.invokeLater(() -> {

            SupplyChainDashboard dashboard = new SupplyChainDashboard(
                    records,
                    demandRecords,
                    forecastResults,
                    riskResults,
                    alerts);
            dashboard.setVisible(true);
        });
    }

    private static boolean isNoDashboardRequested(String[] args) {

        for (String arg : args) {

            if ("--no-dashboard".equalsIgnoreCase(arg)) {

                return true;
            }
        }

        return false;
    }

    private static class ForecastingOutput {

        private final List<DemandRecord> demandRecords;
        private final Path outputPath;

        private ForecastingOutput(List<DemandRecord> demandRecords, Path outputPath) {

            this.demandRecords = demandRecords;
            this.outputPath = outputPath;
        }

        private List<DemandRecord> getDemandRecords() {

            return demandRecords;
        }

        private Path getOutputPath() {

            return outputPath;
        }
    }

    private static class ForecastResultOutput {

        private final List<ForecastResult> forecastResults;
        private final Path outputPath;

        private ForecastResultOutput(List<ForecastResult> forecastResults, Path outputPath) {

            this.forecastResults = forecastResults;
            this.outputPath = outputPath;
        }

        private List<ForecastResult> getForecastResults() {

            return forecastResults;
        }

        private Path getOutputPath() {

            return outputPath;
        }
    }

    private static class RiskIntelligenceOutput {

        private final List<RiskIntelligenceResult> riskResults;
        private final Path outputPath;

        private RiskIntelligenceOutput(List<RiskIntelligenceResult> riskResults, Path outputPath) {

            this.riskResults = riskResults;
            this.outputPath = outputPath;
        }

        private List<RiskIntelligenceResult> getRiskResults() {

            return riskResults;
        }

        private Path getOutputPath() {

            return outputPath;
        }
    }

    private static class AlertOutput {

        private final List<SupplyChainAlert> alerts;
        private final Path outputPath;

        private AlertOutput(List<SupplyChainAlert> alerts, Path outputPath) {

            this.alerts = alerts;
            this.outputPath = outputPath;
        }

        private List<SupplyChainAlert> getAlerts() {

            return alerts;
        }

        private Path getOutputPath() {

            return outputPath;
        }
    }
}
