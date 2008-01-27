/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.Specification;
import net.sf.freecol.util.test.FreeColTestCase;

public class TileTest extends FreeColTestCase {




    public void testGetWorkAmount() {

        Game game = getStandardGame();
        
        Specification s = FreeCol.getSpecification();

        TileType plains = s.getTileType("model.tile.plains");
        TileType desert = s.getTileType("model.tile.desert");
        TileType grassland = s.getTileType("model.tile.grassland");
        TileType prairie = s.getTileType("model.tile.prairie");
        TileType tundra = s.getTileType("model.tile.tundra");
        TileType savannah = s.getTileType("model.tile.savannah");
        TileType marsh = s.getTileType("model.tile.marsh");
        TileType swamp = s.getTileType("model.tile.swamp");
        TileType arctic = s.getTileType("model.tile.arctic");
        
        TileType plainsForest = s.getTileType("model.tile.mixedForest");
        TileType desertForest = s.getTileType("model.tile.scrubForest");
        TileType grasslandForest = s.getTileType("model.tile.coniferForest");
        TileType prairieForest = s.getTileType("model.tile.broadleafForest");
        TileType tundraForest = s.getTileType("model.tile.borealForest");
        TileType savannahForest = s.getTileType("model.tile.tropicalForest");
        TileType marshForest = s.getTileType("model.tile.wetlandForest");
        TileType swampForest = s.getTileType("model.tile.rainForest");

        assertNotNull( plains );
        assertNotNull( desert );
        assertNotNull( grassland );
        assertNotNull( prairie );
        assertNotNull( tundra );
        assertNotNull( savannah );
        assertNotNull( marsh );
        assertNotNull( swamp );
        assertNotNull( arctic );
        
        assertNotNull( plainsForest );
        assertNotNull( desertForest );
        assertNotNull( grasslandForest );
        assertNotNull( prairieForest );
        assertNotNull( tundraForest );
        assertNotNull( savannahForest );
        assertNotNull( marshForest );
        assertNotNull( swampForest );
        
        TileImprovementType plow = s.getTileImprovementType("model.improvement.Plow");
        TileImprovementType buildRoad = s.getTileImprovementType("model.improvement.Road");
        TileImprovementType clearForrest = s.getTileImprovementType("model.improvement.ClearForest");
        
        assertEquals(2, plow.getAddWorkTurns());
        assertEquals(0, buildRoad.getAddWorkTurns());
        assertEquals(2, clearForrest.getAddWorkTurns());
        
        
        assertNotNull(plow);
        assertNotNull(buildRoad);
        assertNotNull(clearForrest);
        
        java.util.Map<TileType, int[]> cost = new HashMap<TileType, int[]>();
        cost.put(plains, new int[] { 5, 3 });
        cost.put(desert, new int[] { 5, 3 });
        cost.put(grassland, new int[] { 5, 3 });
        cost.put(prairie, new int[] { 5, 3 });
        cost.put(tundra, new int[] { 6, 4 });
        cost.put(savannah, new int[] { 5, 3 });
        cost.put(marsh, new int[] { 7, 5 });
        cost.put(swamp, new int[] { 9, 7 });
        cost.put(arctic, new int[] { 6, 4 });
        
        for (java.util.Map.Entry<TileType, int[]> entry : cost.entrySet()){
            Tile tile = new Tile(game, entry.getKey(), 0, 0);
            assertTrue(tile.getType().getName(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().getName(), buildRoad.isTileAllowed(tile));
            assertFalse(tile.getType().getName(), clearForrest.isTileAllowed(tile));
            
            assertEquals(tile.getType().getName(), entry.getValue()[0], tile.getWorkAmount(plow));
            assertEquals(tile.getType().getName(), entry.getValue()[1], tile.getWorkAmount(buildRoad));
        }
        
        // Now check the forests
        cost.clear();
        cost.put(tundraForest, new int[] { 6, 4 });
        cost.put(grasslandForest, new int[] { 6, 4 });
        cost.put(desertForest, new int[] { 6, 4});
        cost.put(prairieForest, new int[] { 6, 4 });
        cost.put(savannahForest, new int[] { 8, 6 });
        cost.put(marshForest, new int[] { 8, 6 });
        cost.put(swampForest, new int[] { 9, 7});
        cost.put(plainsForest, new int[] { 6, 4});
        
        for (java.util.Map.Entry<TileType, int[]> entry : cost.entrySet()){
            Tile tile = new Tile(game, entry.getKey(), 0, 0);
            assertFalse(tile.getType().getName(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().getName(), buildRoad.isTileAllowed(tile));
            assertTrue(tile.getType().getName(), clearForrest.isTileAllowed(tile));
            
            assertEquals(tile.getType().getName(), entry.getValue()[0], tile.getWorkAmount(clearForrest));
            assertEquals(tile.getType().getName(), entry.getValue()[1], tile.getWorkAmount(buildRoad));
        }
        
    }
    
    public void testPrimarySecondaryGoods() {
        
        Game game = getStandardGame();
        
        Specification s = FreeCol.getSpecification();
        
        Tile tile = new Tile(game, s.getTileType("model.tile.prairie"), 0, 0);
        assertEquals(s.getGoodsType("model.goods.food"),tile.primaryGoods());
        assertEquals(s.getGoodsType("model.goods.cotton"),tile.secondaryGoods());
        
        Tile tile2 = new Tile(game, s.getTileType("model.tile.mixedForest"), 0, 0);
        assertEquals(s.getGoodsType("model.goods.food"),tile2.primaryGoods());
        assertEquals(s.getGoodsType("model.goods.furs"),tile2.secondaryGoods());
        
    }

    public void testPotential() {
        Game game = getStandardGame();
        Specification s = FreeCol.getSpecification();
        Tile tile = new Tile(game, s.getTileType("model.tile.mountains"), 0, 0);
        assertEquals(0,tile.potential(s.getGoodsType("model.goods.food")));
        assertEquals(1,tile.potential(s.getGoodsType("model.goods.silver")));
        tile.setResource(s.getResourceType("model.resource.Silver"));
        assertEquals(0,tile.potential(s.getGoodsType("model.goods.food")));
        assertEquals(3,tile.potential(s.getGoodsType("model.goods.silver")));
    }

    public void testMovement() {
        Game game = getStandardGame();
        Specification spec = FreeCol.getSpecification();
        Tile tile1 = new Tile(game, spec.getTileType("model.tile.plains"), 0, 0);
        Tile tile2 = new Tile(game, spec.getTileType("model.tile.plains"), 0, 1);
        assertEquals(3, tile1.getMoveCost(tile2));
    }

}
