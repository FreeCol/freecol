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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.util.test.FreeColTestCase;


public class ProductionTypeTest extends FreeColTestCase {

    private static final Specification s = spec("classic");

    private static final BuildingType armory
        = s.getBuildingType("model.building.armory");
    private static final BuildingType arsenal
        = s.getBuildingType("model.building.arsenal");
    private static final BuildingType blacksmithHouse
        = s.getBuildingType("model.building.blacksmithHouse");
    private static final BuildingType blacksmithShop
        = s.getBuildingType("model.building.blacksmithShop");
    private static final BuildingType carpenterHouse
        = s.getBuildingType("model.building.carpenterHouse");
    private static final BuildingType cathedral
        = s.getBuildingType("model.building.cathedral");
    private static final BuildingType chapel
        = s.getBuildingType("model.building.chapel");
    private static final BuildingType church
        = s.getBuildingType("model.building.church");
    private static final BuildingType cigarFactory
        = s.getBuildingType("model.building.cigarFactory");
    private static final BuildingType college
        = s.getBuildingType("model.building.college");
    private static final BuildingType country
        = s.getBuildingType("model.building.country");
    private static final BuildingType customHouse
        = s.getBuildingType("model.building.customHouse");
    private static final BuildingType depot
        = s.getBuildingType("model.building.depot");
    private static final BuildingType distillerHouse
        = s.getBuildingType("model.building.distillerHouse");
    private static final BuildingType docks
        = s.getBuildingType("model.building.docks");
    private static final BuildingType drydock
        = s.getBuildingType("model.building.drydock");
    private static final BuildingType fort
        = s.getBuildingType("model.building.fort");
    private static final BuildingType fortress
        = s.getBuildingType("model.building.fortress");
    private static final BuildingType furFactory
        = s.getBuildingType("model.building.furFactory");
    private static final BuildingType furTraderHouse
        = s.getBuildingType("model.building.furTraderHouse");
    private static final BuildingType furTradingPost
        = s.getBuildingType("model.building.furTradingPost");
    private static final BuildingType ironWorks
        = s.getBuildingType("model.building.ironWorks");
    private static final BuildingType lumberMill
        = s.getBuildingType("model.building.lumberMill");
    private static final BuildingType magazine
        = s.getBuildingType("model.building.magazine");
    private static final BuildingType newspaper
        = s.getBuildingType("model.building.newspaper");
    private static final BuildingType printingPress
        = s.getBuildingType("model.building.printingPress");
    private static final BuildingType rumDistillery
        = s.getBuildingType("model.building.rumDistillery");
    private static final BuildingType rumFactory
        = s.getBuildingType("model.building.rumFactory");
    private static final BuildingType schoolhouse
        = s.getBuildingType("model.building.schoolhouse");
    private static final BuildingType shipyard
        = s.getBuildingType("model.building.shipyard");
    private static final BuildingType stables
        = s.getBuildingType("model.building.stables");
    private static final BuildingType stockade
        = s.getBuildingType("model.building.stockade");
    private static final BuildingType textileMill
        = s.getBuildingType("model.building.textileMill");
    private static final BuildingType tobacconistHouse
        = s.getBuildingType("model.building.tobacconistHouse");
    private static final BuildingType tobacconistShop
        = s.getBuildingType("model.building.tobacconistShop");
    private static final BuildingType townHall
        = s.getBuildingType("model.building.townHall");
    private static final BuildingType university
        = s.getBuildingType("model.building.university");
    private static final BuildingType warehouse
        = s.getBuildingType("model.building.warehouse");
    private static final BuildingType warehouseExpansion
        = s.getBuildingType("model.building.warehouseExpansion");
    private static final BuildingType weaverHouse
        = s.getBuildingType("model.building.weaverHouse");
    private static final BuildingType weaverShop
        = s.getBuildingType("model.building.weaverShop");

