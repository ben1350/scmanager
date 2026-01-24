package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Path("/production")
public class ProductionBatchController extends HxController {

    // -------------------------------------------------
    // Templates
    // -------------------------------------------------
    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance batch(
                List<ProductionBatch> batches,
                List<Item> items,
                ProductionBatch batch
        );

        public static native TemplateInstance batch$rows(
                List<ProductionBatch> batches
        );
    }

    /**
     * GET /production
     */
    @Path("")
    public TemplateInstance batch() {
        return Templates.batch(
                ProductionBatch.listAll(),
                Item.listAll(),
                null
        );
    }

    /**
     * POST /production
     * (Add new Production Batch)
     */
    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm @NotBlank String batchCode,
            @RestForm String productionDate,
            @RestForm Long finishedItemId,
            @RestForm BigDecimal outputQty,
            @RestForm String remarks
    ) {
        onlyHxRequest();

        ProductionBatch batch = new ProductionBatch();
        batch.batchCode = batchCode;
        batch.productionDate = LocalDate.parse(productionDate);
        batch.finishedItem = Item.findById(finishedItemId);
        batch.outputQty = outputQty;
        batch.remarks = remarks;
        batch.persist();

        return Templates.batch$rows(
                ProductionBatch.listAll()
        );
    }

    /**
     * POST /production/delete/{id}
     */
    @POST
    @Transactional
    public TemplateInstance delete(@RestPath Long id) {
        onlyHxRequest();

        ProductionBatch batch = ProductionBatch.findById(id);
        notFoundIfNull(batch);

        batch.delete();

        return Templates.batch$rows(
                ProductionBatch.listAll()
        );
    }
}

