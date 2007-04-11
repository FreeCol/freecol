package net.sf.freecol.server.generator;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for net.sf.freecol.common");
		//$JUnit-BEGIN$
		suite.addTestSuite(MapGeneratorTest.class);
		//$JUnit-END$
		return suite;
	}

}
