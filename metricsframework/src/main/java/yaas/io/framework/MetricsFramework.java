package yaas.io.framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Symbol;
import io.riemann.riemann.client.RiemannClient;
import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
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
import org.slf4j.LoggerFactory;

/**
 * Created by i303874 on 18/10/2016.
 */
public class MetricsFramework {
    private static MetricsFramework INSTANCE;

    private final static int RIEMANN_BATCHSIZE = 100;
    private final static int INTERVAL = 25;

    private final Random rand = new Random();

    private final MetricRegistry registry = new MetricRegistry();

    public static synchronized MetricsFramework getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MetricsFramework();
        }
        return INSTANCE;
    }

    private MetricsFramework() {
    }

    public MetricRegistry getMetricsRegistry() {
        return registry;
    }

    public String getName() {
        return withDefault(System.getenv("APP_NAME"), "unknown");
    }

    public String getInstanceName() {
        return withDefault(System.getenv("CF_INSTANCE_INDEX"), rand.nextInt(10) * -1) + "_" + rand.nextInt(10000);
    }

    public String getGraphiteEndpoint() {
        return withDefault(System.getenv("GRAPHITE_ENDPOINT"), "localhost:2003");
    }

    public String getRiemannHost() {
        return withDefault(System.getenv("RIEMANN_HOST"), "localhost");
    }

    public int getRiemannTcpPort() {
        return withDefault(System.getenv("RIEMANN_TCP_PORT"), 5555);
    }

    public int getRiemannHttpPort() {
        return withDefault(System.getenv("RIEMANN_HTTP_PORT"), 5556);
    }

    public void startInternal() {
        /* logger configuration */
        final LoggerContext factory = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger root = factory.getLogger(Logger.ROOT_LOGGER_NAME);

        /* log metrics */
        final InstrumentedAppender metrics = new InstrumentedAppender(registry);
        metrics.setContext(root.getLoggerContext());
        metrics.start();
        root.addAppender(metrics);

//        {
//            ConsoleReporter reporter = ConsoleReporter.forRegistry(registry)
//                    .convertRatesTo(TimeUnit.SECONDS).convertRatesTo(TimeUnit.SECONDS)
//                    .convertDurationsTo(TimeUnit.MILLISECONDS)
//                    .convertDurationsTo(TimeUnit.MILLISECONDS)
//                    .build();
//            reporter.start(INTERVAL, TimeUnit.SECONDS);
//        }

        try {
            Riemann riemann = new Riemann(getRiemannHost(), getRiemannTcpPort(), RIEMANN_BATCHSIZE);
            RiemannReporter reporter = RiemannReporter.forRegistry(registry)
                    .prefixedWith(getName())
                    .localHost(getInstanceName())
                    .build(riemann);
            reporter.start(INTERVAL, TimeUnit.SECONDS);
        } catch (IOException e) {
            /* ignore */
        }

        /* jvm metric set */
        registerAll("gc", new GarbageCollectorMetricSet(), registry);
        registerAll("memory", new MemoryUsageGaugeSet(), registry);
        registerAll("thread", new ThreadStatesGaugeSet(), registry);

        /* jvm profiler */
        startJVMProfiler();

        /* hystrix metrics */
        HystrixPlugins.getInstance().registerMetricsPublisher(HystrixServoMetricsPublisher.getInstance());
        final List<MetricObserver> observers = new ArrayList<>();

        // always use <instanceId>.<tag>.hystrix as metric prefix. The riemann server will parse the input to get the host right
        observers.add(new GraphiteMetricObserver(getInstanceName() + "." + getName() + "_remote.hystrix", getGraphiteEndpoint()));
        PollScheduler.getInstance().start();
        PollRunnable task = new PollRunnable(new MonitorRegistryMetricPoller(), BasicMetricFilter.MATCH_ALL, true, observers);
        PollScheduler.getInstance().addPoller(task, INTERVAL, TimeUnit.SECONDS);
    }

    public static void start() {
        getInstance().startInternal();
    }

    private int withDefault(String value, int defaultValue) {
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private String withDefault(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    /* this is a bit dirty ;) */
    private void startJVMProfiler() {
        Clojure.var("clojure.core", "require").invoke(Symbol.intern("riemann.jvm-profiler"));
        IFn start = Clojure.var("riemann.jvm-profiler", "start-global!");
        start.invoke(Clojure.read("{" +
                " :host \"" + getRiemannHost() + "\"" +
                " :port " + getRiemannHttpPort() +
                " :prefix" + "\"" + getName() + " \"" + // WARNING unfortunately the prefix also needs to contain a whitespace in the end !
                " :load " + 0.02 +
                "}"));
    }

    private void registerAll(String prefix, MetricSet metricSet, MetricRegistry registry) {
        for (Map.Entry<String, Metric> entry : metricSet.getMetrics().entrySet()) {
            if (entry.getValue() instanceof MetricSet) {
                registerAll(prefix + "." + entry.getKey(), (MetricSet) entry.getValue(), registry);
            } else {
                registry.register(prefix + "." + entry.getKey(), entry.getValue());
            }
        }
    }
}
