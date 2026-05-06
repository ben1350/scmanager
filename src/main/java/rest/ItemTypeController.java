package rest;

import com.rosswood.entity.ItemType;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;
import java.util.List;

@Path("/itemtype")
public class ItemTypeController extends HxController {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance itemtype(List<ItemType> itemTypes);
        public static native TemplateInstance itemtype$rows(List<ItemType> itemTypes);
        public static native TemplateInstance itemtype$row_readonly(ItemType it);
        public static native TemplateInstance itemtype$row_edit(ItemType it);
    }

    @GET
    public TemplateInstance itemtype() {
        return Templates.itemtype(ItemType.listAll());
    }

    @POST
    @Transactional
    public TemplateInstance add(@RestForm @NotBlank String itemTypeCode) {
        onlyHxRequest();
        ItemType it = new ItemType();
        it.itemTypeCode = itemTypeCode.toUpperCase().trim();
        it.persist();
        return Templates.itemtype$rows(ItemType.listAll());
    }

    @GET @Path("/{id}/edit")
    public TemplateInstance edit(@RestPath Long id) {
        onlyHxRequest();
        return Templates.itemtype$row_edit(ItemType.findById(id));
    }

    @POST @Path("/{id}/update") @Transactional
    public TemplateInstance update(@RestPath Long id, @RestForm @NotBlank String itemTypeCode) {
        onlyHxRequest();
        ItemType it = ItemType.findById(id);
        it.itemTypeCode = itemTypeCode.toUpperCase().trim();
        return Templates.itemtype$row_readonly(it);
    }

    @GET @Path("/{id}/cancel")
    public TemplateInstance cancel(@RestPath Long id) {
        onlyHxRequest();
        return Templates.itemtype$row_readonly(ItemType.findById(id));
    }

    @POST @Path("/{id}/delete") @Transactional
    public TemplateInstance delete(@RestPath Long id) {
        onlyHxRequest();
        ItemType.deleteById(id);
        return Templates.itemtype$rows(ItemType.listAll());
    }
}

