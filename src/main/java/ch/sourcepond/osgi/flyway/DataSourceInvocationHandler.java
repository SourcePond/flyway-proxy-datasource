package ch.sourcepond.osgi.flyway;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class DataSourceInvocationHandler implements InvocationHandler {
    private final DataSource wrapped;
    private final MigrationTask task;

    public DataSourceInvocationHandler(final DataSource pWrapped, final MigrationTask pTask) {
        wrapped = pWrapped;
        task = pTask;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        task.awaitMigration();

        try {
            return method.invoke(wrapped, args);
        } catch (final InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
