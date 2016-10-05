package some.webapp.service;

import com.codahale.metrics.MetricRegistry;

/**
 * Created by i303874 on 05/10/16.
 */
public class HelloWorldService {
    public HelloWorldService(MetricRegistry registry) {
    }

    public String hello(String name) {
        return "Hello: " + name;
    }
}
