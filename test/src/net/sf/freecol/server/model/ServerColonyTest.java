/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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

import java.util.List;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class ServerColonyTest extends FreeColTestCase {
    private static final BuildingType depotType
        = spec().getBuildingType("model.building.depot");
    private static final BuildingType warehouseType
        = spec().getBuildingType("model.building.warehouse");
    private static final BuildingType carpenterHouseType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final BuildingType lumberMillType
        = spec().getBuildingType("model.building.lumberMill");
    private static final BuildingType churchType
        = spec().getBuildingType("model.building.chapel");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType hammerGoodsType
        = spec().getGoodsType("model.goods.hammers");
    private static final GoodsType lumberGoodsType
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType foodType
        = spec().getGoodsType("model.goods.food");
    private static final GoodsType foodGoodsType
        = spec().getPrimaryFoodType();

    private static final TileType plains
        = spec().getTileType("model.tile.plains");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType pioneerType
        = spec().getUnitType("model.unit.hardyPioneer");


    public void testFoodConsumption() {
        Map map = getTestMap(plains);
        Game game = ServerTestHelper.startServerGame(map);
        ServerPlayer dutch = (ServerPlayer) game.getPlayer("model.nation.dutch");
        // Setting test colony and colonist
        Colony colony = FreeColTestUtils.getColonyBuilder()
            .colonyTile(map.getTile(5, 8)).build();
        new ServerUnit(game, colony.getBuildingForProducing(bellsType), dutch,
                       colonistType);
        assertEquals(0, colony.getGoodsCount(foodType));

        int quantity = colony.getFoodConsumption() * 2;
        colony.addGoods(foodGoodsType, quantity);
        int foodStored = colony.getGoodsCount(foodGoodsType);
        assertEquals(quantity, foodStored);
        int foodExpected = foodStored - colony.getFoodConsumption()
            + colony.getFoodProduction();

        ServerTestHelper.newTurn();
        assertEquals("Unexpected value for remaining food, ",
                     foodExpected, colony.getGoodsCount(foodGoodsType));
    }

    public void testEqualFoodProductionConsumptionCase() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        Player dutch = game.getPlayer("model.nation.dutch");
        Map map = getTestMap(spec().getTileType("model.tile.desert"));
        game.setMap(map);

        //////////////////////
        // Setting test colony

        Tile colonyTile = map.getTile(5, 8);

        Colony colony = FreeColTestUtils.getColonyBuilder().colonyTile(colonyTile).build();

        // Set the food production of the center tile of the colony to 2
        // This will be the only food production of the colony
        List<AbstractGoods> colonyTileProduction = colonyTile.getType().getProduction();
        for(int i=0; i< colonyTileProduction.size(); i++ ){
            AbstractGoods production = colonyTileProduction.get(i);

            if(production.getType() == foodGoodsType){
                colonyTile.getType().getProduction().get(i).setAmount(2);
                break;
            }
        }

        new ServerUnit(game, colony.getBuildingForProducing(bellsType), dutch,
                       colonistType);


        // Verify that there is enough food stored
        colony.addGoods(foodGoodsType, colony.getFoodConsumption()*2);

        int colonists = colony.getUnitCount();

        String errMsg = "Production not equal to consumption, required to setup test";
        assertEquals(errMsg,colony.getFoodConsumption(),colony.getFoodProduction());

        assertEquals("Unexpected change of colonists in colony",colonists,colony.getUnitCount());

        assertEquals("Unexpected change of production/consumption ratio",colony.getFoodProduction(),colony.getFoodConsumption());
    }

    public void testDeathByStarvation() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = getTestMap(spec().getTileType("model.tile.marsh"));
        game.setMap(map);

        int consumption, production, unitsBeforeNewTurn = 3;
        Colony colony = getStandardColony(unitsBeforeNewTurn);
        ServerPlayer player = (ServerPlayer) colony.getOwner();

        Building townHall = colony.getBuildingForProducing(bellsType);
        for (Unit u : colony.getUnitList()) {
            u.setLocation(townHall);
        }
        colony.removeGoods(foodGoodsType);
        colony.invalidateCache();

        consumption = colony.getFoodConsumption();
        production = colony.getFoodProduction();
        assertTrue("Food consumption (" + consumption
            + ") should be higher than production (" + production + ")",
            consumption > production);
        assertEquals("No food stored in colony", 0,
            colony.getGoodsCount(foodType));
        assertEquals("Wrong number of units in colony", unitsBeforeNewTurn,
            colony.getUnitCount());

        ServerTestHelper.newTurn();

        consumption = colony.getFoodConsumption();
        production = colony.getFoodProduction();
        assertTrue("Food consumption (" + consumption
            + ") should be higher than production (" + production + ")",
            consumption > production);
        assertEquals("No food stored in colony", 0,
            colony.getGoodsCount(foodType));
        assertEquals("Wrong number of units in colony", unitsBeforeNewTurn-1,
            colony.getUnitCount());
    }

    public void testAvoidStarvation() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        Map map = getTestMap(spec().getTileType("model.tile.marsh"));
        game.setMap(map);

        int unitsBeforeNewTurn = 3;
        Colony colony = getStandardColony(unitsBeforeNewTurn);
        ServerPlayer player = (ServerPlayer) colony.getOwner();
        assertEquals("Wrong number of units in colony", unitsBeforeNewTurn,
            colony.getUnitCount());
        Building townHall = colony.getBuildingForProducing(bellsType);

        for (Unit u : colony.getUnitList()) {
            u.setLocation(townHall);
        }
        colony.removeGoods(foodGoodsType);
        colony.invalidateCache();

        int consumption = colony.getFoodConsumption();
        int production = colony.getTile().getType().getProductionOf(grainType, null);
        assertEquals(6, consumption);
        assertEquals(3, production);
        assertEquals(-3, colony.getNetProductionOf(foodType));
        assertEquals(0, colony.getGoodsCount(foodType));
        assertEquals(0, colony.getTile().getUnitCount());

        colony.addGoods(foodType, 202);
        ServerTestHelper.newTurn();
        assertEquals(199, colony.getGoodsCount(foodType));
        assertEquals(0, colony.getTile().getUnitCount());
        assertEquals(3, colony.getUnitCount());

        colony.addGoods(foodType, 15);
        ServerTestHelper.newTurn();
        assertEquals(11, colony.getGoodsCount(foodType));
        assertEquals(1, colony.getTile().getUnitCount());
    }

    /**
     * Tests completion of buildable
     */
    public void testBuildingCompletion() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getStandardColony();
        ServerBuilding initialWarehouse
            = new ServerBuilding(getGame(), colony, depotType);
        colony.addBuilding(initialWarehouse);
        assertTrue("Colony should be able to build warehouse",
                   colony.canBuild(warehouseType));

        // Simulate that the build is done
        colony.setCurrentlyBuilding(warehouseType);
        colony.addGoods(hammerGoodsType, 90);
        assertFalse("Colony should not have warehouse",
                    colony.getWarehouse().getType() == warehouseType);

        ServerTestHelper.newTurn();

        assertTrue("Colony should have warehouse",
                   colony.getWarehouse().getType() == warehouseType);
    }

    /**
     * Tests invalid completion of buildable, having enough resources
     */
    public void testInvalidCompletion() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        Colony colony = getStandardColony(2);
        ServerBuilding carpenterHouse
            = new ServerBuilding(getGame(), colony, carpenterHouseType);
        colony.addBuilding(carpenterHouse);
        assertFalse("Colony should not be able to build lumber mill",
                    colony.canBuild(lumberMillType));
        colony.setCurrentlyBuilding(lumberMillType);
        assertTrue("Colony should be building lumber mill",
                   colony.getCurrentlyBuilding() == lumberMillType);
        // add sufficient goods to build lumber mill
        for (AbstractGoods ag : lumberMillType.getRequiredGoods()) {
            GoodsType type = ag.getType();
            int amount = ag.getAmount() + 1;
            colony.addGoods(type, amount);
            assertEquals("Wrong quantity of " + type, amount,
                         colony.getGoodsCount(type));
        }

        // test
        assertFalse("Colony should not have lumber mill",
                    colony.getBuilding(lumberMillType).getType() == lumberMillType);

        ServerTestHelper.newTurn();

        assertFalse("Colony should not have lumber mill",
                    colony.getBuilding(lumberMillType).getType() == lumberMillType);
        assertFalse("Colony should no longer be building lumber mill",
                    colony.getCurrentlyBuilding() == lumberMillType);
    }

    public void testNoBuildingMaterialsProductionWhenBuildingNothing() {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();
        ServerBuilding carpenterHouse = new ServerBuilding(getGame(), colony, carpenterHouseType);
        colony.addBuilding(carpenterHouse);
        Unit unit = colony.getUnitList().get(0);
        colony.getBuilding(carpenterHouseType).add(unit);
        // necessary for work production
        int initialLumber = 100;
        int initialHammers = 0;
        colony.addGoods(lumberGoodsType, initialLumber);
        colony.setCurrentlyBuilding(null);

        assertEquals("Wrong initial lumber quantity, ",
                     initialLumber, colony.getGoodsCount(lumberGoodsType));
        assertTrue("Colony shoud be able to produce work (hammers)",
            colony.getTotalProductionOf(hammerGoodsType) > 0);
        assertEquals("Colony shold not have any work production(hammers) initially, ",
                     initialHammers, colony.getGoodsCount(hammerGoodsType));

        ServerTestHelper.newTurn();

        assertEquals("Colony shold not have any work production(hammers) after, ",
                     initialHammers, colony.getGoodsCount(hammerGoodsType));
        assertEquals("Wrong final lumber quantity, ",
                     initialLumber, colony.getGoodsCount(lumberGoodsType));
    }



    public void testLibertyAndImmigration() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));

        int population = 3;
        Colony colony = getStandardColony(population);

        ServerBuilding townHall = new ServerBuilding(getGame(), colony, townHallType);
        colony.addBuilding(townHall);
        Unit statesman = colony.getUnitList().get(0);
        statesman.setLocation(null);
        townHall.add(statesman);

        ServerBuilding church = new ServerBuilding(getGame(), colony, churchType);
        colony.addBuilding(church);
        church.upgrade();
        Unit preacher = colony.getUnitList().get(1);
        preacher.setLocation(null);
        church.add(preacher);

        GoodsType bellsType = spec().getGoodsType("model.goods.bells");
        GoodsType crossType = spec().getGoodsType("model.goods.crosses");

        assertEquals(0, colony.getGoodsCount(bellsType));
        ServerTestHelper.newTurn();

        int bells = 3;
        assertEquals(population, colony.getUnitCount());
        assertEquals(bells, colony.getNetProductionOf(bellsType));
        assertEquals(bells, colony.getGoodsCount(bellsType));

        colony.addGoods(bellsType, 7);
        bells += 7;
        assertEquals(bells, colony.getGoodsCount(bellsType));
        assertEquals(bells, colony.getLiberty());

        colony.removeGoods(bellsType, 5);
        bells -= 5;
        assertEquals(bells, colony.getGoodsCount(bellsType));
        assertEquals(bells, colony.getLiberty());

        int crosses = colony.getTotalProductionOf(crossType)
            - colony.getConsumptionOf(crossType);
        assertEquals(crosses, colony.getNetProductionOf(crossType));
        assertEquals(crosses, colony.getGoodsCount(crossType));
        assertEquals(crosses, colony.getImmigration());

        colony.addGoods(crossType, 7);
        crosses += 7;
        assertEquals(crosses, colony.getGoodsCount(crossType));
        assertEquals(crosses, colony.getImmigration());

        colony.removeGoods(crossType, 5);
        crosses -= 5;
        assertEquals(crosses, colony.getGoodsCount(crossType));
        assertEquals(crosses, colony.getImmigration());

    }

    /** Disabled.  Currently no reliable way to count messages.

    public void testLimitsMessageDelivery() {
        Game game = ServerTestHelper.startServerGame(getTestMap());

        final int lowLevel = 10;
        final int highLevel = 90;
        final int exportLevel = 50;
        final int overfullLevel = 201;
        final boolean canExport = true;
        int foodStart= lowLevel;

        Colony colony = getStandardColony(1);

        //Setup
        colony.addGoods(new AbstractGoods(foodGoodsType, foodStart));
        assertEquals("Setup error, wrong food count", foodStart, colony.getGoodsCount(foodGoodsType));
        colony.setExportData(new ExportData(foodGoodsType,canExport,lowLevel,highLevel,exportLevel));
        assertEquals("Setup error, wrong food low level",lowLevel,colony.getExportData(foodGoodsType).getLowLevel());
        colony.getGoodsContainer().saveState();

        // Test current condition, no warnings
        assertTrue("Setup error, no messages should have bee received yet", colony.getOwner().getModelMessages().isEmpty());

        ServerTestHelper.newTurn();

        assertEquals("Player should not have received any messages",
                     0, colony.getOwner().getModelMessages().size());

        // Simulate consumption of food
        colony.getGoodsContainer().removeGoods(foodGoodsType, 1);

        ServerTestHelper.newTurn();

        // Test new condition food below limits
        assertEquals("Player should have received one message",
                     1, colony.getOwner().getModelMessages().size());

        // Stuff the colony overfull with food.  This should *not* trigger
        // a warning because upper limits are ignored for food.
        colony.getGoodsContainer().addGoods(foodGoodsType, overfullLevel);
        colony.getOwner().clearModelMessages();

        ServerTestHelper.newTurn();

        assertTrue("Food does not have a storage limit", foodGoodsType.limitIgnored());
        assertEquals("Player should not receive a message",
                     0, colony.getOwner().getModelMessages().size());
    }
    */
}
