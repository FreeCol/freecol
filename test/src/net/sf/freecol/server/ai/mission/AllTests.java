/**
 *  Copyright (C) 2002-2024  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.server.ai.mission;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for net.sf.freecol.server.ai.mission");
        //$JUnit-BEGIN$
        suite.addTestSuite(PioneeringMissionTest.class);
        suite.addTestSuite(PrivateerMissionTest.class);
        suite.addTestSuite(ScoutingMissionTest.class);
        suite.addTestSuite(TransportMissionTest.class);
        suite.addTestSuite(UnitSeekAndDestroyMissionTest.class);
        //$JUnit-END$
        return suite;
    }
}
