package rest;

import com.rosswood.entity.Customer;
import com.rosswood.entity.Customer.CustomerType;
import com.rosswood.entity.CustomerBranch;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
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

@Path("/customers")
@Authenticated
public class CustomerController extends HxController {

    private static final int PAGE_SIZE = 10;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<Customer> customers, int page, int totalPages, String q);
        public static native TemplateInstance index$rows(List<Customer> customers, int page, int totalPages, String q);
        public static native TemplateInstance branchDetail(Customer customer);
        public static native TemplateInstance customerFormFragment();
    }

    @GET
    public TemplateInstance index(@RestQuery Integer page, @RestQuery String q) {
        int currentPage = Optional.ofNullable(page).filter(p -> p >= 1).orElse(1);
        return render(currentPage, isHxRequest(), q);
    }

    /**
     * Serves the empty form row. Target: #insertion-point
     */
    @GET
    @Path("/new-form-fragment")
    public TemplateInstance getFormFragment() {
        onlyHxRequest();
        return Templates.customerFormFragment();
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
            @RestForm String customerType,
            @RestForm BigDecimal creditLimit,
            @RestForm Integer page
    ) {
        onlyHxRequest();

        Customer customer = new Customer();
        long count = Customer.count() + 1;
        customer.customerCode = "ROSS-" + String.format("%05d", count);
        customer.name = name;
        customer.address = address;
        customer.email = email;
        customer.phone = phone;
        customer.customerType = (customerType != null && customerType.equals("CREDIT"))
                ? CustomerType.CREDIT : CustomerType.CASH;
        customer.creditLimit = (customer.customerType == CustomerType.CREDIT) ? creditLimit : null;
        customer.persist();

        // Refresh only the rows fragment; #insertion-point will naturally be empty again
        return render(Optional.ofNullable(page).orElse(1), true, null);
    }

    @POST
    @Path("/{id}/credit-settings")
    @Transactional
    public TemplateInstance updateCreditSettings(
            @RestPath Long id,
            @RestForm String customerType,
            @RestForm BigDecimal creditLimit
    ) {
        onlyHxRequest();
        Customer customer = Customer.findById(id);
        if (customer == null) throw new NotFoundException();
        customer.customerType = "CREDIT".equals(customerType) ? CustomerType.CREDIT : CustomerType.CASH;
        customer.creditLimit  = (customer.customerType == CustomerType.CREDIT) ? creditLimit : null;
        customer.persist();
        return Templates.branchDetail(customer);
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

    private TemplateInstance render(int page, boolean fragmentOnly, String q) {
        String term = (q != null && !q.isBlank()) ? "%" + q.trim().toLowerCase() + "%" : null;
        long totalCount = term != null
                ? Customer.count("lower(customerCode) like ?1 or lower(name) like ?1 or lower(email) like ?1 or lower(phone) like ?1", term)
                : Customer.count();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        int currentPage = Math.min(page, totalPages);

        List<Customer> customers = term != null
                ? Customer.find("(lower(customerCode) like ?1 or lower(name) like ?1 or lower(email) like ?1 or lower(phone) like ?1) order by id desc", term)
                        .page(currentPage - 1, PAGE_SIZE).list()
                : Customer.find("order by id desc").page(currentPage - 1, PAGE_SIZE).list();

        return fragmentOnly
                ? Templates.index$rows(customers, currentPage, totalPages, q)
                : Templates.index(customers, currentPage, totalPages, q);
    }
}