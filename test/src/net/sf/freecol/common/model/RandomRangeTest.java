/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.Random;

import net.sf.freecol.common.model.RandomRange;

import net.sf.freecol.util.test.FreeColTestCase;


public class RandomRangeTest extends FreeColTestCase {

    public void testRandomRange() {
        Random random = new Random();
        RandomRange r = new RandomRange(100, 1, 4, 1000);
        for (int i = 0; i < 25; i++) {
            int vc = r.getAmount("test-c", random, true);
            int vd = r.getAmount("test-n", random, false);
            assertTrue("Continuous range (" + vc + ")",
                1000 <= vc && vc <= 5000);
            assertTrue("Discrete range (" + vd + ")",
                vd == 1000 || vd == 2000 || vd == 3000 || vd == 4000);
        }
        try {
            r = new RandomRange(-1, 1, 4, 1000);
            fail("Negative probability should fail");
        } catch (IllegalArgumentException ie) {}
        try {
            r = new RandomRange(100, 1, 0, 1000);
            fail("Min > max should fail");
        } catch (IllegalArgumentException ie) {}
    }
}
