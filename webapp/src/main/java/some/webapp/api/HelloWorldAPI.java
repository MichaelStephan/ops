package some.webapp.api;

import some.webapp.service.HelloWorldService;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Created by i303874 on 05/10/16.
 */
@Singleton
@Path("/hello")
public class HelloWorldAPI {
    private HelloWorldService service;

    public HelloWorldAPI(HelloWorldService service) {
        this.service = service;
    }

    @GET
    @Path("/{param}")
    public Response hello(@PathParam("param") String name) {
        return Response
                .status(Response.Status.OK)
                .entity(service.hello(name))
                .build();
    }
}

