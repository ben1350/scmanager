package rest;

import com.rosswood.entity.UnitOfMeasure;
import io.quarkiverse.renarde.Controller;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;

@Path("/uom")
public class Uom extends HxController {

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance uom(List<UnitOfMeasure> uoms,UnitOfMeasure uom);
        public static native TemplateInstance uom$rows(List<UnitOfMeasure> uoms);
        public static native TemplateInstance uom$row_readonly(UnitOfMeasure uom);
        public static native TemplateInstance uom$row_edit(UnitOfMeasure uom);
    }

    @Path("")
    public TemplateInstance uom() {
        return Templates.uom(UnitOfMeasure.listAll(),null);

    }

    @Transactional
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(@RestForm @NotBlank String uomCode, @RestForm String description) {
        onlyHxRequest();
        new UnitOfMeasure(uomCode, description).persist();
        return Templates.uom$rows(UnitOfMeasure.listAll());
    }

    public TemplateInstance edit(@RestPath Long id) {
        onlyHxRequest();
        UnitOfMeasure uom = UnitOfMeasure.findById(id);
        return Templates.uom$row_edit(uom);

    }

    @Transactional
    @POST
    public TemplateInstance update(@RestPath Long id,
                                   @RestForm String uomCode,
                                   @RestForm String description) {

        UnitOfMeasure uom = UnitOfMeasure.findById(id);
        notFoundIfNull(uom);

        uom.uomCode = uomCode;
        uom.description = description;

        return Templates.uom$row_readonly(uom);
    }


    public TemplateInstance cancel(@RestPath Long id) {
        return Templates.uom$row_readonly(UnitOfMeasure.findById(id));
    }
}


