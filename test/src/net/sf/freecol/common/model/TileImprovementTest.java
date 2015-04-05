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

import net.sf.freecol.common.model.Direction;
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
        assertEquals("7170_&?", style.getString());
        assertEquals("1110", style.getMask());
    }

    public void testAllFrills() {

        // has three characters of additional style information
        TileImprovementStyle style = TileImprovementStyle.getInstance("7170110X_&?");
        assertEquals("7170110X_&?", style.getString());
        assertEquals("11101101", style.getMask());
    }


    public void testOldStyle() {
        final int pad = Direction.longSides.size();
        TileImprovementStyle style;

        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("0", pad));
        assertNull(style);
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("1", pad));
        assertEquals("1000", style.getString());
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("3", pad));
        assertEquals("0100", style.getString());
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("9", pad));
        assertEquals("0010", style.getString());
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("27", pad));
        assertEquals("0001", style.getString());
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("54", pad));
        assertEquals("0002", style.getString());
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("67", pad));
        assertEquals("1112", style.getString());
        style = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("80", pad));
        assertEquals("2222", style.getString());
    }

    public void testEquality() {
        final int pad = Direction.longSides.size();
        TileImprovementStyle style1, style2;

        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("0", pad));
        assertNull(style1);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("1", pad));
        style2 = TileImprovementStyle.getInstance("1000");
        assertTrue(style1 == style2);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("3", pad));
        style2 = TileImprovementStyle.getInstance("0100");
        assertTrue(style1 == style2);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("9", pad));
        style2 = TileImprovementStyle.getInstance("0010");
        assertTrue(style1 == style2);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("27", pad));
        style2 = TileImprovementStyle.getInstance("0001");
        assertTrue(style1 == style2);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("54", pad));
        style2 = TileImprovementStyle.getInstance("0002");
        assertTrue(style1 == style2);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("67", pad));
        style2 = TileImprovementStyle.getInstance("1112");
        assertTrue(style1 == style2);
        style1 = TileImprovementStyle.getInstance(TileImprovementStyle.decodeOldStyle("80", pad));
        style2 = TileImprovementStyle.getInstance("2222");
        assertTrue(style1 == style2);
    }
}
