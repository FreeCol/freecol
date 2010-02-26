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
import net.sf.freecol.common.model.Colony;
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
    TileType plains = spec().getTileType("model.tile.plains");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
    UnitType treasureTrainType = spec().getUnitType("model.unit.treasureTrain");
	
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
        InGameController igc = (InGameController) server.getController();
        igc.establishMission(camp,jesuit);
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

    /*
     * Tests worker allocation regarding building tasks
     */
    public void testCashInTreasure() {
        if (server == null) {
            server = ServerTestHelper.startServer(false, true);
        }
        Map map = getCoastTestMap(plains, true);
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
        // we need to update the reference
        map = game.getMap();

        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(10, 4);
        Unit ship = new Unit(game, tile, dutch, galleonType, UnitState.ACTIVE, galleonType.getDefaultEquipment());
        Unit treasure = new Unit(game, tile, dutch, treasureTrainType, UnitState.ACTIVE, treasureTrainType.getDefaultEquipment());
        assertTrue(treasure.canCarryTreasure());
        treasure.setTreasureAmount(100);

        InGameController igc = (InGameController) server.getController();
        assertFalse(treasure.canCashInTreasureTrain()); // from a tile
        treasure.setLocation(ship);
        assertFalse(treasure.canCashInTreasureTrain()); // from a ship
        ship.setLocation(dutch.getEurope());
        assertTrue(treasure.canCashInTreasureTrain()); // from a ship in Europe
        int fee = treasure.getTransportFee();
        assertEquals(0, fee);
        int oldGold = dutch.getGold();
        igc.cashInTreasureTrain(dutch, treasure);
        assertEquals(100, dutch.getGold() - oldGold);

        // Succeed from a port with a connection to Europe
        Colony port = getStandardColony(1, 9, 4);
        assertFalse(port.isLandLocked());
        assertTrue(port.isConnected());
        treasure.setLocation(port);
        assertTrue(treasure.canCashInTreasureTrain());

        // Fail from a landlocked colony
        Colony inland = getStandardColony(1, 7, 7);
        assertTrue(inland.isLandLocked());
        assertFalse(inland.isConnected());
        treasure.setLocation(inland);
        assertFalse(treasure.canCashInTreasureTrain());

        // Fail from a colony with a port but no connection to Europe
        map.getTile(5, 5).setType(FreeCol.getSpecification().getTileType("model.tile.lake"));
        Colony lake = getStandardColony(1, 4, 5);
        assertFalse(lake.isLandLocked());
        assertFalse(lake.isConnected());
        treasure.setLocation(lake);
        assertFalse(treasure.canCashInTreasureTrain());
    }

}
