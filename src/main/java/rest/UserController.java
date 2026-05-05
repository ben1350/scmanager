package rest;

import com.rosswood.entity.User;
import com.rosswood.entity.UserStatus;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;

@Path("/admin/users")
public class UserController extends HxController {

    private static final List<String> ALL_ROLES = List.of(
            "sales", "inventory", "production",
            "finance", "dispatch", "customer_manager",
            "reports", "admin"
    );

    @CheckedTemplate(requireTypeSafeExpressions = false)
    public static class Templates {
        public static native TemplateInstance index(List<User> users, List<String> allRoles);
        public static native TemplateInstance index$rows(List<User> users, List<String> allRoles);
    }

    @GET
    public TemplateInstance index() {
        List<User> users = User.find("order by id asc").list();
        return isHxRequest()
                ? Templates.index$rows(users, ALL_ROLES)
                : Templates.index(users, ALL_ROLES);
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance create(
            @RestForm String userName,
            @RestForm String email,
            @RestForm String firstName,
            @RestForm String lastName,
            @RestForm String password,
            @RestForm List<String> roles
    ) {
        onlyHxRequest();

        User user = new User();
        user.userName  = userName.trim().toLowerCase();
        user.email     = email.trim().toLowerCase();
        user.firstName = firstName != null ? firstName.trim() : null;
        user.lastName  = lastName  != null ? lastName.trim()  : null;
        user.password  = BcryptUtil.bcryptHash(password);
        user.status    = UserStatus.REGISTERED;
        user.isAdmin   = roles != null && roles.contains("admin");
        user.rolesRaw     = roles != null && !roles.isEmpty() ? String.join(",", roles) : null;
        user.persist();

        return Templates.index$rows(User.find("order by id asc").list(), ALL_ROLES);
    }

    @POST
    @Path("/{id}/roles")
    @Transactional
    public TemplateInstance updateRoles(
            @RestPath Long id,
            @RestForm List<String> roles
    ) {
        onlyHxRequest();

        User user = User.findById(id);
        if (user == null) throw new NotFoundException();

        user.rolesRaw   = roles != null && !roles.isEmpty() ? String.join(",", roles) : null;
        user.isAdmin = roles != null && roles.contains("admin");
        user.persist();

        return Templates.index$rows(User.find("order by id asc").list(), ALL_ROLES);
    }

    @POST
    @Path("/{id}/toggle")
    @Transactional
    public TemplateInstance toggle(@RestPath Long id) {
        onlyHxRequest();

        User user = User.findById(id);
        if (user == null) throw new NotFoundException();

        user.status = user.status == UserStatus.DISABLED
                ? UserStatus.REGISTERED
                : UserStatus.DISABLED;
        user.persist();

        return Templates.index$rows(User.find("order by id asc").list(), ALL_ROLES);
    }
}
