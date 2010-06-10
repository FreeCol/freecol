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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockModelController;
import net.sf.freecol.util.test.MockPseudoRandom;

public class UnitTest extends FreeColTestCase {

    TileType plains = spec().getTileType("model.tile.plains");
    TileType desert = spec().getTileType("model.tile.desert");
    TileType grassland = spec().getTileType("model.tile.grassland");
    TileType prairie = spec().getTileType("model.tile.prairie");
    TileType tundra = spec().getTileType("model.tile.tundra");
    TileType savannah = spec().getTileType("model.tile.savannah");
    TileType marsh = spec().getTileType("model.tile.marsh");
    TileType swamp = spec().getTileType("model.tile.swamp");
    TileType arctic = spec().getTileType("model.tile.arctic");
    TileType ocean = spec().getTileType("model.tile.ocean");    
    
    TileType plainsForest = spec().getTileType("model.tile.mixedForest");
    TileType desertForest = spec().getTileType("model.tile.scrubForest");
    TileType grasslandForest = spec().getTileType("model.tile.coniferForest");
    TileType prairieForest = spec().getTileType("model.tile.broadleafForest");
    TileType tundraForest = spec().getTileType("model.tile.borealForest");
    TileType savannahForest = spec().getTileType("model.tile.tropicalForest");
    TileType marshForest = spec().getTileType("model.tile.wetlandForest");
    TileType swampForest = spec().getTileType("model.tile.rainForest");
    TileType hills = spec().getTileType("model.tile.hills");
    TileType mountains = spec().getTileType("model.tile.mountains");

    TileImprovementType road = spec().getTileImprovementType("model.improvement.road");
    TileImprovementType plow = spec().getTileImprovementType("model.improvement.plow");
    TileImprovementType clear = spec().getTileImprovementType("model.improvement.clearForest");

    EquipmentType toolsType = spec().getEquipmentType("model.equipment.tools");
    EquipmentType horsesType = spec().getEquipmentType("model.equipment.horses");
    EquipmentType musketsType = spec().getEquipmentType("model.equipment.muskets");

    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType hardyPioneerType = spec().getUnitType("model.unit.hardyPioneer");
    UnitType expertFarmerType = spec().getUnitType("model.unit.expertFarmer");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType caravelType = spec().getUnitType("model.unit.caravel");
    UnitType wagonType = spec().getUnitType("model.unit.wagonTrain");
    UnitType soldierType = spec().getUnitType("model.unit.veteranSoldier");
    UnitType artilleryType = spec().getUnitType("model.unit.artillery");
    
    GoodsType foodType = spec().getGoodsType("model.goods.food");
    GoodsType cottonType = spec().getGoodsType("model.goods.cotton");

    BuildingType carpenterHouseType = spec().getBuildingType("model.building.carpenterHouse");
    
