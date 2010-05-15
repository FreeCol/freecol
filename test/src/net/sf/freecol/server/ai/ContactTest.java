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

package net.sf.freecol.server.ai;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;


public class ContactTest extends FreeColTestCase {

    TileType plains = spec().getTileType("model.tile.plains");
    TileType ocean = spec().getTileType("model.tile.ocean");

    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType braveType = spec().getUnitType("model.unit.brave");
    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");

    FreeColServer server = null;


    public void tearDown() throws Exception {
        if (server != null) {
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            FreeCol.setInDebugMode(false);
            FreeColTestCase.setGame(null);
            server = null;
        }
        super.tearDown();
    }

    public void testEuropeanMeetsEuropean() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(6, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, false);
        Tile tile2 = map.getTile(5, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Tile tile3 = map.getTile(4, 8);
        tile3.setExploredBy(dutch, false);
        tile3.setExploredBy(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        @SuppressWarnings("unused")
        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        Unit soldier = new Unit(game, tile3, french, colonistType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        igc.move(french, soldier, tile2);

        assertTrue(french.hasContacted(dutch));
        assertTrue(dutch.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(french));
    }

    public void testEuropeanMeetsNative() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayer("model.nation.iroquois");
        Tile tile1 = map.getTile(6, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(iroquois, false);
        Tile tile2 = map.getTile(5, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(iroquois, true);
        Tile tile3 = map.getTile(4, 8);
        tile3.setExploredBy(dutch, false);
        tile3.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        Unit soldier = new Unit(game, tile3, iroquois, braveType, UnitState.ACTIVE);

        InGameController igc = (InGameController) server.getController();
        igc.move(dutch, colonist, tile2);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));
    }

    public void testEuropeanMeetsColony() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, false);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(dutch, false);
        tile3.setExploredBy(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        Colony colony = getStandardColony(1, 5, 8);
        Unit soldier = new Unit(game, tile3, french, colonistType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        igc.move(french, soldier, tile2);

        assertTrue(french.hasContacted(dutch));
        assertTrue(dutch.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(french));
    }

    public void testEuropeanMeetsIndianSettlement() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayer("model.nation.iroquois");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(iroquois, false);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(iroquois, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(dutch, false);
        tile3.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(dutch));
        assertFalse(dutch.hasContacted(iroquois));

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(iroquois).settlementTile(tile3).skillToTeach(null).build();
        Unit colonist = new Unit(game, tile1, dutch, colonistType, UnitState.FORTIFIED);
        InGameController igc = (InGameController) server.getController();
        igc.move(dutch, colonist, tile2);

        assertTrue(iroquois.hasContacted(dutch));
        assertTrue(dutch.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(iroquois));

        assertNotNull(iroquois.getTension(dutch));
        assertNotNull(settlement.getAlarm(dutch));
    }

    public void testNativeMeetsEuropean() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer apache = (ServerPlayer) game.getPlayer("model.nation.apache");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(french, false);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(french, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(apache, false);
        tile3.setExploredBy(french, true);

        assertFalse(french.hasContacted(apache));
        assertFalse(apache.hasContacted(french));

        Unit brave = new Unit(game, tile1, apache, braveType, UnitState.FORTIFIED);
        Unit colonist = new Unit(game, tile3, french, colonistType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        igc.move(apache, brave, tile2);

        assertTrue(french.hasContacted(apache));
        assertTrue(apache.hasContacted(french));
        assertEquals(Stance.PEACE, french.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(french));
    }

    public void testNativeMeetsNative() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer apache = (ServerPlayer) game.getPlayer("model.nation.apache");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayer("model.nation.iroquois");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(iroquois, false);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(iroquois, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(apache, false);
        tile3.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(apache));
        assertFalse(apache.hasContacted(iroquois));

        Unit brave1 = new Unit(game, tile1, apache, braveType, UnitState.FORTIFIED);
        @SuppressWarnings("unused")
        Unit brave2 = new Unit(game, tile3, iroquois, braveType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        igc.move(apache, brave1, tile2);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(iroquois));

        // TODO: do we need this?
        // assertNotNull(iroquois.getTension(apache));
    }

    public void testNativeMeetsColony() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer apache = (ServerPlayer) game.getPlayer("model.nation.apache");
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, false);
        tile1.setExploredBy(dutch, true);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(dutch, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(apache, true);
        tile3.setExploredBy(dutch, false);

        assertFalse(dutch.hasContacted(apache));
        assertFalse(apache.hasContacted(dutch));

        Colony colony = getStandardColony(1, 5, 8);
        Unit brave = new Unit(game, tile3, apache, braveType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        igc.move(apache, brave, tile2);

        assertTrue(dutch.hasContacted(apache));
        assertTrue(apache.hasContacted(dutch));
        assertEquals(Stance.PEACE, dutch.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(dutch));
    }

    public void testNativeMeetsIndianSettlement() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(plains);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);

        ServerPlayer apache = (ServerPlayer) game.getPlayer("model.nation.apache");
        ServerPlayer iroquois = (ServerPlayer) game.getPlayer("model.nation.iroquois");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(apache, true);
        tile1.setExploredBy(iroquois, false);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(apache, true);
        tile2.setExploredBy(iroquois, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(apache, false);
        tile3.setExploredBy(iroquois, true);

        assertFalse(iroquois.hasContacted(apache));
        assertFalse(apache.hasContacted(iroquois));

        // build settlement
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement settlement = builder.player(iroquois).settlementTile(tile3).skillToTeach(null).build();
        Unit brave = new Unit(game, tile1, apache, braveType, UnitState.FORTIFIED);
        InGameController igc = (InGameController) server.getController();
        igc.move(apache, brave, tile2);

        assertTrue(iroquois.hasContacted(apache));
        assertTrue(apache.hasContacted(iroquois));
        assertEquals(Stance.PEACE, iroquois.getStance(apache));
        assertEquals(Stance.PEACE, apache.getStance(iroquois));

        // TODO: do we need this?
        // assertNotNull(iroquois.getTension(apache));
        // assertNotNull(settlement.getAlarm(iroquois));
    }

    public void testShipMeetsShip() throws Exception {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getTestMap(ocean);
        server.setMapGenerator(new MockMapGenerator(map));
        Controller c = server.getController();
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        Game game = server.getGame();
        FreeColTestCase.setGame(game);


        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        ServerPlayer french = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile1 = map.getTile(5, 8);
        tile1.setExploredBy(dutch, true);
        tile1.setExploredBy(french, false);
        Tile tile2 = map.getTile(4, 8);
        tile2.setExploredBy(dutch, true);
        tile2.setExploredBy(french, true);
        Tile tile3 = map.getTile(3, 8);
        tile3.setExploredBy(dutch, false);
        tile3.setExploredBy(french, true);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));

        Unit ship1 = new Unit(game, tile1, dutch, galleonType, UnitState.FORTIFIED);
        Unit ship2 = new Unit(game, tile3, french, galleonType, UnitState.ACTIVE);
        InGameController igc = (InGameController) server.getController();
        igc.move(dutch, ship1, tile2);

        assertFalse(french.hasContacted(dutch));
        assertFalse(dutch.hasContacted(french));
        assertEquals(Stance.UNCONTACTED, french.getStance(dutch));
        assertEquals(Stance.UNCONTACTED, dutch.getStance(french));
    }
}
