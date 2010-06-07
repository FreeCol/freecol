/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

package net.sf.freecol.client.control;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.ClientTestHelper;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;

public class MoveTest extends FreeColTestCase {

    public void testSimpleMove() {

        FreeColServer server = null;
        FreeColClient client = null;
        try {
            server = ServerTestHelper.startServer(false, true);
            client = ClientTestHelper.startClient(server);

            Game game = getStandardGame();
            Player dutch = game.getPlayer("model.nation.dutch");
            TileType plains = spec().getTileType("model.tile.plains");
            Map map = getTestMap(plains);
            game.setMap(map);
            client.setGame(game);
            Tile plain1 = map.getTile(5, 8);
            plain1.setExploredBy(dutch, true);
            Tile plain2 = map.getTile(5, 7);
            plain2.setExploredBy(dutch, true);
    
            UnitType pionnerType = spec().getUnitType("model.unit.hardyPioneer");
            Unit hardyPioneer = new Unit(game, plain1, dutch, pionnerType, UnitState.ACTIVE);
    
            client.getPreGameController().startGame();
            assertEquals(plain1.getNeighbourOrNull(Direction.NE), plain2);
            client.getInGameController().move(hardyPioneer, Direction.NE);
            
        } finally {
            if (client!=null)
                ClientTestHelper.stopClient(client);
            if (server!=null)
                ServerTestHelper.stopServer(server);
        }
    }

}