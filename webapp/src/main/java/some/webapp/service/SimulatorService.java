package some.webapp.service;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import java.util.Random;

/**
 * Created by i303874 on 05/10/16.
 */
public class SimulatorService {
    private final static long MAX_DURATION = 5000;

    private Meter processes;
    private Random random = new Random();

    public SimulatorService(MetricRegistry registry) {
        processes = registry.meter("processes");
    }

    public void simulate(long duration) {
        if (duration > MAX_DURATION) {
            duration = MAX_DURATION;
        }

        long end = System.currentTimeMillis() + duration;

        while (System.currentTimeMillis() < end) {
            processes.mark(Math.abs(random.nextLong() % 1000 + 1)); // this eats up some cpu ;)

            try {
                Thread.currentThread().sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
