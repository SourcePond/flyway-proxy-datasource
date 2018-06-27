package ch.sourcepond.jdbc.flyway;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;

public class Activator implements BundleActivator {
    private MigrationManager migrationManager;

    @Override
    public void start(final BundleContext context) {
        migrationManager = new MigrationManager(context);
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
