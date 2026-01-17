package com.rosswood.inventory;

import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.time.LocalDate;

@Path("/production")
@Produces(MediaType.TEXT_HTML)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class ProductionBatchController {

    @Inject
    @Location("pub/production/batch")
    Template batchTmpl;

    @GET
    public TemplateInstance list() {
        return batchTmpl.data("batches", ProductionBatch.listAll())
                .data("items", Item.listAll());
    }

    @POST
    @Path("/add")
    @Transactional
    public TemplateInstance add(@FormParam("batchCode") String batchCode,
                                @FormParam("productionDate") String date,
                                @FormParam("finishedItemId") Long itemId,
                                @FormParam("outputQty") BigDecimal qty,
                                @FormParam("remarks") String remarks) {

        ProductionBatch batch = new ProductionBatch();
        batch.batchCode = batchCode;
        batch.productionDate = LocalDate.parse(date);
        batch.finishedItem = Item.findById(itemId);
        batch.outputQty = qty;
        batch.remarks = remarks;
        batch.persist();

        return batchTmpl.getFragment("rows").data("batches", ProductionBatch.listAll());
    }

    @POST
    @Path("/delete")
    @Transactional
    public TemplateInstance delete(@FormParam("id") Long id) {
        ProductionBatch.deleteById(id);
        return batchTmpl.getFragment("rows").data("batches", ProductionBatch.listAll());
    }
}
