/**
 *  Copyright (C) 2002-2021  The FreeCol Team
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

package net.sf.freecol;

import java.util.Locale;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * The main test class for Freecol. All tests in the subfolders will be run.
 */
public class AllTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("Test for net.sf.freecol");
        // $JUnit-BEGIN$

        // $JUnit-END$
        suite.addTest(net.sf.freecol.common.AllTests.suite());
        suite.addTest(net.sf.freecol.server.AllTests.suite());
        suite.addTest(net.sf.freecol.server.generator.AllTests.suite());
        suite.addTest(net.sf.freecol.client.control.AllTests.suite());
        suite.addTest(net.sf.freecol.client.gui.AllTests.suite());

        // Make sure that we run the tests using the english locale
        TestSetup wrapper = new TestSetup(suite) {
            public void setUp() {
                Locale.setDefault(Locale.US);
            }
        };
        return wrapper;

    }

}
