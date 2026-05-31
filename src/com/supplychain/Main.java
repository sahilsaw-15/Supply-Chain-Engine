package com.supplychain;

import com.supplychain.export.CleanedDataExporter;
import com.supplychain.forecast.DemandDatasetBuilder;
import com.supplychain.forecast.DemandForecaster;
import com.supplychain.loader.CSVDataLoader;
import com.supplychain.loader.CSVDataLoader.DataCleaningResult;
import com.supplychain.model.DemandRecord;
import com.supplychain.model.ForecastResult;
import com.supplychain.model.SupplyChainRecord;
import com.supplychain.report.DemandReportGenerator;
import com.supplychain.report.ForecastReportGenerator;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

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
        ForecastResultOutput forecastResultOutput = saveForecastResults(forecastingOutput.getDemandRecords());

        printGeneratedCsvMessage(
                cleanedOutputPath,
                forecastingOutput.getOutputPath(),
                forecastResultOutput.getOutputPath());

        if (shouldGenerateReport()) {

            printForecastReport(forecastResultOutput.getForecastResults());
            printDemandReport(forecastingOutput.getDemandRecords(), records);
        } else {

            System.out.println("\nDemand report skipped.");
        }
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

    private static ForecastResultOutput saveForecastResults(List<DemandRecord> demandRecords) {

        Path outputPath = Path.of("Data", "demand_forecast_results.csv");

        if (demandRecords.isEmpty()) {

            System.out.println("\nForecast result CSV was not created because there are no demand records.");
            return new ForecastResultOutput(List.of(), outputPath);
        }

        DemandForecaster forecaster = new DemandForecaster();
        List<ForecastResult> forecastResults = forecaster.forecast(demandRecords);
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

    private static void printGeneratedCsvMessage(
            Path cleanedOutputPath,
            Path forecastingOutputPath,
            Path forecastResultsOutputPath) {

        System.out.println("\nAll CSV files are generated:");
        System.out.println("1. " + cleanedOutputPath.getFileName());
        System.out.println("2. " + forecastingOutputPath.getFileName());
        System.out.println("3. " + forecastResultsOutputPath.getFileName());
    }

    private static boolean shouldGenerateReport() {

        Scanner scanner = new Scanner(System.in);

        while (true) {

            System.out.print("\nDo you want to generate the forecasting and demand analysis reports? (Y/N): ");
            String answer = scanner.nextLine().trim();

            if (answer.equalsIgnoreCase("Y")) {

                return true;
            }

            if (answer.equalsIgnoreCase("N")) {

                return false;
            }

            System.out.println("Please enter Y or N.");
        }
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
}
