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

package net.sf.freecol.server.ai;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ContactTest extends FreeColTestCase {

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");

    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");
    private static final UnitType braveType
        = spec().getUnitType("model.unit.brave");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    /* Disabled.  European contact now required diplomatic negotiation.

    public void testEuropeanMeetsEuropean() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(6, 8);
        tile1.setExplored(dutch, true);
        tile1.setExplored(french, true);
        Tile tile2 = map.getTile(5, 8);
        tile2.setExplored(dutch, true);
        tile2.setExplored(french, true);
        Tile tile3 = map.getTile(4, 8);
        tile3.setExplored(dutch, true);
        tile3.setExplored(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        colonist.setState(Unit.UnitState.FORTIFYING);
        colonist.setState(Unit.UnitState.FORTIFIED);
        Unit soldier = new ServerUnit(game, tile3, french, colonistType);
        igc.move(french, soldier, tile2);

        assertEquals(Stance.PEACE, french.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(french));
    }
    */

    public void testEuropeanMeetsNative() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayerByNationId("model.nation.iroquois");
        Tile tile1 = map.getTile(6, 8);
        tile1.setExplored(dutch, true);
        Tile tile2 = map.getTile(5, 8);
        tile2.setExplored(dutch, true);
        Tile tile3 = map.getTile(4, 8);
        tile3.setExplored(dutch, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        ServerUnit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        colonist.setState(Unit.UnitState.FORTIFYING);
        colonist.setState(Unit.UnitState.FORTIFIED);
        ServerUnit soldier = new ServerUnit(game, tile3, iroquois, braveType);
        assertNotNull(soldier);

        igc.move(dutch, colonist, tile2);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));
    }

    /* Disabled.  European contact now required diplomatic negotiation.

    public void testEuropeanMeetsColony() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExplored(dutch, true);
        tile1.setExplored(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExplored(dutch, true);
        tile2.setExplored(french, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExplored(dutch, true);
        tile3.setExplored(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        @SuppressWarnings("unused")
        Colony colony = getStandardColony(1, 5, 8);
        Unit soldier = new ServerUnit(game, tile3, french, colonistType);
        igc.move(french, soldier, tile2);

        assertEquals(Stance.PEACE, french.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(french));
    }
    */

    public void testEuropeanMeetsIndianSettlement() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayerByNationId("model.nation.iroquois");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExplored(dutch, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExplored(dutch, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExplored(dutch, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(iroquois).settlementTile(tile3).skillToTeach(null).build();
        ServerUnit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        colonist.setState(Unit.UnitState.FORTIFYING);
        colonist.setState(Unit.UnitState.FORTIFIED);
        igc.move(dutch, colonist, tile2);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));
        assertNotNull(settlement.getAlarm(dutch));
    }

    public void testNativeMeetsEuropean() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer apache = (ServerPlayer) game.getPlayerByNationId("model.nation.apache");
        ServerPlayer french = (ServerPlayer) game.getPlayerByNationId("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExplored(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExplored(french, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExplored(french, true);

        assertFalse(french.hasContacted(apache));
        assertFalse(apache.hasContacted(french));

        ServerUnit brave = new ServerUnit(game, tile1, apache, braveType);
        brave.setState(Unit.UnitState.FORTIFYING);
        brave.setState(Unit.UnitState.FORTIFIED);
        ServerUnit colonist = new ServerUnit(game, tile3, french, colonistType);
        assertNotNull(colonist);
        igc.move(apache, brave, tile2);

        assertTrue(french.hasContacted(apache));
        assertTrue(apache.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(french));
    }

    public void testNativeMeetsNative() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer apache = (ServerPlayer) game.getPlayerByNationId("model.nation.apache");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayerByNationId("model.nation.iroquois");
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Tile tile3 = map.getTile(3, 8);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));

        ServerUnit brave1 = new ServerUnit(game, tile1, apache, braveType);
        brave1.setState(Unit.UnitState.FORTIFYING);
        brave1.setState(Unit.UnitState.FORTIFIED);
        ServerUnit brave2 = new ServerUnit(game, tile3, iroquois, braveType);
        assertNotNull(brave2);
        igc.move(apache, brave1, tile2);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(iroquois));
    }

    public void testNativeMeetsColony() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer apache = (ServerPlayer) game.getPlayerByNationId("model.nation.apache");
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExplored(dutch, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExplored(dutch, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExplored(dutch, true);

        assertFalse(dutch.hasContacted(apache));
        assertFalse(apache.hasContacted(dutch));

        Colony colony = getStandardColony(1, 5, 8);
        assertNotNull(colony);
        ServerUnit brave = new ServerUnit(game, tile3, apache, braveType);
        igc.move(apache, brave, tile2);

        assertTrue(dutch.hasContacted(apache));
        assertTrue(apache.hasContacted(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(dutch));
    }

    public void testNativeMeetsIndianSettlement() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(plains));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer apache = (ServerPlayer) game.getPlayerByNationId("model.nation.apache");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayerByNationId("model.nation.iroquois");
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Tile tile3 = map.getTile(3, 8);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));

        // build settlement
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        builder.player(iroquois).settlementTile(tile3).skillToTeach(null).build();
        ServerUnit brave = new ServerUnit(game, tile1, apache, braveType);
        brave.setState(Unit.UnitState.FORTIFYING);
        brave.setState(Unit.UnitState.FORTIFIED);
        igc.move(apache, brave, tile2);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(iroquois));
    }

    public void testShipMeetsShip() throws Exception {
        Game game = ServerTestHelper.startServerGame(getTestMap(ocean));
        Map map = game.getMap();
        InGameController igc = ServerTestHelper.getInGameController();

        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayerByNationId("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExplored(dutch, true);
        tile1.setExplored(french, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExplored(dutch, true);
        tile2.setExplored(french, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExplored(dutch, true);
        tile3.setExplored(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        ServerUnit ship1 = new ServerUnit(game, tile1, dutch, galleonType);
        ship1.setState(Unit.UnitState.FORTIFYING);
        ship1.setState(Unit.UnitState.FORTIFIED);
        ServerUnit ship2 = new ServerUnit(game, tile3, french, galleonType);
        assertNotNull(ship2);
        igc.move(dutch, ship1, tile2);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));
        assertEquals(Stance.UNCONTACTED, french.getStance(dutch));
        assertEquals(Stance.UNCONTACTED, dutch.getStance(french));
    }
}
