/**
 * Copyright (C) 2002-2024  The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import static net.sf.freecol.common.model.Constants.INFINITY;

import java.util.Random;

import net.sf.freecol.util.test.FreeColTestCase;

public class ForceTest extends FreeColTestCase {

    public void testIsUnderprovisioned() {
        Specification spec = spec();
        Force ref = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");
        UnitType mow = spec.getUnitType("model.unit.manOWar");

        ref.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 20));
        assertTrue("Force with 20 land units and 0 ships should be underprovisioned",
            ref.isUnderprovisioned());

        ref.add(new AbstractUnit(mow, Specification.DEFAULT_ROLE_ID, 4));
        assertFalse("Force with 24 capacity for 20 units should NOT be underprovisioned",
            ref.isUnderprovisioned());
    }

    public void testDownsizeToPrice() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        dutch.setGold(500);

        Force mercs = new Force(spec());
        UnitType art = spec().getUnitType("model.unit.artillery");

        mercs.add(new AbstractUnit(art, Specification.DEFAULT_ROLE_ID, 10));

        int finalPrice = mercs.downsizeToPrice(dutch, new Random(1));

        if (finalPrice != -1) {
            assertTrue("Final price must be within budget",
                dutch.getGold() >= finalPrice);

            int unitCount = mercs.getUnitList().stream()
                .mapToInt(AbstractUnit::getNumber).sum();

            assertTrue("Force should have been reduced in size", unitCount < 10);
        }
    }

    public void testDownsizeToLimit() {
        Specification spec = spec();
        Force force = new Force(spec);
        UnitType dragoon = spec.getUnitType("model.unit.kingsRegular");

        force.add(new AbstractUnit(dragoon, "model.role.dragoon", 50));
        double initialStrength = force.getLandStrength();

        double limit = initialStrength / 2.0;
        force.downsizeToLimit(limit);

        assertTrue("Strength should be under the limit",
            force.getLandStrength() < limit);
    }

    public void testAddMergesIdenticalUnits() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");

        force.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 5));
        force.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 3));

        assertEquals("Units should merge into a single entry",
            1, force.getLandUnitsList().size());

        assertEquals("Merged unit should have total count 8",
            8, force.getLandUnitsList().get(0).getNumber());
    }

    public void testNavalVsLandClassification() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");
        UnitType ship = spec.getUnitType("model.unit.manOWar");

        force.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 1));
        force.add(new AbstractUnit(ship, Specification.DEFAULT_ROLE_ID, 1));

        assertEquals("One land unit expected", 1, force.getLandUnitsList().size());
        assertEquals("One naval unit expected", 1, force.getNavalUnitsList().size());
    }

    public void testPrepareToBoardAddsShips() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");
        UnitType ship = spec.getUnitType("model.unit.manOWar");

        force.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 10));

        int before = force.getNavalUnitsList().size();
        force.prepareToBoard(ship);
        int after = force.getNavalUnitsList().size();

        assertTrue("prepareToBoard should add ships when needed", after > before);
        assertTrue("Capacity should now exceed required space",
            force.getCapacity() >= force.getSpaceRequired());
    }

    public void testMatchAll() {
        Specification spec = spec();
        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");

        Force a = new Force(spec);
        Force b = new Force(spec);

        a.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 5));
        b.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 5));

        assertTrue("Identical forces should match", a.matchAll(b));

        b.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 1));
        assertFalse("Forces with different counts should not match", a.matchAll(b));
    }

    public void testIncreaseLandUnits() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");
        int spacePerUnit = infantry.getSpaceTaken();

        force.add(new AbstractUnit(infantry, Specification.DEFAULT_ROLE_ID, 2));
        int initialSpace = force.getSpaceRequired();

        force.increaseLandUnits(3);

        assertEquals("Unit count should increase by addition",
            5, force.getLandUnitsList().get(0).getNumber());

        assertEquals("Space required should scale correctly",
            initialSpace + (3 * spacePerUnit), force.getSpaceRequired());
    }

    public void testDownsizeToLimitRecalculatesSpaceRequired() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");
        int spaceTaken = infantry.getSpaceTaken();

        force.add(new AbstractUnit(infantry,
            Specification.DEFAULT_ROLE_ID, 20));

        force.downsizeToLimit(force.getLandStrength() / 2);

        int unitCount = force.getLandUnitsList().get(0).getNumber();

        assertEquals("Space required should match remaining units",
            unitCount * spaceTaken,
            force.getSpaceRequired());
    }

    public void testPrepareToBoardExactCapacity() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry = spec.getUnitType("model.unit.kingsRegular");
        UnitType ship = spec.getUnitType("model.unit.manOWar");

        int shipSpace = ship.getSpace();

        force.add(new AbstractUnit(infantry,
            Specification.DEFAULT_ROLE_ID, shipSpace));

        force.prepareToBoard(ship);

        assertEquals("Exactly one ship should be added",
            1,
            force.getNavalUnitsList().get(0).getNumber());

        assertEquals("Capacity should exactly match requirement",
            force.getCapacity(),
            force.getSpaceRequired());
    }

    public void testCalculateMercenaryPrice() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Force force = new Force(spec());
        UnitType artillery = spec().getUnitType("model.unit.artillery");

        force.add(new AbstractUnit(
            artillery,
            Specification.DEFAULT_ROLE_ID,
            2));

        AbstractUnit oneArtillery = new AbstractUnit(
            artillery,
            Specification.DEFAULT_ROLE_ID,
            1);

        int priceOne = dutch.getEuropeanPurchasePrice(oneArtillery);

        assertTrue("Expected a purchasable unit",
            priceOne > 0 && priceOne != INFINITY);

        int expected = (priceOne * 75 / 100) * 2;

        assertEquals(expected,
            force.calculateMercenaryPrice(dutch, 75));
    }

    public void testCalculateMercenaryPriceInvalidUnit() {
        Game game = getStandardGame();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Force force = new Force(spec());

        UnitType colonist =
            spec().getUnitType("model.unit.freeColonist");

        force.add(new AbstractUnit(
            colonist,
            Specification.DEFAULT_ROLE_ID,
            1));

        assertEquals(-1,
            force.calculateMercenaryPrice(dutch, 75));
    }
    
    public void testIncreaseLandUnitsUpdatesSpaceRequired() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry =
            spec.getUnitType("model.unit.kingsRegular");

        force.add(new AbstractUnit(
            infantry,
            Specification.DEFAULT_ROLE_ID,
            1));

        force.increaseLandUnits(9);

        assertEquals(
            10,
            force.getLandUnitsList().get(0).getNumber());

        assertEquals(
            infantry.getSpaceTaken() * 10,
            force.getSpaceRequired());
    }

    public void testCopyIsIndependent() {
        Specification spec = spec();
        Force original = new Force(spec);

        UnitType infantry =
            spec.getUnitType("model.unit.kingsRegular");

        original.add(new AbstractUnit(
            infantry,
            Specification.DEFAULT_ROLE_ID,
            10));

        Force copy = original.copy();

        copy.increaseLandUnits(5);

        assertEquals(
            10,
            original.getLandUnitsList().get(0).getNumber());

        assertEquals(
            15,
            copy.getLandUnitsList().get(0).getNumber());
    }

    public void testDownsizeToLimitNoChangeNeeded() {
        Specification spec = spec();
        Force force = new Force(spec);

        UnitType infantry =
            spec.getUnitType("model.unit.kingsRegular");

        force.add(new AbstractUnit(
            infantry,
            Specification.DEFAULT_ROLE_ID,
            5));

        double strength = force.getLandStrength();

        force.downsizeToLimit(strength + 100);

        assertEquals(
            5,
            force.getLandUnitsList().get(0).getNumber());
    }
}
