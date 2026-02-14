package com.rosswood.interceptor;

import com.rosswood.entity.*;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.math.BigDecimal;
import java.util.List;

@UpdatesStock
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class StockUpdateInterceptor {

    @AroundInvoke
    public Object manageStock(InvocationContext context) throws Exception {
        // 1. Let the original method (e.g., saveBatch) run
        Object result = context.proceed();

        // 2. Logic after successful execution
        if (result instanceof ProductionBatch batch) {
            recordProductionStock(batch);
        } else if (result instanceof ProductionConsumption productionConsumption) {
            recordConsumptionStock(productionConsumption);
        }

        return result;
    }

    private void recordConsumptionStock(ProductionConsumption consumption) {
        // Ensure data exists to avoid NullPointerExceptions
        if (consumption.rawItem != null && consumption.consumedQty != null) {
            StockTransaction consumeTx = new StockTransaction();

            // Use the date from the parent production batch
            consumeTx.transactionDate = (consumption.batch != null)
                    ? consumption.batch.productionDate
                    : java.time.LocalDate.now();

            consumeTx.item = consumption.rawItem;
            consumeTx.transactionType = "PRODUCTION_CONSUMPTION";

            // CONTEXT: Consumption is a deduction, so we store it as a negative value
            consumeTx.quantity = consumption.consumedQty.negate();

            consumeTx.referenceNo = (consumption.batch != null)
                    ? consumption.batch.batchCode
                    : "N/A";

            consumeTx.remarks = "Automated consumption for item: " + consumption.rawItem.itemName;

            consumeTx.persist();

            // Optional: Update the master Item currentStock field immediately
            updateItemMasterStock(consumption.rawItem, consumption.consumedQty.negate());
        }
    }

    private void updateItemMasterStock(Item item, BigDecimal change) {
        //if (itemcurrentStock == null) item.currentStock = BigDecimal.ZERO;
        //item.currentStock = item.currentStock.add(change);
    }

    private void recordProductionStock(ProductionBatch batch) {
        // 1. Record the Finished Good Output (Positive Quantity)
        StockTransaction outputTx = new StockTransaction();
        outputTx.transactionDate = batch.productionDate; //
        outputTx.item = batch.finishedItem; //
        outputTx.transactionType = "PRODUCTION_OUTPUT"; //
        outputTx.quantity = batch.outputQty; //
        outputTx.referenceNo = batch.batchCode; //
        outputTx.remarks = "Automated production output for batch: " + batch.batchCode;
        outputTx.persist(); //

    }
}