    /**
     * Test Plowing with a hardy pioneer
     * 
     */
    public void testDoAssignedWorkHardyPioneerPlowPlain() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, spec().getUnitType("model.unit.hardyPioneer"), 
                                     UnitState.ACTIVE);

        // Before
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(false, plain.hasImprovement(plow));

        plain.setOwner(dutch);
        TileImprovement plowImprovement = new TileImprovement(game, plain, plow);
        plain.add(plowImprovement);
        hardyPioneer.work(plowImprovement);

        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(Unit.UnitState.IMPROVING, hardyPioneer.getState());
        assertEquals(1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(false, plain.hasImprovement(plow));

        // Advance 1 turn
        dutch.newTurn();

        // Pioneer finished work but can only move on next turn
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(true, plain.hasImprovement(plow));

        // Advance last turn
        dutch.newTurn();

        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(true, plain.hasImprovement(plow));
    }

    public void testColonyProfitFromEnhancement() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains);
        game.setMap(map);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);
        Tile plain58 = map.getTile(5, 8);

        //assertEquals(2, dutch.getDifficulty().getIndex());
        //assertEquals("model.difficulty.medium", dutch.getDifficulty().getId());
        assertEquals(6, spec().getIntegerOption("model.option.badGovernmentLimit").getValue());
        assertEquals(10, spec().getIntegerOption("model.option.veryBadGovernmentLimit").getValue());

        // Found colony on 6,8
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, soldierType, UnitState.ACTIVE);

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        GoodsType foodType = spec().getGoodsType("model.goods.food");

        soldier.setWorkType(foodType);
        nonServerBuildColony(soldier, colony);

        soldier.setLocation(colony.getColonyTile(plain58));

        Unit hardyPioneer = new Unit(game, plain58, dutch, spec().getUnitType("model.unit.hardyPioneer"), 
                                     UnitState.ACTIVE);

        // Before
        assertEquals(0, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());
        assertEquals(false, plain58.hasImprovement(plow));
        assertEquals(0, colony.getProductionBonus());
        assertEquals("" + soldier.getLocation(), colony.getColonyTile(map.getTile(5, 8)), soldier.getLocation());

        // One turn to check production
        dutch.newTurn();

        assertEquals(false, plain58.hasImprovement(plow));
        assertEquals(8, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(0, colony.getProductionBonus());
        assertEquals(5 + 5, colony.getFoodProduction());

        // Start Plowing
        TileImprovement plowImprovement = new TileImprovement(game, plain58, plow);
        plain58.add(plowImprovement);
        hardyPioneer.work(plowImprovement);
 
        dutch.newTurn();

        assertEquals(true, plain58.hasImprovement(plow));
        // Production for next turn is updated
        assertEquals(5 + 6, colony.getFoodProduction());
        // But in only 10 - 2 == 8 are added from last turn
        assertEquals(8 + 8, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());

        // Advance last turn
        dutch.newTurn();

        assertEquals(16 + 9, colony.getGoodsCount(foodType));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 6, colony.getFoodProduction());
        assertEquals(true, plain58.hasImprovement(plow));
    }

    /**
     * Test Building a road with a hardy pioneer.
     * 
     * The road is available directly, but the pioneer can only move on the next
     * turn.
     * 
     */
    public void testDoAssignedWorkHardyPioneerBuildRoad() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(savannahForest);
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer1 = new Unit(game, plain, dutch, hardyPioneerType, UnitState.ACTIVE);
        Unit hardyPioneer2 = new Unit(game, plain, dutch, hardyPioneerType, UnitState.ACTIVE);
        Unit hardyPioneer3 = new Unit(game, plain, dutch, hardyPioneerType, UnitState.ACTIVE);

        // Before
        assertEquals(false, plain.hasRoad());

        assertEquals(3, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(100, hardyPioneer1.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer1.getState());

        assertEquals(3, hardyPioneer2.getMovesLeft());
        assertEquals(-1, hardyPioneer2.getWorkLeft());
        assertEquals(100, hardyPioneer2.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer2.getState());

        // Now do it
        plain.setOwner(dutch);
        TileImprovement roadImprovement = new TileImprovement(game, plain, road);
        TileImprovement clearImprovement = new TileImprovement(game, plain, clear);
        plain.add(roadImprovement);
        plain.add(clearImprovement);
        hardyPioneer1.work(roadImprovement);
        hardyPioneer2.work(roadImprovement);
        hardyPioneer3.work(clearImprovement);
        assertEquals(2, hardyPioneer1.getWorkLeft());
        assertEquals(1, hardyPioneer2.getWorkLeft());
        assertEquals(3, hardyPioneer3.getWorkLeft());

        dutch.newTurn();

        // After: both pioneers building road have used up their tools
        assertTrue(plain.hasRoad());
        assertTrue(roadImprovement.isComplete());
        assertFalse(clearImprovement.isComplete());

        assertEquals(0, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(80, hardyPioneer1.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer1.getState());

        // should be zero, but that doesn't work yet
        assertEquals(3, hardyPioneer2.getMovesLeft());
        assertEquals(-1, hardyPioneer2.getWorkLeft());
        assertEquals(80, hardyPioneer2.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer2.getState());

        // Pioneer clearing forest is not affected
        assertEquals(3, hardyPioneer3.getMovesLeft());
        assertEquals(2, hardyPioneer3.getWorkLeft());
        assertEquals(100, hardyPioneer3.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.IMPROVING, hardyPioneer3.getState());

        // Advance 1 turn
        dutch.newTurn();

        assertEquals(3, hardyPioneer1.getMovesLeft());
        assertEquals(-1, hardyPioneer1.getWorkLeft());
        assertEquals(80, hardyPioneer1.getEquipmentCount(toolsType) * 20);
        assertEquals(UnitState.ACTIVE, hardyPioneer1.getState());
    }

    public static int getWorkLeftForPioneerWork(UnitType unitType, TileType tileType, TileImprovementType whichWork) {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile tile = new Tile(game, tileType, 0, 0);

        EquipmentType tools = spec().getEquipmentType("model.equipment.tools");
        Unit unit = new Unit(game, tile, dutch, unitType, UnitState.ACTIVE, tools, tools, tools, tools, tools);

        tile.setOwner(dutch);
        TileImprovement improvement = tile.findTileImprovementType(whichWork);
        if (improvement == null) {
            improvement = new TileImprovement(game, tile, whichWork);
            tile.add(improvement);
        }
        unit.work(improvement);

        return unit.getWorkLeft();
    }

    /**
     * Check for basic time requirements...
     * 
     */
    public void testDoAssignedWorkAmateurAndHardyPioneer() {
    	
        { // Savanna
            assertEquals(7, getWorkLeftForPioneerWork(colonistType, savannahForest, clear));
            assertEquals(5, getWorkLeftForPioneerWork(colonistType, savannahForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(colonistType, savannah, plow));
            assertEquals(2, getWorkLeftForPioneerWork(colonistType, savannah, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneerType, savannahForest, clear));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, savannahForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, savannah, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneerType, savannah, road));
        }

        { // Tundra
            assertEquals(5, getWorkLeftForPioneerWork(colonistType, tundraForest, clear));
            assertEquals(3, getWorkLeftForPioneerWork(colonistType, tundraForest, road));
            assertEquals(5, getWorkLeftForPioneerWork(colonistType, tundra, plow));
            assertEquals(3, getWorkLeftForPioneerWork(colonistType, tundra, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, tundraForest, clear));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, tundraForest, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, tundra, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, tundra, road));
        }

        { // Plains
            assertEquals(5, getWorkLeftForPioneerWork(colonistType, plainsForest, clear));
            assertEquals(3, getWorkLeftForPioneerWork(colonistType, plainsForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(colonistType, plains, plow));
            assertEquals(2, getWorkLeftForPioneerWork(colonistType, plains, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, plainsForest, clear));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, plainsForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, plains, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneerType, plains, road));
        }

        { // Hill
            assertEquals(3, getWorkLeftForPioneerWork(colonistType, hills, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, hills, road));
        }

        { // Mountain
            assertEquals(6, getWorkLeftForPioneerWork(colonistType, mountains, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, mountains, road));
        }

        { // Marsh
            assertEquals(7, getWorkLeftForPioneerWork(colonistType, marshForest, clear));
            assertEquals(5, getWorkLeftForPioneerWork(colonistType, marshForest, road));
            assertEquals(6, getWorkLeftForPioneerWork(colonistType, marsh, plow));
            assertEquals(4, getWorkLeftForPioneerWork(colonistType, marsh, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneerType, marshForest, clear));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, marshForest, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, marsh, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, marsh, road));
        }

        { // Desert
            assertEquals(5, getWorkLeftForPioneerWork(colonistType, desertForest, clear));
            assertEquals(3, getWorkLeftForPioneerWork(colonistType, desertForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(colonistType, desert, plow));
            assertEquals(2, getWorkLeftForPioneerWork(colonistType, desert, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, desertForest, clear));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, desertForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneerType, desert, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneerType, desert, road));
        }

        { // Swamp
            assertEquals(8, getWorkLeftForPioneerWork(colonistType, swampForest, clear));
            assertEquals(6, getWorkLeftForPioneerWork(colonistType, swampForest, road));
            assertEquals(8, getWorkLeftForPioneerWork(colonistType, swamp, plow));
            assertEquals(6, getWorkLeftForPioneerWork(colonistType, swamp, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneerType, swampForest, clear));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, swampForest, road));
            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneerType, swamp, plow));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneerType, swamp, road));
        }
    }
    
    /**
     * Test unit for colonist status
     * 
     */
    public void testIsColonist() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Player sioux = game.getPlayer("model.nation.sioux");
        Map map = getTestMap(plains, true);
        game.setMap(map);
        
        Tile tile1 = map.getTile(6, 8);
        Tile tile2 = map.getTile(6, 9);
      
        Unit merchantman = new Unit(game, tile1, dutch, spec().getUnitType("model.unit.merchantman"),
                                    UnitState.ACTIVE);
        
        assertFalse("Merchantman isnt a colonist",merchantman.isColonist());
        
        Unit soldier = new Unit(game, tile1, dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                UnitState.ACTIVE);
        
        assertTrue("A soldier is a colonist",soldier.isColonist());
        
        UnitType braveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        Unit brave = new Unit(game, tile2, sioux, braveType, UnitState.ACTIVE);
        assertFalse("A brave is not a colonist", brave.isColonist());
    }

    public void testCanAdd() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        Unit galleon = new Unit(game, null, dutch, spec().getUnitType("model.unit.galleon"),
                                UnitState.ACTIVE);
        Unit caravel = new Unit(game, null, dutch, spec().getUnitType("model.unit.caravel"),
                                UnitState.ACTIVE);
        Unit colonist = new Unit(game, null, dutch, colonistType,
                                 UnitState.ACTIVE);
        Unit wagonTrain = new Unit(game, null, dutch, spec().getUnitType("model.unit.wagonTrain"),
                                   UnitState.ACTIVE);
        Unit treasureTrain = new Unit(game, null, dutch, spec().getUnitType("model.unit.treasureTrain"),
                                      UnitState.ACTIVE);

        // tests according to standard rules
        assertTrue(galleon.canAdd(colonist));
        assertTrue(galleon.canAdd(treasureTrain));

        assertFalse(galleon.canAdd(wagonTrain));
        assertFalse(galleon.canAdd(caravel));
        assertFalse(galleon.canAdd(galleon));

        assertTrue(caravel.canAdd(colonist));

        assertFalse(caravel.canAdd(wagonTrain));
        assertFalse(caravel.canAdd(treasureTrain));
        assertFalse(caravel.canAdd(caravel));
        assertFalse(caravel.canAdd(galleon));

        // Save old specification values to restore after test
        int wagonTrainOldSpace = wagonTrain.getType().getSpace();
        int wagonTrainOldSpaceTaken = wagonTrain.getType().getSpace();
        int caravelOldSpaceTaken = caravel.getType().getSpace();
        
        // tests according to other possible rules
        wagonTrain.getType().setSpace(1);
        wagonTrain.getType().setSpaceTaken(2);
        caravel.getType().setSpaceTaken(1);

        assertTrue(galleon.canAdd(wagonTrain));
        assertTrue(caravel.canAdd(wagonTrain));
        // this may seem strange, but ships do carry smaller boats
        assertTrue(galleon.canAdd(caravel));
        assertFalse(caravel.canAdd(caravel));

        // restore values to not affect other tests
        wagonTrain.getType().setSpace(wagonTrainOldSpace);
        wagonTrain.getType().setSpaceTaken(wagonTrainOldSpaceTaken);
        caravel.getType().setSpaceTaken(caravelOldSpaceTaken);
    }
    
    public void testFailedAddGoods(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Colony colony = this.getStandardColony();
        int foodInColony = 300;
        colony.addGoods(foodType, foodInColony);
        assertEquals("Setup error, colony does not have expected goods quantities",foodInColony,colony.getGoodsCount(foodType));
        
        Player dutch = game.getPlayer("model.nation.dutch");
        Unit wagonTrain = new Unit(game, colony.getTile(), dutch, spec().getUnitType("model.unit.wagonTrain"),
                UnitState.ACTIVE);
        int initialMoves = wagonTrain.getInitialMovesLeft();
        assertEquals("Setup error, unit has wrong initial moves", initialMoves, wagonTrain.getMovesLeft());
        assertTrue("Setup error, unit should not carry anything", wagonTrain.getGoodsCount() == 0);
        
        Goods tooManyGoods = colony.goodsContainer.getGoods(foodType);
        try{
        	wagonTrain.add(tooManyGoods);
        	fail("Should have thrown an IllegalStateException");
        }
        catch(IllegalStateException e){
        	assertTrue("Unit should not carry anything", wagonTrain.getGoodsCount() == 0);
        	assertEquals("Unit moves should not have been modified", initialMoves, wagonTrain.getMovesLeft());
        }
    }
        
    public void testMissionary() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player sioux = game.getPlayer("model.nation.sioux");
        Player dutch = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);
        UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
        
        Colony colony = getStandardColony(3);
        BuildingType churchType = FreeCol.getSpecification().getBuildingType("model.building.chapel");
        Building church = colony.getBuilding(churchType);
        church.upgrade();
        Unit jesuit = new Unit(game, tile, dutch, missionaryType, UnitState.ACTIVE);
        Unit colonist = new Unit(game, colony, dutch, colonistType, UnitState.ACTIVE);
        // check abilities
        assertFalse(colonist.hasAbility("model.ability.missionary"));
        colonist.equipWith(spec().getEquipmentType("model.equipment.missionary"));
        assertTrue(colonist.hasAbility("model.ability.missionary"));
        assertFalse(colonist.hasAbility("model.ability.expertMissionary"));
        assertTrue(jesuit.hasAbility("model.ability.missionary"));
        assertTrue(jesuit.hasAbility("model.ability.expertMissionary"));
        // check mission creation
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement s = builder.player(sioux).settlementTile(tile).capital(true).isVisitedByPlayer(dutch, true).build();
       
        // add the missionary
        s.setMissionary(jesuit);
        assertTrue("No missionary set",s.getMissionary() != null);
        assertEquals("Wrong missionary set", s.getMissionary(), jesuit);
        s.setMissionary(null);
        assertTrue("Missionary not removed",s.getMissionary() == null);
    }
    
    public void testLineOfSight() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player player = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);
        
        UnitType frigateType = spec().getUnitType("model.unit.frigate");
        Unit frigate = new Unit(game, tile, player, frigateType, UnitState.ACTIVE);
        assertEquals(2, frigate.getLineOfSight());
        assertTrue(frigate.hasAbility("model.ability.navalUnit"));
        
        UnitType revengerType = spec().getUnitType("model.unit.revenger");
        Unit revenger = new Unit(game, tile, player, revengerType, UnitState.ACTIVE);
        assertEquals(3, revenger.getLineOfSight());
        
        Unit colonist = new Unit(game, tile, player, colonistType, UnitState.ACTIVE);
        assertEquals(1, colonist.getLineOfSight());
        assertTrue(colonist.hasAbility("model.ability.canBeEquipped"));
        
        EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
        assertTrue(colonist.canBeEquippedWith(horses));
        colonist.equipWith(horses, true);
        assertEquals(2, colonist.getLineOfSight());
        
        // with Hernando De Soto, land units should see further 
        FoundingFather father = spec().getFoundingFather("model.foundingFather.hernandoDeSoto");
        player.addFather(father);

        assertEquals(2, frigate.getLineOfSight());  // should not increase
        assertEquals(4, revenger.getLineOfSight()); // should get +1 bonus
        assertEquals(3, colonist.getLineOfSight()); // should get +1 bonus
    }
    
    public void testDisposingUnits() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player player = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);
        
        UnitType frigateType = spec().getUnitType("model.unit.frigate");
        Unit frigate = new Unit(game, tile, player, frigateType, UnitState.ACTIVE);
        Unit colonist = new Unit(game, frigate, player, colonistType, UnitState.ACTIVE);
        
        tile.disposeAllUnits();
        assertTrue(frigate.isDisposed());
        assertTrue(colonist.isDisposed());
        assertEquals(0, frigate.getUnitCount());
        assertEquals(0, tile.getUnitCount());
    }
    
    public void testUnitCanBuildColony() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");
        Player sioux = game.getPlayer("model.nation.sioux");

        TileType plains = FreeCol.getSpecification().getTileType("model.tile.plains");
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Tile tile1 = map.getTile(10, 4);
        
        UnitType farmerType = FreeCol.getSpecification().getUnitType("model.unit.expertFarmer");
        Unit farmer = new Unit(game, tile1, dutch, farmerType, UnitState.ACTIVE, farmerType.getDefaultEquipment());
        assertTrue(farmer.canBuildColony());
        
        UnitType artyType = FreeCol.getSpecification().getUnitType("model.unit.artillery");
        Unit arty = new Unit(game, tile1, dutch, artyType, UnitState.ACTIVE, artyType.getDefaultEquipment());
        assertFalse(arty.canBuildColony());
        
        UnitType shipType = FreeCol.getSpecification().getUnitType("model.unit.galleon");
        Unit ship = new Unit(game, tile1, dutch, shipType, UnitState.ACTIVE, shipType.getDefaultEquipment());
        assertFalse(ship.canBuildColony());
        
        UnitType treasureType = FreeCol.getSpecification().getUnitType("model.unit.treasureTrain");
        Unit treasure = new Unit(game, tile1, dutch, treasureType, UnitState.ACTIVE, treasureType.getDefaultEquipment());
        assertFalse(treasure.canBuildColony());
        
        UnitType wagonType = FreeCol.getSpecification().getUnitType("model.unit.wagonTrain");
        Unit wagon = new Unit(game, tile1, dutch, wagonType, UnitState.ACTIVE, wagonType.getDefaultEquipment());
        assertFalse(wagon.canBuildColony());
        
        UnitType indianConvertType = FreeCol.getSpecification().getUnitType("model.unit.indianConvert");
        Unit indianConvert = new Unit(game, tile1, dutch, indianConvertType, UnitState.ACTIVE, indianConvertType.getDefaultEquipment());
        assertFalse(indianConvert.canBuildColony());
        
        UnitType braveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        @SuppressWarnings("unused")
        Unit brave = new Unit(game, tile1, sioux, braveType, UnitState.ACTIVE, braveType.getDefaultEquipment());
        //assertFalse(brave.canBuildColony());
    }

    public void testIndianDies() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
    	 
        Player indianPlayer = game.getPlayer("model.nation.sioux");

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
         
        UnitType indianBraveType = FreeCol.getSpecification().getUnitType("model.unit.brave");
        Unit brave = new Unit(game, camp, indianPlayer, indianBraveType, UnitState.ACTIVE,
                              indianBraveType.getDefaultEquipment());
        camp.addOwnedUnit(brave);
         
        assertEquals("Brave wasnt added to camp",2, camp.getUnitCount());
        assertFalse("Brave wasnt added to player unit list",indianPlayer.getUnit(brave.getId()) == null);
         
        // unit dies
        brave.dispose();
        
        assertTrue("Brave wasnt disposed properly",brave.isDisposed());
        assertEquals("Brave wasnt removed from camp",1, camp.getUnitCount());
        assertTrue("Brave wasnt removed from player unit list",indianPlayer.getUnit(brave.getId()) == null);
    }
    
    public void testEquipIndian() {
        GoodsType toolsType = FreeCol.getSpecification().getGoodsType("model.goods.tools");
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType musketsType = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        EquipmentType toolsEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.tools");
        EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.horses");
        EquipmentType musketsEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.muskets");
        EquipmentType horsesWrongEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.horses");
        EquipmentType musketsWrongEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.muskets");
        
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        
        int horsesReqPerUnit = horsesEqType.getAmountRequiredOf(horsesType);
        int musketsReqPerUnit = musketsEqType.getAmountRequiredOf(musketsType);        
        int toolsReqPerUnit = toolsEqType.getAmountRequiredOf(toolsType);
        
        // Setup
        camp.addGoods(horsesType,horsesReqPerUnit);
        camp.addGoods(musketsType,musketsReqPerUnit);
        camp.addGoods(toolsType,toolsReqPerUnit);
     
        assertEquals("Initial number of horses in Indian camp not as expected",horsesReqPerUnit,camp.getGoodsCount(horsesType));
        assertEquals("Initial number of muskets in Indian camp not as expected",musketsReqPerUnit,camp.getGoodsCount(musketsType));
        assertEquals("Initial number of tools in Indian camp not as expected",toolsReqPerUnit,camp.getGoodsCount(toolsType));

        Unit brave = camp.getUnitList().get(0);
        
        assertFalse("Brave should not be equiped with tools",brave.canBeEquippedWith(toolsEqType));
        
        assertTrue("Brave should not be mounted",!brave.isMounted());
        assertTrue("Brave should not be armed",!brave.isArmed());
        assertTrue("Indian should be able to be armed",brave.canBeEquippedWith(musketsEqType));
        assertFalse("Indian should not be able to equip with " + musketsWrongEqType,brave.canBeEquippedWith(musketsWrongEqType));
        assertTrue("Indian should be able to be mounted",brave.canBeEquippedWith(horsesEqType));
        assertFalse("Indian should not be able to equip with " + horsesWrongEqType,brave.canBeEquippedWith(horsesWrongEqType));
        
        // Execute
        brave.equipWith(musketsEqType);
        brave.equipWith(horsesEqType);
        
        // Verify results
        assertTrue("Brave should be mounted",brave.isMounted());
        assertTrue("Brave should be armed",brave.isArmed());
        assertEquals("No muskets should remain in camp",0,camp.getGoodsCount(musketsType));
        assertEquals("No horses should remain in camp",0,camp.getGoodsCount(horsesType));
    }

    public void testEquipIndianNotEnoughReqGoods() {
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType musketsType = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
        EquipmentType horsesEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.horses");
        EquipmentType musketsEqType = FreeCol.getSpecification().getEquipmentType("model.equipment.indian.muskets");
        
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();
        
        int horsesAvail = horsesEqType.getAmountRequiredOf(horsesType) / 2;
        int musketsAvail = musketsEqType.getAmountRequiredOf(musketsType) / 2;        
        
        // Setup
        camp.addGoods(horsesType,horsesAvail);
        camp.addGoods(musketsType,musketsAvail);
     
        assertEquals("Initial number of horses in Indian camp not as expected",horsesAvail,camp.getGoodsCount(horsesType));
        assertEquals("Initial number of muskets in Indian camp not as expected",musketsAvail,camp.getGoodsCount(musketsType));

        Unit brave = camp.getUnitList().get(0);
        assertTrue("Inicial brave should not be mounted",!brave.isMounted());
        assertTrue("Inicial brave should not be armed",!brave.isArmed());
        
        // Execute and verify
        try{
        	brave.equipWith(musketsEqType);
        	fail("Exception not thrown when trying to arm unit without enough required goods");
        } catch(IllegalStateException e){}
        assertTrue("Final brave should not be armed",!brave.isArmed());
        assertEquals("The muskets should not have been touched",musketsAvail,camp.getGoodsCount(musketsType));
        
        try{
        	brave.equipWith(horsesEqType);
        	fail("Exception not thrown when trying to mount unit without enough required goods");
        } catch(IllegalStateException e){}
        assertTrue("Final brave should not be mounted",!brave.isMounted());
        assertEquals("The horses should not have been touched",horsesAvail,camp.getGoodsCount(horsesType));
    }
    
    
    public void testUnitAvailability() {
        Game game = getStandardGame();
        
        Player indian = game.getPlayer("model.nation.sioux");
        Player european = game.getPlayer("model.nation.dutch");
        Player king = game.getPlayer("model.nation.dutchREF");
        UnitType regular = FreeCol.getSpecification().getUnitType("model.unit.kingsRegular");
        assertTrue(regular.isAvailableTo(king));
        assertFalse(regular.isAvailableTo(indian));
        assertFalse(regular.isAvailableTo(european));
        UnitType colonial = FreeCol.getSpecification().getUnitType("model.unit.colonialRegular");
        assertFalse(colonial.isAvailableTo(king));
        assertFalse(colonial.isAvailableTo(indian));
        assertFalse(colonial.isAvailableTo(european));
        UnitType brave = FreeCol.getSpecification().getUnitType("model.unit.brave");
        assertFalse(brave.isAvailableTo(king));
        assertTrue(brave.isAvailableTo(indian));
        assertFalse(brave.isAvailableTo(european));
        UnitType undead = FreeCol.getSpecification().getUnitType("model.unit.undead");
        assertFalse(undead.isAvailableTo(king));
        assertFalse(undead.isAvailableTo(indian));
        assertFalse(undead.isAvailableTo(european));

        european.getFeatureContainer().addAbility(new Ability("model.ability.independenceDeclared"));
        assertTrue(colonial.isAvailableTo(european));

    }

    public void testDefaultEquipment() {

        assertEquals(EquipmentType.NO_EQUIPMENT, colonistType.getDefaultEquipment());

        assertEquals(spec().getEquipmentType("model.equipment.tools"), hardyPioneerType.getDefaultEquipmentType());
        assertEquals(5, hardyPioneerType.getDefaultEquipment().length);

        UnitType soldier = spec().getUnitType("model.unit.veteranSoldier");
        assertEquals(spec().getEquipmentType("model.equipment.muskets"), soldier.getDefaultEquipmentType());
        assertEquals(1, soldier.getDefaultEquipment().length);

        UnitType missionary = spec().getUnitType("model.unit.jesuitMissionary");
        assertEquals(spec().getEquipmentType("model.equipment.missionary"), missionary.getDefaultEquipmentType());
        assertEquals(1, missionary.getDefaultEquipment().length);

        UnitType scout = spec().getUnitType("model.unit.seasonedScout");
        assertEquals(spec().getEquipmentType("model.equipment.horses"), scout.getDefaultEquipmentType());
        assertEquals(1, scout.getDefaultEquipment().length);

    }
    
    public void testEquipmentChange(){
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Tile unitTile = map.getTile(6, 8);
        
        Unit unit = new Unit(game, unitTile, dutch, colonistType, UnitState.ACTIVE);
        
        assertFalse("Soldier should not have muskets",unit.getEquipmentCount(musketsType) > 0);
        assertFalse("Soldier should not have horses",unit.getEquipmentCount(horsesType) > 0);
        assertFalse("Soldier should not have tools",unit.getEquipmentCount(toolsType) > 0);
        unit.equipWith(musketsType, 50, true);
        unit.equipWith(horsesType, 50, true);
        assertTrue("Soldier should be equiped with muskets",unit.getEquipmentCount(musketsType) > 0);
        assertTrue("Soldier should be equiped with horses",unit.getEquipmentCount(horsesType) > 0);
        assertFalse("Soldier should not have tools",unit.getEquipmentCount(toolsType) > 0);
        
        unit.equipWith(toolsType, 50, true);
        assertFalse("Soldier should no longer have muskets",unit.getEquipmentCount(musketsType) > 0);
        assertFalse("Soldier should no longer have horses",unit.getEquipmentCount(horsesType) > 0);
        assertTrue("Soldier should be equiped with tools",unit.getEquipmentCount(toolsType) > 0);
    }
    
    public void testUnitLocationAfterBuildingColony() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap();
        game.setMap(map);

        Tile colonyTile = map.getTile(6, 8);
        
        Unit soldier = new Unit(game, colonyTile, dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                UnitState.ACTIVE);
        
        assertTrue("soldier location should be the colony tile",soldier.getLocation() == colonyTile);
        assertTrue("soldier tile should be the colony tile",soldier.getTile() == colonyTile);
        //Boolean found = false;
        boolean found = false;
        for (Unit u : colonyTile.getUnitList()){
            if(u == soldier){
                found = true;
            }
        }
        assertTrue("Unit not found in tile",found);
        
        Colony colony = new Colony(game, dutch, "New Amsterdam", colonyTile);
        nonServerBuildColony(soldier, colony);
        
        assertFalse("soldier should be inside the colony",soldier.getLocation() == colonyTile);
        // There is some inconsistence with the results below
        // Unit.getTile() gives the location tile even though it isnt in the tile itself
        // This may lead to some confusion
        assertTrue("soldier should get the location tile as the colony tile",soldier.getTile() == colonyTile);
        for (Unit u : colonyTile.getUnitList()){
            if(u == soldier){
                fail("Unit building colony still in tile");
            }
        }
                
        found = false;
        for(WorkLocation loc : colony.getWorkLocations()){
            if(loc.getUnitList().contains(soldier)){
                found = true;
            }
        }
        assertTrue("Soldier should be in a work location in the colony",found);
        ColonyTile workTile = soldier.getWorkTile();
        assertTrue("Soldier should be in a work tile in the colony",workTile != null);
        assertFalse("Soldier should not be working in central tile",workTile == colony.getColonyTile(colonyTile));
    }
    
    public void testUnitGetsExperienceThroughWork() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Colony colony = getStandardColony();
        
        Unit colonist = colony.getRandomUnit();
        
        assertEquals("Colonist should not have any experience",0,colonist.getExperience());
        
        // colonist either in building or colony work tile
        WorkLocation loc = colonist.getWorkLocation();
        if(loc == null){
            loc = colonist.getWorkTile();
        }
        // produces goods
        loc.newTurn();
        
        assertTrue("Colonist should have gained some experience",colonist.getExperience() > 0);
    }
    
    public void testUnitLosesExperienceWithWorkChange() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
                
        Player dutch = game.getPlayer("model.nation.dutch");
        Unit colonist = new Unit(game, map.getTile(6, 8), dutch, colonistType, UnitState.ACTIVE);
        
        colonist.setWorkType(foodType);
        colonist.modifyExperience(10);
        assertTrue("Colonist should some initial experience",colonist.getExperience() > 0);
        
        colonist.setWorkType(cottonType);
        assertTrue("Colonist should have lost all experience",colonist.getExperience() == 0);
    }
    
    public void testUnitLosesExperienceWithRoleChange() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
                
        Player dutch = game.getPlayer("model.nation.dutch");
        Unit colonist = new Unit(game, map.getTile(6, 8), dutch, colonistType, UnitState.ACTIVE);
        
        colonist.modifyExperience(10);
        assertTrue("Colonist should some initial experience",colonist.getExperience() > 0);

        colonist.equipWith(musketsType,true);
        assertTrue("Colonist should have lost all experience, different role",colonist.getExperience() == 0);
        
        colonist.modifyExperience(10);
        colonist.equipWith(horsesType,true);
        assertTrue("Colonist should not have lost experience, compatible role",colonist.getExperience() > 0);
    }

    public void testUnitPromotionWorkingInWorkTile(){
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        
        Colony colony = getStandardColony();
        
        assertTrue("Colony should only have 1 colonist for test setup",colony.getUnitCount() == 1);
        
        Unit colonist = colony.getRandomUnit();
        String errMsg = "Error setting test, colonist should not be an expert";
        assertTrue(errMsg, colonist.getType() == colonistType);
        
        ColonyTile workTile = colony.getColonyTile(colony.getTile().getNeighbourOrNull(Direction.N));
        
        // set colonist as farmer
        colonist.setLocation(workTile);
        colonist.setWorkType(foodType);
        assertEquals("Wrong work allocation",foodType,colonist.getWorkType());
        
        // set some experience
        int expectXP = 10;
        colonist.modifyExperience(expectXP);
        assertEquals("Wrong colonist experience",expectXP,colonist.getExperience());
        
        // We need a deterministic random
        List<Integer> setValues = new ArrayList<Integer>();
        setValues.add(1);
        MockPseudoRandom mockRandom = new MockPseudoRandom(setValues,true);
        MockModelController controller = (MockModelController) game.getModelController();
        controller.setPseudoRandom(mockRandom);
        
        // Verify initial state
        boolean isExpert = colonist.getType() == expertFarmerType;
        assertFalse("Unit should not be an expert", isExpert);
        
        // Make upgrade
        colonist.newTurn();

        // verify upgrade
        isExpert = colonist.getType() == expertFarmerType;
        assertTrue("Unit should now be an expert", isExpert);
        
        // necessary to undo the deterministic random
        controller.setPseudoRandom(null);
    }
    
    public void testOwnerChange(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
                
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        
        Unit colonist = new Unit(game, map.getTile(6, 8), dutch, colonistType, UnitState.ACTIVE);
        
        assertTrue("Colonist should be dutch",colonist.getOwner() == dutch);
        assertTrue("Dutch player should have 1 unit",dutch.getUnits().size() == 1);
        assertTrue("French player should have no units",french.getUnits().size() == 0);
        // change owner
        colonist.setOwner(french);
        assertTrue("Colonist should be french",colonist.getOwner() == french);
        assertTrue("Dutch player should have no units",dutch.getUnits().size() == 0);
        assertTrue("French player should have 1 unit",french.getUnits().size() == 1);
    }
    
    public void testCarrierOwnerChange(){
        Game game = getStandardGame();
        Map map = getTestMap(ocean);
        game.setMap(map);
                
        Player dutch = game.getPlayer("model.nation.dutch");
        Player french = game.getPlayer("model.nation.french");
        
        Unit galleon = new Unit(game, map.getTile(6, 8), dutch, galleonType, UnitState.ACTIVE);
        assertTrue("Galleon should be empty",galleon.getUnitCount() == 0);
        assertTrue("Galleon should be able to carry units",galleon.canCarryUnits());
        Unit colonist = new Unit(game, galleon, dutch, colonistType, UnitState.SENTRY);
        assertTrue("Colonist should be aboard the galleon",colonist.getLocation() == galleon);
        assertEquals("Wrong number of units th galleon is carrying",1,galleon.getUnitCount());
        
        assertTrue("Colonist should be dutch",galleon.getOwner() == dutch);
        assertTrue("Colonist should be dutch",colonist.getOwner() == dutch);
        assertTrue("Dutch player should have 2 units",dutch.getUnits().size() == 2);
        assertTrue("French player should have no units",french.getUnits().size() == 0);
        
        // change carrier owner
        galleon.setOwner(french);
        assertTrue("Galleon should be french",galleon.getOwner() == french);
        assertTrue("Colonist should be french",colonist.getOwner() == french);
        assertTrue("Dutch player should have no units",dutch.getUnits().size() == 0);
        assertTrue("French player should have 2 units",french.getUnits().size() == 2);
    }
    
    public void testSwitchEquipmentWith(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Colony colony1 = getStandardColony(1);
        Tile col1Tile = colony1.getTile();
        Tile otherTile = col1Tile.getAdjacentTile(Direction.N);
        
        Unit insideUnit1 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, toolsType);
        Unit insideUnit2 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsType, horsesType);
        Unit insideUnit3 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE);
        Unit artillery = new Unit(game, col1Tile, colony1.getOwner(), artilleryType, UnitState.ACTIVE);
        
        Unit outsideUnit1 = new Unit(game, otherTile, colony1.getOwner(), colonistType, UnitState.ACTIVE, toolsType);
        Unit outsideUnit2 = new Unit(game, otherTile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsType, horsesType);
        
        boolean exceptionThrown = false;
        try{
            insideUnit1.switchEquipmentWith(artillery);
        }
        catch(IllegalArgumentException e){
            exceptionThrown = true;
        }
        if(!exceptionThrown){
            fail("Colonist must not change equipment with a unit not also a colonist");
        }
        
        exceptionThrown = false;
        try{
            outsideUnit1.switchEquipmentWith(outsideUnit2);
        }
        catch(IllegalStateException e){
            exceptionThrown = true;
        }
        if(!exceptionThrown){
            fail("Colonists must not change equipment outside a settlement");
        }
        
        exceptionThrown = false;
        try{
            insideUnit1.switchEquipmentWith(outsideUnit1);
        }
        catch(IllegalStateException e){
            exceptionThrown = true;
        }
        if(!exceptionThrown){
            fail("Colonists must not change equipment when in diferent locations");
        }
        
        insideUnit1.switchEquipmentWith(insideUnit2);
        assertFalse("Unit1 should not have tools",insideUnit1.getEquipmentCount(toolsType) == 1);
        assertTrue("Unit1 should now have horses",insideUnit1.getEquipmentCount(horsesType) == 1);
        assertTrue("Unit1 should now have muskets",insideUnit1.getEquipmentCount(musketsType) == 1);
        
        assertTrue("Unit2 should now have tools",insideUnit2.getEquipmentCount(toolsType) == 1);
        assertFalse("Unit2 should not have horses",insideUnit2.getEquipmentCount(horsesType) == 1);
        assertFalse("Unit2 should not have muskets",insideUnit2.getEquipmentCount(musketsType) == 1);
        
        insideUnit3.switchEquipmentWith(insideUnit1);
        assertTrue("Unit1 should not have equipment",insideUnit1.getEquipment().isEmpty());
        assertTrue("Unit3 should now have horses",insideUnit3.getEquipmentCount(horsesType) == 1);
        assertTrue("Unit3 should now have muskets",insideUnit3.getEquipmentCount(musketsType) == 1);
    }
    
    public void testSwitchEquipmentWithUnitHavingSomeAlredy(){
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Colony colony1 = getStandardColony(1);
        Tile col1Tile = colony1.getTile();
        
        Unit insideUnit1 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsType);
        Unit insideUnit2 = new Unit(game, col1Tile, colony1.getOwner(), colonistType, UnitState.ACTIVE, musketsType, horsesType);


        assertEquals("Unit1 should not have horses",0,insideUnit1.getEquipmentCount(horsesType));
        assertEquals("Unit1 should have muskets",1,insideUnit1.getEquipmentCount(musketsType));
        
        assertEquals("Unit2 should have horses",1,insideUnit2.getEquipmentCount(horsesType));
        assertEquals("Unit2 should have muskets",1,insideUnit2.getEquipmentCount(musketsType));
        insideUnit1.switchEquipmentWith(insideUnit2);
        assertEquals("Unit1 should now have horses",1,insideUnit1.getEquipmentCount(horsesType));
        assertEquals("Unit1 should now have muskets",1,insideUnit1.getEquipmentCount(musketsType));
        
        assertEquals("Unit2 should not have horses",0,insideUnit2.getEquipmentCount(horsesType));
        assertEquals("Unit2 should have muskets",1,insideUnit2.getEquipmentCount(musketsType));
    }

    public void testGetMovesAsString() {

        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        
        Colony colony = getStandardColony(1);
        Unit unit = colony.getUnitList().get(0);
        String initial = "/" + Integer.toString(unit.getInitialMovesLeft() / 3);

        String[] expected = new String[] {
            "0", "(1/3) ", "(2/3) ", "1", "1 (1/3) ", "1 (2/3) ",
            "2", "2 (1/3) ", "2 (2/3) ", "3", "3 (1/3) ", "3 (2/3) "
        };

        for (int index = 0; index < expected.length; index++) {
            unit.setMovesLeft(index);
            String expectedString = expected[index] + initial;
            String actualString = unit.getMovesAsString();
            assertEquals(expectedString + " != " + actualString, expectedString, actualString );
        }
    }

}
