package some.webapp;

import com.codahale.metrics.*;
import io.yaas.monitoring.framework.MetricsFramework;
import io.yaas.monitoring.servlet.filter.MetricsServletFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import some.webapp.api.SimulatorAPI;
import some.webapp.service.SimulatorService;

import javax.servlet.DispatcherType;
import java.util.EnumSet;


/**
 * Created by i303874 on 05/10/16.
 */
public class App {
    public static void main(String[] args) {
        MetricsFramework metricsFramework = MetricsFramework.getInstance();


        /* metrics configuration */
        MetricRegistry registry = new MetricRegistry();

        SimulatorService simulatorService = new SimulatorService(registry);

        /* resources configuration */
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new SimulatorAPI(simulatorService));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");
        context.addFilter(MetricsServletFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        resourceConfig.register(new MetricsServletFilter());

        metricsFramework.startInternal();

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
