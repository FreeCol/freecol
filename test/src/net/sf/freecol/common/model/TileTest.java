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
import java.util.List;
import java.util.Set;

import net.sf.freecol.util.test.FreeColTestCase;

public class TileTest extends FreeColTestCase {
    TileType plains = spec().getTileType("model.tile.plains");
    TileType desert = spec().getTileType("model.tile.desert");
    TileType grassland = spec().getTileType("model.tile.grassland");
    TileType prairie = spec().getTileType("model.tile.prairie");
    TileType tundra = spec().getTileType("model.tile.tundra");
    TileType savannah = spec().getTileType("model.tile.savannah");
    TileType marsh = spec().getTileType("model.tile.marsh");
    TileType swamp = spec().getTileType("model.tile.swamp");
    TileType arctic = spec().getTileType("model.tile.arctic");
        
    TileType plainsForest = spec().getTileType("model.tile.mixedForest");
    TileType desertForest = spec().getTileType("model.tile.scrubForest");
    TileType grasslandForest = spec().getTileType("model.tile.coniferForest");
    TileType prairieForest = spec().getTileType("model.tile.broadleafForest");
    TileType tundraForest = spec().getTileType("model.tile.borealForest");
    TileType savannahForest = spec().getTileType("model.tile.tropicalForest");
    TileType marshForest = spec().getTileType("model.tile.wetlandForest");
    TileType swampForest = spec().getTileType("model.tile.rainForest");

    public void testGetWorkAmount() {

        Game game = getStandardGame();

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
        
        TileImprovementType plow = spec().getTileImprovementType("model.improvement.Plow");
        TileImprovementType buildRoad = spec().getTileImprovementType("model.improvement.Road");
        TileImprovementType clearForrest = spec().getTileImprovementType("model.improvement.ClearForest");
        
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
        
        Tile tile = new Tile(game, spec().getTileType("model.tile.prairie"), 0, 0);
        assertEquals(spec().getGoodsType("model.goods.food"),tile.primaryGoods());
        assertEquals(spec().getGoodsType("model.goods.cotton"),tile.secondaryGoods());
        
        Tile tile2 = new Tile(game, spec().getTileType("model.tile.mixedForest"), 0, 0);
        assertEquals(spec().getGoodsType("model.goods.food"),tile2.primaryGoods());
        assertEquals(spec().getGoodsType("model.goods.furs"),tile2.secondaryGoods());
        
    }

