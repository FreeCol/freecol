/**
 *  Copyright (C) 2002-2014  The FreeCol Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class RoleTest extends FreeColTestCase {

    private static final GoodsType horses
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType muskets
        = spec().getGoodsType("model.goods.muskets");
    private static final GoodsType tools
        = spec().getGoodsType("model.goods.tools");

    private static final Role none
        = spec().getDefaultRole();
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
    private static final Role infantry
        = spec().getRole("model.role.infantry");
    private static final Role cavalry
        = spec().getRole("model.role.cavalry");
    private static final Role armedBrave
        = spec().getRole("model.role.armedBrave");
    private static final Role mountedBrave
        = spec().getRole("model.role.mountedBrave");
    private static final Role nativeDragoon
        = spec().getRole("model.role.nativeDragoon");

    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType kingsRegularType
        = spec().getUnitType("model.unit.kingsRegular");


    public void testComparators() {
        List<Role> roles = new ArrayList<Role>();
        roles.add(soldier);
        roles.add(dragoon);
        roles.add(missionary);
        Collections.sort(roles);
        assertEquals(dragoon, roles.get(0));
        assertEquals(soldier, roles.get(1));
        assertEquals(missionary, roles.get(2));

        assertEquals(0, Role.defensiveComparator.compare(soldier, soldier));
        assertEquals(0, Role.defensiveComparator.compare(dragoon, dragoon));
        assertEquals(0, Role.defensiveComparator.compare(missionary, missionary));

        assertEquals(1, Role.defensiveComparator.compare(dragoon, soldier));
        assertEquals(-1, Role.defensiveComparator.compare(soldier, dragoon));

        assertEquals(-1,  Role.defensiveComparator.compare(missionary, soldier));
        assertEquals(-1,  Role.defensiveComparator.compare(missionary, dragoon));
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

    public void testGoodsDifference() {
        assertTrue(Role.getGoodsDifference(null, 1, none, 1).isEmpty());
        assertTrue(Role.getGoodsDifference(none, 1, none, 1).isEmpty());
        assertTrue(Role.getGoodsDifference(none, 1, missionary, 1).isEmpty());
        assertTrue(Role.getGoodsDifference(missionary, 1, none, 1).isEmpty());

        List<AbstractGoods> goods
            = Role.getGoodsDifference(none, 1, soldier, 1);
        checkGoods("->soldier", goods,
            new AbstractGoods(muskets, 50));

        goods = Role.getGoodsDifference(soldier, 1, dragoon, 1);
        checkGoods("soldier->dragoon", goods,
            new AbstractGoods(horses, 50));

        goods = Role.getGoodsDifference(missionary, 1, dragoon, 1);
        checkGoods("missionary->dragoon", goods,
            new AbstractGoods(horses, 50),
            new AbstractGoods(muskets, 50));

        goods = Role.getGoodsDifference(soldier, 1, none, 1);
        checkGoods("soldier->", goods,
            new AbstractGoods(muskets, -50));

        goods = Role.getGoodsDifference(nativeDragoon, 1, armedBrave, 1);
        checkGoods("nativeDragoon->armedBrave", goods,
            new AbstractGoods(horses, -25));

        goods = Role.getGoodsDifference(soldier, 1, pioneer, 4);
        checkGoods("soldier->pioneer(4)", goods,
            new AbstractGoods(muskets, -50),
            new AbstractGoods(tools, 80));
    }

    public void testMilitaryRoles() {
        final Game game = getStandardGame();
        final List<Role> military = spec().getMilitaryRoles();

        List<Role> expectedRoles = new ArrayList<Role>();
        expectedRoles.add(dragoon);
        expectedRoles.add(soldier);
        expectedRoles.add(scout);
        List<Role> colonialRoles
            = Role.getAvailableRoles(game.getPlayer("model.nation.dutch"),
                                     colonistType, military);
        assertTrue(expectedRoles.equals(colonialRoles));

        expectedRoles.clear();
        expectedRoles.add(cavalry);
        expectedRoles.add(infantry);
        List<Role> royalRoles
            = Role.getAvailableRoles(game.getPlayer("model.nation.dutchREF"),
                                     kingsRegularType, military);
        assertTrue(expectedRoles.equals(royalRoles));

        expectedRoles.clear();
        expectedRoles.add(nativeDragoon);
        expectedRoles.add(armedBrave);
        expectedRoles.add(mountedBrave);
        List<Role> nativeRoles
            = Role.getAvailableRoles(game.getPlayer("model.nation.inca"),
                                     braveType, military);
        assertTrue(expectedRoles.equals(nativeRoles));
    }

    public void testGetRoleWithAbility() {
        Role r;

        r = spec().getRoleWithAbility(Ability.IMPROVE_TERRAIN, null);
        assertNotNull(r);
        assertEquals("model.role.pioneer", r.getId());

        r = spec().getRoleWithAbility(Ability.SPEAK_WITH_CHIEF, null);
        assertNotNull(r);
        assertEquals("model.role.scout", r.getId());

        r = spec().getRoleWithAbility(Ability.INCITE_NATIVES, null);
        assertNotNull(r);
        assertEquals("model.role.missionary", r.getId());

        r = spec().getRoleWithAbility(Ability.INCITE_NATIVES, null);
        assertNotNull(r);
        assertEquals("model.role.missionary", r.getId());
    }
}