    private static final GoodsType bells
        = s.getGoodsType("model.goods.bells");
    private static final GoodsType cigars
        = s.getGoodsType("model.goods.cigars");
    private static final GoodsType cloth
        = s.getGoodsType("model.goods.cloth");
    private static final GoodsType coats
        = s.getGoodsType("model.goods.coats");
    private static final GoodsType cotton
        = s.getGoodsType("model.goods.cotton");
    private static final GoodsType crosses
        = s.getGoodsType("model.goods.crosses");
    private static final GoodsType fish
        = s.getGoodsType("model.goods.fish");
    private static final GoodsType food
        = s.getGoodsType("model.goods.food");
    private static final GoodsType furs
        = s.getGoodsType("model.goods.furs");
    private static final GoodsType grain
        = s.getGoodsType("model.goods.grain");
    private static final GoodsType hammers
        = s.getGoodsType("model.goods.hammers");
    private static final GoodsType horses
        = s.getGoodsType("model.goods.horses");
    private static final GoodsType lumber
        = s.getGoodsType("model.goods.lumber");
    private static final GoodsType muskets
        = s.getGoodsType("model.goods.muskets");
    private static final GoodsType ore
        = s.getGoodsType("model.goods.ore");
    private static final GoodsType rum
        = s.getGoodsType("model.goods.rum");
    private static final GoodsType tobacco
        = s.getGoodsType("model.goods.tobacco");
    private static final GoodsType tools
        = s.getGoodsType("model.goods.tools");
    private static final GoodsType silver
        = s.getGoodsType("model.goods.silver");
    private static final GoodsType sugar
        = s.getGoodsType("model.goods.sugar");

    private static final TileType arctic
        = s.getTileType("model.tile.arctic");
    private static final TileType borealForest
        = s.getTileType("model.tile.borealForest");
    private static final TileType broadleafForest
        = s.getTileType("model.tile.broadleafForest");
    private static final TileType coniferForest
        = s.getTileType("model.tile.coniferForest");
    private static final TileType desert
        = s.getTileType("model.tile.desert");
    private static final TileType grassland
        = s.getTileType("model.tile.grassland");
    private static final TileType greatRiver
        = s.getTileType("model.tile.greatRiver");
    private static final TileType highSeas
        = s.getTileType("model.tile.highSeas");
    private static final TileType hills
        = s.getTileType("model.tile.hills");
    private static final TileType lake
        = s.getTileType("model.tile.lake");
    private static final TileType marsh
        = s.getTileType("model.tile.marsh");
    private static final TileType mixedForest
        = s.getTileType("model.tile.mixedForest");
    private static final TileType mountains
        = s.getTileType("model.tile.mountains");
    private static final TileType ocean
        = s.getTileType("model.tile.ocean");
    private static final TileType plains
        = s.getTileType("model.tile.plains");
    private static final TileType prairie
        = s.getTileType("model.tile.prairie");
    private static final TileType rainForest
        = s.getTileType("model.tile.rainForest");
    private static final TileType savannah
        = s.getTileType("model.tile.savannah");
    private static final TileType scrubForest
        = s.getTileType("model.tile.scrubForest");
    private static final TileType swamp
        = s.getTileType("model.tile.swamp");
    private static final TileType tropicalForest
        = s.getTileType("model.tile.tropicalForest");
    private static final TileType tundra
        = s.getTileType("model.tile.tundra");
    private static final TileType wetlandForest
        = s.getTileType("model.tile.wetlandForest");

    private static final UnitType colonistType = s.getDefaultUnitType();

    private Map<GoodsType, Integer> inputs = new HashMap<>();
    private Map<GoodsType, Integer> outputs = new HashMap<>();


    private void testProduction(Map<GoodsType, Integer> inputs,
                                Map<GoodsType, Integer> outputs,
                                List<ProductionType> productionTypes) {
        for (ProductionType productionType : productionTypes) {
            for (AbstractGoods ag : productionType.getInputs()) {
                Integer i = inputs.get(ag.getType());
                assertNotNull("Input expected for " + ag.getType(), i);
                assertEquals("Input amount mismatch for " + ag.getType(),
                    i.intValue(), ag.getAmount());
                inputs.remove(ag.getType());
            }
            for (AbstractGoods ag : productionType.getOutputs()) {
                Integer i = outputs.get(ag.getType());
                assertNotNull("Output expected for " + ag.getType(), i);
                assertEquals("Output amount mismatch for " + ag.getType(),
                    i.intValue(), ag.getAmount());
                outputs.remove(ag.getType());
            }
        }
        assertEquals("Input remaining", 0, inputs.size());
        assertEquals("Output remaining", 0, outputs.size());
    }

    private int getGenericPotential(TileType tileType, GoodsType goodsType) {
        return tileType.getPotentialProduction(goodsType, colonistType);
    }


