package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ProductionBatch;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import java.util.List;

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
