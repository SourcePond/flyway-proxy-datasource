package ch.sourcepond.osgi.flyway;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.io.Closeable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;

class MigrationManager implements Closeable {
    private static final Logger LOG = getLogger(MigrationManager.class);
    private final ExecutorService executorService = newCachedThreadPool();
    private final ProxyFactory proxyFactory = new ProxyFactory();

    public DataSource startMigration(final DataSource pDataSource) {
        final ClassLoader ldr = currentThread().getContextClassLoader();
        currentThread().setContextClassLoader(Flyway.class.getClassLoader());
        try {
            final Flyway flyway = new Flyway();
            flyway.setDataSource(pDataSource);
            final MigrationTask task = new MigrationTask(flyway);
            executorService.execute(task);
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