    public void testArctic() {
        outputs.put(grain, 2);
        testProduction(inputs, outputs,
                       arctic.getAvailableProductionTypes(true, "veryHigh"));
        testProduction(inputs, outputs,
                       arctic.getAvailableProductionTypes(false, "veryHigh"));

        outputs.put(grain, 1);
        testProduction(inputs, outputs, 
                       arctic.getAvailableProductionTypes(true, "high"));
        testProduction(inputs, outputs,
                       arctic.getAvailableProductionTypes(false, "high"));

        for (String level : new String[] { "medium", "low", "veryLow" }) {
            testProduction(inputs, outputs,
                           arctic.getAvailableProductionTypes(true, level));
            testProduction(inputs, outputs,
                           arctic.getAvailableProductionTypes(false, level));
        }

        assertEquals(0, arctic.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(arctic, grain));
    }

    public void testBorealForest() {
        outputs.put(grain, 2);
        outputs.put(furs, 3);
        testProduction(inputs, outputs,
                       borealForest.getAvailableProductionTypes(true));

        outputs.put(grain, 2);
        outputs.put(furs, 3);
        outputs.put(lumber, 4);
        outputs.put(ore, 1);
        testProduction(inputs, outputs,
                       borealForest.getAvailableProductionTypes(false));

        assertEquals(2, borealForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(borealForest, grain));
    }

    public void testBroadleafForest() {
        outputs.put(grain, 2);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       broadleafForest.getAvailableProductionTypes(true));

        outputs.put(grain, 2);
        outputs.put(cotton, 1);
        outputs.put(furs, 2);
        outputs.put(lumber, 4);
        testProduction(inputs, outputs,
                       broadleafForest.getAvailableProductionTypes(false));

