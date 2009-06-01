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

package net.sf.freecol.server.model;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class ServerPlayerTest extends FreeColTestCase {	
    TileType plains = spec().getTileType("model.tile.plains");
    
    UnitType treasureType = spec().getUnitType("model.unit.treasureTrain");
    
	FreeColServer server = null;
	
	public void tearDown() throws Exception {
		if(server != null){
			// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            FreeColTestCase.setGame(null);
            server = null;
		}
		super.tearDown();
	}
	
	/*
	 * Tests worker allocation regarding building tasks
	 */
    public void testCashInTreasure() {
		// start a server
        server = ServerTestHelper.startServer(false, true);
        
        Map map = getTestMap(plains, true);
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
        
        Tile tile = map.getTile(10, 4);
        
        UnitType shipType = FreeCol.getSpecification().getUnitType("model.unit.galleon");
        Unit ship = new Unit(game, tile, dutch, shipType, UnitState.ACTIVE, shipType.getDefaultEquipment());
        
        Unit treasure = new Unit(game, tile, dutch, treasureType, UnitState.ACTIVE, treasureType.getDefaultEquipment());
        assertTrue(treasure.canCarryTreasure());
        treasure.setTreasureAmount(100);
        
        assertFalse(treasure.canCashInTreasureTrain()); // from a tile
        treasure.setLocation(ship);
        assertFalse(treasure.canCashInTreasureTrain()); // from a ship
        ship.setLocation(dutch.getEurope());    
        assertTrue(treasure.canCashInTreasureTrain()); // from a ship in Europe
        int fee = treasure.getTransportFee();
        assertEquals(0, fee);
        dutch.cashInTreasureTrain(treasure);
        assertEquals(100, dutch.getGold());
	}
}
