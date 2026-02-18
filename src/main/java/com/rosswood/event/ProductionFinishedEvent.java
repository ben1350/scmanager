package com.rosswood.event;

import com.rosswood.entity.ProductionBatch;
import java.math.BigDecimal;

public class ProductionFinishedEvent {
    public final ProductionBatch batch;
    public final BigDecimal finalQty;

    public ProductionFinishedEvent(ProductionBatch batch, BigDecimal finalQty) {
        this.batch = batch;
        this.finalQty = finalQty;
    }
}