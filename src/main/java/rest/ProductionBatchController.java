package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
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

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance batch(
                List<ProductionBatch> batches,
                List<Item> items,
                int page,
                int totalPages
        );

        public static native TemplateInstance batch$rows(
                List<ProductionBatch> batches,
                int page,
                int totalPages
        );
    }

    @GET
    public TemplateInstance batch(@RestQuery Integer page) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        if (page==null){
            return render(currentPage,false);
        }else return render(currentPage,true);

    }

    @POST
    @Transactional
    public TemplateInstance delete(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();
        ProductionBatch.deleteById(id);
        return render(page, true);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm @NotBlank String batchCode,
            @RestForm String productionDate,
            @RestForm Long finishedItemId,
            @RestForm BigDecimal outputQty,
            @RestForm String remarks,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        ProductionBatch batch = new ProductionBatch();
        batch.batchCode = batchCode;
        batch.productionDate = LocalDate.parse(productionDate);
        batch.finishedItem = Item.findById(finishedItemId);
        batch.outputQty = outputQty;
        batch.remarks = remarks;
        batch.persist();

        // After adding, we return the fragment of the current page (or page 1)
        return render(Optional.ofNullable(page).orElse(1), true);
    }

    // Add/Update logic omitted for brevity—they would also call render()

    private TemplateInstance render(Integer page, boolean fragmentOnly) {
        long totalCount = ProductionBatch.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page == null ? 1 : page, totalPages);

        List<ProductionBatch> batches = ProductionBatch.find("order by productionDate desc")
                .page(currentPage - 1, PAGE_SIZE).list();

        if (isHxRequest()) {
            return Templates.batch$rows(batches, currentPage, totalPages);
        }
        return Templates.batch(batches, Item.listAll(), currentPage, totalPages);
    }
}