    public void testPotential() {
        Game game = getStandardGame();
        Tile tile = new Tile(game, spec().getTileType("model.tile.mountains"), 0, 0);
        assertEquals(0,tile.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(1,tile.potential(spec().getGoodsType("model.goods.silver"), null));
        tile.setResource(spec().getResourceType("model.resource.Silver"));
        assertEquals(0,tile.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(3,tile.potential(spec().getGoodsType("model.goods.silver"), null));
    }

    public void testMaximumPotential() {
        Game game = getStandardGame();

        Tile tile1 = new Tile(game, spec().getTileType("model.tile.mountains"), 0, 0);
        assertEquals(0, tile1.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(0, tile1.getMaximumPotential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(1, tile1.potential(spec().getGoodsType("model.goods.silver"), null));
        assertEquals(2, tile1.getMaximumPotential(spec().getGoodsType("model.goods.silver"), null));
        tile1.setResource(spec().getResourceType("model.resource.Silver"));
        assertEquals(0, tile1.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(3, tile1.potential(spec().getGoodsType("model.goods.silver"), null));
        assertEquals(4, tile1.getMaximumPotential(spec().getGoodsType("model.goods.silver"), null));

        Tile tile2 = new Tile(game, spec().getTileType("model.tile.plains"), 0, 1);
        assertEquals(5, tile2.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(6, tile2.getMaximumPotential(spec().getGoodsType("model.goods.food"), null));
        tile2.setResource(spec().getResourceType("model.resource.Grain"));
        // potential assumes expert
        assertEquals(9, tile2.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(10, tile2.getMaximumPotential(spec().getGoodsType("model.goods.food"), null));

        Tile tile3 = new Tile(game, spec().getTileType("model.tile.mixedForest"), 1, 1);
        assertEquals(3, tile3.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(6, tile3.getMaximumPotential(spec().getGoodsType("model.goods.food"), null));

    }

    public void testMovement() {
        Game game = getStandardGame();
        Tile tile1 = new Tile(game, spec().getTileType("model.tile.plains"), 0, 0);
        Tile tile2 = new Tile(game, spec().getTileType("model.tile.plains"), 0, 1);
        assertEquals(3, tile1.getMoveCost(tile2));
    }


    public void testImprovements() throws Exception {

        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);

        TileImprovementType roadType = spec().getTileImprovementType("model.improvement.Road");
        TileImprovementType riverType = spec().getTileImprovementType("model.improvement.River");

        TileImprovement road1 = new TileImprovement(game, tile1, roadType);
        TileImprovement river1 = new TileImprovement(game, tile1, riverType);
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        tile1.setTileItemContainer(new TileItemContainer(game, tile1));
        tile1.getTileItemContainer().addTileItem(road1);
        tile1.getTileItemContainer().addTileItem(river1);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        TileImprovement road2 = new TileImprovement(game, tile2, roadType);
        TileImprovement river2 = new TileImprovement(game, tile2, riverType);
        road2.setTurnsToComplete(0);
        assertTrue(road2.isComplete());
        tile2.setTileItemContainer(new TileItemContainer(game, tile2));
        tile2.getTileItemContainer().addTileItem(road2);
        tile2.getTileItemContainer().addTileItem(river2);
        assertTrue(tile2.hasRoad());
        assertTrue(tile2.hasRiver());

        tile1.setType(spec().getTileType("model.tile.savannah"));
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        tile2.setType(spec().getTileType("model.tile.hills"));
        assertTrue(tile2.hasRoad());
        assertFalse(tile2.hasRiver());

    }

    public void testProductionModifiers() throws Exception {
    	Game game = getGame();
    	game.setMap(getTestMap(plainsType,true));
    	
        Colony colony = getStandardColony();
        
        List<ColonyTile> colonyTiles = colony.getColonyTiles();

        ColonyTile colonyTile1 = colonyTiles.get(0);
        ColonyTile colonyTile2 = colonyTiles.get(1);

        Tile tile1 = colonyTile1.getWorkTile();
        Tile tile2 = colonyTile2.getWorkTile();

        TileImprovementType roadType = spec().getTileImprovementType("model.improvement.Road");
        TileImprovementType riverType = spec().getTileImprovementType("model.improvement.River");

        TileImprovement road1 = new TileImprovement(game, tile1, roadType);
        TileImprovement river1 = new TileImprovement(game, tile1, riverType);
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        tile1.setTileItemContainer(new TileItemContainer(game, tile1));
        tile1.getTileItemContainer().addTileItem(road1);
        tile1.getTileItemContainer().addTileItem(river1);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        TileImprovement road2 = new TileImprovement(game, tile2, roadType);
        TileImprovement river2 = new TileImprovement(game, tile2, riverType);
        road2.setTurnsToComplete(0);
        assertTrue(road2.isComplete());
        tile2.setTileItemContainer(new TileItemContainer(game, tile2));
        tile2.getTileItemContainer().addTileItem(road2);
        tile2.getTileItemContainer().addTileItem(river2);
        assertTrue(tile2.hasRoad());
        assertTrue(tile2.hasRiver());

        tile1.setType(spec().getTileType("model.tile.savannah"));
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        tile2.setType(spec().getTileType("model.tile.hills"));
        assertTrue(tile2.hasRoad());
        assertFalse(tile2.hasRiver());

        GoodsType sugar = spec().getGoodsType("model.goods.sugar");
        GoodsType tobacco = spec().getGoodsType("model.goods.tobacco");
        GoodsType lumber = spec().getGoodsType("model.goods.lumber");
        GoodsType ore = spec().getGoodsType("model.goods.ore");

        assertTrue(hasBonusFromSource(tile1.getProductionBonus(sugar, null), river1.getType()));
        assertFalse(hasBonusFromSource(tile1.getProductionBonus(lumber, null), river1.getType()));
        assertFalse(hasBonusFromSource(tile2.getProductionBonus(sugar, null), road2.getType()));
        assertTrue(hasBonusFromSource(tile2.getProductionBonus(ore, null), road2.getType()));

        ResourceType sugarResource = spec().getResourceType("model.resource.Sugar");
        tile1.setResource(sugarResource);

        assertTrue(hasBonusFromSource(tile1.getProductionBonus(sugar, null), savannah));
        assertTrue(hasBonusFromSource(tile1.getProductionBonus(sugar, null), river1.getType()));
        assertTrue(hasBonusFromSource(tile1.getProductionBonus(sugar, null), sugarResource));

    }

    private boolean hasBonusFromSource(Set<Modifier> modifierSet, FreeColGameObjectType source) {
        for (Modifier modifier : modifierSet) {
            if (source.equals(modifier.getSource())) {
                return true;
            }
        }
        return false;
    }


    public void testColonyImprovements() throws Exception {

        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Player dutch = game.getPlayer("model.nation.dutch");

        Colony colony = new Colony(game, dutch, "testColony", tile1);
        Unit soldier = new Unit(game, tile1, dutch, spec().getUnitType("model.unit.veteranSoldier"),
                                Unit.UnitState.ACTIVE);
        soldier.buildColony(colony);

        assertTrue(colony.getTile().getTileItemContainer().hasRoad());

        colony.dispose();

        assertFalse(colony.getTile().getTileItemContainer().hasRoad());

    }

}
