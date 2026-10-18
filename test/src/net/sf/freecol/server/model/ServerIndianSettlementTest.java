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

package net.sf.freecol.server.model;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;

import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;


public class ServerIndianSettlementTest extends FreeColTestCase {

    private static final GoodsType clothType
        = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType coatsType
        = spec().getGoodsType("model.goods.coats");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");
    private static final GoodsType rumType
        = spec().getGoodsType("model.goods.rum");
    private static final GoodsType toolsType
        = spec().getGoodsType("model.goods.tools");

    private static final TileType desertType
        = spec().getTileType("model.tile.desert");
    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType brave
        = spec().getUnitType("model.unit.brave");


    public void testFoodConsumption() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();

        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getGoodsCount(foodType));

        int foodProduced = camp.getTotalProductionOf(grainType);
        int foodConsumed = camp.getFoodConsumption();
        assertTrue("Food Produced should be more the food consumed",foodProduced > foodConsumed);

        ServerTestHelper.newTurn();

        int foodRemaining = Math.max(foodProduced - foodConsumed, 0);
        assertEquals("Unexpected value for remaining food, ", foodRemaining,camp.getGoodsCount(foodType));
    }

    public void testHorseBreedingWhenFoodAvailable() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.build();

        //verify initial conditions
        assertEquals(1, camp.getUnitCount());
        assertEquals(0, camp.getGoodsCount(foodType));

        //add horses
        int initialHorses = horsesType.getBreedingNumber();
        camp.addGoods(horsesType, initialHorses);

        // verify that there is food production for the horses.
        // Using freecol rules where horses eat grain
        assertEquals("Horses need grain", grainType, horsesType.getInputType());
        int foodProduced = camp.getTotalProductionOf(grainType);
        int foodConsumed = camp.getFoodConsumption();
        int foodAvail = foodProduced - foodConsumed;
        assertTrue("Food Produced should be more the food consumed",
                   foodProduced > foodConsumed);

        int expectedHorseProd = Math.min(ServerIndianSettlement.MAX_HORSES_PER_TURN,
                                         foodAvail);
        assertTrue("Horses should breed", expectedHorseProd > 0);

        ServerTestHelper.newTurn();

        int horsesBred = camp.getGoodsCount(horsesType) - initialHorses;
        assertEquals("Wrong number of horses bred",
                     expectedHorseProd, horsesBred);
    }

    public void testHorseBreedingNoFoodAvail() {
        Game game = ServerTestHelper.startServerGame(getTestMap(desertType));

        int initialBravesInCamp = 3;
        FreeColTestCase.IndianSettlementBuilder builder = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp1 = builder.initialBravesInCamp(initialBravesInCamp).build();
        IndianSettlement camp2 = builder.reset()
            .settlementTile(camp1.getTile().getNeighbourOrNull(Direction.N)
                            .getNeighbourOrNull(Direction.N)).build();

        //////////////////////
        // Simulate that only the center tile is owned by camp 1
        // Does not matter where camp 2 is, so we put it in the same tile as camp1
        for (Tile t: camp1.getTile().getSurroundingTiles(camp1.getRadius())) {
            t.changeOwnership(camp2.getOwner(), camp2);
        }


        //verify initial conditions
        assertEquals(initialBravesInCamp, camp1.getUnitCount());
        assertEquals(0, camp1.getGoodsCount(foodType));

        int foodProduced = camp1.getTotalProductionOf(grainType);
        int foodConsumed = camp1.getFoodConsumption();
        assertEquals(2, brave.getConsumptionOf(foodType));
        assertEquals(2 * camp1.getUnitCount(), foodConsumed);
        assertTrue("Food Produced should be less the food consumed",foodProduced < foodConsumed);

        //add horses
        int initialHorses = 2;
        camp1.addGoods(horsesType, initialHorses);

        ServerTestHelper.newTurn();

        int expectedHorsesBreeded = 0;
        int horsesBreeded = camp1.getGoodsCount(horsesType) - initialHorses;
        assertEquals("No horses should be bred",expectedHorsesBreeded,horsesBreeded);
    }

    public void testSettlementPurchasePricing() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plainsType));

        final int braveCount = 4;
        FreeColTestCase.IndianSettlementBuilder builder
            = new FreeColTestCase.IndianSettlementBuilder(game);
        IndianSettlement camp = builder.initialBravesInCamp(braveCount).build();
        final int topPrice = IndianSettlement.GOODS_BASE_PRICE
            + camp.getType().getTradeBonus();

        // Clear wanted goods so as not to confuse comparisons
        camp.setWantedGoods(0, null);
        camp.setWantedGoods(1, null);
        camp.setWantedGoods(2, null);

        assertEquals(braveCount, camp.getUnitCount());

        // Should initially value military goods highly
        assertEquals("High price for horses", topPrice,
                     camp.getPriceToBuy(horsesType, 1));

        // But once there are enough for all the braves, the price should fall
        camp.addGoods(horsesType, 50);
        assertEquals("Still high price for horses", topPrice,
                     camp.getPriceToBuy(horsesType, 1));
        camp.addGoods(horsesType, 50);
        assertTrue("Commercial price for horses",
                   camp.getPriceToBuy(horsesType, 1) <= topPrice / 2);

        // Farmed goods should be much cheaper
        assertTrue("Grain is farmed", grainType.isFarmed());
        assertTrue("Devalue farmed goods",
                   camp.getPriceToBuy(grainType, 100) <= 100 * topPrice / 2);

        // Rum is more interesting...
        assertEquals(0, camp.getGoodsCount(rumType));
        assertEquals("Buy rum", topPrice,
                     camp.getPriceToBuy(rumType, 1));

        // ...but the price falls with amount present
        camp.addGoods(rumType, 100);
        assertTrue("Add rum",
                   camp.getPriceToBuy(rumType, 1) <= topPrice / 2);
        assertTrue("Add more rum",
                   camp.getPriceToBuy(rumType, 99) <= 99 * topPrice / 2);
        camp.addGoods(rumType, 100);
        assertEquals("Do not buy more rum", 0,
                     camp.getPriceToBuy(rumType, 1));

        // On plains cotton can be grown, so cloth should be cheaper than
        // coats.
        assertTrue("Cloth ("
                   + camp.getPriceToBuy(clothType, 50)
                   + ") cheaper than coats ("
                   + camp.getPriceToBuy(coatsType, 50) + ")",
            camp.getPriceToBuy(clothType, 50) < camp.getPriceToBuy(coatsType, 50));
        camp.addGoods(clothType, 20);
        camp.addGoods(coatsType, 20);
        assertTrue("Cloth still ("
                   + camp.getPriceToBuy(clothType, 50)
                   + ") cheaper than coats ("
                   + camp.getPriceToBuy(coatsType, 50) + ")",
            camp.getPriceToBuy(clothType, 50) < camp.getPriceToBuy(coatsType, 50));
        camp.addGoods(clothType, 100);
        camp.addGoods(coatsType, 100);
        assertEquals("Cloth now ignored", 0,
                     camp.getPriceToBuy(clothType, 1));
        assertEquals("Coats now ignored", 0,
                     camp.getPriceToBuy(coatsType, 1));

        // Check that wanted goods at least increases the price
        camp.setWantedGoods(2, horsesType);
        camp.setWantedGoods(1, horsesType);
        camp.setWantedGoods(0, horsesType);
        int p3 = camp.getPriceToBuy(toolsType, 50);
        camp.setWantedGoods(2, toolsType);
        camp.setWantedGoods(1, horsesType);
        camp.setWantedGoods(0, horsesType);
        int p2 = camp.getPriceToBuy(toolsType, 50);
        assertTrue("Wanted 2: (" + p2 + " > " + p3 + ")",  p2 > p3);
        camp.setWantedGoods(2, horsesType);
        camp.setWantedGoods(1, toolsType);
        camp.setWantedGoods(0, horsesType);
        int p1 = camp.getPriceToBuy(toolsType, 50);
        assertTrue("Wanted 1: (" + p1 + " > " + p2 + ")",  p1 > p2);
        camp.setWantedGoods(2, horsesType);
        camp.setWantedGoods(1, horsesType);
        camp.setWantedGoods(0, toolsType);
        int p0 = camp.getPriceToBuy(toolsType, 50);
        assertTrue("Wanted 0: (" + p0 + " > " + p1 + ")",  p0 > p1);
    }

    public void testHorseBreedingAtThreshold() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp = new IndianSettlementBuilder(game).build();

        int threshold = horsesType.getBreedingNumber();

        camp.addGoods(horsesType, threshold);

        int foodProduced = camp.getTotalProductionOf(grainType);
        int foodConsumed = camp.getFoodConsumption();

        assertTrue(foodProduced > foodConsumed);

        int expected = Math.min(
            ServerIndianSettlement.MAX_HORSES_PER_TURN,
            foodProduced - foodConsumed);

        ServerTestHelper.newTurn();

        assertEquals(threshold + expected,
                     camp.getGoodsCount(horsesType));
    }

    public void testHorseBreedingBelowThreshold() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp = new IndianSettlementBuilder(game).build();

        int horses = horsesType.getBreedingNumber() - 1;
        camp.addGoods(horsesType, horses);

        ServerTestHelper.newTurn();

        assertEquals(horses, camp.getGoodsCount(horsesType));
    }

    public void testHorseBreedingRespectsMaximumPerTurn() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlementBuilder builder
            = new IndianSettlementBuilder(game);

        IndianSettlement camp = builder.build();

        camp.addGoods(horsesType, horsesType.getBreedingNumber());

        int horseCountBefore = camp.getGoodsCount(horsesType);

        ServerTestHelper.newTurn();

        int bred = camp.getGoodsCount(horsesType) - horseCountBefore;

        assertTrue(bred <= ServerIndianSettlement.MAX_HORSES_PER_TURN);
        assertEquals(ServerIndianSettlement.MAX_HORSES_PER_TURN, bred);
    }

    public void testNoBirthAtMaximumSettlementSize() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlementBuilder builder
            = new IndianSettlementBuilder(game);

        IndianSettlement camp = builder.build();

        int max = camp.getType().getMaximumSize();

        while (camp.getUnitCount() < max + 1) {
            ((ServerIndianSettlement) camp).addUnits(1);
        }

        int before = camp.getUnitCount();

        camp.addGoods(foodType, 1000);
        camp.addGoods(rumType, 1000);

        ServerTestHelper.newTurn();

        assertEquals(before, camp.getUnitCount());
    }

    public void testNewTurnConsumesFood() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp =
            new IndianSettlementBuilder(game).build();

        camp.addGoods(foodType, 50);

        int before = camp.getGoodsCount(foodType);

        ServerTestHelper.newTurn();

        int after = camp.getGoodsCount(foodType);

        assertTrue(after < before
            + camp.getTotalProductionOf(grainType));
    }

    public void testWarehouseCapacityLimit() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp =
            new IndianSettlementBuilder(game).build();

        int capacity = camp.getWarehouseCapacity();

        camp.addGoods(toolsType, capacity * 2);

        ServerTestHelper.newTurn();

        assertTrue(
            camp.getGoodsCount(toolsType) <= capacity);
    }

    public void testAddUnitsIncreasesPopulation() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        int before = camp.getUnitCount();

        camp.addUnits(4);

        assertEquals(before + 4, camp.getUnitCount());
    }

    public void testAddUnitsSetsHomeSettlement() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
                new IndianSettlementBuilder(game).build();

        camp.addUnits(3);

        for (Unit unit : camp.getUnitList()) {
            assertEquals(camp, unit.getHomeIndianSettlement());
        }
    }

    public void testMostHatedUsesHighestAlarm() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp =
            new IndianSettlementBuilder(game).build();

        List<Player> players =
            game.getLiveEuropeanPlayerList();

        assertTrue(players.size() >= 2);

        Player player1 = players.get(0);
        Player player2 = players.get(1);

        camp.setAlarm(player1, new Tension(100));
        camp.setAlarm(player2, new Tension(200));

        assertEquals(player2, camp.getMostHated());
    }

    public void testRemoveAlarmUpdatesMostHated() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        List<Player> players =
            game.getLiveEuropeanPlayerList();

        Player player1 = players.get(0);
        Player player2 = players.get(1);

        camp.setAlarm(player1, new Tension(300));
        camp.setAlarm(player2, new Tension(400));

        assertEquals(player2, camp.getMostHated());

        camp.removeAlarm(player2);

        assertEquals(player1, camp.getMostHated());
    }

    public void testSetAlarmForOwnerIgnored() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp =
            new IndianSettlementBuilder(game).build();

        Player owner = camp.getOwner();

        camp.setAlarm(owner, new Tension(100));

        assertNull(camp.getAlarm(owner));
    }

    public void testRandomAddUnitsRespectsSettlementSizeLimits() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        int before = camp.getUnitCount();

        camp.addUnits(new Random(1));

        int added = camp.getUnitCount() - before;

        assertTrue(added > 0);

        assertTrue(
            added >= camp.getType().getMinimumSize());

        assertTrue(
            added <= camp.getType().getMaximumSize());
    }

    public void testWarehouseCapacityExactLimit() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        IndianSettlement camp =
            new IndianSettlementBuilder(game).build();

        int capacity = camp.getWarehouseCapacity();

        camp.addGoods(toolsType, capacity);

        ServerTestHelper.newTurn();

        assertEquals(capacity,
                     camp.getGoodsCount(toolsType));
    }

    public void testAddMissionary() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        assertNull(camp.getMissionary());

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        camp.csChangeMissionary(missionary, new ChangeSet());

        assertEquals(missionary, camp.getMissionary());
    }

    public void testRemoveMissionary() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        ChangeSet cs = new ChangeSet();

        camp.csChangeMissionary(missionary, cs);

        assertNotNull(camp.getMissionary());

        camp.csChangeMissionary(null, cs);

        assertNull(camp.getMissionary());
    }

    public void testKillMissionaryRemovesMissionary() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        ChangeSet cs = new ChangeSet();

        camp.csChangeMissionary(missionary, cs);

        assertNotNull(camp.getMissionary());

        camp.csKillMissionary(null, cs);

        assertNull(camp.getMissionary());
    }

    public void testMissionaryIncreasesConvertProgress() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        ChangeSet cs = new ChangeSet();

        camp.csChangeMissionary(missionary, cs);

        int before = camp.getConvertProgress();

        camp.csStartTurn(new Random(1), cs);

        assertTrue(
            camp.getConvertProgress() >= before);
    }

    public void testNewMissionaryResetsConvertProgress() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        camp.setConvertProgress(100);

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        camp.csChangeMissionary(missionary,
                                new ChangeSet());

        assertEquals(0, camp.getConvertProgress());
    }

    public void testReplaceMissionary() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game).build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        ChangeSet cs = new ChangeSet();

        Unit missionary1 = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        Unit missionary2 = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        camp.csChangeMissionary(missionary1, cs);

        assertEquals(missionary1, camp.getMissionary());

        camp.setConvertProgress(50);

        camp.csChangeMissionary(missionary2, cs);

        assertEquals(missionary2, camp.getMissionary());

        assertNotSame(missionary1, camp.getMissionary());

        assertEquals(0, camp.getConvertProgress());
    }

    public void testConversionDoesNotOccurWithoutNearbyColony() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game)
                .initialBravesInCamp(3)
                .build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        ChangeSet cs = new ChangeSet();

        camp.csChangeMissionary(missionary, cs);

        int threshold =
            camp.getType().getConvertThreshold();

        camp.setConvertProgress(threshold);

        camp.csStartTurn(new Random(1), cs);

        assertTrue(
            camp.getConvertProgress() >= threshold);
    }

    public void testConversionBlockedWithTooFewUnits() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game)
                .initialBravesInCamp(1)
                .build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        ChangeSet cs = new ChangeSet();

        camp.csChangeMissionary(missionary, cs);

        int threshold =
            camp.getType().getConvertThreshold();

        camp.setConvertProgress(threshold);

        camp.csStartTurn(new Random(1), cs);

        assertTrue(camp.getConvertProgress() >= threshold);
    }

    public void testSuccessfulConversion() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        ServerIndianSettlement camp =
            (ServerIndianSettlement)
            new IndianSettlementBuilder(game)
                .initialBravesInCamp(3)
                .build();

        Player european =
            game.getLiveEuropeanPlayerList().get(0);

        createStandardColony(1, 6, 8);

        Unit missionary = new ServerUnit(
            game,
            camp.getTile(),
            european,
            spec().getDefaultUnitType(european));

        ChangeSet cs = new ChangeSet();

        camp.csChangeMissionary(missionary, cs);

        camp.setConvertProgress(
            camp.getType().getConvertThreshold());

        int europeanUnitsBefore = european.getUnitCount();
        int settlementUnitsBefore = camp.getAllUnitsList().size();

        camp.csStartTurn(new Random(1), cs);

        assertEquals(0, camp.getConvertProgress());

        assertEquals(europeanUnitsBefore + 1,
                     european.getUnitCount());

        assertEquals(settlementUnitsBefore - 1,
                     camp.getAllUnitsList().size());
    }
}
