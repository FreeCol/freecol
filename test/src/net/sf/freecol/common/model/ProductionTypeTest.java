/**
 *  Copyright (C) 2002-2013  The FreeCol Team
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

import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;
import net.sf.freecol.common.model.Map.Direction;


public class ProductionTypeTest extends FreeColTestCase {

    private static final GoodsType cotton
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType fish
        = spec().getGoodsType("model.goods.fish");
    private static final GoodsType furs
        = spec().getGoodsType("model.goods.furs");
    private static final GoodsType grain
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType lumber
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType ore
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType tobacco
        = spec().getGoodsType("model.goods.tobacco");
    private static final GoodsType silver
        = spec().getGoodsType("model.goods.silver");
    private static final GoodsType sugar
        = spec().getGoodsType("model.goods.sugar");

    private static final TileType arctic
        = spec().getTileType("model.tile.arctic");
    private static final TileType borealForest
        = spec().getTileType("model.tile.borealForest");
    private static final TileType broadleafForest
        = spec().getTileType("model.tile.broadleafForest");
    private static final TileType coniferForest
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType desert
        = spec().getTileType("model.tile.desert");
    private static final TileType grassland
        = spec().getTileType("model.tile.grassland");
    private static final TileType greatRiver
        = spec().getTileType("model.tile.greatRiver");
    private static final TileType highSeas
        = spec().getTileType("model.tile.highSeas");
    private static final TileType hills
        = spec().getTileType("model.tile.hills");
    private static final TileType lake
        = spec().getTileType("model.tile.lake");
    private static final TileType marsh
        = spec().getTileType("model.tile.marsh");
    private static final TileType mixedForest
        = spec().getTileType("model.tile.mixedForest");
    private static final TileType mountains
        = spec().getTileType("model.tile.mountains");
    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType prairie
        = spec().getTileType("model.tile.prairie");
    private static final TileType rainForest
        = spec().getTileType("model.tile.rainForest");
    private static final TileType savannah
        = spec().getTileType("model.tile.savannah");
    private static final TileType scrubForest
        = spec().getTileType("model.tile.scrubForest");
    private static final TileType swamp
        = spec().getTileType("model.tile.swamp");
    private static final TileType tropicalForest
        = spec().getTileType("model.tile.tropicalForest");
    private static final TileType tundra
        = spec().getTileType("model.tile.tundra");
    private static final TileType wetlandForest
        = spec().getTileType("model.tile.wetlandForest");

    private static final UnitType colonistType
        = spec().getDefaultUnitType();

    private Map<GoodsType, Integer> production
        = new HashMap<GoodsType, Integer>();


    private void testProduction(Map<GoodsType, Integer> production,
                                List<ProductionType> productionTypes) {
        for (ProductionType productionType : productionTypes) {
            assertEquals("No inputs expected", 0,
                         productionType.getInputs().size());
            for (AbstractGoods ag : productionType.getOutputs()) {
                Integer i = production.get(ag.getType());
                assertNotNull("Production expected for " + ag.getType(), i);
                assertEquals("Production amount mismatch for " + ag.getType(),
                    i.intValue(), ag.getAmount());
                production.remove(ag.getType());
            }
        }
        assertEquals("Production should remain", 0, production.size());
    }

    private int getGenericPotential(TileType tileType, GoodsType goodsType) {
        return tileType.getPotentialProduction(goodsType, colonistType);
    }


    public void testArctic() {
        production.put(grain, 2);
        testProduction(production,
                       arctic.getAvailableProductionTypes(true, "veryHigh"));
        testProduction(production,
                       arctic.getAvailableProductionTypes(false, "veryHigh"));

        production.put(grain, 1);
        testProduction(production, 
                       arctic.getAvailableProductionTypes(true, "high"));
        testProduction(production,
                       arctic.getAvailableProductionTypes(false, "high"));

        for (String level : new String[] { "medium", "low", "veryLow" }) {
            testProduction(production,
                           arctic.getAvailableProductionTypes(true, level));
            testProduction(production,
                           arctic.getAvailableProductionTypes(false, level));
        }

        assertEquals(0, arctic.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(arctic, grain));
    }

    public void testBorealForest() {
        production.put(grain, 2);
        production.put(furs, 3);
        testProduction(production,
                       borealForest.getAvailableProductionTypes(true));

        production.put(grain, 2);
        production.put(furs, 3);
        production.put(lumber, 4);
        production.put(ore, 1);
        testProduction(production,
                       borealForest.getAvailableProductionTypes(false));

        assertEquals(2, borealForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(borealForest, grain));
    }

    public void testBroadleafForest() {
        production.put(grain, 2);
        production.put(furs, 2);
        testProduction(production,
                       broadleafForest.getAvailableProductionTypes(true));

        production.put(grain, 2);
        production.put(cotton, 1);
        production.put(furs, 2);
        production.put(lumber, 4);
        testProduction(production,
                       broadleafForest.getAvailableProductionTypes(false));

        assertEquals(2, broadleafForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(broadleafForest, grain));
    }

    public void testConiferForest() {
        production.put(grain, 2);
        production.put(furs, 2);
        testProduction(production,
                       coniferForest.getAvailableProductionTypes(true));

        production.put(grain, 2);
        production.put(tobacco, 1);
        production.put(furs, 2);
        production.put(lumber, 6);
        testProduction(production,
                       coniferForest.getAvailableProductionTypes(false));

        assertEquals(2, coniferForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(coniferForest, grain));
    }

    public void testDesert() {
        production.put(grain, 3);
        production.put(cotton, 1);
        testProduction(production,
                       desert.getAvailableProductionTypes(true, "veryHigh"));

        production.put(grain, 2);
        production.put(cotton, 1);
        testProduction(production,
                       desert.getAvailableProductionTypes(true));

        production.put(grain, 1);
        production.put(cotton, 1);
        testProduction(production,
                       desert.getAvailableProductionTypes(true, "veryLow"));

        production.put(grain, 2);
        production.put(cotton, 1);
        production.put(ore, 2);
        testProduction(production,
                       desert.getAvailableProductionTypes(false));

        assertEquals(2, desert.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(desert, grain));
    }

    public void testGrassland() {
        production.put(grain, 3);
        production.put(tobacco, 3);
        testProduction(production,
                       grassland.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(tobacco, 3);
        testProduction(production,
                       grassland.getAvailableProductionTypes(false));

        assertEquals(3, grassland.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(grassland, grain));
    }

    public void testGreatRiver() {
        testProduction(production,
                       greatRiver.getAvailableProductionTypes(true));

        production.put(fish, 2);
        testProduction(production,
                       greatRiver.getAvailableProductionTypes(false));

        assertEquals(0, greatRiver.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(greatRiver, grain));
    }

    public void testHighSeas() {
        testProduction(production,
                       highSeas.getAvailableProductionTypes(true));

        production.put(fish, 2);
        testProduction(production,
                       highSeas.getAvailableProductionTypes(false));

        assertEquals(0, highSeas.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(highSeas, grain));
    }

    public void testHills() {
        production.put(grain, 2);
        production.put(ore, 4);
        testProduction(production,
                       hills.getAvailableProductionTypes(true));

        production.put(grain, 2);
        production.put(ore, 4);
        testProduction(production,
                       hills.getAvailableProductionTypes(false));

        assertEquals(2, hills.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(hills, grain));
    }

    public void testLake() {
        testProduction(production,
                       lake.getAvailableProductionTypes(true));

        production.put(fish, 2);
        testProduction(production,
                       lake.getAvailableProductionTypes(false));

        assertEquals(0, lake.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(lake, grain));
    }

    public void testMarsh() {
        production.put(grain, 3);
        production.put(tobacco, 2);
        testProduction(production,
                       marsh.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(tobacco, 2);
        production.put(ore, 2);
        production.put(silver, 0);
        testProduction(production,
                       marsh.getAvailableProductionTypes(false));

        assertEquals(3, marsh.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(marsh, grain));
    }

    public void testMixedForest() {
        production.put(grain, 3);
        production.put(furs, 3);
        testProduction(production,
                       mixedForest.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(cotton, 1);
        production.put(furs, 3);
        production.put(lumber, 6);
        testProduction(production,
                       mixedForest.getAvailableProductionTypes(false));

        assertEquals(3, mixedForest.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(mixedForest, grain));
    }

    public void testMountains() {
        testProduction(production,
                       mountains.getAvailableProductionTypes(true));

        production.put(ore, 4);
        production.put(silver, 1);
        testProduction(production,
                       mountains.getAvailableProductionTypes(false));

        assertEquals(0, mountains.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(mountains, grain));
    }

    public void testOcean() {
        testProduction(production,
                       ocean.getAvailableProductionTypes(true));

        production.put(fish, 2);
        testProduction(production,
                       ocean.getAvailableProductionTypes(false));

        assertEquals(0, ocean.getPotentialProduction(grain, null));
        assertEquals(0, getGenericPotential(ocean, grain));
    }

    public void testPlains() {
        production.put(grain, 5);
        production.put(cotton, 2);
        testProduction(production,
                       plains.getAvailableProductionTypes(true));

        production.put(grain, 5);
        production.put(cotton, 2);
        production.put(ore, 1);
        testProduction(production,
                       plains.getAvailableProductionTypes(false));

        assertEquals(5, plains.getPotentialProduction(grain, null));
        assertEquals(5, getGenericPotential(plains, grain));
    }

    public void testPrairie() {
        production.put(grain, 3);
        production.put(cotton, 3);
        testProduction(production,
                       prairie.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(cotton, 3);
        testProduction(production,
                       prairie.getAvailableProductionTypes(false));

        assertEquals(3, prairie.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(prairie, grain));
    }

    public void testRainForest() {
        production.put(grain, 2);
        production.put(furs, 1);
        testProduction(production,
                       rainForest.getAvailableProductionTypes(true));

        production.put(grain, 2);
        production.put(sugar, 1);
        production.put(furs, 1);
        production.put(lumber, 4);
        production.put(ore, 1);
        production.put(silver, 0);
        testProduction(production,
                       rainForest.getAvailableProductionTypes(false));

        assertEquals(2, rainForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(rainForest, grain));
    }

    public void testSavannah() {
        production.put(grain, 4);
        production.put(sugar, 3);
        testProduction(production,
                       savannah.getAvailableProductionTypes(true));

        production.put(grain, 4);
        production.put(sugar, 3);
        testProduction(production,
                       savannah.getAvailableProductionTypes(false));

        assertEquals(4, savannah.getPotentialProduction(grain, null));
        assertEquals(4, getGenericPotential(savannah, grain));
    }

    public void testScrubForest() {
        production.put(grain, 3);
        production.put(furs, 2);
        testProduction(production,
                       scrubForest.getAvailableProductionTypes(true, "veryHigh"));

        production.put(grain, 2);
        production.put(furs, 2);
        testProduction(production,
                       scrubForest.getAvailableProductionTypes(true));

        production.put(grain, 1);
        production.put(furs, 2);
        testProduction(production,
                       scrubForest.getAvailableProductionTypes(true, "veryLow"));

        production.put(grain, 2);
        production.put(cotton, 1);
        production.put(furs, 2);
        production.put(lumber, 2);
        production.put(ore, 1);
        testProduction(production,
                       scrubForest.getAvailableProductionTypes(false));

        assertEquals(2, scrubForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(scrubForest, grain));
    }

    public void testSwamp() {
        production.put(grain, 3);
        production.put(sugar, 2);
        testProduction(production,
                       swamp.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(sugar, 2);
        production.put(ore, 2);
        production.put(silver, 0);
        testProduction(production,
                       swamp.getAvailableProductionTypes(false));

        assertEquals(3, swamp.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(swamp, grain));
    }

    public void testTropicalForest() {
        production.put(grain, 3);
        production.put(furs, 2);
        testProduction(production,
                       tropicalForest.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(sugar, 1);
        production.put(furs, 2);
        production.put(lumber, 4);
        testProduction(production,
                       tropicalForest.getAvailableProductionTypes(false));

        assertEquals(3, tropicalForest.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(tropicalForest, grain));
    }

    public void testTundra() {
        production.put(grain, 3);
        production.put(ore, 2);
        testProduction(production,
                       tundra.getAvailableProductionTypes(true));

        production.put(grain, 3);
        production.put(ore, 2);
        production.put(silver, 0);
        testProduction(production,
                       tundra.getAvailableProductionTypes(false));

        assertEquals(3, tundra.getPotentialProduction(grain, null));
        assertEquals(3, getGenericPotential(tundra, grain));
    }

    public void testWetlandForest() {
        production.put(grain, 2);
        production.put(furs, 2);
        testProduction(production,
                       wetlandForest.getAvailableProductionTypes(true));

        production.put(grain, 2);
        production.put(tobacco, 1);
        production.put(furs, 2);
        production.put(lumber, 4);
        production.put(ore, 1);
        production.put(silver, 0);
        testProduction(production,
                       wetlandForest.getAvailableProductionTypes(false));

        assertEquals(2, wetlandForest.getPotentialProduction(grain, null));
        assertEquals(2, getGenericPotential(wetlandForest, grain));
    }
}
