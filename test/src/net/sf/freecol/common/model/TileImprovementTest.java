/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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

import net.sf.freecol.util.test.FreeColTestCase;


public class TileImprovementTest extends FreeColTestCase {


    public void testRiverNoExtras() {

        TileImprovementStyle style = TileImprovementStyle.getInstance("0102");
        assertEquals("0102", style.getString());
        assertEquals("0101", style.getMask());
    }

    public void testRiverWithExtras() {

        // has three characters of additional style information
        TileImprovementStyle style = TileImprovementStyle.getInstance("7170_&?");
        assertEquals(null, style);
    }

    public void testRoadNoExtras() {

        TileImprovementStyle style = TileImprovementStyle.getInstance("11101101");
        assertEquals("11101101", style.getString());
        assertEquals("11101101", style.getMask());
    }

    public void testRoadWithExtras() {

        // has three characters of additional style information
        TileImprovementStyle style = TileImprovementStyle.getInstance("7170110X_&?");
        assertEquals(null, style);
    }

}
