package ch.sourcepond.osgi.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;

import java.sql.SQLException;

import static java.lang.Thread.currentThread;
import static org.slf4j.LoggerFactory.getLogger;

final class MigrationTask implements Runnable {
    private static final Logger LOG = getLogger(MigrationTask.class);
    private final Flyway flyway;
    private volatile boolean migrate;
    private volatile SQLException migrationFailure;

    public MigrationTask(final Flyway pFlyway) {
        flyway = pFlyway;
    }

    public void awaitMigration() throws SQLException {
        if (migrate) {
            synchronized (this) {
                while (migrate) {
                    try {
                        wait();
                    } catch (final InterruptedException e) {
                        migrate = false;
                        migrationFailure = new SQLException(e.getMessage(), e);
                        currentThread().interrupt();
                    }
                }
            }
        }

        if (migrationFailure != null) {
            throw migrationFailure;
        }
    }

    @Override
    public void run() {
        try {
            int successfulMigrationCount = flyway.migrate();
            LOG.info("Successfully applied {0} migrations", successfulMigrationCount);
        } catch (final FlywayException e) {
            migrationFailure = new SQLException(e.getMessage(), e);
        } finally {
            migrate = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
