package com.rosswood.observer;

import com.rosswood.entity.StockTransaction;
import com.rosswood.event.ProductionFinishedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.time.LocalDate;

@ApplicationScoped
public class InventoryObserver {

    @Transactional
    public void onProductionFinished(@Observes ProductionFinishedEvent event) {
        var batch = event.batch;

        // 1. Create Output Entry (Finished Good)
        StockTransaction output = new StockTransaction();
        output.item = batch.finishedItem;
        output.quantity = event.finalQty;
        output.transactionType = "PRODUCTION_OUTPUT"; // Explicitly defined here
        output.referenceNo = batch.batchCode;
        output.transactionDate = LocalDate.now();
        output.persist();

        // 2. Create Consumption Entries (Raw Materials)
        batch.consumptions.forEach(c -> {
            StockTransaction sub = new StockTransaction();
            sub.item = c.rawItem;
            sub.quantity = c.consumedQty.negate(); // Subtract from stock
            sub.transactionType = "PRODUCTION_CONSUME";
            sub.referenceNo = batch.batchCode;
            sub.transactionDate = LocalDate.now();
            sub.persist();
        });
    }
}
