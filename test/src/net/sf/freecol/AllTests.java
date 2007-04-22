package net.sf.freecol;

import java.util.Locale;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * The main test class for Freecol. All tests in the subfolders will be run.
 * 
 */
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for net.sf.freecol");
        // $JUnit-BEGIN$

        // $JUnit-END$
        suite.addTest(net.sf.freecol.common.AllTests.suite());
        suite.addTest(net.sf.freecol.server.generator.AllTests.suite());
        suite.addTest(net.sf.freecol.client.gui.i18n.AllTests.suite());

        // Make sure that we run the tests using the english locale
        TestSetup wrapper = new TestSetup(suite) {
            public void setUp() {
                Locale.setDefault(Locale.US);
            }
        };
        return wrapper;

    }

}
