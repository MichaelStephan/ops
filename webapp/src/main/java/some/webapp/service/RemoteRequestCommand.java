package some.webapp.service;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

/**
 * Created by i303874 on 10/10/2016.
 */
public class RemoteRequestCommand extends HystrixCommand<String> {
    private boolean fail;

    public RemoteRequestCommand(boolean fail) {
        super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
        this.fail = fail;
    }

    @Override
    protected String run() {
        if (fail) {
            throw new RuntimeException("Some issue occurred");
        } else {
            return "ok";
        }
    }

    @Override
    protected String getFallback() {
        return null;
    }
}
