package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import com.rosswood.entity.ProductionConsumption;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.math.BigDecimal;

@Path("/consumption")
public class ProductionConsumptionController extends HxController {

    @POST
    @Path("/batch/{batchId}")
    @Transactional
    public TemplateInstance add(
            @RestPath Long batchId,
            @RestForm Long rawItemId,
            @RestForm BigDecimal consumedQty
    ) {
        onlyHxRequest();
        ProductionBatch batch = ProductionBatch.findById(batchId);
        ProductionConsumption c = new ProductionConsumption();
        c.batch = batch;
        c.rawItem = Item.findById(rawItemId);
        c.consumedQty = consumedQty;
        batch.consumptions.add(c);
        c.persistAndFlush();

        return ProductionBatchController.Templates.consumptionDetail(batch, Item.listAll());
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public TemplateInstance delete(@RestPath Long id) {
        onlyHxRequest();
        ProductionConsumption c = ProductionConsumption.findById(id);
        ProductionBatch batch = c.batch;
        c.delete();
        return ProductionBatchController.Templates.consumptionDetail(batch, Item.listAll());
    }
}
