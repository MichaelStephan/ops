package some.webapp.api.filter;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by i303874 on 05/10/16.
 */
public class MetricFilter implements Filter {
    private Meter requests;
    private Histogram durations;

    public MetricFilter(MetricRegistry registry) {
        this.requests = registry.meter("requests");
        this.durations = registry.histogram("durations");
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        requests.mark();
        long start = System.currentTimeMillis();
        filterChain.doFilter(servletRequest, servletResponse);
        long duration = System.currentTimeMillis() - start;
        this.durations.update(duration);
    }

    public void destroy() {
    }
}
