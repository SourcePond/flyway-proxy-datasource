package ch.sourcepond.osgi.flyway;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.UUID;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.reflect.Proxy.isProxyClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.FRAGMENT_HOST;
import static org.osgi.framework.Constants.OBJECTCLASS;

@RunWith(PaxExam.class)
public class ProxyTest {
    private static final String TEST = "test";
    private final DataSourceListener listener = new DataSourceListener();

    @Inject
    private BundleContext bundleContext;
    private JdbcDataSource dataSource;

    private static UrlProvisionOption sqlResourceBundle() throws MalformedURLException {
        return streamBundle(
                bundle().add("db/test/V1__InitialDbSetup.sql", new File("src/test/resources/db/test/V1__InitialDbSetup.sql").toURI().toURL())
                        .add("db/test/V2__AddAdditionalTable.sql", new File("src/test/resources/db/test/V2__AddAdditionalTable.sql").toURI().toURL())
                        .set(BUNDLE_SYMBOLICNAME, "SQLTestResource")
                        .set(FRAGMENT_HOST, "org.flywaydb.core;bundle-version=5.1.3")
                        .build(withBnd()));
    }

    @Configuration
    public Option[] configure() throws MalformedURLException {
        return new Option[]{
                bundle(new File("build/paxexam/examinee.jar").toURI().toURL().toString()),
                junitBundles(),
                sqlResourceBundle().noStart(),
                mavenBundle("com.h2database", "h2").versionAsInProject(),
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
            dataSource = new JdbcDataSource();
            dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
            dataSource.setUser(TEST);
            dataSource.setPassword(TEST);
        } finally {
            currentThread().setContextClassLoader(ldr);
        }
        bundleContext.addServiceListener(listener, format("(%s=%s)", OBJECTCLASS, DataSource.class.getName()));
    }

    @After
    public void tearDown() {
        bundleContext.removeServiceListener(listener);
    }

    private void insert(final Connection pConnection, final String pTableName, final String oid) throws SQLException {
        try (final PreparedStatement stmt = pConnection.prepareStatement(format("INSERT INTO %s (OID) VALUES (?)", pTableName))) {
            stmt.setString(1, oid);
            stmt.executeUpdate();
        }
    }

    private void verifyOid(final Connection pConnection, final String pTableName, final String expectedOid) throws SQLException {
        try (final PreparedStatement stmt = pConnection.prepareStatement(format("SELECT OID FROM %s", pTableName))) {
            ResultSet result = stmt.executeQuery();
            result.next();
            assertEquals(expectedOid, result.getString(1));
            assertFalse(result.next());
        }
    }

    @Test//(timeout = 10000)
    public void verifyTest() throws Exception {
        // Register service
        final Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("dataSourceName", TEST);
        bundleContext.registerService(DataSource.class, dataSource, properties);

        // Await DataSource proxy and check if it's a proxy
        final DataSource proxy = listener.awaitDataSource();
        assertTrue(isProxyClass(proxy.getClass()));

        // Get a connection and check whether the test table has been created
        final String expectedOid1 = UUID.randomUUID().toString();
        final String expectedOid2 = UUID.randomUUID().toString();

        try (final Connection conn = proxy.getConnection()) {
            insert(conn, "TEST_TABLE", expectedOid1);
            insert(conn, "TEST_TABLE_2", expectedOid2);
            verifyOid(conn, "TEST_TABLE", expectedOid1);
            verifyOid(conn, "TEST_TABLE_2", expectedOid2);
        }
    }
}
