package some.webapp.api.filter;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import javax.servlet.*;
import java.io.IOException;

/**
 * Created by i303874 on 05/10/16.
 */
public class MetricFilter implements Filter {
    private Meter requests;

    public MetricFilter(MetricRegistry registry) {
        this.requests = registry.meter("requets");
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        requests.mark();

        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }
}
