package some.webapp;

import com.codahale.metrics.*;
import com.codahale.metrics.jetty9.InstrumentedHandler;
import io.MetricsFramework;
import io.riemann.riemann.client.RiemannClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import some.webapp.api.SimulatorAPI;
import some.webapp.service.SimulatorService;

import java.io.IOException;


/**
 * Created by i303874 on 05/10/16.
 */
public class App {
    public static void main(String[] args) {
        MetricsFramework metricsFramework = new MetricsFramework();

        /* metrics configuration */
        MetricRegistry registry = new MetricRegistry();

        /* service configs */
        RiemannClient riemannClient = null;
        try {
            riemannClient = RiemannClient.tcp(metricsFramework.getRiemannHost(), metricsFramework.getRiemannTcpPort());
            riemannClient.connect();
        } catch (IOException e) {
            /* ignore */
        }

        SimulatorService simulatorService = new SimulatorService(registry, riemannClient);

        /* resources configuration */
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new SimulatorAPI(simulatorService));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");

        /* handler configuration */
        context.setHandler(new InstrumentedHandler(registry));

        metricsFramework.start();

        /* server configuration */
        Server server = new Server(6666);
        server.setStopTimeout(60 * 1000);
        server.setHandler(context);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.destroy();
        }
    }
}
