package ch.sourcepond.osgi.flyway;

import org.junit.Assert;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import javax.sql.DataSource;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.osgi.framework.ServiceEvent.REGISTERED;

class DataSourceListener implements ServiceListener {
    private List<DataSource> dataSources = new LinkedList<>();

    public void verifyNoMoreDataSourcesRegistered() throws InterruptedException {
        Thread.sleep(3000);
        assertTrue(dataSources.size() == 1);
    }

    public DataSource awaitDataSource() throws InterruptedException {
        synchronized (this) {
            while (dataSources.isEmpty()) {
                wait();
            }
            assertTrue(dataSources.size() == 1);
            return dataSources.get(0);
        }
    }

    @Override
    public void serviceChanged(final ServiceEvent event) {
        switch (event.getType()) {
            case REGISTERED: {
                final ServiceReference<?> ref = event.getServiceReference();
                synchronized (this) {
                    dataSources.add((DataSource) ref.getBundle().getBundleContext().getService(ref));
                }
                break;
            }
            default: {
                // noop
            }
        }
    }
}
