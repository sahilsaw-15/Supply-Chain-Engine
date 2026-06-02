package com.supplychain.alert;

import com.supplychain.model.ForecastResult;
import com.supplychain.model.SupplyChainAlert;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupplyChainAlertGenerator {

    private static final double HIGH_ABSOLUTE_FORECAST = 50.0;
    private static final double HIGH_DELAY_RISK_THRESHOLD = 50.0;
    private static final double LOW_PROFIT_MARGIN_THRESHOLD = 10.0;

    public List<SupplyChainAlert> generate(List<ForecastResult> forecastResults) {

        List<SupplyChainAlert> alerts = new ArrayList<>();

        for (ForecastResult result : latestForecasts(forecastResults).values()) {

            if (isHighDemand(result) && result.getDelayRiskRate() > HIGH_DELAY_RISK_THRESHOLD) {

                alerts.add(new SupplyChainAlert(
                        "HIGH DEMAND DELAY ALERT",
                        result.getProductName(),
                        result.getCategoryName(),
                        result.getFinalPredictedDemand(),
                        result.getDelayRiskRate(),
                        result.getProfitMargin(),
                        "HIGH",
                        "Increase inventory and review supplier lead times."));
            }

            if (isHighDemand(result) && result.getProfitMargin() < LOW_PROFIT_MARGIN_THRESHOLD) {

                alerts.add(new SupplyChainAlert(
                        "POPULAR BUT UNPROFITABLE ALERT",
                        result.getProductName(),
                        result.getCategoryName(),
                        result.getFinalPredictedDemand(),
                        result.getDelayRiskRate(),
                        result.getProfitMargin(),
                        "MEDIUM",
                        "Review pricing strategy or supplier costs."));
            }
        }

        alerts.sort(Comparator
                .comparingInt(this::severityRank)
                .thenComparing(Comparator.comparingDouble(SupplyChainAlert::getForecastDemand).reversed())
                .thenComparing(Comparator.comparingDouble(SupplyChainAlert::getDelayRisk).reversed())
                .thenComparing(SupplyChainAlert::getProductName));

        return alerts;
    }

    private Map<String, ForecastResult> latestForecasts(List<ForecastResult> forecastResults) {

        Map<String, ForecastResult> latestByProductCategory = new LinkedHashMap<>();

        for (ForecastResult result : forecastResults) {

            String key = result.getProductName() + "|" + result.getCategoryName();
            ForecastResult current = latestByProductCategory.get(key);

            if (current == null || result.getForecastDate().isAfter(current.getForecastDate())) {

                latestByProductCategory.put(key, result);
            }
        }

        return latestByProductCategory;
    }

    private boolean isHighDemand(ForecastResult result) {

        return result.getFinalPredictedDemand() >= HIGH_ABSOLUTE_FORECAST;
    }

    private int severityRank(SupplyChainAlert alert) {

        if ("HIGH".equals(alert.getSeverity())) {

            return 0;
        }

        if ("MEDIUM".equals(alert.getSeverity())) {

            return 1;
        }

        return 2;
    }
}
