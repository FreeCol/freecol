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

package net.sf.freecol.server.control;

import java.io.File;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Tension.Level;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.generator.IMapGenerator;
import net.sf.freecol.server.generator.MapGeneratorOptions;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;

public class InGameControllerTest extends FreeColTestCase {
    UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
	
    FreeColServer server = null;
    
	public void tearDown() throws Exception {
		if(server != null){
			// must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;
		}
		super.tearDown();
	}

    public void testCreateMissionFailed() {
		// start a server
        server = ServerTestHelper.startServer(false, true);
                
        server.setMapGenerator(new MockMapGenerator(getTestMap()));
        
        PreGameController pgc = (PreGameController) server.getController();
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
        Map map = game.getMap();
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Player indianPlayer = camp.getOwner();
        
        Tile tile = map.getNeighbourOrNull(Map.Direction.N, camp.getTile());
        
        Unit jesuit = new Unit(game, tile, dutchPlayer, missionaryType, UnitState.ACTIVE);
        
        // set players at war
        indianPlayer.changeRelationWithPlayer(dutchPlayer, Stance.WAR);
        Tension tension = new Tension(Level.HATEFUL.getLimit());
        camp.setAlarm(dutchPlayer, tension);
        
        assertEquals("Wrong camp alarm", tension, camp.getAlarm(dutchPlayer));
        InGameController controller = (InGameController) server.getController();
        controller.establishMission(camp,jesuit);
        boolean result = !jesuit.isDisposed();

        assertFalse("Mission creation should have failed",result);
        assertNull("Indian settlement should not have a mission",camp.getMissionary());
    }
    
    public void testDumpGoods() {
    	final TileType ocean = spec().getTileType("model.tile.ocean");
    	final GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        final UnitType privateerType = spec().getUnitType("model.unit.privateer");
    	
		// start a server
        server = ServerTestHelper.startServer(false, true);
                
        server.setMapGenerator(new MockMapGenerator(getTestMap(ocean)));
        
        PreGameController pgc = (PreGameController) server.getController();
        
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game");
        }
        
        Game game = server.getGame();
        FreeColTestCase.setGame(game);
        Map map = game.getMap();
        
        Player dutchPlayer = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(1, 1);
        Unit privateer = new Unit(game, tile, dutchPlayer, privateerType, UnitState.ACTIVE);
        assertEquals("Setup Error, privateer should not carry anything", 0, privateer.getGoodsCount());
        Goods cotton = new Goods(game,privateer,cottonType,100);
        privateer.add(cotton);
        assertEquals("Setup Error, privateer should carry cotton", 1, privateer.getGoodsCount());
        assertTrue("Setup Error, cotton should be aboard privateer",cotton.getLocation() == privateer);
        
        InGameController controller = (InGameController) server.getController();
        // moving a good to a null location means dumping the good
        controller.moveGoods(cotton, null);
        
        assertEquals("Privateer should no longer carry cotton", 0, privateer.getGoodsCount());
        assertNull("Cotton should have no location",cotton.getLocation());
    }
}
