package rest;

import com.rosswood.entity.User;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Request-scoped CDI bean exposed to all Qute templates via {inject:nav}.
 * Provides the current user's display name and role-based nav visibility flags.
 */
@Named("nav")
@RequestScoped
public class NavHelper {

    @Inject
    SecurityIdentity identity;

    private User cached;
    private boolean loaded = false;

    private User currentUser() {
        if (!loaded) {
            loaded = true;
            if (!identity.isAnonymous()) {
                cached = User.findByUserName(identity.getPrincipal().getName());
            }
        }
        return cached;
    }

    /** Display name shown in the nav chip (full name or username). */
    public String displayName() {
        User u = currentUser();
        return u != null ? u.fullName() : "User";
    }

    /** Items — inventory managers, production staff, admins. */
    public boolean showItems() {
        User u = currentUser();
        return u != null && (u.isAdmin || u.hasRole("inventory") || u.hasRole("production"));
    }

    /** Production — production staff and admins. */
    public boolean showProduction() {
        User u = currentUser();
        return u != null && (u.isAdmin || u.hasRole("production"));
    }

    /** Sales — sales reps, finance, admins. */
    public boolean showSales() {
        User u = currentUser();
        return u != null && (u.isAdmin || u.hasRole("sales") || u.hasRole("finance"));
    }

    /** Customers — sales reps, customer managers, admins. */
    public boolean showCustomers() {
        User u = currentUser();
        return u != null && (u.isAdmin || u.hasRole("sales") || u.hasRole("customer_manager"));
    }

    /** Reports — visible to all authenticated users. */
    public boolean showReports() {
        return currentUser() != null;
    }

    /** Setup (UOM, Item Types, Users) — admins only. */
    public boolean showSetup() {
        User u = currentUser();
        return u != null && u.isAdmin;
    }
}
