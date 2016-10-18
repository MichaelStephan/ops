package some.webapp.api;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Metered;
import com.codahale.metrics.annotation.Timed;
import some.webapp.service.SimulatorService;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

/**
 * Created by i303874 on 05/10/16.
 */
@Singleton
@Path("/simulator")
public class SimulatorAPI {
    private SimulatorService service;

    public SimulatorAPI(SimulatorService service) {
        this.service = service;
    }

    @GET
    @Path("/something/{param}")
    public Response simulateSomething(@PathParam("param") long duration) {
        service.something(duration);
        return Response
                .status(Response.Status.OK)
                .build();
    }

    @GET
    @Path("/response/{param}")
    public Response simulateStateChange(@PathParam("param") int status) {
        return Response
                .status(status)
                .build();
    }

    @GET
    @Path("/dependency/{fail}")
    public Response simulateDependency(@PathParam("fail") boolean fail) {
        service.simulateDependency(fail);
        return Response
                .status(Response.Status.OK)
                .build();
    }

    @GET
    @Path("/error-log")
    public Response simulateErrorLog() {
        service.simulateErrorLog();
        return Response
                .status(Response.Status.OK)
                .build();
    }
}

