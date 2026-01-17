package com.rosswood.inventory;

import com.rosswood.entity.UnitOfMeasure;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/uoms")
@Produces(MediaType.TEXT_HTML)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class UomController {

    @Inject
    @Location("pub/uom/uom")
    Template uoms;

    @Inject
    @Location("pub/uom/uom-rows")
    Template uomRows;

    @Inject
    @Location("pub/uom/uom-row-edit")
    Template uomRowEdit;

    @Inject
    @Location("pub/uom/uom-row-readonly")
    Template uomRow;

    // Show UOM page
    @GET
    public TemplateInstance list() {
        List<UnitOfMeasure> all = UnitOfMeasure.listAll();
        return uoms.data("uoms", all);
    }

    // Add UOM
    @POST
    @Path("/add")
    @Transactional
    public TemplateInstance add(@FormParam("uomCode") String uomCode,
                                @FormParam("description") String description) {
        UnitOfMeasure.persist(new UnitOfMeasure(uomCode, description));
        var allUnits = UnitOfMeasure.listAll();
        return uomRows.data("uoms", allUnits);
    }

    // Delete UOM
    @POST
    @Path("/delete")
    @Transactional
    public TemplateInstance delete(@FormParam("id") Long id) {
        UnitOfMeasure.deleteById(id);
        var allUnits = UnitOfMeasure.listAll();
        return uomRows.data("uoms", allUnits);
    }

    // Show Edit Form (inline)
    @GET
    @Path("/edit/{id}")
    public TemplateInstance editForm(@PathParam("id") Long id) {
        UnitOfMeasure uom = UnitOfMeasure.findById(id);
        return uomRowEdit.data("uom",uom);
    }

    @GET
    @Path("/cancel/{id}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance row(@PathParam("id") Long id) {
        UnitOfMeasure uom = UnitOfMeasure.findById(id);
        return uomRow.data("uom",uom);
    }


    // Update UOM
    @POST
    @Path("/update/{id}")
    @Transactional
    public TemplateInstance edit(@PathParam("id") Long id,
                                 @FormParam("uomCode") String uomCode,
                                 @FormParam("description") String description) {
        UnitOfMeasure uom = UnitOfMeasure.findById(id);
        if (uom != null) {
            uom.uomCode = uomCode;
            uom.description = description;
            uom.persist();
        }
        return uomRow.data("uom",uom);

    }
}

