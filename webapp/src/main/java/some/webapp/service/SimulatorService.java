package some.webapp.service;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import io.riemann.riemann.client.RiemannClient;

import java.io.IOException;
import java.util.Random;

/**
 * Created by i303874 on 05/10/16.
 */
public class SimulatorService {
    private final static long MAX_DURATION = 5000;

    RiemannClient riemannClient;
    private Meter something;
    private Random random = new Random();

    public SimulatorService(MetricRegistry registry, RiemannClient riemannClient) {
        this.something = registry.meter("something");
        this.riemannClient = riemannClient;
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

    public void stateChange(String state) {
        try {
            riemannClient.event().
                    service("fridge").
                    state(state).
                    metric(5.3).
                    tags("appliance", "cold").
                    send().
                    deref(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
