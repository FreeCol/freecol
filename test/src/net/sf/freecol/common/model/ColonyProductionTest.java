/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import java.util.List;

import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonyProductionTest extends FreeColTestCase {

    private static final BuildingType countryType
        = spec().getBuildingType("model.building.country");
    private static final BuildingType depotType
        = spec().getBuildingType("model.building.depot");
    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType clothType
        = spec().getGoodsType("model.goods.cloth");
    private static final GoodsType cottonType
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType crossesType
        = spec().getGoodsType("model.goods.crosses");
    private static final GoodsType foodType
        = spec().getGoodsType("model.goods.food");
    private static final GoodsType grainType
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType horsesType
        = spec().getGoodsType("model.goods.horses");

    private static final ResourceType grainResource
        = spec().getResourceType("model.resource.grain");

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");

    private static final UnitType freeColonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType pioneerType
        = spec().getUnitType("model.unit.hardyPioneer");
    private static final UnitType veteranSoldierType
        = spec().getUnitType("model.unit.veteranSoldier");


    public void testProductionSoldier() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Tile tile = map.getTile(5, 8);
        Resource grain = new Resource(game, tile, grainResource);
        tile.addResource(grain);

        Tile tileOfColony = map.getTile(6, 8);
        Unit soldier = new ServerUnit(game, tileOfColony, dutch,
            veteranSoldierType);

        Colony colony = new ServerColony(game, dutch, "New Amsterdam",
                                         tileOfColony);
        dutch.addSettlement(colony);
        nonServerBuildColony(soldier, colony);
        soldier.setLocation(colony.getWorkLocationFor(soldier, grainType));

        // Test the colony
        assertEquals(map.getTile(6, 8), colony.getTile());
        assertEquals("New Amsterdam", colony.getName());
        assertEquals(colony, colony.getTile().getSettlement());
        assertEquals(dutch, colony.getTile().getOwner());

        // Disabled.  Removal of equipment has moved to the server, so
        // nonServerBuildColony is not going to work.
        //// Should have 50 Muskets and nothing else
        //GoodsType muskets = spec().getGoodsType("model.goods.muskets");
        //assertNotNull(muskets);
        //
        //for (GoodsType type : spec().getGoodsTypeList()){
        //    if (type == muskets)
        //        assertEquals(50, colony.getGoodsCount(type));
        //    else
        //        assertEquals(type.toString(), 0, colony.getGoodsCount(type));
        //}
        //assertEquals(false, soldier.isArmed());

        // Test the state of the soldier
        // Soldier should be working on the field with the bonus

        assertEquals(grainType, soldier.getWorkType());
        assertEquals(tileOfColony, soldier.getTile());
        assertEquals(0, soldier.getMovesLeft());
    }

    public void testProductionPioneer() {
        Game game = getStandardGame();
        Map map = getTestMap();
        game.setMap(map);
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Tile tile = map.getTile(5, 8);
        Resource grain = new Resource(game, tile, grainResource);
        tile.addResource(grain);

        Tile tileOfColony = map.getTile(6, 8);
        Unit pioneer = new ServerUnit(game, tileOfColony, dutch, pioneerType);

        Colony colony = new ServerColony(game, dutch, "New Amsterdam",
                                         tileOfColony);
        dutch.addSettlement(colony);
        nonServerBuildColony(pioneer, colony);
        pioneer.setLocation(colony.getWorkLocationFor(pioneer, grainType));

        // Test the colony
        assertEquals(map.getTile(6, 8), colony.getTile());
        assertEquals("New Amsterdam", colony.getName());
        assertEquals(colony, colony.getTile().getSettlement());
        assertEquals(dutch, colony.getTile().getOwner());

        // Disabled.  Removal of equipment has moved to the server, so
        // nonServerBuildColony is not going to work.
        //// Should have 100 tools and nothing else
        //GoodsType tools = spec().getGoodsType("model.goods.tools");
        //assertNotNull(tools);
        //
        //for (GoodsType type : spec().getGoodsTypeList()){
        //    if (type == tools)
        //        assertEquals(100, colony.getGoodsCount(type));
        //    else
        //        assertEquals(type.toString(), 0, colony.getGoodsCount(type));
        //}
        //assertEquals(false, pioneer.isArmed());

        // Test the state of the pioneer
        // Pioneer should be working on the field with the bonus
        assertEquals(grainType, pioneer.getWorkType());
        assertEquals(tileOfColony, pioneer.getTile());
        assertEquals(0, pioneer.getMovesLeft());
    }

    public void testBellNetProduction() {
        Game game = getStandardGame();
        game.setMap(getTestMap());

        // Get a minimal colony so that the units-that-use-no-bells
        // parameter will not be relevant.
        final Colony colony = getStandardColony(1);
        final Player player = colony.getOwner();
        final int noBellUnits = colony.getSpecification()
            .getInteger(GameOptions.UNITS_THAT_USE_NO_BELLS);

        // Clear the town hall
        Building townHall = colony.getBuilding(townHallType);
        for (Unit u : townHall.getUnitList()) {
            u.setLocation(colony.getWorkLocationFor(u, foodType));
        }
        assertTrue(townHall.isEmpty());

        int initialBellCount = colony.getGoodsCount(bellsType);
        int expectedBellCount = 0;
        assertEquals("Wrong initial bell count", expectedBellCount,
                     initialBellCount);

        // Check the consumption absent the unit threshold
        int bellsUpkeep = colony.getConsumptionOf(bellsType);
        int expectedBellUpkeep = Math.max(colony.getUnitCount() - noBellUnits,
                                          0);
        assertEquals("Wrong bell upkeep", expectedBellUpkeep,
                     bellsUpkeep);

        // Add enough units to activate the units-that-use-no-bells.
        for (int i = 0; i < noBellUnits; i++) {
            Unit u = new ServerUnit(game, colony.getTile(), player,
                                    freeColonistType);
            assertTrue(u.setLocation(colony.getWorkLocationFor(u, foodType)));
        }
        assertTrue(townHall.isEmpty());
        colony.invalidateCache();

        // Recheck the consumption
        bellsUpkeep = colony.getConsumptionOf(bellsType);
        expectedBellUpkeep = Math.max(colony.getUnitCount() - noBellUnits, 0);
        assertEquals("Wrong bell upkeep (more units)", expectedBellUpkeep,
                     bellsUpkeep);

        int bellsProdPerTurn = colony.getTotalProductionOf(bellsType);
        int expectedBellProd = townHallType.getBaseProduction(null, bellsType,
                                                              null);
        assertEquals("Wrong unattended bell production", expectedBellProd,
                     bellsProdPerTurn);

        int bellsNetProdPerTurn = colony.getNetProductionOf(bellsType);
        int expectedBellNetProd = expectedBellProd - expectedBellUpkeep;
        assertEquals("Wrong unattended bell net production", expectedBellNetProd,
                     bellsNetProdPerTurn);
        
        Unit unit = colony.getFirstUnit();
        assertTrue(unit.setLocation(townHall));
        colony.invalidateCache();

        ProductionType productionType = townHall.getProductionType();
        bellsProdPerTurn = colony.getTotalProductionOf(bellsType);
        expectedBellProd = townHallType.getBaseProduction(productionType,
            bellsType, unit.getType())
            + townHallType.getBaseProduction(null, bellsType, null);
        assertEquals("Wrong attended bell production", expectedBellProd,
                     bellsProdPerTurn);

        bellsNetProdPerTurn = colony.getNetProductionOf(bellsType);
        expectedBellNetProd = expectedBellProd - expectedBellUpkeep;
        assertEquals("Wrong attended bell net production", expectedBellNetProd,
                     bellsNetProdPerTurn);
    }

    /**
     * Tests that there is no over production of horses, to avoid them
     * being thrown out.  A test of the proper production of horses is
     * in <code>BuildingTest</code>
     */
    public void testNoHorsesOverProduction() {
        Game game = getGame();
        game.setMap(getTestMap());

        Colony colony = getStandardColony(1);
        Building pasture = colony.getBuilding(countryType);
        Unit unit = colony.getFirstUnit();
        unit.setLocation(colony.getWorkLocationFor(unit, bellsType));

        List<AbstractGoods> outputs = pasture.getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(horsesType, outputs.get(0).getType());
        assertEquals("Wrong warehouse capacity in colony",
            GoodsContainer.CARGO_SIZE, colony.getWarehouseCapacity());

        // Still room for more
        colony.addGoods(horsesType, 99);
        colony.invalidateCache();

        assertEquals(99, colony.getGoodsCount(horsesType));
        assertTrue(colony.getNetProductionOf(foodType) > 0);
        assertEquals("Wrong horse production", 1,
            pasture.getTotalProductionOf(horsesType));
        assertEquals("Wrong maximum horse production", 1,
            pasture.getMaximumProductionOf(horsesType));
        assertEquals("Wrong net horse production", 1,
            colony.getNetProductionOf(horsesType));

        // No more room available
        colony.addGoods(horsesType, 1);
        colony.invalidateCache();

        assertEquals("Wrong number of horses in colony",
            colony.getWarehouseCapacity(), colony.getGoodsCount(horsesType));
        assertEquals("Wrong horse production", 0,
            pasture.getTotalProductionOf(horsesType));
        assertEquals("Wrong maximum horse production", 0,
            pasture.getMaximumProductionOf(horsesType));
        assertEquals("Wrong net horse production", 0,
            colony.getNetProductionOf(horsesType));
    }


    public void testConsumers() {

        Game game = getGame();
        game.setMap(getTestMap());


        Colony colony = getStandardColony(3);
        int units = colony.getUnitCount();
        int buildings = colony.getBuildings().size();

        List<Consumer> consumers = colony.getConsumers();

        // units come first
        for (int index = 0; index < units; index++) {
            assertTrue(consumers.get(index).toString(),
                       consumers.get(index) instanceof Unit);
        }
        // buildings come next
        for (int index = units; index < units + buildings; index++) {
            assertTrue(consumers.get(index).toString(),
                       consumers.get(index) instanceof Building);
        }
        // build and population queues come last
        for (int index = units + buildings; index < units + buildings + 2; index++) {
            assertTrue(consumers.get(index).toString(),
                       consumers.get(index) instanceof BuildQueue);
        }

        Building country = colony.getBuilding(countryType);
        assertTrue(consumers.contains(country));

        Building depot = colony.getBuilding(depotType);
        assertTrue(consumers.contains(depot));

        int countryIndex = consumers.indexOf(country);
        int depotIndex = consumers.indexOf(depot);
        assertTrue(countryIndex >= 0);
        assertTrue(depotIndex >= 0);
        assertTrue("Priority of depot should be higher than that of country",
                   depotIndex < countryIndex);

        BuildingType armoryType = spec().getBuildingType("model.building.armory");
        Building armory = new ServerBuilding(getGame(), colony, armoryType);
        colony.addBuilding(armory);
        consumers = colony.getConsumers();

        // units come first
        for (int index = 0; index < units; index++) {
            assertTrue(consumers.get(index).toString(),
                       consumers.get(index) instanceof Unit);
        }
        int offset = units + buildings;
        // buildings come next
        for (int index = units; index < offset; index++) {
            assertTrue(consumers.get(index).toString(),
                       consumers.get(index) instanceof Building);
        }
        // build queue comes last
        assertTrue(consumers.get(offset).toString(),
                   consumers.get(offset) instanceof BuildQueue);
        // armory has a lower priority than the build queue
        assertTrue(consumers.get(offset + 1).toString(),
                   consumers.get(offset + 1) instanceof Building);
        assertEquals(armoryType, ((Building) consumers.get(offset + 1)).getType());
        // population queue comes last
        assertTrue(consumers.get(offset + 2).toString(),
                   consumers.get(offset + 2) instanceof BuildQueue);
    }

    public void testProductionMap() {
        ProductionMap pm = new ProductionMap();

        pm.add(new AbstractGoods(cottonType, 33));
        assertEquals(33, pm.get(cottonType).getAmount());

        pm.add(new AbstractGoods(grainType, 44));
        assertEquals(44, pm.get(grainType).getAmount());
        assertEquals(44, pm.get(foodType).getAmount());

        pm.remove(new AbstractGoods(grainType, 22));
        assertEquals(22, pm.get(grainType).getAmount());
        assertEquals(22, pm.get(foodType).getAmount());

        pm.remove(new AbstractGoods(foodType, 11));
        assertEquals(11, pm.get(grainType).getAmount());
        assertEquals(11, pm.get(foodType).getAmount());
    }

    public void testProduction() {
        Game game = getGame();
        game.setMap(getTestMap());

        Colony colony = getStandardColony(3);
        ColonyTile tile = colony.getColonyTile(colony.getTile());

        assertEquals(0, colony.getGoodsCount(foodType));
        assertEquals(grainType, tile.getProduction().get(0).getType());
        assertEquals(5, tile.getProduction().get(0).getAmount());
        assertEquals(cottonType, tile.getProduction().get(1).getType());
        assertEquals(2, tile.getProduction().get(1).getAmount());

        for (Unit unit : colony.getUnitList()) {
            ProductionInfo unitInfo = colony.getProductionInfo(unit);
            assertNotNull(unitInfo);
            assertEquals(2, unitInfo.getConsumption().size());
            assertEquals(2, unitInfo.getMaximumConsumption().size());
            ProductionInfo pi = colony.getProductionInfo(unit.getLocation());
            if (unit.getLocation() instanceof ColonyTile) {
                // Producing grain to satisfy food demand
                assertEquals(1, pi.getProduction().size());
                assertEquals(grainType, pi.getProduction().get(0).getType());
                assertEquals(5, pi.getProduction().get(0).getAmount());
            } else {
                // Producing bells to satisfy bells demand
                assertEquals(1, pi.getProduction().size());
                assertEquals(bellsType, pi.getProduction().get(0).getType());
                assertEquals(3, pi.getProduction().get(0).getAmount());
            }
        }

        /*
        TypeCountMap<GoodsType> grossProduction = new TypeCountMap<GoodsType>();
        TypeCountMap<GoodsType> netProduction = new TypeCountMap<GoodsType>();
        for (ProductionInfo productionInfo : info.values()) {
            for (AbstractGoods goods : productionInfo.getProduction()) {
                grossProduction.incrementCount(goods.getType(), goods.getAmount());
                netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
            }
            for (AbstractGoods goods : productionInfo.getStorage()) {
                grossProduction.incrementCount(goods.getType(), goods.getAmount());
                netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
            }
            for (AbstractGoods goods : productionInfo.getConsumption()) {
                netProduction.incrementCount(goods.getType().getStoredAs(), -goods.getAmount());
            }
        }

        assertEquals(2, grossProduction.getCount(cottonType));
        assertEquals(2, colony.getNetProductionOf(cottonType));

        assertEquals(20, grossProduction.getCount(grainType));
        assertEquals(0, colony.getNetProductionOf(grainType));

        assertEquals(3, grossProduction.getCount(bellsType));
        assertEquals(0, colony.getNetProductionOf(bellsType));

        assertEquals(1, grossProduction.getCount(crossesType));
        assertEquals(1, colony.getNetProductionOf(crossesType));

        // this is storage only
        assertEquals(7, grossProduction.getCount(foodType));
        // this includes implicit type change and consumption
        assertEquals(14, colony.getNetProductionOf(foodType));

        colony.addGoods(horsesType, 50);
        colony.getUnitList().get(0).changeWorkType(cottonType);
        Building weaverHouse = colony.getBuilding(spec().getBuildingType("model.building.weaverHouse"));
        colony.getUnitList().get(1).setLocation(weaverHouse);

        info = colony.getProductionAndConsumption();

        assertEquals(grainType, tile.getProduction().get(0).getType());
        assertEquals(5, tile.getProduction().get(0).getAmount());
        assertEquals(cottonType, tile.getProduction().get(1).getType());
        assertEquals(2, tile.getProduction().get(1).getAmount());

        grossProduction = new TypeCountMap<GoodsType>();
        netProduction = new TypeCountMap<GoodsType>();
        for (ProductionInfo productionInfo : info.values()) {
            for (AbstractGoods goods : productionInfo.getProduction()) {
                grossProduction.incrementCount(goods.getType(), goods.getAmount());
                netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
            }
            for (AbstractGoods goods : productionInfo.getStorage()) {
                grossProduction.incrementCount(goods.getType(), goods.getAmount());
                netProduction.incrementCount(goods.getType().getStoredAs(), goods.getAmount());
            }
            for (AbstractGoods goods : productionInfo.getConsumption()) {
                netProduction.incrementCount(goods.getType().getStoredAs(), -goods.getAmount());
            }
        }

        assertEquals(4, grossProduction.getCount(cottonType));
        assertEquals(1, colony.getNetProductionOf(cottonType));

        assertEquals(3, grossProduction.getCount(clothType));
        assertEquals(3, colony.getNetProductionOf(clothType));

        assertEquals(10, grossProduction.getCount(grainType));
        assertEquals(0, colony.getNetProductionOf(grainType));

        assertEquals(2, grossProduction.getCount(horsesType));
        assertEquals(2, colony.getNetProductionOf(horsesType));

        assertEquals(3, grossProduction.getCount(bellsType));
        assertEquals(0, colony.getNetProductionOf(bellsType));

        assertEquals(1, grossProduction.getCount(crossesType));
        assertEquals(1, colony.getNetProductionOf(crossesType));

        // this is storage only
        assertEquals(2, grossProduction.getCount(foodType));
        // this includes implicit type change and consumption
        assertEquals(2, colony.getNetProductionOf(foodType));

        */
    }

    public void testGetPotentialProduction() {
        Game game = getGame();
        game.setMap(getTestMap());

        Colony colony = getStandardColony(1);
        ColonyTile colonyTile = colony.getColonyTile(colony.getTile());
        assertNotNull(colonyTile);
        assertEquals(plainsType, colony.getTile().getType());
        Building townHall = colony.getBuilding(townHallType);
        assertNotNull(townHall);
        assertTrue(townHall.isEmpty());
        UnitType colonistType = spec().getDefaultUnitType(colony.getOwner());
        assertNotNull(colonistType);

        assertEquals("Zero potential production of cotton in town hall", 0,
            townHall.getPotentialProduction(cottonType, null));
        assertEquals("Basic potential production of bells in town hall",
            townHallType.getBaseProduction(null, bellsType, null),
            townHall.getPotentialProduction(bellsType, null));
        assertEquals("Unit potential production of bells in town hall",
            townHallType.getBaseProduction(null, bellsType, colonistType),
            townHall.getPotentialProduction(bellsType, colonistType));

        assertEquals("Zero potential production of bells on center tile", 0,
            colonyTile.getPotentialProduction(bellsType, null));
        assertEquals("Basic potential production of cotton on center tile",
            plainsType.getBaseProduction(null, cottonType, null),
            colonyTile.getPotentialProduction(cottonType, null));
        assertEquals("Zero unit potential production of cotton on center tile",
            0,
            colonyTile.getPotentialProduction(cottonType, colonistType));
    }
}
