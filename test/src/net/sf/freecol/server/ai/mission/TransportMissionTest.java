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

package net.sf.freecol.server.ai.mission;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.Transportable;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.TransportMission.Destination;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.generator.MapGeneratorOptions;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.util.test.MockMapGenerator;

public class TransportMissionTest extends FreeColTestCase {
    FreeColServer server = null;
    
    public void tearDown(){
        if(server != null){
            // must make sure that the server is stopped
            ServerTestHelper.stopServer(server);
            server = null;   
        }
        setGame(null);
    }
    
    private Game startServerGame(){
    	return startServerGame(null);
    }
    
    private Game startServerGame(Map map){
        // Reset import file option value (may have been set by previous tests)
        ((FileOption) Specification.getSpecification().getOption(MapGeneratorOptions.IMPORT_FILE)).setValue(null);
        
        // start a server
        server = ServerTestHelper.startServer(false, true);
        
        if(map != null){
        	server.setMapGenerator(new MockMapGenerator(map));
        }
        
        Controller c = server.getController();
        assertNotNull(c);
        assertTrue(c instanceof PreGameController);
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail();
        }
        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        
        Game game = server.getGame();
        assertNotNull(game);
        FreeColTestCase.setGame(game);
        return game;
    }
    
    public void testTransportMissionInvalidAfterCombatLost() {   
        Game game = startServerGame();
        
        Map map = game.getMap();
        assertNotNull(map);
        
        AIMain aiMain = server.getAIMain();
        assertNotNull(aiMain);
        
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        AIPlayer aiPlayer = (AIPlayer)aiMain.getAIObject(player1.getId());
    
        // create a ship carrying a colonist
        Tile tile1 = map.getTile(6, 9);
        
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, tile1, player1, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        assertTrue(galleon.hasAbility("model.ability.navalUnit"));
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new Unit(game, galleon, player1, colonistType, UnitState.SENTRY);
        assertTrue(colonist.getLocation()==galleon);
        
        //Create the attacker
        ServerPlayer player2 = (ServerPlayer) game.getPlayer("model.nation.french");
        Tile tile2 = map.getTile(5, 9);
        UnitType privateerType = spec().getUnitType("model.unit.privateer");
        Unit privateer = new Unit(game, tile2, player2, privateerType, UnitState.ACTIVE);
                
        // assign transport mission to the ship
        aiUnit.setMission(new TransportMission(aiMain, aiUnit));
        
        //Simulate the combat
        CombatModel combatModel = game.getCombatModel();
        CombatModel.CombatResult combatResult = new CombatModel.CombatResult(CombatModel.CombatResultType.WIN,galleon.getHitpoints());
        combatModel.attack(privateer, galleon, combatResult , 0, player1.getEurope());

        // Verify that the outcome of the combat is  a return to Europe for repairs
        // and also invalidation of the transport mission as side effect
        assertTrue(galleon.isUnderRepair());
        assertFalse(aiUnit.getMission().isValid());
                
        // this will call AIPlayer.abortInvalidMissions() and change the carrier mission
        aiPlayer.startWorking();
        assertFalse(aiUnit.getMission() instanceof TransportMission);
    }
    
    public void testGetNextStopAlreadyAtDestination(){
    	final TileType plainsType = spec().getTileType("model.tile.plains");
    	final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
    	
    	Map map = getCoastTestMap(plainsType);
    	
    	Game game = startServerGame(map);
    	map = game.getMap();  // update reference
    	
        AIMain aiMain = server.getAIMain();
        assertNotNull(aiMain);
        
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
    
        // create a ship carrying a colonist
        Tile colonyTile = map.getTile(9, 9);
        getStandardColony(1, colonyTile.getX(), colonyTile.getY());
        
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, colonyTile, player1, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        
        // assign transport mission to the ship
        TransportMission mission = new TransportMission(aiMain, aiUnit); 
        aiUnit.setMission(mission);
        Transportable goods = new AIGoods(aiMain,galleon, horsesType,50, colonyTile);
        mission.addToTransportList(goods);

        // Exercise
        Destination dest = mission.getNextStop();
        
        // Test
        assertNotNull("Unit should have a destination",dest);
        assertTrue("Unit should be already at the destination", dest.isAtDestination());
    }
    
    public void testGetNextStopIsEurope(){
    	final TileType plainsType = spec().getTileType("model.tile.plains");
    	final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
    	
    	Map map = getCoastTestMap(plainsType);
    	
    	Game game = startServerGame(map);
    	map = game.getMap();  // update reference
    	
        AIMain aiMain = server.getAIMain();
        assertNotNull(aiMain);
        
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Europe europe = player1.getEurope();
        assertNotNull("Setup error, europe is null", europe);
        
        // create a ship carrying a colonist
        Tile colonyTile = map.getTile(9, 9);
        getStandardColony(1, colonyTile.getX(), colonyTile.getY());
        
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, colonyTile, player1, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        
        // assign transport mission to the ship
        TransportMission mission = new TransportMission(aiMain, aiUnit); 
        aiUnit.setMission(mission);
        Transportable goods = new AIGoods(aiMain, galleon, horsesType,50, europe);
        mission.addToTransportList(goods);

        // Exercise
        Destination dest = mission.getNextStop();
        
        // Test
        assertNotNull("Unit should have a destination",dest);
        assertTrue("Destination should be Europe", dest.moveToEurope());
        assertNotNull("Unit should have a path",dest.getPath());
    }
    
    public void testGetNextStopIsColony(){
    	final TileType plainsType = spec().getTileType("model.tile.plains");
    	final GoodsType horsesType = spec().getGoodsType("model.goods.horses");
    	
    	Map map = getCoastTestMap(plainsType);
    	
    	Game game = startServerGame(map);
    	map = game.getMap();  // update reference
    	
        AIMain aiMain = server.getAIMain();
        assertNotNull(aiMain);
        
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Europe europe = player1.getEurope();
        assertNotNull("Setup error, europe is null", europe);
        
        // create a ship carrying a colonist
        Tile colonyTile = map.getTile(9, 9);
        Tile galleonTile = map.getTile(9, 10);
        getStandardColony(1, colonyTile.getX(), colonyTile.getY());
        
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, galleonTile, player1, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        
        // assign transport mission to the ship
        TransportMission mission = new TransportMission(aiMain, aiUnit); 
        aiUnit.setMission(mission);
        Transportable goods = new AIGoods(aiMain, galleon, horsesType,50, colonyTile);
        mission.addToTransportList(goods);

        // Exercise
        Destination dest = mission.getNextStop();
        
        // Test
        assertNotNull("Unit should have a destination",dest);
        assertFalse("Destination should not be Europe", dest.moveToEurope());
        PathNode destPath = dest.getPath();
        assertNotNull("Unit should have a path", destPath);
        assertEquals("Unit destiny should be the colony", destPath.getLastNode().getTile(),colonyTile);
    }
    
    public void testGetDefaultDestination(){
    	final TileType plainsType = spec().getTileType("model.tile.plains");
    	
    	Map map = getCoastTestMap(plainsType);
    	
    	Game game = startServerGame(map);
    	map = game.getMap();  // update reference
    	
        AIMain aiMain = server.getAIMain();
        assertNotNull(aiMain);
        
        ServerPlayer player1 = (ServerPlayer) game.getPlayer("model.nation.dutch");
        Europe europe = player1.getEurope();
        assertNotNull("Setup error, europe is null", europe);
        
        // create a ship

        Tile galleonTile = map.getTile(9, 10);
        UnitType galleonType = spec().getUnitType("model.unit.galleon");
        Unit galleon = new Unit(game, galleonTile, player1, galleonType, UnitState.ACTIVE);
        AIUnit aiUnit = (AIUnit) aiMain.getAIObject(galleon);
        assertNotNull(aiUnit);
        
        // assign transport mission to the ship
        TransportMission mission = new TransportMission(aiMain, aiUnit); 
        aiUnit.setMission(mission);

        assertTrue("Setup error, player should not have colonies", player1.getColonies().isEmpty());
        
        // Exercise
        Destination dest = mission.getDefaultDestination();
        
        // Test
        assertNotNull("Unit should have a destination",dest);
        assertTrue("Destination should be Europe", dest.moveToEurope());
        
        // add colony
        Tile colonyTile = map.getTile(9, 9);
        FreeColTestUtils.ColonyBuilder builder = FreeColTestUtils.getColonyBuilder();
        builder.colonyTile(colonyTile).initialColonists(1).player(player1).build();
        assertFalse("Player should now have a colony", player1.getColonies().isEmpty());
        
        // Exercise
        dest = mission.getDefaultDestination();
        
        // Test
        assertNotNull("Unit should have a destination",dest);
        assertFalse("Destination should not be Europe", dest.moveToEurope());
        PathNode destPath = dest.getPath();
        assertNotNull("Unit should have a path", destPath);
        assertEquals("Unit destiny should be the colony", destPath.getLastNode().getTile(),colonyTile);
    }
}