        assertEquals(2, broadleafForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(broadleafForest, grain));
    }

    public void testConiferForest() {
        outputs.put(grain, 2);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       coniferForest.getAvailableProductionTypes(true));

        outputs.put(grain, 2);
        outputs.put(tobacco, 1);
        outputs.put(furs, 2);
        outputs.put(lumber, 6);
        testProduction(inputs, outputs,
                       coniferForest.getAvailableProductionTypes(false));

        assertEquals(2, coniferForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(coniferForest, grain));
    }

    public void testDesert() {
        outputs.put(grain, 3);
        outputs.put(cotton, 1);
        testProduction(inputs, outputs,
                       desert.getAvailableProductionTypes(true, "veryHigh"));

        outputs.put(grain, 2);
        outputs.put(cotton, 1);
        testProduction(inputs, outputs,
                       desert.getAvailableProductionTypes(true));

        outputs.put(grain, 1);
        outputs.put(cotton, 1);
        testProduction(inputs, outputs,
                       desert.getAvailableProductionTypes(true, "veryLow"));

        outputs.put(grain, 2);
        outputs.put(cotton, 1);
        outputs.put(ore, 2);
        testProduction(inputs, outputs,
                       desert.getAvailableProductionTypes(false));

        assertEquals(2, desert.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(desert, grain));
    }

    public void testGrassland() {
        outputs.put(grain, 3);
        outputs.put(tobacco, 3);
        testProduction(inputs, outputs,
                       grassland.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(tobacco, 3);
        testProduction(inputs, outputs,
                       grassland.getAvailableProductionTypes(false));

        assertEquals(3, grassland.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(grassland, grain));
    }

    public void testGreatRiver() {
        testProduction(inputs, outputs,
                       greatRiver.getAvailableProductionTypes(true));

        outputs.put(fish, 2);
        testProduction(inputs, outputs,
                       greatRiver.getAvailableProductionTypes(false));

        assertEquals(0, greatRiver.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(greatRiver, grain));
    }

    public void testHighSeas() {
        testProduction(inputs, outputs,
                       highSeas.getAvailableProductionTypes(true));

        outputs.put(fish, 2);
        testProduction(inputs, outputs,
                       highSeas.getAvailableProductionTypes(false));

        assertEquals(0, highSeas.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(highSeas, grain));
    }

    public void testHills() {
        outputs.put(grain, 2);
        outputs.put(ore, 4);
        testProduction(inputs, outputs,
                       hills.getAvailableProductionTypes(true));

        outputs.put(grain, 2);
        outputs.put(ore, 4);
        testProduction(inputs, outputs,
                       hills.getAvailableProductionTypes(false));

        assertEquals(2, hills.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(hills, grain));
    }

    public void testLake() {
        testProduction(inputs, outputs,
                       lake.getAvailableProductionTypes(true));

        outputs.put(fish, 2);
        testProduction(inputs, outputs,
                       lake.getAvailableProductionTypes(false));

        assertEquals(0, lake.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(lake, grain));
    }

    public void testMarsh() {
        outputs.put(grain, 3);
        outputs.put(tobacco, 2);
        testProduction(inputs, outputs,
                       marsh.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(tobacco, 2);
        outputs.put(ore, 2);
        outputs.put(silver, 0);
        testProduction(inputs, outputs,
                       marsh.getAvailableProductionTypes(false));

        assertEquals(3, marsh.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(marsh, grain));
    }

    public void testMixedForest() {
        outputs.put(grain, 3);
        outputs.put(furs, 3);
        testProduction(inputs, outputs,
                       mixedForest.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(cotton, 1);
        outputs.put(furs, 3);
        outputs.put(lumber, 6);
        testProduction(inputs, outputs,
                       mixedForest.getAvailableProductionTypes(false));

        assertEquals(3, mixedForest.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(mixedForest, grain));
    }

    public void testMountains() {
        testProduction(inputs, outputs,
                       mountains.getAvailableProductionTypes(true));

        outputs.put(ore, 4);
        outputs.put(silver, 1);
        testProduction(inputs, outputs,
                       mountains.getAvailableProductionTypes(false));

        assertEquals(0, mountains.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(mountains, grain));
    }

    public void testOcean() {
        testProduction(inputs, outputs,
                       ocean.getAvailableProductionTypes(true));

        outputs.put(fish, 2);
        testProduction(inputs, outputs,
                       ocean.getAvailableProductionTypes(false));

        assertEquals(0, ocean.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(ocean, grain));
    }

    public void testPlains() {
        outputs.put(grain, 5);
        outputs.put(cotton, 2);
        testProduction(inputs, outputs,
                       plains.getAvailableProductionTypes(true));

        outputs.put(grain, 5);
        outputs.put(cotton, 2);
        outputs.put(ore, 1);
        testProduction(inputs, outputs,
                       plains.getAvailableProductionTypes(false));

        assertEquals(5, plains.getPotentialProduction(grain, null));
        assertEquals(5, getGenericPotential(plains, grain));
    }

    public void testPrairie() {
        outputs.put(grain, 3);
        outputs.put(cotton, 3);
        testProduction(inputs, outputs,
                       prairie.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(cotton, 3);
        testProduction(inputs, outputs,
                       prairie.getAvailableProductionTypes(false));

        assertEquals(3, prairie.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(prairie, grain));
    }

    public void testRainForest() {
        outputs.put(grain, 2);
        outputs.put(furs, 1);
        testProduction(inputs, outputs,
                       rainForest.getAvailableProductionTypes(true));

        outputs.put(grain, 2);
        outputs.put(sugar, 1);
        outputs.put(furs, 1);
        outputs.put(lumber, 4);
        outputs.put(ore, 1);
        outputs.put(silver, 0);
        testProduction(inputs, outputs,
                       rainForest.getAvailableProductionTypes(false));

        assertEquals(2, rainForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(rainForest, grain));
    }

    public void testSavannah() {
        outputs.put(grain, 4);
        outputs.put(sugar, 3);
        testProduction(inputs, outputs,
                       savannah.getAvailableProductionTypes(true));

        outputs.put(grain, 4);
        outputs.put(sugar, 3);
        testProduction(inputs, outputs,
                       savannah.getAvailableProductionTypes(false));

        assertEquals(4, savannah.getPotentialProduction(grain, null));
        assertEquals(4, getGenericPotential(savannah, grain));
    }

    public void testScrubForest() {
        outputs.put(grain, 3);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       scrubForest.getAvailableProductionTypes(true, "veryHigh"));

        outputs.put(grain, 2);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       scrubForest.getAvailableProductionTypes(true));

        outputs.put(grain, 1);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       scrubForest.getAvailableProductionTypes(true, "veryLow"));

        outputs.put(grain, 2);
        outputs.put(cotton, 1);
        outputs.put(furs, 2);
        outputs.put(lumber, 2);
        outputs.put(ore, 1);
        testProduction(inputs, outputs,
                       scrubForest.getAvailableProductionTypes(false));

        assertEquals(2, scrubForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(scrubForest, grain));
    }

    public void testSwamp() {
        outputs.put(grain, 3);
        outputs.put(sugar, 2);
        testProduction(inputs, outputs,
                       swamp.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(sugar, 2);
        outputs.put(ore, 2);
        outputs.put(silver, 0);
        testProduction(inputs, outputs,
                       swamp.getAvailableProductionTypes(false));

        assertEquals(3, swamp.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(swamp, grain));
    }

    public void testTropicalForest() {
        outputs.put(grain, 3);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       tropicalForest.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(sugar, 1);
        outputs.put(furs, 2);
        outputs.put(lumber, 4);
        testProduction(inputs, outputs,
                       tropicalForest.getAvailableProductionTypes(false));

        assertEquals(3, tropicalForest.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(tropicalForest, grain));
    }

    public void testTundra() {
        outputs.put(grain, 3);
        outputs.put(ore, 2);
        testProduction(inputs, outputs,
                       tundra.getAvailableProductionTypes(true));

        outputs.put(grain, 3);
        outputs.put(ore, 2);
        outputs.put(silver, 0);
        testProduction(inputs, outputs,
                       tundra.getAvailableProductionTypes(false));

        assertEquals(3, tundra.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(tundra, grain));
    }

    public void testWetlandForest() {
        outputs.put(grain, 2);
        outputs.put(furs, 2);
        testProduction(inputs, outputs,
                       wetlandForest.getAvailableProductionTypes(true));

        outputs.put(grain, 2);
        outputs.put(tobacco, 1);
        outputs.put(furs, 2);
        outputs.put(lumber, 4);
        outputs.put(ore, 1);
        outputs.put(silver, 0);
        testProduction(inputs, outputs,
                       wetlandForest.getAvailableProductionTypes(false));

        assertEquals(2, wetlandForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(wetlandForest, grain));
    }

    public void testBellProduction() {
        outputs.put(bells, 1);
        testProduction(inputs, outputs,
                       townHall.getAvailableProductionTypes(true));

        outputs.put(bells, 3);
        testProduction(inputs, outputs,
                       townHall.getAvailableProductionTypes(false));
    }        

    public void testHammerProduction() {
        testProduction(inputs, outputs,
                       carpenterHouse.getAvailableProductionTypes(true));

        inputs.put(lumber, 3);
        outputs.put(hammers, 3);
        testProduction(inputs, outputs,
                       carpenterHouse.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       lumberMill.getAvailableProductionTypes(true));

        inputs.put(lumber, 6);
        outputs.put(hammers, 6);
        testProduction(inputs, outputs,
                       lumberMill.getAvailableProductionTypes(false));
    }        

    public void testToolsProduction() {
        testProduction(inputs, outputs,
                       blacksmithHouse.getAvailableProductionTypes(true));

        inputs.put(ore, 3);
        outputs.put(tools, 3);
        testProduction(inputs, outputs,
                       blacksmithHouse.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       blacksmithShop.getAvailableProductionTypes(true));

        inputs.put(ore, 6);
        outputs.put(tools, 6);
        testProduction(inputs, outputs,
                       blacksmithShop.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       ironWorks.getAvailableProductionTypes(true));

        inputs.put(ore, 6);
        outputs.put(tools, 9);
        testProduction(inputs, outputs,
                       ironWorks.getAvailableProductionTypes(false));
    }

    public void testCigarProduction() {
        testProduction(inputs, outputs,
                       tobacconistHouse.getAvailableProductionTypes(true));

        inputs.put(tobacco, 3);
        outputs.put(cigars, 3);
        testProduction(inputs, outputs,
                       tobacconistHouse.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       tobacconistShop.getAvailableProductionTypes(true));

        inputs.put(tobacco, 6);
        outputs.put(cigars, 6);
        testProduction(inputs, outputs,
                       tobacconistShop.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       cigarFactory.getAvailableProductionTypes(true));

        inputs.put(tobacco, 6);
        outputs.put(cigars, 9);
        testProduction(inputs, outputs,
                       cigarFactory.getAvailableProductionTypes(false));
    }

    public void testClothProduction() {
        testProduction(inputs, outputs,
                       weaverHouse.getAvailableProductionTypes(true));

        inputs.put(cotton, 3);
        outputs.put(cloth, 3);
        testProduction(inputs, outputs,
                       weaverHouse.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       weaverShop.getAvailableProductionTypes(true));

        inputs.put(cotton, 6);
        outputs.put(cloth, 6);
        testProduction(inputs, outputs,
                       weaverShop.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       textileMill.getAvailableProductionTypes(true));

        inputs.put(cotton, 6);
        outputs.put(cloth, 9);
        testProduction(inputs, outputs,
                       textileMill.getAvailableProductionTypes(false));
    }

    public void testRumProduction() {
        testProduction(inputs, outputs,
                       distillerHouse.getAvailableProductionTypes(true));

        inputs.put(sugar, 3);
        outputs.put(rum, 3);
        testProduction(inputs, outputs,
                       distillerHouse.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       rumDistillery.getAvailableProductionTypes(true));

        inputs.put(sugar, 6);
        outputs.put(rum, 6);
        testProduction(inputs, outputs,
                       rumDistillery.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       rumFactory.getAvailableProductionTypes(true));

        inputs.put(sugar, 6);
        outputs.put(rum, 9);
        testProduction(inputs, outputs,
                       rumFactory.getAvailableProductionTypes(false));
    }

    public void testCoatProduction() {
        testProduction(inputs, outputs,
                       furTraderHouse.getAvailableProductionTypes(true));

        inputs.put(furs, 3);
        outputs.put(coats, 3);
        testProduction(inputs, outputs,
                       furTraderHouse.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       furTradingPost.getAvailableProductionTypes(true));

        inputs.put(furs, 6);
        outputs.put(coats, 6);
        testProduction(inputs, outputs,
                       furTradingPost.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       furFactory.getAvailableProductionTypes(true));

        inputs.put(furs, 6);
        outputs.put(coats, 9);
        testProduction(inputs, outputs,
                       furFactory.getAvailableProductionTypes(false));
    }

    public void testMusketProduction() {
        testProduction(inputs, outputs,
                       armory.getAvailableProductionTypes(true));

        inputs.put(tools, 3);
        outputs.put(muskets, 3);
        testProduction(inputs, outputs,
                       armory.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       magazine.getAvailableProductionTypes(true));

        inputs.put(tools, 6);
        outputs.put(muskets, 6);
        testProduction(inputs, outputs,
                       magazine.getAvailableProductionTypes(false));

        testProduction(inputs, outputs,
                       arsenal.getAvailableProductionTypes(true));

        inputs.put(tools, 9);
        outputs.put(muskets, 9);
        testProduction(inputs, outputs,
                       arsenal.getAvailableProductionTypes(false));
    }

    public void testCrossProduction() {
        outputs.put(crosses, 1); 
        testProduction(inputs, outputs,
                       chapel.getAvailableProductionTypes(true));

        testProduction(inputs, outputs,
                       chapel.getAvailableProductionTypes(false));

        outputs.put(crosses, 2); 
        testProduction(inputs, outputs,
                       church.getAvailableProductionTypes(true));

        outputs.put(crosses, 3);
        testProduction(inputs, outputs,
                       church.getAvailableProductionTypes(false));

        outputs.put(crosses, 3); 
        testProduction(inputs, outputs,
                       cathedral.getAvailableProductionTypes(true));

        outputs.put(crosses, 6);
        testProduction(inputs, outputs,
                       cathedral.getAvailableProductionTypes(false));
    }

    public void testHorseProduction() {
        inputs.put(food, 1);
        outputs.put(horses, 1);
        testProduction(inputs, outputs,
                       country.getAvailableProductionTypes(true));

        testProduction(inputs, outputs,
                       country.getAvailableProductionTypes(false));

        inputs.put(food, 1);
        outputs.put(horses, 1);
        testProduction(inputs, outputs,
                       stables.getAvailableProductionTypes(true));

        testProduction(inputs, outputs,
                       stables.getAvailableProductionTypes(false));
    }

    public void testOtherBuildings() {
        inputs.clear();

        for (BuildingType b : new BuildingType[] {
                schoolhouse, college, university,
                stockade, fort, fortress,
                docks, drydock, shipyard,
                depot, warehouse, warehouseExpansion,
                printingPress, newspaper,
                customHouse, }) {
            testProduction(inputs, outputs,
                           b.getAvailableProductionTypes(true));
            testProduction(inputs, outputs,
                           b.getAvailableProductionTypes(false));
        }
    }
}
