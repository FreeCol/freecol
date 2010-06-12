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

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;

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
    TileType hills = spec().getTileType("model.tile.hills");
    TileType mountains = spec().getTileType("model.tile.mountains");
    TileType ocean = spec().getTileType("model.tile.ocean");
    TileType highSeas = spec().getTileType("model.tile.highSeas");

    TileType plainsForest = spec().getTileType("model.tile.mixedForest");
    TileType desertForest = spec().getTileType("model.tile.scrubForest");
    TileType grasslandForest = spec().getTileType("model.tile.coniferForest");
    TileType prairieForest = spec().getTileType("model.tile.broadleafForest");
    TileType tundraForest = spec().getTileType("model.tile.borealForest");
    TileType savannahForest = spec().getTileType("model.tile.tropicalForest");
    TileType marshForest = spec().getTileType("model.tile.wetlandForest");
    TileType swampForest = spec().getTileType("model.tile.rainForest");

    TileImprovementType plow = spec().getTileImprovementType("model.improvement.plow");
    TileImprovementType river = spec().getTileImprovementType("model.improvement.river");
    TileImprovementType road = spec().getTileImprovementType("model.improvement.road");
    TileImprovementType clearForest = spec().getTileImprovementType("model.improvement.clearForest");
    TileImprovementType fishBonusLand = spec().getTileImprovementType("model.improvement.fishBonusLand");
    TileImprovementType fishBonusRiver = spec().getTileImprovementType("model.improvement.fishBonusRiver");

    GoodsType food = spec().getGoodsType("model.goods.food");
    GoodsType fish = spec().getGoodsType("model.goods.fish");
    GoodsType sugar = spec().getGoodsType("model.goods.sugar");
    GoodsType cotton = spec().getGoodsType("model.goods.cotton");
    GoodsType tobacco = spec().getGoodsType("model.goods.tobacco");
    GoodsType lumber = spec().getGoodsType("model.goods.lumber");
    GoodsType ore = spec().getGoodsType("model.goods.ore");


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
        
        
        assertEquals(2, plow.getAddWorkTurns());
        assertEquals(0, road.getAddWorkTurns());
        assertEquals(2, clearForest.getAddWorkTurns());
        
        
        assertNotNull(plow);
        assertNotNull(road);
        assertNotNull(clearForest);
        
        java.util.Map<TileType, int[]> cost = new HashMap<TileType, int[]>();
        cost.put(plains, new int[] { 5, 3 });
        cost.put(desert, new int[] { 5, 3 });
        cost.put(grassland, new int[] { 5, 3 });
        cost.put(prairie, new int[] { 5, 3 });
        cost.put(tundra, new int[] { 6, 4 });
        cost.put(savannah, new int[] { 5, 3 });
        cost.put(marsh, new int[] { 7, 5 });
        cost.put(swamp, new int[] { 9, 7 });
        // TODO: fix test
        //cost.put(arctic, new int[] { 6, 4 });
        
        for (java.util.Map.Entry<TileType, int[]> entry : cost.entrySet()){
            Tile tile = new Tile(game, entry.getKey(), 0, 0);
            assertTrue(tile.getType().toString(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().toString(), road.isTileAllowed(tile));
            assertFalse(tile.getType().toString(), clearForest.isTileAllowed(tile));
            
            assertEquals(tile.getType().toString(), entry.getValue()[0], tile.getWorkAmount(plow));
            assertEquals(tile.getType().toString(), entry.getValue()[1], tile.getWorkAmount(road));
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
            assertFalse(tile.getType().toString(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().toString(), road.isTileAllowed(tile));
            assertTrue(tile.getType().toString(), clearForest.isTileAllowed(tile));
            
            assertEquals(tile.getType().toString(), entry.getValue()[0], tile.getWorkAmount(clearForest));
            assertEquals(tile.getType().toString(), entry.getValue()[1], tile.getWorkAmount(road));
        }
        
    }
    
    public void testPrimarySecondaryGoods() {
        
        Game game = getStandardGame();
        
        Tile tile = new Tile(game, spec().getTileType("model.tile.prairie"), 0, 0);
        assertEquals(spec().getGoodsType("model.goods.food"),
                     tile.getType().getPrimaryGoods().getType());
        assertEquals(3, tile.getPrimaryProduction());
        assertEquals(spec().getGoodsType("model.goods.cotton"),
                     tile.getType().getSecondaryGoods().getType());
        assertEquals(3, tile.getSecondaryProduction());
        
        Tile tile2 = new Tile(game, spec().getTileType("model.tile.mixedForest"), 0, 0);
        assertEquals(spec().getGoodsType("model.goods.food"),
                     tile2.getType().getPrimaryGoods().getType());
        assertEquals(3, tile2.getPrimaryProduction());
        assertEquals(spec().getGoodsType("model.goods.furs"),
                     tile2.getType().getSecondaryGoods().getType());
        assertEquals(3, tile2.getSecondaryProduction());
        
    }

    public void testPotential() {
        Game game = getStandardGame();
        Tile tile = new Tile(game, spec().getTileType("model.tile.mountains"), 0, 0);
        assertEquals(0,tile.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(1,tile.potential(spec().getGoodsType("model.goods.silver"), null));
        tile.setResource(new Resource(game, tile, spec().getResourceType("model.resource.silver")));
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
        tile1.setResource(new Resource(game, tile1, spec().getResourceType("model.resource.silver")));
        assertEquals(0, tile1.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(3, tile1.potential(spec().getGoodsType("model.goods.silver"), null));
        assertEquals(4, tile1.getMaximumPotential(spec().getGoodsType("model.goods.silver"), null));

        Tile tile2 = new Tile(game, spec().getTileType("model.tile.plains"), 0, 1);
        assertEquals(5, tile2.potential(spec().getGoodsType("model.goods.food"), null));
        assertEquals(6, tile2.getMaximumPotential(spec().getGoodsType("model.goods.food"), null));
        tile2.setResource(new Resource(game, tile2, spec().getResourceType("model.resource.grain")));
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


    public void testCanHaveImprovement() {

        for (TileType tileType : spec().getTileTypeList()) {

            if (tileType.isWater()) {
                if (highSeas.equals(tileType)) {
                    assertFalse(tileType.canHaveImprovement(fishBonusLand));
                    assertFalse(tileType.canHaveImprovement(fishBonusRiver));
                } else {
                    assertTrue(tileType.canHaveImprovement(fishBonusLand));
                    assertTrue(tileType.canHaveImprovement(fishBonusRiver));
                }
                assertFalse(tileType.canHaveImprovement(river));
                assertFalse(tileType.canHaveImprovement(road));
                assertFalse(tileType.canHaveImprovement(plow));
                assertFalse(tileType.canHaveImprovement(clearForest));
            } else {
                if (tileType.isForested()) {
                    assertTrue(tileType.canHaveImprovement(clearForest));
                } else {
                    assertFalse(tileType.canHaveImprovement(clearForest));
                }
                if (arctic.equals(tileType) || hills.equals(tileType)
                    || mountains.equals(tileType)) {
                    assertFalse(tileType.canHaveImprovement(river));
                    assertFalse(tileType.canHaveImprovement(plow));
                } else {
                    assertTrue(tileType.canHaveImprovement(river));
                    if (tileType.isForested()) {
                        assertFalse(tileType.canHaveImprovement(plow));
                    } else {
                        assertTrue(tileType.canHaveImprovement(plow));
                    }
                }

                assertTrue(tileType.canHaveImprovement(road));
            }
        }
    }


    public void testImprovements() throws Exception {

        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);

        TileImprovement road1 = new TileImprovement(game, tile1, road);
        TileImprovement river1 = new TileImprovement(game, tile1, river);
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        tile1.setTileItemContainer(new TileItemContainer(game, tile1));
        tile1.getTileItemContainer().addTileItem(road1);
        tile1.getTileItemContainer().addTileItem(river1);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        TileImprovement road2 = new TileImprovement(game, tile2, road);
        TileImprovement river2 = new TileImprovement(game, tile2, river);
        road2.setTurnsToComplete(0);
        assertTrue(road2.isComplete());
        tile2.setTileItemContainer(new TileItemContainer(game, tile2));
        tile2.getTileItemContainer().addTileItem(road2);
        tile2.getTileItemContainer().addTileItem(river2);
        assertTrue(tile2.hasRoad());
        assertTrue(tile2.hasRiver());

        tile1.setType(savannah);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        tile2.setType(hills);
        assertTrue(tile2.hasRoad());
        assertFalse(tile2.hasRiver());

    }

    public void testProductionModifiers() throws Exception {
    	Game game = getGame();
    	game.setMap(getTestMap(true));
    	
        Colony colony = getStandardColony();
        
        List<ColonyTile> colonyTiles = colony.getColonyTiles();

        ColonyTile colonyTile1 = colonyTiles.get(0);
        ColonyTile colonyTile2 = colonyTiles.get(1);

        Tile tile1 = colonyTile1.getWorkTile();
        Tile tile2 = colonyTile2.getWorkTile();

        TileImprovement road1 = new TileImprovement(game, tile1, road);
        TileImprovement river1 = new TileImprovement(game, tile1, river);
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        tile1.setTileItemContainer(new TileItemContainer(game, tile1));
        tile1.getTileItemContainer().addTileItem(road1);
        tile1.getTileItemContainer().addTileItem(river1);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        TileImprovement road2 = new TileImprovement(game, tile2, road);
        TileImprovement river2 = new TileImprovement(game, tile2, river);
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

        assertTrue(hasBonusFromSource(tile1.getProductionBonus(sugar, null), river1.getType()));
        assertFalse(hasBonusFromSource(tile1.getProductionBonus(lumber, null), river1.getType()));
        assertFalse(hasBonusFromSource(tile2.getProductionBonus(sugar, null), road2.getType()));
        assertTrue(hasBonusFromSource(tile2.getProductionBonus(ore, null), road2.getType()));

        ResourceType sugarResource = spec().getResourceType("model.resource.sugar");
        tile1.setResource(new Resource(game, tile1, sugarResource));

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
        
        Colony colony = FreeColTestUtils.getColonyBuilder().build();

        assertTrue(colony.getTile().hasRoad());

        colony.dispose();

        assertFalse(colony.getTile().hasRoad());
    }

    public void testBestImprovements() throws Exception {

        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);

        tile1.setType(savannah);
        assertEquals(plow, TileImprovement.findBestTileImprovementType(tile1, food));
        assertEquals(plow, TileImprovement.findBestTileImprovementType(tile1, sugar));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, tobacco));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, lumber));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, ore));

        tile1.setType(marsh);
        assertEquals(plow, TileImprovement.findBestTileImprovementType(tile1, food));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, sugar));
        assertEquals(plow, TileImprovement.findBestTileImprovementType(tile1, tobacco));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, lumber));
        assertEquals(road, TileImprovement.findBestTileImprovementType(tile1, ore));

        tile1.setType(savannahForest);
        assertEquals(clearForest, TileImprovement.findBestTileImprovementType(tile1, food));
        assertEquals(clearForest, TileImprovement.findBestTileImprovementType(tile1, sugar));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, tobacco));
        assertEquals(road, TileImprovement.findBestTileImprovementType(tile1, lumber));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, ore));

        tile1.setType(hills);
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, food));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, sugar));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, tobacco));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, lumber));
        assertEquals(road, TileImprovement.findBestTileImprovementType(tile1, ore));

        tile1.setType(arctic);
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, food));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, sugar));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, tobacco));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, lumber));
        assertEquals(null, TileImprovement.findBestTileImprovementType(tile1, ore));

    }

    /*
    public void testSortedPotential() {
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Player dutch = game.getPlayer("model.nation.dutch");
        Market market = dutch.getMarket();
        UnitType sugarPlanter = spec().getUnitType("model.unit.masterSugarPlanter");
        UnitType cottonPlanter = spec().getUnitType("model.unit.masterCottonPlanter");
        UnitType farmer = spec().getUnitType("model.unit.expertFarmer");
        Tile tile1 = map.getTile(5, 8);

        tile1.setType(savannah);
        assertEquals(3, savannah.getProductionOf(sugar, null));
        assertEquals(6, savannah.getProductionOf(sugar, sugarPlanter));

        List<AbstractGoods> sortedPotential = tile1.getSortedPotential();
        // savannah produces more food than sugar
        assertEquals(food, sortedPotential.get(0).getType());
        assertEquals(4, sortedPotential.get(0).getAmount());
        assertEquals(sugar, sortedPotential.get(1).getType());
        assertEquals(3, sortedPotential.get(1).getAmount());

        // 3 sugar is more expensive than 4 food
        assertNotNull(sugarPlanter);
        assertTrue(tile1.potential(sugar, sugarPlanter) > tile1.potential(sugar, null));
        sortedPotential = tile1.getSortedPotential(sugarPlanter, dutch);
        assertEquals(sugar, sortedPotential.get(0).getType());
        assertEquals(6, sortedPotential.get(0).getAmount());

        sortedPotential = tile1.getSortedPotential(farmer, dutch);
        assertEquals(food, sortedPotential.get(0).getType());
        assertEquals(7, sortedPotential.get(0).getAmount());
        assertTrue(market.getSalePrice(food, 7) > market.getSalePrice(sugar, 3));

        tile1.setType(plains);
        // make sure 2 cotton is more expensive than 5 food
        market.getMarketData(cotton).setPaidForSale(3);

        // plains produces more food than sugar
        assertEquals(food, tile1.getSortedPotential().get(0).getType());
        assertEquals(food, tile1.getSortedPotential(farmer, dutch).get(0).getType());
        assertEquals(cotton, tile1.getSortedPotential(cottonPlanter, dutch).get(0).getType());

        tile1.setType(ocean);
        sortedPotential = tile1.getSortedPotential();
        assertEquals(1, sortedPotential.size());
        assertEquals(fish, sortedPotential.get(0).getType());

        sortedPotential = tile1.getSortedPotential(farmer, null);
        assertEquals(1, sortedPotential.size());
        assertEquals(fish, sortedPotential.get(0).getType());
        
    }
    */

    public void testArctic() {
        TileType arctic = spec().getTileType("model.tile.arctic");

        arctic.applyDifficultyLevel(spec().getDifficultyLevel("model.difficulty.veryEasy"));
        assertTrue(arctic.canSettle());
        assertEquals(2, arctic.getPrimaryGoods().getAmount());
        assertNull(arctic.getSecondaryGoods());

        arctic.applyDifficultyLevel(spec().getDifficultyLevel("model.difficulty.easy"));
        assertTrue(arctic.canSettle());
        assertEquals(1, arctic.getPrimaryGoods().getAmount());
        assertNull(arctic.getSecondaryGoods());

        arctic.applyDifficultyLevel(spec().getDifficultyLevel("model.difficulty.medium"));
        assertTrue(arctic.canSettle());
        assertNull(arctic.getPrimaryGoods());
        assertNull(arctic.getSecondaryGoods());

        arctic.applyDifficultyLevel(spec().getDifficultyLevel("model.difficulty.hard"));
        assertTrue(arctic.canSettle());
        assertNull(arctic.getPrimaryGoods());
        assertNull(arctic.getSecondaryGoods());

        arctic.applyDifficultyLevel(spec().getDifficultyLevel("model.difficulty.veryHard"));
        assertTrue(arctic.canSettle());
        assertNull(arctic.getPrimaryGoods());
        assertNull(arctic.getSecondaryGoods());


    }

    public void testMinerals() {
        Game game = getGame();
        Map map = getTestMap(tundra);
    	game.setMap(map);
    	
    	Colony colony = getStandardColony();
        Tile tile = colony.getTile().getNeighbourOrNull(Map.Direction.N);
        ResourceType minerals = spec().getResourceType("model.resource.minerals");
        tile.setResource(new Resource(game, tile, minerals));
        GoodsType silver = spec().getGoodsType("model.goods.silver");
        UnitType colonist = spec().getUnitType("model.unit.freeColonist");

        Unit unit = colony.getUnitList().get(0);
        assertEquals(colonist, unit.getType());
        assertTrue(silver.isFarmed());
        assertEquals(0, tundra.getProductionOf(silver, colonist));
        assertEquals(1, tile.potential(silver, colonist));

        ColonyTile colonyTile = colony.getColonyTile(tile);

        Set<Modifier> modifiers = tile.getProductionBonus(silver, unit.getType());
        assertFalse(modifiers.isEmpty());

        assertEquals(1, colonyTile.getProductionOf(unit, silver));
        assertEquals(colonyTile, colony.getVacantColonyTileFor(unit, false, silver));
    }

    public void testZIndex() {

        assertTrue(GUI.OVERLAY_INDEX < GUI.FOREST_INDEX);
        assertTrue(GUI.FOREST_INDEX < TileItem.RESOURCE_ZINDEX);
        assertTrue(TileItem.RESOURCE_ZINDEX < TileItem.RUMOUR_ZINDEX);
        assertTrue(plow.getZIndex() < river.getZIndex());
        assertTrue(river.getZIndex() < road.getZIndex());
        assertTrue(GUI.FOREST_INDEX < road.getZIndex());
        assertTrue(road.getZIndex() < TileItem.RESOURCE_ZINDEX);

    }

}
