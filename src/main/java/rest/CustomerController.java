package rest;

import com.rosswood.entity.Customer;
import com.rosswood.entity.CustomerBranch;
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

@Path("/customers")
public class CustomerController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        // Main view and row fragment
        public static native TemplateInstance index(List<Customer> customers, int page, int totalPages);
        public static native TemplateInstance index$rows(List<Customer> customers, int page, int totalPages);

        // Standalone template for branch details (separate file: branchDetail.html)
        public static native TemplateInstance branchDetail(Customer customer);
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
            @RestForm @NotBlank String customerCode,
            @RestForm @NotBlank String name,
            @RestForm String address,
            @RestForm String email,
            @RestForm String phone,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        Customer customer = new Customer();
        customer.customerCode = customerCode;
        customer.name = name;
        customer.address = address;
        customer.email = email;
        customer.phone = phone;
        customer.persist();

        return render(Optional.ofNullable(page).orElse(1), true);
    }

    @GET
    @Path("/{id}/branches")
    public TemplateInstance getBranches(@RestPath Long id) {
        onlyHxRequest();
        Customer customer = Customer.findById(id);
        if (customer == null) throw new NotFoundException();
        return Templates.branchDetail(customer);
    }

    @POST
    @Path("/{id}/branches")
    @Transactional
    public TemplateInstance addBranch(
            @RestPath Long id,
            @RestForm @NotBlank String branchName,
            @RestForm String branchAddress
    ) {
        onlyHxRequest();
        Customer customer = Customer.findById(id);

        CustomerBranch branch = new CustomerBranch();
        branch.branchName = branchName;
        branch.branchAddress = branchAddress;

        customer.addBranch(branch);
        branch.persist();

        return Templates.branchDetail(customer);
    }

    private TemplateInstance render(int page, boolean fragmentOnly) {
        long totalCount = Customer.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<Customer> customers = Customer.find("order by id desc")
                .page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(customers, currentPage, totalPages)
                : Templates.index(customers, currentPage, totalPages);
    }
}