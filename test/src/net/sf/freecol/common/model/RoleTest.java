/**
 *  Copyright (C) 2002-2013  The FreeCol Team
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

import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class RoleTest extends FreeColTestCase {

    private static final Role none
        = spec().getRole("model.role.default");
    private static final Role scout
        = spec().getRole("model.role.scout");
    private static final Role soldier
        = spec().getRole("model.role.soldier");
    private static final Role dragoon
        = spec().getRole("model.role.dragoon");
    private static final Role pioneer
        = spec().getRole("model.role.pioneer");
    private static final Role missionary
        = spec().getRole("model.role.missionary");


    public void testComparators() {

        assertEquals(0, Role.defensiveComparator.compare(soldier, soldier));
        assertEquals(0, Role.defensiveComparator.compare(dragoon, dragoon));
        assertEquals(0, Role.defensiveComparator.compare(missionary, missionary));

        assertEquals(-1, Role.defensiveComparator.compare(dragoon, soldier));
        assertEquals(1, Role.defensiveComparator.compare(soldier, dragoon));

        assertEquals(1,  Role.defensiveComparator.compare(missionary, soldier));
        assertEquals(1,  Role.defensiveComparator.compare(missionary, dragoon));

    }

    public void testCompatibleRoles() {
        assertFalse(soldier.isCompatibleWith(none));
        assertFalse(soldier.isCompatibleWith(pioneer));
        assertFalse(soldier.isCompatibleWith(missionary));
        assertTrue(soldier.isCompatibleWith(soldier));
        assertFalse(soldier.isCompatibleWith(scout));
        assertTrue(soldier.isCompatibleWith(dragoon));

        assertFalse(missionary.isCompatibleWith(none));
        assertFalse(missionary.isCompatibleWith(pioneer));
        assertTrue(missionary.isCompatibleWith(missionary));
        assertFalse(missionary.isCompatibleWith(soldier));
        assertFalse(missionary.isCompatibleWith(scout));
        assertFalse(missionary.isCompatibleWith(dragoon));
    }

    public void testSetRole() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player dutchREF = game.getPlayer("model.nation.dutchREF");
        Player sioux = game.getPlayer("model.nation.sioux");
        /*
        TileType plains = spec().getTileType("model.tile.plains");
        game.setMap(getTestMap(plains, true));

        Tile tile1 = game.getMap().getTile(6, 8);
        Tile tile2 = game.getMap().getTile(6, 9);
        */

        EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
        EquipmentType horses = spec().getEquipmentType("model.equipment.horses");

        UnitType merchantmanType = spec().getUnitType("model.unit.merchantman");
        Unit merchantman = new ServerUnit(game, null, dutch, merchantmanType);
        assertEquals(none, merchantman.getRole());

        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new ServerUnit(game, null, dutch, colonistType);
        assertEquals(none, colonist.getRole());
        colonist = new ServerUnit(game, null, dutch, colonistType, muskets);
        assertEquals(soldier, colonist.getRole());
        colonist = new ServerUnit(game, null, dutch, colonistType, horses);
        assertEquals(scout, colonist.getRole());
        colonist = new ServerUnit(game, null, dutch, colonistType, horses, muskets);
        assertEquals(dragoon, colonist.getRole());
        colonist = new ServerUnit(game, null, dutch, colonistType,
                                  spec().getEquipmentType("model.equipment.tools"));
        assertEquals(pioneer, colonist.getRole());
        colonist = new ServerUnit(game, null, dutch, colonistType,
                                  spec().getEquipmentType("model.equipment.missionary"));
        assertEquals(missionary, colonist.getRole());

        UnitType regularType = spec().getUnitType("model.unit.kingsRegular");
        Unit regular = new ServerUnit(game, null, dutchREF, regularType);
        assertEquals(none, regular.getRole());
        regular = new ServerUnit(game, null, dutchREF, regularType, muskets);
        assertEquals(spec().getRole("model.role.infantry"), regular.getRole());
        regular = new ServerUnit(game, null, dutchREF, regularType, horses);
        assertEquals(none, regular.getRole());
        regular = new ServerUnit(game, null, dutchREF, regularType, horses, muskets);
        assertEquals(spec().getRole("model.role.cavalry"), regular.getRole());

        muskets = spec().getEquipmentType("model.equipment.indian.muskets");
        horses = spec().getEquipmentType("model.equipment.indian.horses");

        UnitType braveType = spec().getUnitType("model.unit.brave");
        Unit brave = new ServerUnit(game, null, sioux, braveType);
        assertEquals(none, brave.getRole());
        brave = new ServerUnit(game, null, sioux, braveType, muskets);
        assertEquals(spec().getRole("model.role.armedBrave"), brave.getRole());
        brave = new ServerUnit(game, null, sioux, braveType, horses);
        assertEquals(spec().getRole("model.role.mountedBrave"), brave.getRole());
        brave = new ServerUnit(game, null, sioux, braveType, muskets, horses);
        assertEquals(spec().getRole("model.role.nativeDragoon"), brave.getRole());
    }

}
