/**
 *  Copyright (C) 2002-2024  The FreeCol Team
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
import java.util.Random;

import net.sf.freecol.common.model.BuildQueue.CompletionAction;
import net.sf.freecol.common.model.Constants.IntegrityType;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.util.test.FreeColTestCase;

public class BuildQueueTest extends FreeColTestCase {

    private BuildQueue<BuildableType> queue;
    private Colony colony;
    private Game game;
    private Specification spec;
    private UnitType colonistType;
    private BuildingType warehouseType;
    private GoodsType hammers;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        game = getStandardGame();
        spec = game.getSpecification();

        Map map = getTestMap(spec.getTileType("model.tile.plains"));
        game.setMap(map);
        Tile tile = map.getTile(2, 2);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        colony = new ServerColony(game, dutch, "TestColony", tile);

        colonistType = spec.getUnitType("model.unit.freeColonist");
        warehouseType = spec.getBuildingType("model.building.warehouse");
        hammers = spec.getGoodsType("model.goods.hammers");

        queue = new BuildQueue<>(colony, BuildQueue.CompletionAction.REMOVE, 500);
    }

    public void testAddAndSize() {
        queue.add(colonistType);
        queue.add(warehouseType);
        assertEquals(2, queue.size());
    }

    public void testSetCurrentlyBuilding() {
        queue.add(colonistType);
        queue.add(warehouseType);
        queue.setCurrentlyBuilding(warehouseType);
        assertEquals(warehouseType, queue.getCurrentlyBuilding());
        assertEquals(2, queue.size());
    }

    public void testApplyCompletionActionRemove() {
        queue.setCompletionAction(BuildQueue.CompletionAction.REMOVE);
        queue.add(colonistType);
        queue.add(warehouseType);
        
        queue.applyCompletionAction(new Random());
        
        assertEquals(1, queue.size());
        assertEquals(warehouseType, queue.getCurrentlyBuilding());
    }

    public void testApplyCompletionActionRemoveExceptLast() {
        queue.setCompletionAction(BuildQueue.CompletionAction.REMOVE_EXCEPT_LAST);
        queue.add(colonistType);
        
        queue.applyCompletionAction(new Random());
        assertEquals(1, queue.size());
        
        queue.add(warehouseType);
        queue.applyCompletionAction(new Random());
        assertEquals(1, queue.size());
        assertEquals(warehouseType, queue.getCurrentlyBuilding());
    }

    public void testGetProductionInfo() {
        queue.add(warehouseType);
        List<AbstractGoods> input = new ArrayList<>();
        
        input.add(new AbstractGoods(hammers, 0));
        ProductionInfo infoEmpty = queue.getProductionInfo(input);
        assertTrue(infoEmpty.getConsumption().isEmpty());

        input.clear();
        for (AbstractGoods req : warehouseType.getRequiredGoodsList()) {
            input.add(new AbstractGoods(req.getType(), req.getAmount() * 2));
        }
        ProductionInfo infoFull = queue.getProductionInfo(input);
        assertFalse(infoFull.getConsumption().isEmpty());
    }

    public void testGetConsumedGoods() {
        queue.add(colonistType);
        List<AbstractGoods> consumed = queue.getConsumedGoods();
        List<AbstractGoods> required = colonistType.getRequiredGoodsList();

        assertEquals(required.size(), consumed.size());
        for (int i = 0; i < required.size(); i++) {
            assertEquals(required.get(i).getType(), consumed.get(i).getType());
        }
    }

    public void testPriority() {
        assertEquals(500, queue.getPriority());
    }

    public void testToString() {
        queue.add(colonistType);
        String s = queue.toString();
        assertTrue(s.contains("TestColony"));
        assertTrue(s.contains(colonistType.getId()));
    }

    public void testMixedQueue() {
        queue.clear();
        queue.add(warehouseType);
        queue.add(spec.getUnitType("model.unit.wagonTrain"));
        
        assertEquals(2, queue.size());
        
        queue.applyCompletionAction(new Random());
        
        assertEquals("model.unit.wagonTrain", queue.getCurrentlyBuilding().getId());
    }

    public void testCheckIntegrity() {
        BuildingType dockType = spec.getBuildingType("model.building.docks");
        queue.clear();
        queue.add(dockType);
        
        net.sf.freecol.common.util.LogBuilder lb = new net.sf.freecol.common.util.LogBuilder(0);
        IntegrityType result = queue.checkIntegrity(colony, false, lb);
        
        assertNotSame(IntegrityType.INTEGRITY_GOOD, result);
        assertEquals(1, queue.size());

        result = queue.checkIntegrity(colony, true, lb);
        
        assertEquals(IntegrityType.INTEGRITY_FIXED, result);
        assertEquals(0, queue.size());
    }

    public void testGetProductionInfoWithConsumption() {
        queue.clear();
        queue.add(warehouseType);
        List<AbstractGoods> requirements = warehouseType.getRequiredGoodsList();
        GoodsType hammerType = requirements.get(0).getType();
        int requiredAmount = requirements.get(0).getAmount();

        List<AbstractGoods> input = new ArrayList<>();
        input.add(new AbstractGoods(hammerType, requiredAmount / 2));
        
        ProductionInfo info = queue.getProductionInfo(input);
        assertTrue(info.getConsumption().isEmpty());

        input.clear();
        input.add(new AbstractGoods(hammerType, requiredAmount));
        info = queue.getProductionInfo(input);
        
        assertEquals(requiredAmount, info.getConsumption().get(0).getAmount());

        spec.setBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW, false);
        input.clear();
        input.add(new AbstractGoods(hammerType, requiredAmount + 50));
        
        info = queue.getProductionInfo(input);
        
        assertEquals(requiredAmount + 50, info.getConsumption().get(0).getAmount());
    }

    public void testRedundantBuildingRemoval() {
        queue.clear();
        BuildingType school = spec.getBuildingType("model.building.schoolhouse");
        queue.add(school);
        
        colony.addBuilding(new net.sf.freecol.server.model.ServerBuilding(game, colony, school));
        
        BuildableType next = queue.getNextBuildable(colony);
        
        assertNull(next);
        assertEquals(0, queue.size());
    }

    public void testPopulationDropPurge() {
        queue.clear();
        BuildingType news = spec.getBuildingType("model.building.newspaper");
        queue.add(news);
        
        BuildableType next = queue.getNextBuildable(colony);
        
        assertNull(next);
        assertEquals(0, queue.size());
    }

    public void testUpgradeChainWithPopulation() {
        queue.clear();
        BuildingType house = spec.getBuildingType("model.building.warehouse");
        BuildingType upgrade = spec.getBuildingType("model.building.warehouseExpansion");

        forcePopulation(10);
        queue.add(house);
        queue.add(upgrade);

        assertEquals("Should be warehouse", house, queue.getNextBuildable(colony));

        queue.applyCompletionAction(new Random());

        colony.addBuilding(new net.sf.freecol.server.model.ServerBuilding(game, colony, house));

        assertEquals("Should now be expansion", upgrade, queue.getNextBuildable(colony));
    }

    public void testDeepQueueAssumption() {
        queue.clear();
        forcePopulation(10);

        BuildingType house = spec.getBuildingType("model.building.warehouse");
        BuildingType upgrade = spec.getBuildingType("model.building.warehouseExpansion");

        queue.add(house);
        queue.add(upgrade);
       
        BuildableType next = queue.getNextBuildable(colony);
        assertEquals("Should still suggest the first item", house, next);
        assertEquals("Queue size should still be 2", 2, queue.size());
    }

    public void testCoastalRequirementPurge() {
        queue.clear();
        BuildingType docks = spec.getBuildingType("model.building.docks");
        queue.add(docks);
        assertNull("Docks should be invalid for inland colony", queue.getNextBuildable(colony));
        assertEquals("Invalid coastal building should be purged", 0, queue.size());
    }

    public void testResourceStarvation() {
        queue.clear();
        BuildingType house = spec.getBuildingType("model.building.warehouse");
        queue.add(house);

        List<AbstractGoods> input = new ArrayList<>();
        input.add(new AbstractGoods(spec.getGoodsType("model.goods.hammers"), 0));

        ProductionInfo info = queue.getProductionInfo(input);

        assertTrue("Should consume nothing", info.getConsumption().isEmpty());
        assertEquals("Queue should not purge for lack of materials", 1, queue.size());
    }

    public void testIntegrityRepair() {
        queue.clear();
        BuildingType school = spec.getBuildingType("model.building.schoolhouse");
        colony.addBuilding(new ServerBuilding(game, colony, school));
        queue.add(school);
        LogBuilder lb = new LogBuilder(0);
        queue.checkIntegrity(colony, true, lb);
        assertEquals("Integrity check should have removed the redundant building", 0, queue.size());
    }

    public void testAddRandomCompletion() {
        BuildQueue<UnitType> q = new BuildQueue<>(colony,
            CompletionAction.ADD_RANDOM, 0);

        UnitType base = spec.getUnitType("model.unit.freeColonist");
        q.add(base);

        q.applyCompletionAction(new Random(1));

        assertEquals(1, q.size());
        assertNotSame(base, q.getCurrentlyBuilding());
    }

    public void testAddRandomProducesOnlyRecruitable() {
        BuildQueue<UnitType> q = new BuildQueue<>(colony,
            CompletionAction.ADD_RANDOM, 0);

        UnitType base = spec.getUnitType("model.unit.freeColonist");
        q.add(base);

        for (int i = 0; i < 20; i++) {
            q.applyCompletionAction(new Random(i));
            UnitType u = q.getCurrentlyBuilding();
            assertTrue("Unit must be recruitable", u.isRecruitable());
        }
    }

    public void testAddRandomPreservesSize() {
        BuildQueue<UnitType> q = new BuildQueue<>(colony,
            CompletionAction.ADD_RANDOM, 0);

        q.add(spec.getUnitType("model.unit.freeColonist"));

        for (int i = 0; i < 10; i++) {
            q.applyCompletionAction(new Random(i));
            assertEquals(1, q.size());
        }
    }

    public void testAddRandomDeterministic() {
        BuildQueue<UnitType> q1 = new BuildQueue<>(colony,
            CompletionAction.ADD_RANDOM, 0);
        BuildQueue<UnitType> q2 = new BuildQueue<>(colony,
            CompletionAction.ADD_RANDOM, 0);

        UnitType base = spec.getUnitType("model.unit.freeColonist");
        q1.add(base);
        q2.add(base);

        Random r1 = new Random(123);
        Random r2 = new Random(123);

        for (int i = 0; i < 5; i++) {
            q1.applyCompletionAction(r1);
            q2.applyCompletionAction(r2);
        }

        assertEquals(q1.getCurrentlyBuilding(), q2.getCurrentlyBuilding());
    }

    public void testAddRandomProducesRecruitable() {
        BuildQueue<UnitType> q = new BuildQueue<>(colony,
            CompletionAction.ADD_RANDOM, 0);

        q.add(spec.getUnitType("model.unit.freeColonist"));

        for (int i = 0; i < 20; i++) {
            q.applyCompletionAction(new Random(i));
            assertTrue(q.getCurrentlyBuilding().isRecruitable());
        }
    }

    private void forcePopulation(int n) {
        for (Unit u : new ArrayList<>(colony.getUnitList())) {
            u.dispose();
        }
        for (int i = 0; i < n; i++) {
            Unit u = new net.sf.freecol.server.model.ServerUnit(game, 
                    colony.getTile(), colony.getOwner(), colonistType);
            u.setLocation(colony); 
        }
    }
}
