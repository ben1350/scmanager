package com.rosswood.inventory;

import com.rosswood.entity.ItemType;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/itemtypes")
@Produces(MediaType.TEXT_HTML)
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
public class ItemTypeController {

    @Inject
    @Location("pub/item_type/item_type")
    Template itemTypeTmpl;

    @GET
    public TemplateInstance list() {
        return itemTypeTmpl.data("itemTypes", ItemType.listAll());
    }

    @POST
    @Path("/add")
    @Transactional
    public TemplateInstance add(@FormParam("itemTypeCode") String code) {
        ItemType it = new ItemType();
        it.itemTypeCode = code;
        it.persist();
        return itemTypeTmpl.getFragment("rows").data("itemTypes", ItemType.listAll());
    }

    @POST
    @Path("/delete")
    @Transactional
    public TemplateInstance delete(@FormParam("id") Long id) {
        ItemType.deleteById(id);
        return itemTypeTmpl.getFragment("rows").data("itemTypes", ItemType.listAll());
    }

    @GET
    @Path("/edit/{id}")
    public TemplateInstance editForm(@PathParam("id") Long id) {
        return itemTypeTmpl.getFragment("row_edit").data("it", ItemType.findById(id));
    }

    @GET
    @Path("/cancel/{id}")
    public TemplateInstance cancel(@PathParam("id") Long id) {
        ItemType it = ItemType.findById(id);
        return itemTypeTmpl.getFragment("row-readonly").data("it", it); //
    }

    @POST
    @Path("/update/{id}")
    @Transactional
    public TemplateInstance update(@PathParam("id") Long id, @FormParam("itemTypeCode") String code) {
        ItemType it = ItemType.findById(id);
        if (it != null) {
            it.itemTypeCode = code; //
        }
        return itemTypeTmpl.getFragment("row_readonly").data("it", it); //
    }
}
