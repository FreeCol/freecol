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

import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

public class ContactTest extends FreeColTestCase {


    TileType plains = spec().getTileType("model.tile.plains");
    TileType ocean = spec().getTileType("model.tile.ocean");

    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType braveType = spec().getUnitType("model.unit.brave");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");


    public void testEuropeanMeetsEuropean() throws Exception {

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

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        Unit soldier = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);

        assertTrue(french.hasContacted(dutch));
        assertTrue(dutch.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(french));

    }

    public void testEuropeanMeetsNative() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(iroquois, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        Unit soldier = new Unit(game, tile2, iroquois, braveType, UnitState.ACTIVE);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));

    }

    public void testEuropeanMeetsColony() throws Exception {

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

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        Colony colony = getStandardColony(1, 5, 8);
        Unit soldier = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);

        assertTrue(french.hasContacted(dutch));
        assertTrue(dutch.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(french));

    }

    public void testEuropeanMeetsIndianSettlement() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(iroquois, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(iroquois).settlementTile(tile2).skillToTeach(null).build();
        settlement.placeSettlement();
        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));
        assertNotNull(settlement.getAlarm(dutch));

    }

    public void testNativeMeetsEuropean() throws Exception {

        Game game = getStandardGame();
        Player apache = game.getPlayer("model.nation.apache");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(french, true);

        assertFalse(french.hasContacted(apache));
        assertFalse(apache.hasContacted(french));

        Unit brave = new Unit(game, tile1, apache, braveType, UnitState.FORTIFIED);
        Unit colonist = new Unit(game, tile2, french, colonistType, UnitState.ACTIVE);

        assertTrue(french.hasContacted(apache));
        assertTrue(apache.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(french));

    }

    public void testNativeMeetsNative() throws Exception {

        Game game = getStandardGame();
        Player apache = game.getPlayer("model.nation.apache");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(iroquois, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(apache));
        assertFalse(apache.hasContacted(iroquois));

        Unit brave1 = new Unit(game, tile1, apache, braveType, UnitState.FORTIFIED);
        Unit brave2 = new Unit(game, tile2, iroquois, braveType, UnitState.ACTIVE);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(iroquois));

        // TODO: do we need this?
        // assertNotNull(iroquois.getTension(apache));

    }

    public void testNativeMeetsColony() throws Exception {

        Game game = getStandardGame();
        Player apache = game.getPlayer("model.nation.apache");
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(dutch, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(dutch, true);

        assertFalse(dutch.hasContacted(apache));
        assertFalse(apache.hasContacted(dutch));

        Colony colony = getStandardColony(1, 5, 8);
        Unit brave = new Unit(game, tile2, apache, braveType, UnitState.ACTIVE);

        assertTrue(dutch.hasContacted(apache));
        assertTrue(apache.hasContacted(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(dutch));

    }

    public void testNativeMeetsIndianSettlement() throws Exception {

        Game game = getStandardGame();
        Player apache = game.getPlayer("model.nation.apache");
        Player iroquois = game.getPlayer("model.nation.iroquois");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(iroquois, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(apache));
        assertFalse(apache.hasContacted(iroquois));

        // build settlement
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(iroquois).settlementTile(tile2).skillToTeach(null).build();
        settlement.placeSettlement();
        Unit brave = new Unit(game, tile1, apache, braveType, UnitState.FORTIFIED);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(iroquois));

        // TODO: do we need this?
        // assertNotNull(iroquois.getTension(apache));
        // assertNotNull(settlement.getAlarm(iroquois));

    }

    public void testShipMeetsShip() throws Exception {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        Map map = getTestMap(ocean);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        Unit ship1 = new Unit(game, tile1, dutch, galleonType, UnitState.FORTIFIED);
        Unit ship2 = new Unit(game, tile2, french, galleonType, UnitState.ACTIVE);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));
        assertEquals(Stance.UNCONTACTED, french.getStance(dutch));
        assertEquals(Stance.UNCONTACTED, dutch.getStance(french));

    }

}