/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

public class DemotionTest extends FreeColTestCase {

    TileType plains = spec().getTileType("model.tile.plains");

    UnitType braveType = spec().getUnitType("model.unit.brave");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType veteranType = spec().getUnitType("model.unit.veteranSoldier");
    UnitType artilleryType = spec().getUnitType("model.unit.artillery");
    UnitType damagedArtilleryType = spec().getUnitType("model.unit.damagedArtillery");

    EquipmentType muskets = spec().getEquipmentType("model.equipment.muskets");
    EquipmentType horses = spec().getEquipmentType("model.equipment.horses");

    public void testColonistDemotedBySoldier() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit colonist = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        assertTrue(colonist.hasAbility("model.ability.canBeCaptured"));
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        assertTrue(soldier.hasAbility("model.ability.canBeCaptured"));
        soldier.equipWith(muskets, true);
        assertFalse(soldier.hasAbility("model.ability.canBeCaptured"));

        colonist.demote(soldier);
        assertEquals(colonistType, colonist.getType());
        assertEquals(french, colonist.getOwner());
        assertEquals(tile2, colonist.getTile());

    }

    public void testSoldierDemotedBySoldier() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit soldier1 = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier2.equipWith(muskets, true);

        soldier1.demote(soldier2);
        assertEquals(colonistType, soldier1.getType());
        assertEquals(dutch, soldier1.getOwner());
        assertEquals(tile1, soldier1.getTile());
        assertTrue(soldier1.getEquipment().isEmpty());

        soldier1.demote(soldier2);
        assertEquals(colonistType, soldier1.getType());
        assertEquals(french, soldier1.getOwner());
        assertEquals(tile2, soldier1.getTile());
    }

    public void testDragoonDemotedBySoldier() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit dragoon = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier.equipWith(muskets, true);

        dragoon.demote(soldier);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertEquals(1, dragoon.getEquipment().size());
        assertEquals(muskets, dragoon.getEquipment().get(0));

        dragoon.demote(soldier);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertTrue(dragoon.getEquipment().isEmpty());

        dragoon.demote(soldier);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(french, dragoon.getOwner());
        assertEquals(tile2, dragoon.getTile());
    }

    public void testDragoonDemotedByBrave() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player inca = game.getPlayer("model.nation.inca");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(inca, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(inca, true);

        Unit dragoon = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        dragoon.equipWith(muskets, true);
        dragoon.equipWith(horses, true);
        Unit brave = new Unit(game, tile2, inca, braveType, Unit.ACTIVE);

        dragoon.demote(brave);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertEquals(1, dragoon.getEquipment().size());
        assertEquals(muskets, dragoon.getEquipment().get(0));
        assertEquals(1, brave.getEquipment().size());
        assertEquals(horses, brave.getEquipment().get(0));

        dragoon.demote(brave);
        assertEquals(colonistType, dragoon.getType());
        assertEquals(dutch, dragoon.getOwner());
        assertEquals(tile1, dragoon.getTile());
        assertTrue(dragoon.getEquipment().isEmpty());
        assertEquals(2, brave.getEquipment().size());
        assertEquals(horses, brave.getEquipment().get(0));
        assertEquals(muskets, brave.getEquipment().get(1));

        dragoon.demote(brave);
        assertTrue(dragoon.isDisposed());

    }

    public void testScoutDemotedBySoldier() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit scout = new Unit(game, tile1, dutch, colonistType, Unit.ACTIVE);
        scout.equipWith(horses, true);
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier.equipWith(muskets, true);

        scout.demote(soldier);
        scout.isDisposed();
    }

    public void testVeteranSoldierDemotedBySoldier() {

        Game game = getStandardGame();
        assertEquals(colonistType, veteranType.getDowngrade(UnitType.CAPTURE));
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit soldier1 = new Unit(game, tile1, dutch, veteranType, Unit.ACTIVE);
        soldier1.equipWith(muskets, true);
        Unit soldier2 = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier2.equipWith(muskets, true);

        soldier1.demote(soldier2);
        assertEquals(veteranType, soldier1.getType());
        assertEquals(dutch, soldier1.getOwner());
        assertEquals(tile1, soldier1.getTile());
        assertTrue(soldier1.getEquipment().isEmpty());

        soldier1.demote(soldier2);
        assertEquals(colonistType, soldier1.getType());
        assertEquals(french, soldier1.getOwner());
        assertEquals(tile2, soldier1.getTile());
    }

    public void testArtilleryDemotedBySoldier() {

        Game game = getStandardGame();
        assertEquals(damagedArtilleryType, artilleryType.getDowngrade(UnitType.DEMOTION));
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        Unit artillery = new Unit(game, tile1, dutch, artilleryType, Unit.ACTIVE);
        Unit soldier = new Unit(game, tile2, french, colonistType, Unit.ACTIVE);
        soldier.equipWith(muskets, true);

        artillery.demote(soldier);
        assertEquals(damagedArtilleryType, artillery.getType());
        assertEquals(dutch, artillery.getOwner());
        assertEquals(tile1, artillery.getTile());

        artillery.demote(soldier);
        assertTrue(artillery.isDisposed());
    }

}