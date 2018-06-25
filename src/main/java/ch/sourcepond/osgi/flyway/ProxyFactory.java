package ch.sourcepond.osgi.flyway;

import javax.sql.DataSource;

import static java.lang.reflect.Proxy.newProxyInstance;

class ProxyFactory {

    public DataSource createProxy(final DataSource pWrapped, final MigrationTask pTask) {
        return (DataSource) newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DataSource.class},
                new DataSourceInvocationHandler(pWrapped, pTask));
    }
}
