package com.supplychain.forecast;

import com.supplychain.model.DemandRecord;
import com.supplychain.model.SupplyChainRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DemandDatasetBuilder {

    public List<DemandRecord> buildDailyProductDemand(List<SupplyChainRecord> records) {

        Map<String, DemandRecord> demandMap = new LinkedHashMap<>();

        for (SupplyChainRecord record : records) {

            if (record.getOrderDate() == null) {

                continue;
            }

            LocalDate demandDate = record.getOrderDate().toLocalDate();
            String key = demandDate + "|" + record.getProductName() + "|" + record.getCategoryName();

            DemandRecord demandRecord = demandMap.get(key);

            if (demandRecord == null) {

                demandRecord = new DemandRecord(
                        demandDate,
                        record.getProductName(),
                        record.getCategoryName()
                );
                demandMap.put(key, demandRecord);
            }

            demandRecord.addOrder(
                    record.getOrderItemQuantity(),
                    record.getSales(),
                    record.getProfit()
            );
        }

        List<DemandRecord> demandRecords = new ArrayList<>(demandMap.values());
        demandRecords.sort(Comparator
                .comparing(DemandRecord::getDemandDate)
                .thenComparing(DemandRecord::getProductName)
                .thenComparing(DemandRecord::getCategoryName));

        return demandRecords;
    }
}
