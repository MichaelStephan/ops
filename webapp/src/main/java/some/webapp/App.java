package some.webapp;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Symbol;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.riemann.Riemann;
import com.codahale.metrics.riemann.RiemannReporter;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import some.webapp.api.SimulatorAPI;
import some.webapp.api.filter.MetricFilter;
import some.webapp.service.SimulatorService;

import javax.servlet.DispatcherType;
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by i303874 on 05/10/16.
 */
public class App {
    private final static String RIEMANN_HOST = "localhost";
    private final static int RIEMANN_PORT = 5555;
    private final static int RIEMANN_HTTP_PORT = 5556;
    private final static String TAG = "someWebApp";
    private final static String[] RIEMANN_TAGS = {TAG};
    private final static int RIEMANN_BATCHSIZE = 1000;

    private static void startJVMProfiler() {
        Clojure.var("clojure.core", "require").invoke(Symbol.intern("riemann.jvm-profiler"));
        IFn start = Clojure.var("riemann.jvm-profiler", "start-global!");
        start.invoke(Clojure.read("{" +
                " :host \"" + RIEMANN_HOST + "\"" +
                " :port " + RIEMANN_HTTP_PORT +
                " :prefix" + "\"" + TAG + " \"" + // unfortunately the prefix also needs to contain a whitespace in the end !
                " :load " + 0.02 +
                "}"));
    }

    public static void main(String[] args) {
        /* logger configuration */
        BasicConfigurator.configure();

        /* metrics configuration */
        MetricRegistry registry = new MetricRegistry();
//        {
//            ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                    .convertRatesTo(TimeUnit.SECONDS)
//                    .convertDurationsTo(TimeUnit.MILLISECONDS)
//                    .build();
//            reporter.start(1, TimeUnit.SECONDS);
//        }

        try {
            Riemann riemann = new Riemann(RIEMANN_HOST, RIEMANN_PORT, RIEMANN_BATCHSIZE);
            RiemannReporter reporter = RiemannReporter.forRegistry(registry)
                    .tags(Arrays.asList(RIEMANN_TAGS))
                    .build(riemann);
            reporter.start(5, TimeUnit.SECONDS);
        } catch (IOException e) {
            /* ignore */
        }

        /* jvm profiler, this is a bit dirty ;) */
        startJVMProfiler();

        /* services */
        SimulatorService simulatorService = new SimulatorService(registry);

        /* resources configuration */
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.register(new SimulatorAPI(simulatorService));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), "/*");

        /* filter configuration */
        FilterHolder filterHolder = new FilterHolder();
        filterHolder.setFilter(new MetricFilter(registry));
        context.addFilter(filterHolder, "/*", EnumSet.of(DispatcherType.REQUEST));

        /* server configuration */
        Server server = new Server(2222);
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
