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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
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
    private static final Role mission
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


    public void testRoleComparator() {
        assertEquals(0,  Role.militaryComparator.compare(soldier, soldier));
        assertEquals(0,  Role.militaryComparator.compare(dragoon, dragoon));
        assertEquals(0,  Role.militaryComparator.compare(mission, mission));
        assertEquals(-1, Role.militaryComparator.compare(dragoon, soldier));
        assertEquals(1,  Role.militaryComparator.compare(soldier, dragoon));
        assertEquals(1,  Role.militaryComparator.compare(mission, soldier));
        assertEquals(-1, Role.militaryComparator.compare(dragoon, mission));
    }

    public void testCompatibleRoles() {
        assertFalse(soldier.isCompatibleWith(none));
        assertFalse(soldier.isCompatibleWith(pioneer));
        assertFalse(soldier.isCompatibleWith(mission));
        assertTrue(soldier.isCompatibleWith(soldier));
        assertFalse(soldier.isCompatibleWith(scout));
        assertTrue(soldier.isCompatibleWith(dragoon));

        assertFalse(mission.isCompatibleWith(none));
        assertFalse(mission.isCompatibleWith(pioneer));
        assertTrue(mission.isCompatibleWith(mission));
        assertFalse(mission.isCompatibleWith(soldier));
        assertFalse(mission.isCompatibleWith(scout));
        assertFalse(mission.isCompatibleWith(dragoon));
    }

    public void testGoodsDifference() {
        assertTrue(Role.getGoodsDifference(null, 1, none, 1).isEmpty());
        assertTrue(Role.getGoodsDifference(none, 1, none, 1).isEmpty());
        assertTrue(Role.getGoodsDifference(none, 1, mission, 1).isEmpty());
        assertTrue(Role.getGoodsDifference(mission, 1, none, 1).isEmpty());

        List<AbstractGoods> goods
            = Role.getGoodsDifference(none, 1, soldier, 1);
        checkGoods("->soldier", goods,
            new AbstractGoods(muskets, 50));

        goods = Role.getGoodsDifference(soldier, 1, dragoon, 1);
        checkGoods("soldier->dragoon", goods,
            new AbstractGoods(horses, 50));

        goods = Role.getGoodsDifference(mission, 1, dragoon, 1);
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
        final List<Role> military = spec().getMilitaryRolesList();

        List<Role> expectedRoles = new ArrayList<>();
        expectedRoles.add(dragoon);
        expectedRoles.add(soldier);
        expectedRoles.add(scout);
        List<Role> colonialRoles
            = Role.getAvailableRoles(game.getPlayerByNationId("model.nation.dutch"),
                                     colonistType, military);
        assertTrue(expectedRoles.equals(colonialRoles));

        expectedRoles.clear();
        expectedRoles.add(cavalry);
        expectedRoles.add(infantry);
        List<Role> royalRoles
            = Role.getAvailableRoles(game.getPlayerByNationId("model.nation.dutchREF"),
                                     kingsRegularType, military);
        assertTrue(expectedRoles.equals(royalRoles));

        expectedRoles.clear();
        expectedRoles.add(nativeDragoon);
        expectedRoles.add(armedBrave);
        expectedRoles.add(mountedBrave);
        List<Role> nativeRoles
            = Role.getAvailableRoles(game.getPlayerByNationId("model.nation.inca"),
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

    public void testCopyInCopiesFields() {
        Specification spec = spec();

        Role a = new Role("model.role.testCopy", spec);
        Role b = new Role("model.role.testCopy", spec);

        GoodsType muskets = spec.getGoodsType("model.goods.muskets");
        a.setRequiredPopulation(3);
        a.addRequiredAbility("model.ability.buildShips", true);
        a.setRequiredGoods(Arrays.asList(new AbstractGoods(muskets, 40)));

        Role downgrade = spec.getRole("model.role.soldier");
        a.setDowngrade(downgrade);
        a.setMaximumCount(5);
        UnitType expert = spec.getUnitType("model.unit.freeColonist");
        a.setExpertUnit(expert);
        a.setRoleChanges(Arrays.asList(
            new Role.RoleChange("model.role.soldier", "model.role.dragoon")
        ));

        boolean ok = b.copyIn(a);
        assertTrue(ok);

        assertEquals(3, b.getRequiredPopulation());
        assertTrue(b.requiresAbility("model.ability.buildShips"));
        assertEquals(40, b.getRequiredAmountOf(muskets));

        assertEquals(downgrade, b.getDowngrade());
        assertEquals(5, b.getMaximumCount());
        assertEquals(expert, b.getExpertUnit());
        assertEquals(1, b.getRoleChanges().size());
        assertEquals("model.role.soldier", b.getRoleChanges().get(0).from);
        assertEquals("model.role.dragoon", b.getRoleChanges().get(0).capture);
    }

    public void testGetRequiredGoodsListScaling() {
        Specification spec = spec();
        Role soldier = spec.getRole("model.role.soldier");

        List<AbstractGoods> base = soldier.getRequiredGoodsList();
        assertEquals(1, base.size());
        assertEquals(50, base.get(0).getAmount());

        List<AbstractGoods> scaled = soldier.getRequiredGoodsList(3);
        assertEquals(1, scaled.size());
        assertEquals(150, scaled.get(0).getAmount());
    }

    public void testGetGoodsDifferenceComplex() {
        Specification spec = spec();
        Role soldier = spec.getRole("model.role.soldier");
        Role pioneer = spec.getRole("model.role.pioneer");
        GoodsType muskets = spec.getGoodsType("model.goods.muskets");
        GoodsType tools = spec.getGoodsType("model.goods.tools");

        List<AbstractGoods> diff = Role.getGoodsDifference(soldier, 1, pioneer, 1);

        assertEquals(2, diff.size());

        AbstractGoods g1 = diff.get(0);
        AbstractGoods g2 = diff.get(1);

        boolean musketsFound = 
            (g1.getType() == muskets && g1.getAmount() == -50) ||
            (g2.getType() == muskets && g2.getAmount() == -50);

        boolean toolsFound =
            (g1.getType() == tools && g1.getAmount() == 20) ||
            (g2.getType() == tools && g2.getAmount() == 20);

        assertTrue(musketsFound);
        assertTrue(toolsFound);
    }

    public void testRolesCompatible() {
        Specification spec = spec();

        Role soldier = spec.getRole("model.role.soldier");
        Role dragoon = spec.getRole("model.role.dragoon");
        Role pioneer = spec.getRole("model.role.pioneer");
        Role none = spec.getDefaultRole();

        assertTrue(Role.rolesCompatible(soldier, soldier));

        assertTrue(Role.rolesCompatible(dragoon, soldier));
        assertTrue(Role.rolesCompatible(soldier, dragoon));

        assertFalse(Role.rolesCompatible(soldier, pioneer));
        assertFalse(Role.rolesCompatible(pioneer, dragoon));

        assertTrue(Role.rolesCompatible(none, none));
        assertFalse(Role.rolesCompatible(none, soldier));
    }

    public void testGetAvailableRolesBasic() {
        final Game game = getStandardGame();
        final Specification spec = spec();

        final List<Role> allRoles = spec.getMilitaryRolesList();

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        UnitType colonist = spec.getUnitType("model.unit.freeColonist");

        List<Role> dutchRoles = Role.getAvailableRoles(dutch, colonist, allRoles);

        // Expected: scout, soldier, dragoon (same as testMilitaryRoles)
        assertTrue(dutchRoles.contains(spec.getRole("model.role.scout")));
        assertTrue(dutchRoles.contains(spec.getRole("model.role.soldier")));
        assertTrue(dutchRoles.contains(spec.getRole("model.role.dragoon")));
        assertEquals(3, dutchRoles.size());

        Player inca = game.getPlayerByNationId("model.nation.inca");
        UnitType brave = spec.getUnitType("model.unit.brave");

        List<Role> nativeRoles = Role.getAvailableRoles(inca, brave, allRoles);

        assertTrue(nativeRoles.contains(spec.getRole("model.role.armedBrave")));
        assertTrue(nativeRoles.contains(spec.getRole("model.role.mountedBrave")));
        assertTrue(nativeRoles.contains(spec.getRole("model.role.nativeDragoon")));
        assertEquals(3, nativeRoles.size());
    }

    public void testGetRoleWithAbilityBasic() {
        Specification spec = spec();

        Role r;

        r = spec.getRoleWithAbility(Ability.IMPROVE_TERRAIN, null);
        assertNotNull(r);
        assertEquals("model.role.pioneer", r.getId());

        r = spec.getRoleWithAbility(Ability.SPEAK_WITH_CHIEF, null);
        assertNotNull(r);
        assertEquals("model.role.scout", r.getId());

        r = spec.getRoleWithAbility(Ability.INCITE_NATIVES, null);
        assertNotNull(r);
        assertEquals("model.role.missionary", r.getId());
    }

    public void testSerializationRoundTrip() throws Exception {
        Specification spec = spec();
        Role original = new Role("model.role.testXml", spec);

        original.setRequiredPopulation(2);
        original.setMaximumCount(3);
        original.setDowngrade(spec.getRole("model.role.soldier"));
        original.setExpertUnit(spec.getUnitType("model.unit.hardyPioneer"));
        original.addRequiredAbility("model.ability.native", true);
        
        original.setRoleChanges(Arrays.asList(
            new Role.RoleChange("model.role.soldier", "model.role.dragoon"),
            new Role.RoleChange("model.role.scout", "model.role.dragoon")
        ));

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        FreeColXMLWriter xw = new FreeColXMLWriter(out, FreeColXMLWriter.WriteScope.toSave(), false);

        xw.writeStartElement(original.getXMLTagName());
        original.writeAttributes(xw);
        original.writeChildren(xw);
        xw.writeEndElement();
        xw.close();

        String xml = out.toString("UTF-8");
        
        assertTrue(xml.contains("maximum-count=\"3\""));
        assertTrue(xml.contains("downgrade=\"model.role.soldier\""));
        assertTrue(xml.contains("<role-change from=\"model.role.soldier\""));

        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(out.toByteArray());
        FreeColXMLReader xr = new FreeColXMLReader(in);
        xr.nextTag();

        Role loaded = new Role("model.role.testXml", spec);
        loaded.readFromXML(xr);
        xr.close();

        assertEquals(original.getId(), loaded.getId());
        assertEquals(2, loaded.getRequiredPopulation());
        assertEquals(3, loaded.getMaximumCount());
        assertEquals(original.getDowngrade(), loaded.getDowngrade());
        assertEquals(original.getExpertUnit(), loaded.getExpertUnit());
        assertTrue(loaded.requiresAbility("model.ability.native"));
        
        assertEquals(2, loaded.getRoleChanges().size());
        assertEquals("model.role.soldier", loaded.getRoleChanges().get(0).from);
        assertEquals("model.role.dragoon", loaded.getRoleChanges().get(0).capture);
    }
}
