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


public class ProductionTypeTest extends FreeColTestCase {


    public void testArctic() {
        TileType arctic = spec().getTileType("model.tile.arctic");

        List<ProductionType> production = arctic.getProductionTypes(true, "veryHigh");
        assertEquals(1, production.size());
        List<AbstractGoods> outputs = production.get(0).getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(2, outputs.get(0).getAmount());
        assertEquals("model.goods.grain", outputs.get(0).getType().getId());

        production = arctic.getProductionTypes(true, "high");
        assertEquals(1, production.size());
        outputs = production.get(0).getOutputs();
        assertEquals(1, outputs.size());
        assertEquals(1, outputs.get(0).getAmount());
        assertEquals("model.goods.grain", outputs.get(0).getType().getId());

        production = arctic.getProductionTypes(true, "medium");
        assertEquals(0, production.size());

        production = arctic.getProductionTypes(true, "low");
        assertEquals(0, production.size());

        production = arctic.getProductionTypes(true, "veryLow");
        assertEquals(0, production.size());

    }


    public void testPlains() {
        TileType plains = spec().getTileType("model.tile.plains");
        GoodsType grain = spec().getGoodsType("model.goods.grain");
        GoodsType cotton = spec().getGoodsType("model.goods.cotton");
        Map<String, Integer> production = new HashMap<String, Integer>();
        for (ProductionType productionType : plains.getProductionTypes()) {
            assertNull(productionType.getInputs());
            assertNull(productionType.getProductionLevel());
            List<AbstractGoods> outputs = productionType.getOutputs();
            if (productionType.isColonyCenterTile()) {
                assertEquals(2, outputs.size());
                assertEquals(grain, outputs.get(0).getType());
                assertEquals(5, outputs.get(0).getAmount());
                assertEquals(cotton, outputs.get(1).getType());
                assertEquals(2, outputs.get(1).getAmount());
            } else {
                assertEquals(1, outputs.size());
                production.put(outputs.get(0).getType().getId(), outputs.get(0).getAmount());
            }
        }
        assertEquals(5, (int) production.get("model.goods.grain"));
        assertEquals(5, plains.getProductionOf(grain, null));
        assertEquals(2, (int) production.get("model.goods.cotton"));
        assertEquals(1, (int) production.get("model.goods.ore"));
    }



}