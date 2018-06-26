package ch.sourcepond.osgi.flyway;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static org.mockito.Mockito.mock;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.osgi.framework.Constants.OBJECTCLASS;

@RunWith(PaxExam.class)
public class ProxyTest {

    private final DataSourceListener listener = new DataSourceListener();

    @Inject
    private BundleContext bundleContext;
    private DataSource dataSource;

    @Configuration
    public Option[] configure() throws MalformedURLException {
        return new Option[]{
                junitBundles(),
                bundle(new File("build/paxexam/examinee.jar").toURI().toURL().toString()),
                mavenBundle("org.slf4j", "slf4j-api").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-core").versionAsInProject(),
                mavenBundle("ch.qos.logback", "logback-classic").versionAsInProject(),
                mavenBundle("org.flywaydb", "flyway-core").versionAsInProject(),
                mavenBundle("org.objenesis", "objenesis").versionAsInProject(),
                mavenBundle("org.mockito", "mockito-core").versionAsInProject(),
                mavenBundle("net.bytebuddy", "byte-buddy").versionAsInProject(),
                mavenBundle("net.bytebuddy", "byte-buddy-agent").versionAsInProject()
        };
    }

    @Before
    public void setup() throws Exception {
        final ClassLoader ldr = currentThread().getContextClassLoader();
        currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            dataSource = mock(DataSource.class);
        } finally {
            currentThread().setContextClassLoader(ldr);
        }
        bundleContext.addServiceListener(listener, format("(%s=%s)", OBJECTCLASS, DataSource.class.getName()));
    }

    @After
    public void tearDown() {
        bundleContext.removeServiceListener(listener);
    }

    @Test(timeout = 10000)
    public void verifyTest() throws Exception {
        // Register service
        bundleContext.registerService(DataSource.class, dataSource, null);

        // Await DataSource proxy
        final DataSource proxy = listener.awaitDataSource();
        System.out.println(proxy);
    }
}
