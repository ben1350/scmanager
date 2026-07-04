package rest;

import com.rosswood.entity.JourneyPlan;
import com.rosswood.entity.JourneyStop;
import com.rosswood.entity.JourneyVisit;
import com.rosswood.entity.User;
import io.quarkiverse.renarde.htmx.HxController;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import org.jboss.resteasy.reactive.RestPath;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/**
 * Rep-facing screen: the logged-in sales rep sees the stops on their own
 * assigned route for today and checks each one off as visited or skipped.
 * Reads/writes the same {@link JourneyVisit} data as the mobile app.
 */
@Path("/my-route")
@Authenticated
public class MyRouteController extends HxController {

    @Inject
    NavHelper nav;

    @Inject
    SecurityIdentity identity;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance index(List<RouteView> routes, String dateLabel);
        public static native TemplateInstance index$routeList(List<RouteView> routes, String dateLabel);
    }

    /** A stop plus its visit status for today, ready for the template. */
    public record StopView(JourneyStop stop, String status) {
        public boolean isVisited() { return "VISITED".equals(status); }
        public boolean isSkipped() { return "SKIPPED".equals(status); }
        public boolean isPending() { return status == null; }
    }

    /** A route the rep runs today, with its ordered stops and progress. */
    public record RouteView(JourneyPlan plan, List<StopView> stops, long visitedCount) {}

    // ── Helpers ───────────────────────────────────────────────────────────

    private void requireRep() {
        if (!nav.showMyRoute()) throw new ForbiddenException();
    }

    private User me() {
        return User.findByUserName(identity.getPrincipal().getName());
    }

    private String dateLabel(LocalDate date) {
        return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + ", " + date;
    }

    private List<RouteView> myRoutesToday(User user, LocalDate date) {
        DayOfWeek today = date.getDayOfWeek();
        return JourneyPlan.findByRepAndDay(user.id, today).stream()
                .map(plan -> {
                    List<JourneyStop> stops =
                            JourneyStop.list("journeyPlan.id = ?1 order by visitOrder", plan.id);
                    long visited = 0;
                    List<StopView> views = new java.util.ArrayList<>();
                    for (JourneyStop s : stops) {
                        JourneyVisit v = JourneyVisit.forStopOnDate(s.id, date);
                        String status = v != null ? v.status.name() : null;
                        if ("VISITED".equals(status)) visited++;
                        views.add(new StopView(s, status));
                    }
                    return new RouteView(plan, views, visited);
                })
                .toList();
    }

    // ── Screens ───────────────────────────────────────────────────────────

    @GET
    public TemplateInstance index() {
        requireRep();
        LocalDate date = LocalDate.now();
        return Templates.index(myRoutesToday(me(), date), dateLabel(date));
    }

    @POST
    @Path("/stops/{stopId}/visit/{status}")
    @Transactional
    public TemplateInstance markVisit(@RestPath Long stopId, @RestPath String status) {
        requireRep();
        onlyHxRequest();

        User user = me();
        LocalDate date = LocalDate.now();
        JourneyStop stop = JourneyStop.findById(stopId);

        // Only let a rep touch stops on their own route.
        if (stop != null && stop.journeyPlan.assignedRep.id.equals(user.id)) {
            JourneyVisit visit = JourneyVisit.forStopOnDate(stopId, date);
            if ("PENDING".equalsIgnoreCase(status)) {
                if (visit != null) visit.delete();
            } else {
                JourneyVisit.VisitStatus st;
                try {
                    st = JourneyVisit.VisitStatus.valueOf(status.toUpperCase());
                    if (visit == null) {
                        visit = new JourneyVisit();
                        visit.journeyStop = stop;
                        visit.visitDate = date;
                    }
                    visit.status = st;
                    visit.persist();
                } catch (IllegalArgumentException ignored) {
                    // unknown status → leave unchanged
                }
            }
        }
        return Templates.index$routeList(myRoutesToday(user, date), dateLabel(date));
    }
}
