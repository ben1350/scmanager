package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import com.rosswood.entity.ProductionConsumption;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Path("/consumption")
public class ProductionConsumptionController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<ProductionConsumption> consumptions, List<ProductionBatch> batches, List<Item> rawItems, int page, int totalPages);
        public static native TemplateInstance index$rows(List<ProductionConsumption> consumptions, int page, int totalPages);
    }

    @GET
    @Path("")
    public TemplateInstance index(@RestQuery Integer page) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        return render(currentPage, isHxRequest());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm Long batchId,
            @RestForm Long rawItemId,
            @RestForm BigDecimal consumedQty,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        ProductionConsumption consumption = new ProductionConsumption();
        consumption.batch = ProductionBatch.findById(batchId);
        consumption.rawItem = Item.findById(rawItemId);
        consumption.consumedQty = consumedQty;
        consumption.persist();

        // Standard logic: find current page and return rows
        return render(Optional.ofNullable(page).orElse(1), true);
    }

    @POST
    @Path("/delete/{id}")
    @Transactional
    public TemplateInstance delete(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();
        ProductionConsumption.deleteById(id);
        return render(Optional.ofNullable(page).orElse(1), true);
    }

    private TemplateInstance render(int page, boolean fragmentOnly) {
        long totalCount = ProductionConsumption.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<ProductionConsumption> consumptions = ProductionConsumption.find("order by id desc")
                .page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(consumptions, currentPage, totalPages)
                : Templates.index(consumptions, ProductionBatch.listAll(), Item.listAll(), currentPage, totalPages);
    }
}
