package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.ItemType;
import com.rosswood.entity.UnitOfMeasure;
import com.rosswood.entity.ItemUom;
import com.rosswood.entity.ItemUomConversion;
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
import java.util.List;
import java.util.Optional;

@Path("/items")
public class ItemController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate(requireTypeSafeExpressions = false)
    public static class Templates {
        public static native TemplateInstance index(List<Item> items, List<ItemType> itemTypes, List<UnitOfMeasure> uoms, int page, int totalPages, String q);
        public static native TemplateInstance index$rows(List<Item> items, int page, int totalPages, String q);
        public static native TemplateInstance itemFormFragment(List<ItemType> itemTypes, List<UnitOfMeasure> uoms, int page);
        // Updated to include both global uoms (for selection) and item-specific itemUoms (for display/conversions)
        public static native TemplateInstance itemDetailPane(Item item, List<UnitOfMeasure> uoms, List<ItemUom> itemUoms, List<ItemUomConversion> itemConversions);
    }

    @GET
    public TemplateInstance index(@RestQuery Integer page, @RestQuery String q) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        return render(currentPage, isHxRequest(), q);
    }

    @GET
    @Path("/form-fragment")
    public TemplateInstance getFormFragment(@RestQuery Integer page) {
        onlyHxRequest();
        return Templates.itemFormFragment(ItemType.listAll(), UnitOfMeasure.listAll(), page != null ? page : 1);
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
        item.itemCode = itemCode.toUpperCase().trim();
        item.itemName = itemName;
        item.itemType = ItemType.findById(itemTypeId);
        item.uom = UnitOfMeasure.findById(uomId);
        item.activeFlag = true;
        item.persist();

        ItemUom itemUom= new ItemUom();
        itemUom.item = item;
        itemUom.isBase = true;
        itemUom.uomCode = item.uom.uomCode;
        itemUom.persist();


        return render(Optional.ofNullable(page).orElse(1), true, null);
    }

    @GET
    @Path("/{id}/details")
    public TemplateInstance getDetails(@RestPath Long id) {
        onlyHxRequest();
        Item item = Item.findById(id);
        return renderDetailPane(item);
    }

    @POST
    @Path("/{id}/uom")
    @Transactional
    public TemplateInstance addItemUom(
            @RestPath Long id,
            @RestForm Long uomId
    ) {
        onlyHxRequest();
        Item item = Item.findById(id);
        UnitOfMeasure uom = UnitOfMeasure.findById(uomId);

        // Check if UOM already exists for this item to prevent duplicates
        long exists = ItemUom.count("item = ?1 and uomCode = ?2", item, uom.uomCode);
        if (exists == 0 && !item.uom.uomCode.equals(uom.uomCode)) {
            ItemUom itemUom = new ItemUom();
            itemUom.item = item;
            itemUom.uomCode = uom.uomCode;
            itemUom.persist();
        }

        return renderDetailPane(item);
    }

    @POST
    @Path("/{id}/conversion")
    @Transactional
    public TemplateInstance addConversion(
            @RestPath Long id,
            @RestForm String fromUom,
            @RestForm String toUom,
            @RestForm BigDecimal factor
    ) {
        onlyHxRequest();
        Item item = Item.findById(id);

        ItemUomConversion conversion = new ItemUomConversion();
        conversion.item = item;
        conversion.fromUom = fromUom.toUpperCase().trim();
        conversion.toUom = toUom.toUpperCase().trim();
        conversion.conversionFactor = factor;
        conversion.persist();

        return renderDetailPane(item);
    }

    /**
     * Update reorder level on an item.
     */
    @POST
    @Path("/{id}/reorder-level")
    @Transactional
    public TemplateInstance updateReorderLevel(
            @RestPath Long id,
            @RestForm BigDecimal reorderLevel
    ) {
        onlyHxRequest();
        Item item = Item.findById(id);
        if (item == null) throw new NotFoundException();
        item.reorderLevel = reorderLevel;
        item.persist();
        return renderDetailPane(item);
    }

    /**
     * Update selling price and VAT rate on an item.
     */
    @POST
    @Path("/{id}/price")
    @Transactional
    public TemplateInstance updatePrice(
            @RestPath Long id,
            @RestForm BigDecimal sellingPriceExVat,
            @RestForm BigDecimal vatRate
    ) {
        onlyHxRequest();
        Item item = Item.findById(id);
        if (item == null) throw new NotFoundException();
        item.sellingPriceExVat = sellingPriceExVat;
        item.vatRate = vatRate != null ? vatRate : BigDecimal.ZERO;
        item.persist();
        return renderDetailPane(item);
    }

    /**
     * Helper to render the detail pane with all necessary data lists
     */
    private TemplateInstance renderDetailPane(Item item) {
        List<UnitOfMeasure> allUoms = UnitOfMeasure.listAll();
        List<ItemUom> itemUoms = ItemUom.find("item", item).list();
        List<ItemUomConversion> convs = ItemUomConversion.find("item", item).list();
        return Templates.itemDetailPane(item, allUoms, itemUoms, convs);
    }

    private TemplateInstance render(int page, boolean fragmentOnly, String q) {
        String term = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase() + "%" : null;
        long totalCount = term != null
                ? Item.count("lower(itemCode) like ?1 or lower(itemName) like ?1", term)
                : Item.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<Item> items = term != null
                ? Item.find("(lower(itemCode) like ?1 or lower(itemName) like ?1) order by createdAt desc", term)
                        .page(currentPage - 1, PAGE_SIZE).list()
                : Item.find("order by createdAt desc").page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(items, currentPage, totalPages, q)
                : Templates.index(items, ItemType.listAll(), UnitOfMeasure.listAll(), currentPage, totalPages, q);
    }
}