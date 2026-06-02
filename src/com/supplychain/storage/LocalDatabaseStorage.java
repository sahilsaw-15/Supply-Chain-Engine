package com.supplychain.storage;

import com.supplychain.model.ForecastResult;
import com.supplychain.model.RiskIntelligenceResult;
import com.supplychain.model.SupplyChainAlert;
import com.supplychain.model.SupplyChainRecord;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalDatabaseStorage {

    public Path saveSnapshot(
            List<SupplyChainRecord> records,
            List<ForecastResult> forecasts,
            List<RiskIntelligenceResult> risks,
            List<SupplyChainAlert> alerts,
            Path outputPath) throws IOException {

        Path parent = outputPath.getParent();

        if (parent != null) {

            Files.createDirectories(parent);
        }

        DatabaseSnapshot snapshot = new DatabaseSnapshot();
        snapshot.cleanedRecordCount = records.size();
        snapshot.forecasts = forecastRows(forecasts);
        snapshot.risks = riskRows(risks);
        snapshot.alerts = alertRows(alerts);

        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(outputPath))) {

            output.writeObject(snapshot);
        }

        return outputPath;
    }

    private List<Map<String, String>> forecastRows(List<ForecastResult> forecasts) {

        List<Map<String, String>> rows = new ArrayList<>();

        for (ForecastResult forecast : forecasts) {

            Map<String, String> row = new LinkedHashMap<>();
            row.put("productName", forecast.getProductName());
            row.put("categoryName", forecast.getCategoryName());
            row.put("forecastDate", forecast.getForecastDate().toString());
            row.put("actualDemand", String.valueOf(forecast.getActualDemand()));
            row.put("forecastDemand", String.valueOf(forecast.getFinalPredictedDemand()));
            row.put("bestModel", forecast.getBestModel());
            row.put("demandTrend", forecast.getDemandTrend());
            row.put("riskLevel", forecast.getCombinedRiskClassification());
            rows.add(row);
        }

        return rows;
    }

    private List<Map<String, String>> riskRows(List<RiskIntelligenceResult> risks) {

        List<Map<String, String>> rows = new ArrayList<>();

        for (RiskIntelligenceResult risk : risks) {

            Map<String, String> row = new LinkedHashMap<>();
            row.put("productName", risk.getProductName());
            row.put("categoryName", risk.getCategoryName());
            row.put("forecastDemand", String.valueOf(risk.getForecastDemand()));
            row.put("delayRisk", String.valueOf(risk.getDelayRisk()));
            row.put("profitMargin", String.valueOf(risk.getProfitMargin()));
            row.put("riskScore", String.valueOf(risk.getRiskScore()));
            row.put("riskLevel", risk.getRiskLevel());
            rows.add(row);
        }

        return rows;
    }

    private List<Map<String, String>> alertRows(List<SupplyChainAlert> alerts) {

        List<Map<String, String>> rows = new ArrayList<>();

        for (SupplyChainAlert alert : alerts) {

            Map<String, String> row = new LinkedHashMap<>();
            row.put("alertType", alert.getAlertType());
            row.put("productName", alert.getProductName());
            row.put("categoryName", alert.getCategoryName());
            row.put("forecastDemand", String.valueOf(alert.getForecastDemand()));
            row.put("severity", alert.getSeverity());
            row.put("recommendation", alert.getRecommendation());
            rows.add(row);
        }

        return rows;
    }

    private static class DatabaseSnapshot implements Serializable {

        private static final long serialVersionUID = 1L;

        private int cleanedRecordCount;
        private List<Map<String, String>> forecasts;
        private List<Map<String, String>> risks;
        private List<Map<String, String>> alerts;
    }
}
