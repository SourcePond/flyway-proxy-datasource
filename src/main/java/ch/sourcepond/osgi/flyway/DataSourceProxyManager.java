package ch.sourcepond.osgi.flyway;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.slf4j.Logger;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.valueOf;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.ServiceEvent.REGISTERED;
import static org.osgi.framework.ServiceEvent.UNREGISTERING;
import static org.slf4j.LoggerFactory.getLogger;

class DataSourceProxyManager implements FindHook, EventListenerHook {
    private static final Logger LOG = getLogger(DataSourceProxyManager.class);
    private static final String FLYWAY_PROXY = "_$FLYWAY_proxy_$";

    // See OSGi compendium release 7, 125.5.2.2
    private static final String JDBC_DATASOURCE_NAME = "dataSourceName";

    private final Map<ServiceReference<?>, ServiceRegistration<?>> wrappedReferences = new ConcurrentHashMap<>();
    private final MigrationManager migrationManager;
    private final BundleContext thisContext;

    public DataSourceProxyManager(final MigrationManager pMigrationManager,
                                  final BundleContext pThisContext) {
        migrationManager = pMigrationManager;
        thisContext = pThisContext;
    }

    private static boolean isDataSource(final String... pClassNameOrNull) {
        if (pClassNameOrNull != null) {
            for (final String className : pClassNameOrNull) {
                if (DataSource.class.getName().equals(className)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isNotThis(final BundleContext pBundleContext) {
        return !thisContext.equals(pBundleContext);
    }

    private Hashtable<String, Object> buildProxyProperties(final ServiceReference<?> pReference) {
        final Hashtable<String, Object> properties = new Hashtable<>();
        for (final String key : pReference.getPropertyKeys()) {
            properties.put(key, pReference.getProperty(key));
        }
        properties.put(FLYWAY_PROXY, valueOf(true));
        return properties;
    }

    @Override
    public void find(final BundleContext context,
                     final String name,
                     final String filter,
                     final boolean allServices,
                     final Collection<ServiceReference<?>> references) {
        if (isDataSource(name) && isNotThis(context)) {
            for (final Iterator<ServiceReference<?>> it = references.iterator(); it.hasNext(); ) {
                final ServiceReference<?> ref = it.next();
                if (ref.getProperty(FLYWAY_PROXY) == null) {
                    LOG.debug("{0} should get a proxy [ name: {1}, filter: {2} ]; removing original reference [ {3} ]",
                            context.getBundle(),
                            name,
                            filter,
                            ref);
                    it.remove();
                }
            }
        }
    }

    @Override
    public void event(final ServiceEvent event, final Map<BundleContext, Collection<ListenerHook.ListenerInfo>> listeners) {
        final ServiceReference<?> serviceReference = event.getServiceReference();
        final String[] classNames = (String[]) serviceReference.getProperty(OBJECTCLASS);

        if (isDataSource(classNames) && isNotThis(serviceReference.getBundle().getBundleContext())) {
            switch (event.getType()) {
                case REGISTERED: {
                    final String datasourceName = (String) serviceReference.getProperty(JDBC_DATASOURCE_NAME);
                    final DataSource wrapped = (DataSource) thisContext.getService(serviceReference);

                    // Create a proxy for the DataSource and register it as service. Additionally, keep track of the
                    // original service. This is necessary to unget the original service and unregister the proxy when
                    // the original service is being unregistered.
                    wrappedReferences.put(serviceReference,
                            thisContext.registerService(DataSource.class,
                                    migrationManager.startMigration(wrapped, datasourceName),
                                    buildProxyProperties(serviceReference)));

                    // Nobody should receive a reference to the original service!
                    listeners.clear();

                    break;
                }
                case UNREGISTERING: {
                    final ServiceRegistration<?> wrapperRegistration = wrappedReferences.remove(serviceReference);
                    if (wrapperRegistration != null) {
                        wrapperRegistration.unregister();
                        thisContext.ungetService(serviceReference);
                    }
                    break;
                }
                default: {
                    // noop
                }
            }
        }
    }
}
