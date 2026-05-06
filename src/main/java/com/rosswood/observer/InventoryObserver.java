package com.rosswood.observer;

import com.rosswood.entity.ProductionBatch;
import com.rosswood.event.ProductionFinishedEvent;
import com.rosswood.service.StockTransactionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@ApplicationScoped
public class InventoryObserver {

    @Inject
    StockTransactionService stockService;

    @Transactional
    public void onProductionFinished(@Observes ProductionFinishedEvent event) {
        ProductionBatch batch = event.batch;

        // 1. Raw material consumptions OUT
        batch.consumptions.forEach(c -> stockService.postProductionConsume(batch, c));

        // 2. Roll up raw material costs to derive finished good unit cost.
        //    Only lines where the raw item has a WAC contribute to the total.
        BigDecimal totalBatchCost = batch.consumptions.stream()
                .filter(c -> c.rawItem.averageCost != null)
                .map(c -> c.consumedQty.multiply(c.rawItem.averageCost))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finishedUnitCost = null;
        if (totalBatchCost.compareTo(BigDecimal.ZERO) > 0
                && event.finalQty != null
                && event.finalQty.compareTo(BigDecimal.ZERO) > 0) {
            finishedUnitCost = totalBatchCost.divide(event.finalQty, 4, RoundingMode.HALF_UP);
        }

        // 3. Finished good output IN — stamped with computed unit cost, WAC updated
        stockService.postProductionOutput(batch, event.finalQty, finishedUnitCost);

        // 4. Mark batch finished
        batch.status = ProductionBatch.BatchStatus.FINISHED;
        batch.persist();
    }
}