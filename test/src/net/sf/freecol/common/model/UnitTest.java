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
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.util.test.FreeColTestCase;

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

    TileImprovementType road = spec().getTileImprovementType("model.improvement.Road");
    TileImprovementType plow = spec().getTileImprovementType("model.improvement.Plow");
    TileImprovementType clear = spec().getTileImprovementType("model.improvement.ClearForest");

    EquipmentType toolsType = spec().getEquipmentType("model.equipment.tools");
    EquipmentType horsesType = spec().getEquipmentType("model.equipment.horses");
    EquipmentType musketsType = spec().getEquipmentType("model.equipment.muskets");

    UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
    UnitType galleonType = spec().getUnitType("model.unit.galleon");
    UnitType caravelType = spec().getUnitType("model.unit.caravel");
    UnitType wagonType = spec().getUnitType("model.unit.wagonTrain");
    
    GoodsType foodType = spec().getGoodsType("model.goods.food");
    GoodsType cottonType = spec().getGoodsType("model.goods.cotton");

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

        TileImprovement plowImprovement = new TileImprovement(game, plain, plow);
        plain.add(plowImprovement);
        hardyPioneer.work(plowImprovement);

        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(Unit.UnitState.IMPROVING, hardyPioneer.getState());
        assertEquals(1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(false, plain.hasImprovement(plow));

        // Advance 1 turn
        game.newTurn();

        // Pioneer finished work but can only move on next turn
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(true, plain.hasImprovement(plow));

        // Advance last turn
        game.newTurn();

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

        assertEquals(2, dutch.getDifficulty().getIndex());
        assertEquals("model.difficulty.medium", dutch.getDifficulty().getId());
        assertEquals(6, Specification.getSpecification().getIntegerOption("model.option.badGovernmentLimit").getValue());
        assertEquals(10, Specification.getSpecification().getIntegerOption("model.option.veryBadGovernmentLimit").getValue());

        // Found colony on 6,8
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                UnitState.ACTIVE);

        Colony colony = new Colony(game, dutch, "New Amsterdam", soldier.getTile());
        soldier.setWorkType(Goods.FOOD);
        soldier.buildColony(colony);

        soldier.setLocation(colony.getColonyTile(plain58));

        Unit hardyPioneer = new Unit(game, plain58, dutch, spec().getUnitType("model.unit.hardyPioneer"), 
                                     UnitState.ACTIVE);

        // Before
        assertEquals(0, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(5 + 5, colony.getFoodProduction());
        assertEquals(false, plain58.hasImprovement(plow));
        assertEquals(0, colony.getProductionBonus());
        assertEquals("" + soldier.getLocation(), colony.getColonyTile(map.getTile(5, 8)), soldier.getLocation());

        // One turn to check production
        game.newTurn();

        assertEquals(false, plain58.hasImprovement(plow));
        assertEquals(8, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());
        assertEquals(0, colony.getProductionBonus());
        assertEquals(5 + 5, colony.getFoodProduction());

        // Start Plowing
        TileImprovement plowImprovement = new TileImprovement(game, plain58, plow);
        plain58.add(plowImprovement);
        hardyPioneer.work(plowImprovement);
 
        game.newTurn();

        assertEquals(true, plain58.hasImprovement(plow));
        // Production for next turn is updated
        assertEquals(5 + 6, colony.getFoodProduction());
        // But in only 10 - 2 == 8 are added from last turn
        assertEquals(8 + 8, colony.getGoodsCount(Goods.FOOD));
        assertEquals(2, colony.getFoodConsumption());

        // Advance last turn
        game.newTurn();

        assertEquals(16 + 9, colony.getGoodsCount(Goods.FOOD));
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
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile plain = map.getTile(5, 8);
        map.getTile(5, 8).setExploredBy(dutch, true);

        Unit hardyPioneer = new Unit(game, plain, dutch, spec().getUnitType("model.unit.hardyPioneer"),
                                     UnitState.ACTIVE);

        // Before
        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(100, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(false, plain.hasRoad());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());

        // Now do it
        TileImprovement roadImprovement = new TileImprovement(game, plain, road);
        plain.add(roadImprovement);
        hardyPioneer.work(roadImprovement);

        // After
        assertEquals(0, hardyPioneer.getMovesLeft());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getEquipmentCount(toolsType) * 20);
        assertEquals(true, plain.hasRoad());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());

        // Advance 1 turn
        game.newTurn();

        assertEquals(3, hardyPioneer.getMovesLeft());
        assertEquals(UnitState.ACTIVE, hardyPioneer.getState());
        assertEquals(-1, hardyPioneer.getWorkLeft());
        assertEquals(80, hardyPioneer.getEquipmentCount(toolsType) * 20);
    }

    public static int getWorkLeftForPioneerWork(UnitType unitType, TileType tileType, TileImprovementType whichWork) {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile tile = new Tile(game, tileType, 0, 0);

        EquipmentType tools = spec().getEquipmentType("model.equipment.tools");
        Unit unit = new Unit(game, tile, dutch, unitType, UnitState.ACTIVE, tools, tools, tools, tools, tools);

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

        UnitType freeColonist = spec().getUnitType("model.unit.freeColonist");
        UnitType hardyPioneer = spec().getUnitType("model.unit.hardyPioneer");
    	
        { // Savanna
            assertEquals(7, getWorkLeftForPioneerWork(freeColonist, savannahForest, clear));
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, savannahForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, savannah, plow));
            assertEquals(2, getWorkLeftForPioneerWork(freeColonist, savannah, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, savannahForest, clear));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, savannahForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, savannah, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneer, savannah, road));
        }

        { // Tundra
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, tundraForest, clear));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, tundraForest, road));
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, tundra, plow));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, tundra, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, tundraForest, clear));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, tundraForest, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, tundra, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, tundra, road));
        }

        { // Plains
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, plainsForest, clear));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, plainsForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, plains, plow));
            assertEquals(2, getWorkLeftForPioneerWork(freeColonist, plains, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, plainsForest, clear));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, plainsForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, plains, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneer, plains, road));
        }

        { // Hill
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, hills, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, hills, road));
        }

        { // Mountain
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, mountains, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, mountains, road));
        }

        { // Marsh
            assertEquals(7, getWorkLeftForPioneerWork(freeColonist, marshForest, clear));
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, marshForest, road));
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, marsh, plow));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, marsh, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, marshForest, clear));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, marshForest, road));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, marsh, plow));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, marsh, road));
        }

        { // Desert
            assertEquals(5, getWorkLeftForPioneerWork(freeColonist, desertForest, clear));
            assertEquals(3, getWorkLeftForPioneerWork(freeColonist, desertForest, road));
            assertEquals(4, getWorkLeftForPioneerWork(freeColonist, desert, plow));
            assertEquals(2, getWorkLeftForPioneerWork(freeColonist, desert, road));

            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, desertForest, clear));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, desertForest, road));
            assertEquals(1, getWorkLeftForPioneerWork(hardyPioneer, desert, plow));
            assertEquals(-1, getWorkLeftForPioneerWork(hardyPioneer, desert, road));
        }

        { // Swamp
            assertEquals(8, getWorkLeftForPioneerWork(freeColonist, swampForest, clear));
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, swampForest, road));
            assertEquals(8, getWorkLeftForPioneerWork(freeColonist, swamp, plow));
            assertEquals(6, getWorkLeftForPioneerWork(freeColonist, swamp, road));

            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, swampForest, clear));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, swampForest, road));
            assertEquals(3, getWorkLeftForPioneerWork(hardyPioneer, swamp, plow));
            assertEquals(2, getWorkLeftForPioneerWork(hardyPioneer, swamp, road));
        }
    }
    
    /**
     * Test unit for colonist status
     * 
     */
    public void testIsColonist() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        
        Unit merchantman = new Unit(game, map.getTile(6, 8), dutch, spec().getUnitType("model.unit.merchantman"),
                                    UnitState.ACTIVE);
        
        assertFalse("Merchantman isnt a colonist",merchantman.isColonist());
        
        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                UnitState.ACTIVE);
        
        assertTrue("A soldier is a colonist",soldier.isColonist());
    }

    /**
     * Make sure that a colony can only be built by a worker on the
     * same tile as the colony to be built.
     * 
     */
    public void testBuildColonySameTile() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(plains, true);
        game.setMap(map);

        Unit soldier = new Unit(game, map.getTile(6, 8), dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                UnitState.ACTIVE);

        Colony colony = new Colony(game, dutch, "New Amsterdam", map.getTile(6, 9));
        soldier.setWorkType(Goods.FOOD);

        try {
            soldier.buildColony(colony);
            fail();
        } catch (IllegalStateException e) {
        }

        soldier.setLocation(map.getTile(6, 9));
        soldier.buildColony(colony);

        assertEquals(colony, map.getTile(6, 9).getSettlement());
    }

    public void testCanAdd() {

        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");

        Unit galleon = new Unit(game, null, dutch, spec().getUnitType("model.unit.galleon"),
                                UnitState.ACTIVE);
        Unit caravel = new Unit(game, null, dutch, spec().getUnitType("model.unit.caravel"),
                                UnitState.ACTIVE);
        Unit colonist = new Unit(game, null, dutch, spec().getUnitType("model.unit.freeColonist"),
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

        // tests according to other possible rules
        wagonTrain.getType().setSpace(1);
        wagonTrain.getType().setSpaceTaken(2);
        caravel.getType().setSpaceTaken(1);

        assertTrue(galleon.canAdd(wagonTrain));
        assertTrue(caravel.canAdd(wagonTrain));
        // this may seem strange, but ships do carry smaller boats
        assertTrue(galleon.canAdd(caravel));
        assertFalse(caravel.canAdd(caravel));

    }
    
    public void testMissionary() {
        Game game = getStandardGame();
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Player sioux = game.getPlayer("model.nation.sioux");
        Player dutch = game.getPlayer("model.nation.dutch");
        Tile tile = map.getTile(6, 9);
        UnitType missionaryType = spec().getUnitType("model.unit.jesuitMissionary");
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        
        Colony colony = getStandardColony(3);
        BuildingType churchType = FreeCol.getSpecification().getBuildingType("model.building.Chapel");
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
        
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
        Unit colonist = new Unit(game, tile, player, colonistType, UnitState.ACTIVE);
        assertEquals(1, colonist.getLineOfSight());
        
        EquipmentType horses = spec().getEquipmentType("model.equipment.horses");
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
        
        UnitType colonistType = spec().getUnitType("model.unit.freeColonist");
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
        Unit brave = new Unit(game, tile1, sioux, braveType, UnitState.ACTIVE, braveType.getDefaultEquipment());
        assertFalse(brave.canBuildColony());
    }
    
    public void testCashInTreasure() {
        Game game = getStandardGame();
        Player dutch = game.getPlayer("model.nation.dutch");
        TileType plains = FreeCol.getSpecification().getTileType("model.tile.ocean");
        Map map = getTestMap(plains, true);
        game.setMap(map);
        Tile tile = map.getTile(10, 4);
        
        UnitType shipType = FreeCol.getSpecification().getUnitType("model.unit.galleon");
        Unit ship = new Unit(game, tile, dutch, shipType, UnitState.ACTIVE, shipType.getDefaultEquipment());
        
        UnitType treasureType = FreeCol.getSpecification().getUnitType("model.unit.treasureTrain");
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
        treasure.cashInTreasureTrain();
        assertEquals(100, dutch.getGold());
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
		GoodsType horsesType = FreeCol.getSpecification().getGoodsType("model.goods.horses");
        GoodsType musketsType = FreeCol.getSpecification().getGoodsType("model.goods.muskets");
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
        
        // Setup
        camp.addGoods(horsesType,horsesReqPerUnit);
        camp.addGoods(musketsType,musketsReqPerUnit);
     
        assertEquals("Initial number of horses in Indian camp not as expected",horsesReqPerUnit,camp.getGoodsCount(horsesType));
        assertEquals("Initial number of muskets in Indian camp not as expected",musketsReqPerUnit,camp.getGoodsCount(musketsType));

        Unit brave = camp.getUnitList().get(0);
        
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

        UnitType colonist = spec().getUnitType("model.unit.freeColonist");
        assertEquals(EquipmentType.NO_EQUIPMENT, colonist.getDefaultEquipment());

        UnitType pioneer = spec().getUnitType("model.unit.hardyPioneer");
        assertEquals(spec().getEquipmentType("model.equipment.tools"), pioneer.getDefaultEquipmentType());
        assertEquals(5, pioneer.getDefaultEquipment().length);

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
        soldier.buildColony(colony);
        
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

    public void boardShipTest() {
        Game game = getStandardGame();
        Map map = getTestMap();
        Tile tile = map.getTile(6, 8);
        game.setMap(map);

        Player dutch = game.getPlayer("model.nation.dutch");
        Unit colonist = new Unit(game, tile, dutch, colonistType, UnitState.ACTIVE);
        Unit galleon = new Unit(game, tile, dutch, galleonType, UnitState.ACTIVE);
        Unit caravel = new Unit(game, tile, dutch, caravelType, UnitState.ACTIVE);
        Unit wagon = new Unit(game, tile, dutch, wagonType, UnitState.ACTIVE);

        caravel.getType().setSpaceTaken(2);
        wagon.getType().setSpaceTaken(2);

        // can't put ship on carrier
        caravel.boardShip(galleon);
        assertEquals(tile, caravel.getLocation());
        assertEquals(UnitState.ACTIVE, caravel.getState());

        // can put wagon on carrier
        wagon.boardShip(galleon);
        assertEquals(galleon, wagon.getLocation());
        assertEquals(UnitState.SENTRY, wagon.getState());

        colonist.boardShip(galleon);
        assertEquals(galleon, colonist.getLocation());
        assertEquals(UnitState.SENTRY, colonist.getState());

    }

}
