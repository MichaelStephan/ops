package some.webapp.service;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.rmi.Remote;
import java.util.Random;

/**
 * Created by i303874 on 05/10/16.
 */
public class SimulatorService {
    private final static long MAX_DURATION = 5000;

    final static Logger logger = LoggerFactory.getLogger(SimulatorService.class);
    private Meter something;
    private Random random = new Random();

    public SimulatorService(MetricRegistry registry) {
        this.something = registry.meter("something");
    }

    public void something(long duration) {
        if (duration > MAX_DURATION) {
            duration = MAX_DURATION;
        }

        long end = System.currentTimeMillis() + duration;

        while (System.currentTimeMillis() < end) {
            something.mark(Math.abs(random.nextLong() % 1000 + 1)); // this eats up some cpu ;)
            try {
                Thread.currentThread().sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void simulateDependency(boolean fail) {
        RemoteRequestCommand cmd = new RemoteRequestCommand(fail);
        cmd.queue();
    }

    public void simulateErrorLog() {
        logger.error("error");
    }
}
