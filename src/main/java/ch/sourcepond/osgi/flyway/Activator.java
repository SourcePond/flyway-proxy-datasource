package ch.sourcepond.osgi.flyway;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;

public class Activator implements BundleActivator {
    private final MigrationManager migrationManager = new MigrationManager();

    @Override
    public void start(final BundleContext context) {
        context.registerService(new String[]{
                        FindHook.class.getName(),
                        EventListenerHook.class.getName()},
                new DataSourceProxyManager(migrationManager, context), null);

    }

    @Override
    public void stop(final BundleContext context) {
        migrationManager.close();
    }
}
