package net.sf.freecol;

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
        return suite;
    }

}
