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

package net.sf.freecol.server.ai.mission;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class PrivateerMissionTest extends FreeColTestCase {

    private static final GoodsType musketsType
        = spec().getGoodsType("model.goods.muskets");

    private static final TileType plains
        = spec().getTileType("model.tile.plains");

    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");
    private static final UnitType galleonType
        = spec().getUnitType("model.unit.galleon");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    private void setupPrivateerTestMap(Game game) {
        Map map = game.getMap();

        // Create player and unit
        ServerPlayer french = (ServerPlayer) game.getPlayerByNationId("model.nation.french");
        ServerPlayer dutch = (ServerPlayer) game.getPlayerByNationId("model.nation.dutch");

        Tile colonyTile = map.getTile(9, 9);
        Tile privateerTile = map.getTile(10, 9);
        Tile frenchGalleonTile = map.getTile(11, 9);
        Tile dutchGalleonTile = map.getTile(12, 9);
        assertTrue("Setup error, colony tile should be land",colonyTile.isLand());
        assertFalse("Privateer tile should be ocean",privateerTile.isLand());
        assertFalse("French galleon tile should be ocean",frenchGalleonTile.isLand());
        assertFalse("Dutch galleon tile should be ocean",dutchGalleonTile.isLand());

        // setup colony and units
        getStandardColony(1, colonyTile.getX(), colonyTile.getY());
        new ServerUnit(game, privateerTile, dutch, privateerType);
        new ServerUnit(game, frenchGalleonTile, french, galleonType);
        new ServerUnit(game, dutchGalleonTile, dutch, galleonType);
    }

    private Game setupPrivateerTestGame() {
        Game game = ServerTestHelper.startServerGame(getCoastTestMap(plains));
        setupPrivateerTestMap(game);
        return game;
    }

    /**
     * Tests validity of mission assignment
     */
    public void testIsMissionValid() {
        Game game = setupPrivateerTestGame();
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();

        Tile privateerTile = map.getTile(10, 9);
        Tile dutchGalleonTile = map.getTile(12, 9);

        Unit privateer = privateerTile.getFirstUnit();
        assertNotNull("Setup error, could not get privateer", privateer);
        Unit dutchGalleon = dutchGalleonTile.getFirstUnit();
        assertNotNull("Setup error, could not get galleon", dutchGalleon);

        AIPlayer aiPlayer = aiMain.getAIPlayer(privateer.getOwner());
        AIUnit privateerAI = aiMain.getAIUnit(privateer);
        assertNotNull("Setup error, could not get privateerAI", privateerAI);

        privateerAI.setMission(null);
        assertFalse("Privateer has no mission", privateerAI.hasMission());
        assertEquals("PrivateeringMission valid", null,
            PrivateerMission.invalidReason(privateerAI));
    }
}
