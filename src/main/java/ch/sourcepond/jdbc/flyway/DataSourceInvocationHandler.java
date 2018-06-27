package ch.sourcepond.jdbc.flyway;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static java.lang.reflect.Proxy.getInvocationHandler;
import static java.lang.reflect.Proxy.isProxyClass;

final class DataSourceInvocationHandler implements InvocationHandler {
    private final DataSource wrapped;
    private final MigrationTask task;

    public DataSourceInvocationHandler(final DataSource pWrapped, final MigrationTask pTask) {
        wrapped = pWrapped;
        task = pTask;
    }



    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        if (Object.class != method.getDeclaringClass()) {
            task.awaitMigration();
        } else if ("equals".equals(method.getName())) {
            final Object other = args[0];
            if (other != null &&
                    isProxyClass(other.getClass())) {
                final InvocationHandler ih = getInvocationHandler(other);
                if (getClass() == ih.getClass()){
                    args[0] = ((DataSourceInvocationHandler)ih).wrapped;
                }
            }
        }
        try {
            return method.invoke(wrapped, args);
        } catch (final InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
