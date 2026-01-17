package com.rosswood.inventory;

import com.rosswood.entity.*;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@ApplicationScoped
public class DummyDataLoader {

    private final Random rand = new Random();

    @Transactional
    void loadDummyData(@Observes StartupEvent event) {

        // --------------------------
        // UNIT OF MEASURE
        // --------------------------
        if (UnitOfMeasure.count() == 0) {
            UnitOfMeasure pcs = new UnitOfMeasure();
            pcs.uomCode = "PCS";
            pcs.description = "Pieces";
            pcs.persist();

            UnitOfMeasure kg = new UnitOfMeasure();
            kg.uomCode = "KG";
            kg.description = "Kilograms";
            kg.persist();
        }

        // --------------------------
        // ITEM TYPES
        // --------------------------
        if (ItemType.count() == 0) {
            ItemType raw = new ItemType();
            raw.itemTypeCode = "RAW_MATERIAL";
            raw.persist();

            ItemType finished = new ItemType();
            finished.itemTypeCode = "FINISHED_GOOD";
            finished.persist();
        }

        // --------------------------
        // ITEMS
        // --------------------------
        if (Item.count() == 0) {
            UnitOfMeasure pcs = UnitOfMeasure.find("uomCode", "PCS").firstResult();
            UnitOfMeasure kg = UnitOfMeasure.find("uomCode", "KG").firstResult();
            ItemType rawType = ItemType.find("itemTypeCode", "RAW_MATERIAL").firstResult();
            ItemType finishedType = ItemType.find("itemTypeCode", "FINISHED_GOOD").firstResult();

            // Raw materials
            String[] rawNames = {"Sugar", "Flour", "Milk", "Butter", "Yeast"};
            for (int i = 0; i < rawNames.length; i++) {
                Item item = new Item();
                item.itemCode = "RM" + String.format("%03d", i + 1);
                item.itemName = rawNames[i];
                item.itemType = rawType;
                item.uom = kg;
                item.persist();
            }

            // Finished goods
            String[] finishedNames = {"Bread", "Cake", "Muffin", "Bun", "Cookie"};
            for (int i = 0; i < finishedNames.length; i++) {
                Item item = new Item();
                item.itemCode = "FG" + String.format("%03d", i + 1);
                item.itemName = finishedNames[i];
                item.itemType = finishedType;
                item.uom = pcs;
                item.persist();
            }
        }

        // --------------------------
        // STOCK OPENINGS
        // --------------------------
        if (StockOpening.count() == 0) {
            List<Item> allItems = Item.listAll();
            LocalDate yesterday = LocalDate.now().minusDays(1);
            for (Item item : allItems) {
                StockOpening so = new StockOpening();
                so.stockDate = yesterday;
                so.item = item;
                so.openingQty = BigDecimal.valueOf(rand.nextInt(100) + 20);
                so.persist();
            }
        }

        // --------------------------
        // STOCK TRANSACTIONS
        // --------------------------
        if (StockTransaction.count() == 0) {
            List<Item> rawItems = Item.find("itemType.itemTypeCode", "RAW_MATERIAL").list();
            List<Item> finishedItems = Item.find("itemType.itemTypeCode", "FINISHED_GOOD").list();
            LocalDate today = LocalDate.now();

            // Purchases for raw materials
            for (Item item : rawItems) {
                StockTransaction purchase = new StockTransaction();
                purchase.transactionDate = today.minusDays(rand.nextInt(5));
                purchase.item = item;
                purchase.transactionType = "PURCHASE";
                purchase.quantity = BigDecimal.valueOf(rand.nextInt(50) + 10);
                purchase.referenceNo = "PO-" + rand.nextInt(1000);
                purchase.persist();
            }

            // Sales for finished goods
            for (Item item : finishedItems) {
                StockTransaction sale = new StockTransaction();
                sale.transactionDate = today.minusDays(rand.nextInt(3));
                sale.item = item;
                sale.transactionType = "SALE";
                sale.quantity = BigDecimal.valueOf(rand.nextInt(20) + 1);
                sale.referenceNo = "INV-" + rand.nextInt(1000);
                sale.persist();
            }
        }

        // --------------------------
        // PRODUCTION BATCHES & CONSUMPTION
        // --------------------------
        if (ProductionBatch.count() == 0) {
            List<Item> finishedItems = Item.find("itemType.itemTypeCode", "FINISHED_GOOD").list();
            List<Item> rawItems = Item.find("itemType.itemTypeCode", "RAW_MATERIAL").list();
            LocalDate today = LocalDate.now();

            int batchCounter = 1;
            for (Item finished : finishedItems) {
                int batches = rand.nextInt(3) + 1;
                for (int i = 0; i < batches; i++) {
                    ProductionBatch batch = new ProductionBatch();
                    batch.batchCode = "PB" + String.format("%03d", batchCounter++);
                    batch.productionDate = today.minusDays(rand.nextInt(3));
                    batch.finishedItem = finished;
                    batch.outputQty = BigDecimal.valueOf(rand.nextInt(30) + 5);
                    batch.persist();

                    // Consume 1–3 raw items randomly
                    int consumeCount = rand.nextInt(3) + 1;
                    for (int j = 0; j < consumeCount; j++) {
                        Item raw = rawItems.get(rand.nextInt(rawItems.size()));
                        ProductionConsumption pc = new ProductionConsumption();
                        pc.batch = batch;
                        pc.rawItem = raw;
                        pc.consumedQty = BigDecimal.valueOf(rand.nextInt(10) + 1);
                        pc.persist();
                    }

                    // Add a STOCK_TRANSACTION for PRODUCTION_OUTPUT
                    StockTransaction output = new StockTransaction();
                    output.transactionDate = batch.productionDate;
                    output.item = finished;
                    output.transactionType = "PRODUCTION_OUTPUT";
                    output.quantity = batch.outputQty;
                    output.referenceNo = batch.batchCode;
                    output.persist();
                }
            }
        }

        System.out.println("Dummy inventory data loaded successfully!");
    }
}

