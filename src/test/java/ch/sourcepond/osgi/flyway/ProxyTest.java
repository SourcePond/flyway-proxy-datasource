package ch.sourcepond.osgi.flyway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

import javax.inject.Inject;
import javax.sql.DataSource;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

@RunWith(PaxExam.class)
public class ProxyTest {

    @Inject
    private BundleContext bundleContext;

    private DataSource dataSource = Mockito.mock(DataSource.class);

    @Configuration
    public Option[] configure() {
        return new Option[]{
                mavenBundle("org.objenesis", "objenesis").versionAsInProject(),
                mavenBundle("org.mockito", "mockito-core").versionAsInProject(),
                mavenBundle("net.bytebuddy", "byte-buddy").versionAsInProject(),
                mavenBundle("net.bytebuddy", "byte-buddy-agent").versionAsInProject()
        };
    }

    @Test
    public void verifyTest() {
        System.out.println(bundleContext.getBundle().getSymbolicName());
    }
}
