package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.StockOpening;
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

@Path("/stock-opening")
public class StockOpeningController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance opening(
                List<StockOpening> openings,
                List<Item> items,
                int page,
                int totalPages,
                String q
        );

        public static native TemplateInstance opening$rows(
                List<StockOpening> openings,
                int page,
                int totalPages,
                String q
        );
    }

    @GET
    public TemplateInstance opening(@RestQuery Integer page, @RestQuery String q) {
        int currentPage = (page == null || page < 1) ? 1 : page;
        return render(currentPage, page != null, q);
    }

    @POST
    @Transactional
    @Path("/{id}/delete")
    public TemplateInstance delete(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();
        StockOpening.deleteById(id);
        return render(page, true, null);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm String stockDate,
            @RestForm Long itemId,
            @RestForm BigDecimal openingQty,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        StockOpening opening = new StockOpening();
        opening.stockDate = LocalDate.parse(stockDate);
        opening.item = Item.findById(itemId);
        opening.openingQty = openingQty;
        opening.persist();

        return render(Optional.ofNullable(page).orElse(1), true, null);
    }

    private TemplateInstance render(Integer page, boolean fragmentOnly, String q) {
        String term = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase() + "%" : null;
        long totalCount = term != null
                ? StockOpening.count("lower(item.itemName) like ?1 or lower(item.itemCode) like ?1", term)
                : StockOpening.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page == null ? 1 : page, totalPages);

        List<StockOpening> openings = term != null
                ? StockOpening.find("(lower(item.itemName) like ?1 or lower(item.itemCode) like ?1) order by stockDate desc", term)
                        .page(currentPage - 1, PAGE_SIZE).list()
                : StockOpening.find("order by stockDate desc").page(currentPage - 1, PAGE_SIZE).list();

        if (fragmentOnly || isHxRequest()) {
            return Templates.opening$rows(openings, currentPage, totalPages, q);
        }
        return Templates.opening(openings, Item.listAll(), currentPage, totalPages, q);
    }
}
