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

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.common.model.Unit.UnitState;

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
        assertEquals(6, dutch.getDifficulty().getBadGovernmentLimit());
        assertEquals(10, dutch.getDifficulty().getVeryBadGovernmentLimit());

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
        UnitType farmerSkill = spec().getUnitType("model.unit.expertFarmer");
        Unit missionary = new Unit(game, tile, dutch, missionaryType, UnitState.ACTIVE);
        IndianSettlement s = new IndianSettlement(game, sioux, tile, true, farmerSkill, true, null);
        // add the missionary
        s.setMissionary(missionary);
        // remove the missionary (SimpleCombatModel.getConvert(...)
        s.setMissionary(null);
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
        colonist.equipWith(horses);
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
        
        // The following method call can occur in the client InGameController.removeUnitsOutsideLOS()
        // Unfortunately, it throws a ConcurrentModificationException
        tile.disposeAllUnits();
        
    }
}
