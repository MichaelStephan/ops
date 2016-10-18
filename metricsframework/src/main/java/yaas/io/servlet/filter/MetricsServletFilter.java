package yaas.io.servlet.filter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import yaas.io.framework.MetricsFramework;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by i303874 on 18/10/2016.
 */
public class MetricsServletFilter implements Filter {
    private final static String PREFIX = "javax.servlet.Filter.";

    private Counter requests;
    private Counter response2xx;
    private Counter response3xx;
    private Counter response4xx;
    private Counter response5xx;
    private Histogram responseTimes;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        MetricsFramework metricsFramework = MetricsFramework.getInstance();
        MetricRegistry registry = metricsFramework.getMetricsRegistry();
        this.requests = registry.counter(PREFIX + "requests");
        this.response2xx = registry.counter(PREFIX + "2xx-responses");
        this.response3xx = registry.counter(PREFIX + "3xx-responses");
        this.response4xx = registry.counter(PREFIX + "4xx-responses");
        this.response5xx = registry.counter(PREFIX + "5xx-responses");
        this.responseTimes = registry.histogram(PREFIX + "responsetimes");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        requests.inc();

        int status = httpResponse.getStatus();
        if (status >= 200 && status < 300) {
            response2xx.inc();
        } else if (status >= 300 && status < 400) {
            response3xx.inc();
        } else if (status >= 400 && status < 500) {
            response4xx.inc();
        } else {
            response5xx.inc();
        }

        long start = System.currentTimeMillis();
        filterChain.doFilter(request, response);
        responseTimes.update(System.currentTimeMillis() - start);
    }

    @Override
    public void destroy() {

    }
}
