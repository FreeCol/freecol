package net.sf.freecol.common.model;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for net.sf.freecol.common");
        //$JUnit-BEGIN$
        suite.addTestSuite(MapTest.class);
        suite.addTestSuite(UnitTest.class);
        suite.addTestSuite(ColonyProductionTest.class);
        suite.addTestSuite(GameTest.class);
        //$JUnit-END$
        return suite;
    }

}
