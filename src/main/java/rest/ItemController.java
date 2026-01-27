package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ItemType;
import com.rosswood.entity.UnitOfMeasure;
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

import java.util.List;
import java.util.Optional;

@Path("/items")
public class ItemController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(
                List<Item> items,
                List<ItemType> itemTypes,
                List<UnitOfMeasure> uoms,
                int page,
                int totalPages
        );

        public static native TemplateInstance index$rows(
                List<Item> items,
                int page,
                int totalPages
        );
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
            @RestForm @NotBlank String itemCode,
            @RestForm @NotBlank String itemName,
            @RestForm Long itemTypeId,
            @RestForm Long uomId,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        Item item = new Item();
        item.itemCode = itemCode;
        item.itemName = itemName;
        item.itemType = ItemType.findById(itemTypeId);
        item.uom = UnitOfMeasure.findById(uomId);
        item.activeFlag = true;
        item.persist();

        return render(Optional.ofNullable(page).orElse(1), true);
    }

    @POST
    @Path("/delete/{id}")
    @Transactional
    public TemplateInstance delete(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();
        Item.deleteById(id);
        return render(Optional.ofNullable(page).filter(p -> p >= 1).orElse(1), true);
    }

    private TemplateInstance render(int page, boolean fragmentOnly) {
        long totalCount = Item.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<Item> items = Item.find("order by createdAt desc")
                .page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(items, currentPage, totalPages)
                : Templates.index(items, ItemType.listAll(), UnitOfMeasure.listAll(), currentPage, totalPages);
    }
}
