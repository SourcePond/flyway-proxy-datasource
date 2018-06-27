package ch.sourcepond.jdbc.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;

import java.sql.SQLException;

import static org.slf4j.LoggerFactory.getLogger;

final class MigrationTask extends Thread {
    private static final Logger LOG = getLogger(MigrationTask.class);
    private final Flyway flyway;
    private volatile boolean migrate = true;
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
            flyway.migrate();
        } catch (final FlywayException e) {
            LOG.error(e.getMessage(), e);
            migrationFailure = new SQLException(e.getMessage(), e);
        } finally {
            migrate = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
