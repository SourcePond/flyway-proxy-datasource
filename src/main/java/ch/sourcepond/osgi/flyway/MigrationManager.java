package ch.sourcepond.osgi.flyway;

import org.flywaydb.core.Flyway;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.Closeable;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

class MigrationManager implements Closeable {
    private static final Logger LOG = getLogger(MigrationManager.class);

    // TODO: Make this configurable
    private static final String ROOT_LOCATION = "db";

    private final ExecutorService executorService = newCachedThreadPool();
    private final ProxyFactory proxyFactory = new ProxyFactory();

    private final BundleContext bundleContext;

    public MigrationManager(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public DataSource startMigration(final DataSource pDataSource, final String pDataSourceName) {
        final String path;
        if (pDataSourceName == null) {
            path = format("%s", ROOT_LOCATION);
        } else {
            path = format("%s/%s", ROOT_LOCATION, pDataSourceName);
        }

        final ClassLoader ldr = currentThread().getContextClassLoader();
        currentThread().setContextClassLoader(Flyway.class.getClassLoader());
        try {
            final Flyway flyway = new Flyway();
            flyway.setLocations(path);
            flyway.setDataSource(pDataSource);
            final MigrationTask task = new MigrationTask(flyway);
            executorService.execute(task);

            LOG.info("Started migration for datasource '{}' and [{}]", pDataSourceName, path);
            return proxyFactory.createProxy(pDataSource, task);
        } finally {
            currentThread().setContextClassLoader(ldr);
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
        try {
            // TODO: Make this configurable
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            LOG.warn("DB-Migration interrupted, leaving the database in a potentially corrupted state!", e);
            currentThread().interrupt();
        }
    }
}
