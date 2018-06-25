package ch.sourcepond.osgi.flyway;

import org.flywaydb.core.Flyway;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.slf4j.Logger;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.defaultThreadFactory;
import static org.slf4j.LoggerFactory.getLogger;

public class Activator implements BundleActivator {
    private static final Logger LOG = getLogger(Activator.class);
    private final ProxyFactory proxyFactory;
    private final MigrationTask task;
    private final Thread migrationThread;

    public Activator() {
        this(new ProxyFactory(), new MigrationTask(new Flyway()));
    }

    // Constructor for testing
    Activator(final ProxyFactory pProxyFactory, final MigrationTask pTask) {
        proxyFactory = pProxyFactory;
        task = pTask;
        migrationThread = defaultThreadFactory().newThread(pTask);
    }

    @Override
    public void start(final BundleContext context) {
        migrationThread.start();
        context.registerService(new String[]{
                        FindHook.class.getName(),
                        EventListenerHook.class.getName()},
                new DataSourceProxyManager(proxyFactory, task, context), null);
    }

    @Override
    public void stop(final BundleContext context) {
        try {
            migrationThread.join();
        } catch (final InterruptedException e) {
            LOG.warn("DB-Migration interrupted, leaving the database in a potentially corrupted state!", e);
            currentThread().interrupt();
        }
    }
}
