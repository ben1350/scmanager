package rest;

import com.rosswood.entity.Customer;
import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import com.rosswood.entity.ProductionConsumption;
import com.rosswood.event.ProductionFinishedEvent;
import com.rosswood.interceptor.UpdatesStock;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Path("/production")
public class ProductionBatchController extends HxController {

    @Inject
    jakarta.enterprise.event.Event<ProductionFinishedEvent> finishedEvent;

    private static final int PAGE_SIZE = 5;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance batch(List<ProductionBatch> batches, List<Item> items, int page, int totalPages);
        public static native TemplateInstance batch$rows(List<ProductionBatch> batches, int page, int totalPages);
        public static native TemplateInstance batchFormFragment(List<Item> items);
        public static native TemplateInstance consumptionDetail(ProductionBatch batch, List<Item> rawItems);
        public static native TemplateInstance editQtyForm(ProductionBatch batch);
    }

    @GET
    public TemplateInstance batch(@RestQuery Integer page) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        return render(currentPage, isHxRequest());
    }

    @GET
    @Path("/new-form")
    public TemplateInstance getFormFragment() {
        onlyHxRequest();
        return Templates.batchFormFragment(Item.listAll());
    }

    @GET
    @Path("/{id}/consumption")
    public TemplateInstance getConsumption(@RestPath Long id) {
        onlyHxRequest();
        ProductionBatch batch = ProductionBatch.findById(id);
        return Templates.consumptionDetail(batch, Item.listAll());
    }

    @POST
    @Transactional
    public TemplateInstance add(
            @RestForm @NotBlank String batchCode,
            @RestForm String productionDate,
            @RestForm Long finishedItemId,
            @RestForm BigDecimal outputQty,
            @RestQuery Integer page
    ) {
        onlyHxRequest();
        ProductionBatch batch = new ProductionBatch();

        long count = ProductionBatch.count() + 1;
        batch.batchCode = "PRD-" + String.format("%05d", count);
        batch.productionDate = LocalDate.parse(productionDate);
        batch.finishedItem = Item.findById(finishedItemId);
        batch.outputQty = outputQty;
        batch.persist();

        return render(Optional.ofNullable(page).orElse(1), true);
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public TemplateInstance delete(@RestPath Long id, @RestQuery Integer page) {
        onlyHxRequest();

        ProductionBatch batch = ProductionBatch.findById(id);
        if (batch != null) {
            // Double-check status before allowing deletion
            if (!"FINISHED".equals(batch.status)) {
                batch.delete();
            } else {
                // Optional: You could throw an exception or return a specific
                // header/message saying "Cannot delete finished batches"
                throw new WebApplicationException("Cannot delete a finalized batch", 403);
            }
        }

        return render(Optional.ofNullable(page).orElse(1), true);
    }


    private TemplateInstance render(int page, boolean fragmentOnly) {
        long totalCount = ProductionBatch.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        List<ProductionBatch> batches = ProductionBatch.find("order by productionDate desc")
                .page(page - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.batch$rows(batches, page, totalPages)
                : Templates.batch(batches, Item.listAll(), page, totalPages);
    }

    @GET
    @Path("/{id}/edit-qty")
    public TemplateInstance editQty(@RestPath Long id) {
        onlyHxRequest();
        ProductionBatch batch = ProductionBatch.findById(id);
        return Templates.editQtyForm(batch); // We will define this template below
    }

    @POST
    @Path("/{id}/update-qty")
    @Transactional
    public TemplateInstance updateQty(
            @RestPath Long id,
            @RestForm BigDecimal outputQty,
            @RestQuery Integer page
    ) {
        onlyHxRequest();
        ProductionBatch batch = ProductionBatch.findById(id);
        if (batch != null && !"FINISHED".equals(batch.status)) {
            batch.outputQty = outputQty;
            batch.persist();

        }
        // Return the updated rows to refresh the UI
        return render(Optional.ofNullable(page).orElse(1), true);
    }



    @POST
    @Path("/{id}/finish")
    @Transactional
    public TemplateInstance finishBatch(@RestPath Long id, @RestQuery Integer page) {
        onlyHxRequest();

        ProductionBatch batch = ProductionBatch.findById(id);
        if (batch == null) throw new NotFoundException();

        // Set status to Finished
        batch.status = "FINISHED";
        batch.persist();
        finishedEvent.fire(new ProductionFinishedEvent(batch, batch.outputQty));

        // Return the updated rows fragment
        return render(Optional.ofNullable(page).orElse(1), true);
    }
}


/*

TODO ALERTS RE - ORDER PROCUREMENT
 */