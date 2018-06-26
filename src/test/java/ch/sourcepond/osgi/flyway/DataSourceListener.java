package ch.sourcepond.osgi.flyway;

import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import javax.sql.DataSource;

import static org.osgi.framework.ServiceEvent.REGISTERED;

class DataSourceListener implements ServiceListener {
    private DataSource dataSource;

    public DataSource awaitDataSource() throws InterruptedException {
        synchronized (this) {
            while (dataSource == null) {
                wait();
            }
            return dataSource;
        }
    }

    @Override
    public synchronized void serviceChanged(final ServiceEvent event) {
        switch (event.getType()) {
            case REGISTERED: {
                final ServiceReference<?> ref = event.getServiceReference();
                dataSource = (DataSource) ref.getBundle().getBundleContext().getService(ref);
                break;
            }
            default: {
                // noop
            }
        }
    }
}
