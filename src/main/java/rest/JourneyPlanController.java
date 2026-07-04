package rest;

import com.rosswood.entity.CustomerBranch;
import com.rosswood.entity.JourneyPlan;
import com.rosswood.entity.JourneyStop;
import com.rosswood.entity.User;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.RestPath;

import java.time.DayOfWeek;
import java.util.List;

/**
 * Manager-facing screen for building recurring weekly journey plans (routes)
 * and assigning them to sales reps. Gated to admins and users with the
 * {@code route_manager} role.
 */
@Path("/journey-plans")
@Authenticated
public class JourneyPlanController extends HxController {

    @Inject
    NavHelper nav;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<JourneyPlan> plans, List<User> reps);
        public static native TemplateInstance index$planList(List<JourneyPlan> plans, List<User> reps);
        public static native TemplateInstance planDetail(JourneyPlan plan, List<JourneyStop> stops, List<CustomerBranch> branches);
    }

    // ── Guards / helpers ──────────────────────────────────────────────────

    private void requireManager() {
        if (!nav.showJourneyPlans()) throw new ForbiddenException();
    }

    private List<User> salesReps() {
        List<User> all = User.listAll();
        return all.stream().filter(u -> u.hasRole("sales")).toList();
    }

    private List<JourneyPlan> allPlans() {
        return JourneyPlan.list("order by weekday, name");
    }

    private TemplateInstance renderDetail(JourneyPlan plan) {
        List<JourneyStop> stops = JourneyStop.list("journeyPlan.id = ?1 order by visitOrder", plan.id);
        List<Long> used = stops.stream().map(s -> s.customerBranch.id).toList();
        List<CustomerBranch> branches = CustomerBranch.<CustomerBranch>list(
                        "activeFlag = true or activeFlag is null order by customer.name, branchName")
                .stream()
                .filter(b -> !used.contains(b.id))
                .toList();
        return Templates.planDetail(plan, stops, branches);
    }

    // ── Plan CRUD ─────────────────────────────────────────────────────────

    @GET
    public TemplateInstance index() {
        requireManager();
        return Templates.index(allPlans(), salesReps());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public TemplateInstance add(
            @RestForm @NotBlank String name,
            @RestForm Long repId,
            @RestForm String weekday
    ) {
        requireManager();
        onlyHxRequest();

        User rep = User.findById(repId);
        if (rep != null) {
            JourneyPlan plan = new JourneyPlan();
            plan.name = name.trim();
            plan.assignedRep = rep;
            plan.weekday = DayOfWeek.valueOf(weekday);
            plan.activeFlag = true;
            plan.persist();
        }
        return Templates.index$planList(allPlans(), salesReps());
    }

    @POST
    @Path("/{id}/delete")
    @Transactional
    public TemplateInstance delete(@RestPath Long id) {
        requireManager();
        onlyHxRequest();
        JourneyPlan plan = JourneyPlan.findById(id);
        if (plan != null) plan.delete();
        return Templates.index$planList(allPlans(), salesReps());
    }

    // ── Stop management ───────────────────────────────────────────────────

    @GET
    @Path("/{id}/stops")
    public TemplateInstance getStops(@RestPath Long id) {
        requireManager();
        onlyHxRequest();
        JourneyPlan plan = JourneyPlan.findById(id);
        if (plan == null) throw new NotFoundException();
        return renderDetail(plan);
    }

    @POST
    @Path("/{id}/stops")
    @Transactional
    public TemplateInstance addStop(@RestPath Long id, @RestForm Long branchId) {
        requireManager();
        onlyHxRequest();
        JourneyPlan plan = JourneyPlan.findById(id);
        if (plan == null) throw new NotFoundException();

        CustomerBranch branch = CustomerBranch.findById(branchId);
        boolean already = JourneyStop.count("journeyPlan.id = ?1 and customerBranch.id = ?2", id, branchId) > 0;
        if (branch != null && !already) {
            long existing = JourneyStop.count("journeyPlan.id = ?1", id);
            JourneyStop stop = new JourneyStop();
            stop.journeyPlan = plan;
            stop.customerBranch = branch;
            stop.visitOrder = (int) existing + 1;
            stop.persist();
        }
        return renderDetail(plan);
    }

    @POST
    @Path("/{id}/stops/{stopId}/delete")
    @Transactional
    public TemplateInstance removeStop(@RestPath Long id, @RestPath Long stopId) {
        requireManager();
        onlyHxRequest();
        JourneyPlan plan = JourneyPlan.findById(id);
        if (plan == null) throw new NotFoundException();

        JourneyStop stop = JourneyStop.findById(stopId);
        if (stop != null && stop.journeyPlan.id.equals(id)) {
            stop.delete();
        }
        // Re-number the remaining stops 1..n
        List<JourneyStop> remaining = JourneyStop.list("journeyPlan.id = ?1 order by visitOrder", id);
        int i = 1;
        for (JourneyStop s : remaining) s.visitOrder = i++;
        return renderDetail(plan);
    }

    @POST
    @Path("/{id}/stops/{stopId}/move/{dir}")
    @Transactional
    public TemplateInstance moveStop(@RestPath Long id, @RestPath Long stopId, @RestPath String dir) {
        requireManager();
        onlyHxRequest();
        JourneyPlan plan = JourneyPlan.findById(id);
        if (plan == null) throw new NotFoundException();

        List<JourneyStop> stops = JourneyStop.list("journeyPlan.id = ?1 order by visitOrder", id);
        int idx = -1;
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).id.equals(stopId)) { idx = i; break; }
        }
        if (idx >= 0) {
            int swap = "up".equals(dir) ? idx - 1 : idx + 1;
            if (swap >= 0 && swap < stops.size()) {
                JourneyStop a = stops.get(idx);
                JourneyStop b = stops.get(swap);
                Integer tmp = a.visitOrder;
                a.visitOrder = b.visitOrder;
                b.visitOrder = tmp;
            }
        }
        return renderDetail(plan);
    }
}
