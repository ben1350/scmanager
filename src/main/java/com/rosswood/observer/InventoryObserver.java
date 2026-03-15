package com.rosswood.observer;

import com.rosswood.entity.ProductionBatch;
import com.rosswood.event.ProductionFinishedEvent;
import com.rosswood.service.StockTransactionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class InventoryObserver {

    @Inject
    StockTransactionService stockService;

    @Transactional
    public void onProductionFinished(@Observes ProductionFinishedEvent event) {
        ProductionBatch batch = event.batch;

        // 1. Raw material consumptions OUT
        batch.consumptions.forEach(c -> stockService.postProductionConsume(batch, c));

        // 2. Finished good output IN (with expiry date + batchCode)
        stockService.postProductionOutput(batch, event.finalQty);

        // 3. Mark batch finished — done inside postProductionOutput via service
        batch.status = ProductionBatch.BatchStatus.FINISHED;
        batch.persist();
    }
}