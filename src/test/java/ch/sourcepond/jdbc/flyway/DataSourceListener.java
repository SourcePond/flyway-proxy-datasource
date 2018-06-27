package ch.sourcepond.jdbc.flyway;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import javax.sql.DataSource;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;

class DataSourceListener implements ServiceListener {
    private List<DataSource> dataSources = new LinkedList<>();

    public DataSource awaitDataSource() throws InterruptedException {
        synchronized (this) {
            while (dataSources.isEmpty()) {
                wait();
            }
            assertTrue(dataSources.size() == 1);
            return dataSources.get(0);
        }
    }

    public void awaitUnregistration() throws InterruptedException {
        synchronized (this) {
            while (!dataSources.isEmpty()) {
                wait();
            }
        }
    }

    private DataSource getService(final ServiceEvent event) {
        final ServiceReference<?> ref = event.getServiceReference();
        return (DataSource) ref.getBundle().getBundleContext().getService(ref);
    }

    @Override
    public void serviceChanged(final ServiceEvent event) {
        synchronized (this) {
            switch (event.getType()) {
                case REGISTERED: {
                    dataSources.add(getService(event));
                    break;
                }
                case UNREGISTERING: {
                    dataSources.remove(getService(event));
                    event.getServiceReference().
                            getBundle().
                            getBundleContext().
                            ungetService(event.getServiceReference());
                }
                default: {
                    // noop
                }
            }
        }
    }
}
