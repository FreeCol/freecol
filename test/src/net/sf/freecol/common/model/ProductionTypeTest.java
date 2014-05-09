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

    public static final GoodsType cotton
        = spec().getGoodsType("model.goods.cotton");
    public static final GoodsType grain
        = spec().getGoodsType("model.goods.grain");
    public static final GoodsType ore
        = spec().getGoodsType("model.goods.ore");
    public static final GoodsType silver
        = spec().getGoodsType("model.goods.silver");

    public static final TileType arctic
        = spec().getTileType("model.tile.arctic");
    public static final TileType plains
        = spec().getTileType("model.tile.plains");
    public static final TileType tundra
        = spec().getTileType("model.tile.tundra");


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



    public void testArctic() {
        Map<GoodsType, Integer> production = new HashMap<GoodsType, Integer>();

        production.put(grain, 2);
        testProduction(production,
                       arctic.getProductionTypes(true, "veryHigh"));
        testProduction(production,
                       arctic.getProductionTypes(false, "veryHigh"));

        production.put(grain, 1);
        testProduction(production, 
                       arctic.getProductionTypes(true, "high"));
        testProduction(production,
                       arctic.getProductionTypes(false, "high"));

        for (String level : new String[] { "medium", "low", "veryLow" }) {
            testProduction(production,
                           arctic.getProductionTypes(true, level));
            testProduction(production,
                           arctic.getProductionTypes(false, level));
        }
    }

    public void testPlains() {
        Map<GoodsType, Integer> production = new HashMap<GoodsType, Integer>();

        production.put(grain, 5);
        production.put(cotton, 2);
        testProduction(production, plains.getProductionTypes(true));

        production.put(grain, 5);
        production.put(cotton, 2);
        production.put(ore, 1);
        testProduction(production, plains.getProductionTypes(false));

        assertEquals(5, plains.getPotentialProduction(grain, null));
    }

    public void testResource() {
        Game game = getGame();
        game.setMap(getTestMap(tundra));

        Colony colony = getStandardColony(1);
        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        ColonyTile colonyTile = colony.getColonyTile(tile);
        ProductionType productionType = colonyTile.getBestProductionType(silver);
        assertNull("tundra can not produce silver", productionType);

        ResourceType minerals = spec().getResourceType("model.resource.minerals");
        tile.addResource(new Resource(game, tile, minerals));
        productionType = colonyTile.getBestProductionType(silver);
        assertNotNull("production type should not be null", productionType);
        assertNotNull("resource should produce silver",
                      productionType.getOutput(silver));
        assertEquals("base production still must be zero",
                     0, productionType.getOutput(silver).getAmount());

    }
}
