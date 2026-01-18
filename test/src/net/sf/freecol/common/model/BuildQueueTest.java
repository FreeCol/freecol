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
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.util.test.FreeColTestCase;

public class BuildQueueTest extends FreeColTestCase {

    private BuildQueue<BuildableType> queue;
    private Colony colony;
    private UnitType colonistType;
    private UnitType pettyCriminal;
    private UnitType indenturedServant;
    private UnitType randomUnit;
    private BuildingType schoolhouseType;
    private GoodsType hammers;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        Game game = getStandardGame();
        Specification spec = game.getSpecification();

        Map map = getTestMap(spec.getTileType("model.tile.plains"));
        game.setMap(map);
        Tile tile = map.getTile(2, 2);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        colony = new ServerColony(game, dutch, "TestColony", tile);

        colonistType = spec.getUnitType("model.unit.freeColonist");
        pettyCriminal = spec.getUnitType("model.unit.pettyCriminal");
        indenturedServant = spec.getUnitType("model.unit.indenturedServant");
        schoolhouseType = spec.getBuildingType("model.building.schoolhouse");
        hammers = spec.getGoodsType("model.goods.hammers");
        randomUnit = spec.getUnitTypeList().get(0);

        queue = new BuildQueue<>(colony, BuildQueue.CompletionAction.REMOVE, 500);
    }

    public void testAddAndSize() {
        assertEquals(0, queue.size());
        queue.add(colonistType);
        assertEquals(1, queue.size());
        queue.add(schoolhouseType);
        assertEquals(2, queue.size());
    }

    public void testSetCurrentlyBuildingMovesToFront() {
        queue.add(colonistType);
        queue.add(schoolhouseType);
        queue.setCurrentlyBuilding(schoolhouseType);
        assertEquals(schoolhouseType, queue.getCurrentlyBuilding());
        assertEquals(2, queue.size());
    }

    public void testGetProductionInfoInsufficientGoods() {
        queue.add(schoolhouseType);
        List<AbstractGoods> input = new ArrayList<>();
        input.add(new AbstractGoods(hammers, 0));
        ProductionInfo info = queue.getProductionInfo(input);
        assertTrue(info.getConsumption().isEmpty());
    }

    public void testGetProductionInfoSufficientGoods() {
        queue.add(colonistType);
        List<AbstractGoods> requirements = colonistType.getRequiredGoodsList();
        List<AbstractGoods> input = new ArrayList<>();

        for (AbstractGoods req : requirements) {
            input.add(new AbstractGoods(req.getType(), req.getAmount() * 2));
        }

        if (requirements.isEmpty()) {
            queue.clear();
            queue.add(schoolhouseType);
            for (AbstractGoods req : schoolhouseType.getRequiredGoodsList()) {
                input.add(new AbstractGoods(req.getType(), req.getAmount() * 2));
            }
        }

        ProductionInfo info = queue.getProductionInfo(input);
        assertFalse(info.getConsumption().isEmpty());

        GoodsType expectedType = queue.getCurrentlyBuilding()
                                      .getRequiredGoodsList()
                                      .get(0)
                                      .getType();
        assertEquals(expectedType, info.getConsumption().get(0).getType());
    }

    public void testCompletionActionRemoveExceptLast() {
        queue.setCompletionAction(BuildQueue.CompletionAction.REMOVE_EXCEPT_LAST);
        queue.add(colonistType);
        queue.add(colonistType);
        assertEquals(2, queue.size());
        queue.remove(0);
        assertEquals(1, queue.size());
        assertEquals(colonistType, queue.getCurrentlyBuilding());
    }

    public void testPriority() {
        assertEquals(500, queue.getPriority());
    }

    public void testToStringContainsColonyName() {
        queue.add(colonistType);
        String s = queue.toString();
        assertTrue(s.contains("TestColony"));
        assertTrue(s.contains(colonistType.getId()));
    }

    public void testSetCurrentlyBuildingRemovesDuplicateBuilding() {
        queue.add(schoolhouseType);
        queue.add(schoolhouseType);
        assertEquals(2, queue.size());
        assertEquals(schoolhouseType, queue.getCurrentlyBuilding());
    }

    public void testGetProductionInfoWithOverflow() {
        colony.getSpecification().setBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW, true);
        queue.add(schoolhouseType);

        List<AbstractGoods> input = new ArrayList<>();
        for (AbstractGoods req : schoolhouseType.getRequiredGoodsList()) {
            input.add(new AbstractGoods(req.getType(), req.getAmount() * 2));
        }

        ProductionInfo info = queue.getProductionInfo(input);

        assertFalse(info.getConsumption().isEmpty());
        for (int i = 0; i < info.getConsumption().size(); i++) {
            assertEquals(
                schoolhouseType.getRequiredGoodsList().get(i).getAmount(),
                info.getConsumption().get(i).getAmount()
            );
        }
    }

    public void testEmptyQueueBehavior() {
        assertNull(queue.getCurrentlyBuilding());
        ProductionInfo info = queue.getProductionInfo(new ArrayList<>());
        assertTrue(info.getConsumption().isEmpty());
        assertTrue(queue.getConsumedGoods().isEmpty());
    }

    public void testGetConsumedGoodsExplicit() {
        queue.add(colonistType);

        List<AbstractGoods> consumed = queue.getConsumedGoods();
        List<AbstractGoods> required = colonistType.getRequiredGoodsList();

        assertEquals(required.size(), consumed.size());
        for (int i = 0; i < required.size(); i++) {
            assertEquals(required.get(i).getType(), consumed.get(i).getType());
            assertEquals(required.get(i).getAmount(), consumed.get(i).getAmount());
        }
    }

    public void testPopulationQueueShuffle() {
        BuildQueue<UnitType> popQueue =
            new BuildQueue<>(colony, BuildQueue.CompletionAction.SHUFFLE, 999);

        popQueue.add(colonistType);
        popQueue.add(pettyCriminal);
        popQueue.add(indenturedServant);
        List<UnitType> before = new ArrayList<>(popQueue.getValues());
        Collections.shuffle(before, new Random(12345));
        List<UnitType> after = popQueue.getValues();
        assertFalse(before.equals(after));
    }

    public void testCompletionActionRemove() {
        BuildQueue<UnitType> q =
            new BuildQueue<>(colony, BuildQueue.CompletionAction.REMOVE, 999);

        q.add(colonistType);
        q.add(colonistType);

        assertEquals(2, q.size());
        q.remove(0);

        assertEquals(1, q.size());
        assertEquals(colonistType, q.getCurrentlyBuilding());
    }

    public void testCompletionActionShuffle() {
        BuildQueue<UnitType> q =
            new BuildQueue<>(colony, BuildQueue.CompletionAction.SHUFFLE, 999);

        q.add(colonistType);
        q.add(pettyCriminal);
        q.add(indenturedServant);

        List<UnitType> before = new ArrayList<>(q.getValues());

        if (q.size() > 1) {
            List<UnitType> shuffled = new ArrayList<>(before);
            Collections.shuffle(shuffled, new Random(12345));
            q.setValues(shuffled);
        }

        List<UnitType> after = q.getValues();
        assertFalse(before.equals(after));
    }

    public void testCompletionActionAddRandom() {
        BuildQueue<UnitType> q =
            new BuildQueue<>(colony, BuildQueue.CompletionAction.ADD_RANDOM, 999);

        q.add(colonistType);
        assertEquals(1, q.size());
        q.remove(0);
        q.add(randomUnit);
        assertEquals(1, q.size());
        assertEquals(randomUnit, q.getCurrentlyBuilding());
    }
}
