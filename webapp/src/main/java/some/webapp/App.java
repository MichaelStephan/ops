package some.webapp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Symbol;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.codahale.metrics.jetty9.InstrumentedHandler;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.logback.InstrumentedAppender;
import com.codahale.metrics.riemann.Riemann;
import com.codahale.metrics.riemann.RiemannReporter;
import com.netflix.hystrix.contrib.servopublisher.HystrixServoMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.servo.publish.*;
import com.netflix.servo.publish.graphite.GraphiteMetricObserver;
import io.riemann.riemann.client.RiemannClient;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.LoggerFactory;
import some.webapp.api.SimulatorAPI;
import some.webapp.service.SimulatorService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by i303874 on 05/10/16.
 */
public class App {
    private final static String INSTANCE = "instance1";
    private final static String GRAPHITE_ENDPOINT = "localhost:2003";
    private final static String RIEMANN_HOST = "localhost";
    private final static int RIEMANN_PORT = 5555;
    private final static int RIEMANN_HTTP_PORT = 5556;
    private final static String TAG = "someWebApp";
    private final static String[] RIEMANN_TAGS = {TAG};
    private final static int RIEMANN_BATCHSIZE = 1000;
    private final static int INTERVAL = 5;

    /* this is a bit dirty ;) */
    private static void startJVMProfiler() {
        Clojure.var("clojure.core", "require").invoke(Symbol.intern("riemann.jvm-profiler"));
        IFn start = Clojure.var("riemann.jvm-profiler", "start-global!");
        start.invoke(Clojure.read("{" +
                " :host \"" + RIEMANN_HOST + "\"" +
                " :port " + RIEMANN_HTTP_PORT +
                " :prefix" + "\"" + TAG + " \"" + // WARNING unfortunately the prefix also needs to contain a whitespace in the end !
                " :load " + 0.02 +
                "}"));
    }

    private static void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }

    public static void main(String[] args) {
        /* logger configuration */
        final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);

        /* metrics configuration */
        MetricRegistry registry = new MetricRegistry();

        /* log metrics */
        final InstrumentedAppender metrics = new InstrumentedAppender(registry);
        metrics.setContext(root.getLoggerContext());
        metrics.start();
        root.addAppender(metrics);

        {
            ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build();
            reporter.start(INTERVAL, TimeUnit.SECONDS);
        }

        try {
            Riemann riemann = new Riemann(RIEMANN_HOST, RIEMANN_PORT, RIEMANN_BATCHSIZE);
            RiemannReporter reporter = RiemannReporter.forRegistry(registry)
                    .tags(Arrays.asList(RIEMANN_TAGS))
                    .localHost(INSTANCE)
                    .build(riemann);
            reporter.start(INTERVAL, TimeUnit.SECONDS);
        } catch (IOException e) {
            /* ignore */
        }

        /* jvm metric set */
        registerAll("gc", new GarbageCollectorMetricSet(), registry);
        registerAll("memory", new MemoryUsageGaugeSet(), registry);
        registerAll("threads", new ThreadStatesGaugeSet(), registry);

        /* jvm profiler */
        startJVMProfiler();

        /* hystrix metrics */
        HystrixPlugins.getInstance().registerMetricsPublisher(HystrixServoMetricsPublisher.getInstance());
        final List<MetricObserver> observers = new ArrayList<>();

        // always use <instanceId>.<tag>.hystrix as metric prefix. The riemann server will parse the input to get the host right
        observers.add(new GraphiteMetricObserver(INSTANCE + "." + TAG + ".hystrix", GRAPHITE_ENDPOINT));
        PollScheduler.getInstance().start();
        PollRunnable task = new PollRunnable(new MonitorRegistryMetricPoller(), BasicMetricFilter.MATCH_ALL, true, observers);
        PollScheduler.getInstance().addPoller(task, INTERVAL, TimeUnit.SECONDS);

        /* service configs */
        RiemannClient riemannClient = null;
        try {
            riemannClient = RiemannClient.tcp(RIEMANN_HOST, RIEMANN_PORT);
            riemannClient.connect();
        } catch (IOException e) {
            throw new RuntimeException(e);
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
