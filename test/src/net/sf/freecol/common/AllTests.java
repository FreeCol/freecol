package net.sf.freecol.common;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for net.sf.freecol.common");
		//$JUnit-BEGIN$
		suite.addTestSuite(SpecificationTest.class);
		//$JUnit-END$
		suite.addTest(net.sf.freecol.common.model.AllTests.suite());
		return suite;
	}

}
