/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonyProductionTest extends FreeColTestCase {

    public void testProductionSoldier() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, spec().getTileType("model.tile.plains"), x, y);
            }
        }

        Map map = new Map(game, tiles);

        Resource grain = new Resource(game, map.getTile(5, 8),
                                      spec().getResourceType("model.resource.grain"));
        map.getTile(5, 8).addResource(grain);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);

        game.setMap(map);
        UnitType veteran = spec().getUnitType("model.unit.veteranSoldier");
        Unit soldier = new ServerUnit(game, map.getTile(6, 8), dutch, veteran, UnitState.ACTIVE, veteran.getDefaultEquipment());

        Colony colony = new ServerColony(game, dutch, "New Amsterdam", soldier.getTile());
        GoodsType foodType = spec().getGoodsType("model.goods.grain");
        soldier.setWorkType(foodType);
        nonServerBuildColony(soldier, colony);

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

        // Test the state of the soldier
        // Soldier should be working on the field with the bonus

        assertEquals(foodType, soldier.getWorkType());

        assertEquals(colony.getColonyTile(map.getTile(5,8)).getTile(), soldier.getLocation().getTile());

        assertEquals(0, soldier.getMovesLeft());

        //assertEquals(false, soldier.isArmed());
    }

    public void testProductionPioneer() {

        Game game = getStandardGame();

        Player dutch = game.getPlayer("model.nation.dutch");

        Tile[][] tiles = new Tile[10][15];

        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 15; y++) {
                tiles[x][y] = new Tile(game, spec().getTileType("model.tile.plains"), x, y);
            }
        }

        Map map = new Map(game, tiles);

        Resource grain = new Resource(game, map.getTile(5, 8),
                                      spec().getResourceType("model.resource.grain"));
        map.getTile(5, 8).addResource(grain);
        map.getTile(5, 8).setExploredBy(dutch, true);
        map.getTile(6, 8).setExploredBy(dutch, true);

        game.setMap(map);
        UnitType pioneerType = spec().getUnitType("model.unit.hardyPioneer");
        GoodsType foodType = spec().getGoodsType("model.goods.grain");
        Unit pioneer = new ServerUnit(game, map.getTile(6, 8), dutch,
                                      pioneerType, UnitState.ACTIVE,
                                      pioneerType.getDefaultEquipment());

        Colony colony = new ServerColony(game, dutch, "New Amsterdam", pioneer.getTile());
        pioneer.setWorkType(foodType);
        nonServerBuildColony(pioneer, colony);

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

        // Test the state of the pioneer
        // Pioneer should be working on the field with the bonus
        assertEquals(foodType, pioneer.getWorkType());

        assertEquals(colony.getColonyTile(map.getTile(5,8)).getTile(), pioneer.getLocation().getTile());

        assertEquals(0, pioneer.getMovesLeft());

        //assertEquals(false, pioneer.isArmed());
    }

    public void testBellNetProduction(){
    	GoodsType bellsType = spec().getGoodsType("model.goods.bells");

    	Game game = getStandardGame();

    	game.setMap(getTestMap());

    	Colony colony = getStandardColony(7);

    	int initialBellCount = colony.getGoodsCount(bellsType);
    	int expectedBellCount = 0;
    	int bellsProdPerTurn = colony.getProductionOf(bellsType);
    	int expectedBellProd = 1;
    	int bellsUpkeep = colony.getConsumptionOf(bellsType);
    	int expectedBellUpkeep =  colony.getUnitCount() - 2;
    	int bellsNetProdPerTurn = colony.getNetProduction().getCount(bellsType);
    	int expectedBellNetProd = expectedBellProd - expectedBellUpkeep;

    	assertEquals("Wrong bell count", expectedBellCount, initialBellCount);
    	assertEquals("Wrong bell production",expectedBellProd,bellsProdPerTurn);
    	assertEquals("Wrong bell upkeep",expectedBellUpkeep,bellsUpkeep);
    	assertEquals("Wrong bell net production",expectedBellNetProd,bellsNetProdPerTurn);
    }
    /**
     * Tests that there is no over production of horses, to avoid being thrown out
     * A test of the proper production of horses is in <code>BuildingTest</code>
     */
    public void testNoHorsesOverProduction() {
        Game game = getGame();
        game.setMap(getTestMap());

        Colony colony = getStandardColony(1);
        GoodsType foodType = spec().getGoodsType("model.goods.food");
        GoodsType horsesType = spec().getGoodsType("model.goods.horses");

        Building pasture = colony.getBuilding(spec().getBuildingType("model.building.country"));
        assertEquals(horsesType, pasture.getGoodsOutputType());
        assertEquals("Wrong warehouse capacity in colony",100,colony.getWarehouseCapacity());

        // Still room for more
        colony.addGoods(horsesType, 99);
        TypeCountMap<GoodsType> netProduction = colony.getNetProduction();
        assertTrue(netProduction.getCount(foodType) > 0);

        assertEquals("Wrong horse production",1, pasture.getProductionOf(horsesType));
        assertEquals("Wrong maximum horse production", 4, pasture.getMaximumProduction());
        assertEquals("Wrong net horse production",1, netProduction.getCount(horsesType));

        // No more room available
        colony.addGoods(horsesType, 1);
        netProduction = colony.getNetProduction();
        assertEquals("Wrong number of horses in colony",colony.getWarehouseCapacity(), colony.getGoodsCount(horsesType));
        assertEquals("Wrong horse production",0, pasture.getProductionOf(horsesType));
        assertEquals("Wrong maximum horse production", 4, pasture.getMaximumProduction());
        assertEquals("Wrong net horse production",0, netProduction.getCount(horsesType));
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

        BuildingType countryType = spec().getBuildingType("model.building.country");
        Building country = colony.getBuilding(countryType);
        assertTrue(consumers.contains(country));

        BuildingType depotType = spec().getBuildingType("model.building.depot");
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

        GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        GoodsType foodType = spec().getGoodsType("model.goods.food");
        GoodsType grainType = spec().getGoodsType("model.goods.grain");

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

        GoodsType foodType = spec().getGoodsType("model.goods.food");
        GoodsType grainType = spec().getGoodsType("model.goods.grain");
        GoodsType clothType = spec().getGoodsType("model.goods.cloth");
        GoodsType bellsType = spec().getGoodsType("model.goods.bells");
        GoodsType cottonType = spec().getGoodsType("model.goods.cotton");
        GoodsType horsesType = spec().getGoodsType("model.goods.horses");
        GoodsType crossesType = spec().getGoodsType("model.goods.crosses");

        assertEquals(0, colony.getGoodsCount(foodType));

        java.util.Map<Object, ProductionInfo> info = colony.getProductionAndConsumption();

        assertEquals(grainType, tile.getProduction().get(0).getType());
        assertEquals(5, tile.getProduction().get(0).getAmount());
        assertEquals(cottonType, tile.getProduction().get(1).getType());
        assertEquals(2, tile.getProduction().get(1).getAmount());

        for (Unit unit : colony.getUnitList()) {
            ProductionInfo unitInfo = info.get(unit);
            assertNotNull(unitInfo);
            assertEquals(2, unitInfo.getConsumption().size());
            assertEquals(2, unitInfo.getMaximumConsumption().size());
            ProductionInfo tileInfo = info.get(unit.getLocation());
            assertEquals(1, tileInfo.getProduction().size());
            assertEquals(grainType, tileInfo.getProduction().get(0).getType());
            assertEquals(5, tileInfo.getProduction().get(0).getAmount());
        }

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
        assertEquals(2, netProduction.getCount(cottonType));

        assertEquals(20, grossProduction.getCount(grainType));
        assertEquals(0, netProduction.getCount(grainType));

        assertEquals(3, grossProduction.getCount(bellsType));
        assertEquals(0, netProduction.getCount(bellsType));

        assertEquals(1, grossProduction.getCount(crossesType));
        assertEquals(1, netProduction.getCount(crossesType));

        // this is storage only
        assertEquals(7, grossProduction.getCount(foodType));
        // this includes implicit type change and consumption
        assertEquals(14, netProduction.getCount(foodType));

        colony.addGoods(horsesType, 50);
        colony.getUnitList().get(0).setWorkType(cottonType);
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
        assertEquals(1, netProduction.getCount(cottonType));

        assertEquals(3, grossProduction.getCount(clothType));
        assertEquals(3, netProduction.getCount(clothType));

        assertEquals(10, grossProduction.getCount(grainType));
        assertEquals(0, netProduction.getCount(grainType));

        assertEquals(2, grossProduction.getCount(horsesType));
        assertEquals(2, netProduction.getCount(horsesType));

        assertEquals(3, grossProduction.getCount(bellsType));
        assertEquals(0, netProduction.getCount(bellsType));

        assertEquals(1, grossProduction.getCount(crossesType));
        assertEquals(1, netProduction.getCount(crossesType));

        // this is storage only
        assertEquals(2, grossProduction.getCount(foodType));
        // this includes implicit type change and consumption
        assertEquals(2, netProduction.getCount(foodType));
    }

}
