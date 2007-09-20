package net.sf.freecol.common.model;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";

	public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

	public static final String REVISION = "$Revision$";

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for net.sf.freecol.common");
		//$JUnit-BEGIN$
		suite.addTestSuite(BuildingTest.class);
		suite.addTestSuite(SchoolTest.class);
		suite.addTestSuite(GoodsTest.class);
		suite.addTestSuite(GameTest.class);
		suite.addTestSuite(MapTest.class);
		suite.addTestSuite(TileTest.class);
		suite.addTestSuite(ColonyProductionTest.class);
		suite.addTest(AllTests.suite());
		suite.addTestSuite(UnitTest.class);
		suite.addTestSuite(ModelMessageTest.class);
		suite.addTestSuite(PlayerTest.class);
		suite.addTestSuite(NationTypeTest.class);
		//$JUnit-END$
		return suite;
	}

}
