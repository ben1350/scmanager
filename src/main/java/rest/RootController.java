package rest;

import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/")
public class RootController {

    @GET
    @Authenticated
    public Response index() {
        return Response.seeOther(URI.create("/d")).build();
    }
}
