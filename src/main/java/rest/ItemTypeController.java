package rest;

import com.rosswood.entity.ItemType;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;

@Path("/itemtype")
public class ItemTypeController extends HxController {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance itemtype(List<ItemType> itemTypes, ItemType it);
        public static native TemplateInstance itemtype$rows(List<ItemType> itemTypes);
        public static native TemplateInstance itemtype$row_readonly(ItemType it);
        public static native TemplateInstance itemtype$row_edit(ItemType it);
    }

    /**
     * GET /itemtype
     */
    @Path("")
    public TemplateInstance itemtype() {
        return Templates.itemtype(ItemType.listAll(),null);
    }

    /**
     * POST /itemtype
     * (Add new ItemType)
     */
    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(@RestForm @NotBlank String itemTypeCode) {
        onlyHxRequest();

        ItemType it = new ItemType();
        it.itemTypeCode = itemTypeCode;
        it.persist();

        return Templates.itemtype$rows(ItemType.listAll());
    }

    /**
     * GET /itemtype/edit/{id}
     */
    public TemplateInstance edit(@RestPath Long id) {
        onlyHxRequest();

        ItemType it = ItemType.findById(id);
        notFoundIfNull(it);

        return Templates.itemtype$row_edit(it);
    }

    /**
     * POST /itemtype/update/{id}
     */
    @POST
    @Transactional
    public TemplateInstance update(@RestPath Long id,
                                   @RestForm @NotBlank String itemTypeCode) {

        onlyHxRequest();

        ItemType it = ItemType.findById(id);
        notFoundIfNull(it);

        it.itemTypeCode = itemTypeCode;

        return Templates.itemtype$row_readonly(it);
    }

    /**
     * GET /itemtype/cancel/{id}
     */
    public TemplateInstance cancel(@RestPath Long id) {
        onlyHxRequest();

        ItemType it = ItemType.findById(id);
        notFoundIfNull(it);

        return Templates.itemtype$row_readonly(it);
    }

    /**
     * DELETE /itemtype/{id}
     */
    @Transactional
    @POST
    public TemplateInstance delete(@RestPath Long id) {
        onlyHxRequest();

        ItemType it = ItemType.findById(id);
        notFoundIfNull(it);

        it.delete();

        return Templates.itemtype$rows(ItemType.listAll());
    }
}

