package rest;

import com.rosswood.entity.Item;
import com.rosswood.entity.StockOpening;
import com.rosswood.entity.StockTransaction;
import com.rosswood.service.StockTransactionService;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import org.jboss.resteasy.reactive.RestQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Path("/stock-transactions")
public class StockTransactionController extends HxController {

    @Inject
    StockTransactionService stockService;

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<StockTransaction> transactions, List<Item> items, int page, int totalPages);
        public static native TemplateInstance index$rows(List<StockTransaction> transactions, int page, int totalPages);
    }

    @GET
    public TemplateInstance index(@RestQuery Integer page) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        return render(currentPage, isHxRequest());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm Long itemId,
            @RestForm String transactionType,
            @RestForm BigDecimal quantity,
            @RestForm String transactionDate,
            @RestForm String referenceNo,
            @RestForm String remarks,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        Item item = Item.findById(itemId);
        LocalDate date = LocalDate.parse(transactionDate);

        switch (StockTransaction.TransactionType.valueOf(transactionType)) {
            case PURCHASE ->
                    stockService.postPurchase(item, quantity,
                            item.uom != null ? item.uom.uomCode : null,
                            referenceNo, date, remarks);

            case OPENING_STOCK -> {
                StockOpening opening = new StockOpening();
                opening.item       = item;
                opening.openingQty = quantity;
                opening.stockDate  = date;
                opening.persist();
                stockService.postOpeningStock(opening);
            }

            case DAMAGE ->
                    stockService.postDamage(item, quantity, null, referenceNo, remarks);

            case EXPIRY_WRITEOFF ->
                    stockService.postExpiryWriteOff(item, quantity, null, date, remarks);

            case ADJUSTMENT_IN ->
                    stockService.postAdjustmentIn(item, quantity, referenceNo, remarks);

            case ADJUSTMENT_OUT ->
                    stockService.postAdjustmentOut(item, quantity, referenceNo, remarks);

            default -> throw new IllegalArgumentException(
                    "'" + transactionType + "' cannot be posted manually. " +
                            "Use the production or sales workflow.");
        }

        return render(Optional.ofNullable(page).orElse(1), true);
    }

    @POST
    @Path("/delete/{id}")
    @Transactional
    public TemplateInstance delete(@RestPath Long id, @RestForm Integer page) {
        onlyHxRequest();
        StockTransaction.deleteById(id);
        return render(Optional.ofNullable(page).orElse(1), true);
    }

    private TemplateInstance render(int page, boolean fragmentOnly) {
        long totalCount = StockTransaction.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<StockTransaction> transactions = StockTransaction.find("order by transactionDate desc, id desc")
                .page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(transactions, currentPage, totalPages)
                : Templates.index(transactions, Item.listAll(), currentPage, totalPages);
    }
}
