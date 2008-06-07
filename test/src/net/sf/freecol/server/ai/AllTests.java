package net.sf.freecol.server.ai;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Test for net.sf.freecol.server.ai");
		//$JUnit-BEGIN$
		suite.addTestSuite(MissionAssignmentTest.class);
		//$JUnit-END$
		suite.addTest(net.sf.freecol.server.ai.mission.AllTests.suite());
		return suite;
	}
}
