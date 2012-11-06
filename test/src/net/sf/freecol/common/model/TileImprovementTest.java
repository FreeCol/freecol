/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.util.test.FreeColTestCase;

public class TileImprovementTest extends FreeColTestCase {


    public void testRiverNoExtras() {

        TileImprovementStyle style = new TileImprovementStyle("0102");
        assertEquals("0102", style.getString());
        assertEquals("0101", style.getMask());
        assertFalse(style.isConnectedTo(Direction.N));
        assertFalse(style.isConnectedTo(Direction.NE));
        assertFalse(style.isConnectedTo(Direction.E));
        assertTrue(style.isConnectedTo(Direction.SE));
        assertFalse(style.isConnectedTo(Direction.S));
        assertFalse(style.isConnectedTo(Direction.SW));
        assertFalse(style.isConnectedTo(Direction.W));
        assertTrue(style.isConnectedTo(Direction.NW));

    }

    public void testRiverWithExtras() {

        // has three characters of additional style information
        TileImprovementStyle style = new TileImprovementStyle("7!70_&?");
        assertEquals("7!70_&?", style.getString());
        assertEquals("1110", style.getMask());
        assertFalse(style.isConnectedTo(Direction.N));
        assertTrue(style.isConnectedTo(Direction.NE));
        assertFalse(style.isConnectedTo(Direction.E));
        assertTrue(style.isConnectedTo(Direction.SE));
        assertFalse(style.isConnectedTo(Direction.S));
        assertTrue(style.isConnectedTo(Direction.SW));
        assertFalse(style.isConnectedTo(Direction.W));
        assertFalse(style.isConnectedTo(Direction.NW));

    }

    public void testAllFrills() {

        // has three characters of additional style information
        TileImprovementStyle style = new TileImprovementStyle("7!70110X_&?");
        assertEquals("7!70110X_&?", style.getString());
        assertEquals("11101101", style.getMask());
        assertTrue(style.isConnectedTo(Direction.N));
        assertTrue(style.isConnectedTo(Direction.NE));
        assertTrue(style.isConnectedTo(Direction.E));
        assertFalse(style.isConnectedTo(Direction.SE));
        assertTrue(style.isConnectedTo(Direction.S));
        assertTrue(style.isConnectedTo(Direction.SW));
        assertFalse(style.isConnectedTo(Direction.W));
        assertTrue(style.isConnectedTo(Direction.NW));

    }


    public void testOldStyle() {
        TileImprovementStyle style;

        style = new TileImprovementStyle("0");
        assertEquals("0000", style.getString());
        style = new TileImprovementStyle("1");
        assertEquals("1000", style.getString());
        style = new TileImprovementStyle("3");
        assertEquals("0100", style.getString());
        style = new TileImprovementStyle("9");
        assertEquals("0010", style.getString());
        style = new TileImprovementStyle("27");
        assertEquals("0001", style.getString());
        style = new TileImprovementStyle("54");
        assertEquals("0002", style.getString());
        style = new TileImprovementStyle("67");
        assertEquals("1112", style.getString());
        style = new TileImprovementStyle("80");
        assertEquals("2222", style.getString());


    }

}