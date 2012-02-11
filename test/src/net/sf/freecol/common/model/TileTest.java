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

package net.sf.freecol.common.model;

import java.util.List;
import java.util.Set;

import net.sf.freecol.client.gui.MapViewer;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.FreeColTestUtils;


public class TileTest extends FreeColTestCase {

    private static final BuildingType townHallType
        = spec().getBuildingType("model.building.townHall");

    private static final GoodsType cotton
        = spec().getGoodsType("model.goods.cotton");
    private static final GoodsType fish
        = spec().getGoodsType("model.goods.fish");
    private static final GoodsType food
        = spec().getPrimaryFoodType();
    private static final GoodsType furs
        = spec().getGoodsType("model.goods.furs");
    private static final GoodsType grain
        = spec().getGoodsType("model.goods.grain");
    private static final GoodsType lumber
        = spec().getGoodsType("model.goods.lumber");
    private static final GoodsType ore
        = spec().getGoodsType("model.goods.ore");
    private static final GoodsType silver
        = spec().getGoodsType("model.goods.silver");
    private static final GoodsType sugar
        = spec().getGoodsType("model.goods.sugar");
    private static final GoodsType tobacco
        = spec().getGoodsType("model.goods.tobacco");

    private static final ResourceType grainResource
        = spec().getResourceType("model.resource.grain");
    private static final ResourceType mineralsResource
        = spec().getResourceType("model.resource.minerals");
    private static final ResourceType silverResource
        = spec().getResourceType("model.resource.silver");
    private static final ResourceType sugarResource
        = spec().getResourceType("model.resource.sugar");

    private static final TileImprovementType clearForest
        = spec().getTileImprovementType("model.improvement.clearForest");
    private static final TileImprovementType fishBonusLand
        = spec().getTileImprovementType("model.improvement.fishBonusLand");
    private static final TileImprovementType fishBonusRiver
        = spec().getTileImprovementType("model.improvement.fishBonusRiver");
    private static final TileImprovementType plow
        = spec().getTileImprovementType("model.improvement.plow");
    private static final TileImprovementType river
        = spec().getTileImprovementType("model.improvement.river");
    private static final TileImprovementType road
        = spec().getTileImprovementType("model.improvement.road");

    private static final TileType arctic
        = spec().getTileType("model.tile.arctic");
    private static final TileType desert
        = spec().getTileType("model.tile.desert");
    private static final TileType desertForest
        = spec().getTileType("model.tile.scrubForest");
    private static final TileType grassland
        = spec().getTileType("model.tile.grassland");
    private static final TileType grasslandForest
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType highSeas
        = spec().getTileType("model.tile.highSeas");
    private static final TileType hills
        = spec().getTileType("model.tile.hills");
    private static final TileType marsh
        = spec().getTileType("model.tile.marsh");
    private static final TileType marshForest
        = spec().getTileType("model.tile.wetlandForest");
    private static final TileType mountains
        = spec().getTileType("model.tile.mountains");
    private static final TileType ocean
        = spec().getTileType("model.tile.ocean");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final TileType plainsForest
        = spec().getTileType("model.tile.mixedForest");
    private static final TileType prairie
        = spec().getTileType("model.tile.prairie");
    private static final TileType prairieForest
        = spec().getTileType("model.tile.broadleafForest");
    private static final TileType savannah
        = spec().getTileType("model.tile.savannah");
    private static final TileType savannahForest
        = spec().getTileType("model.tile.tropicalForest");
    private static final TileType swamp
        = spec().getTileType("model.tile.swamp");
    private static final TileType swampForest
        = spec().getTileType("model.tile.rainForest");
    private static final TileType tundra
        = spec().getTileType("model.tile.tundra");
    private static final TileType tundraForest
        = spec().getTileType("model.tile.borealForest");

    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");


    private class Work {
        public TileType type;
        public int plow;
        public int road;

        public Work(TileType type, int plow, int road) {
            this.type = type;
            this.plow = plow;
            this.road = road;
        }
    }

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

        Work[] cost = new Work[] {
            new Work(plains, 5, 3),
            new Work(desert, 5, 3),
            new Work(grassland, 5, 3),
            new Work(prairie, 5, 3),
            new Work(tundra, 6, 4),
            new Work(savannah, 5, 3),
            new Work(marsh, 7, 5),
            new Work(swamp, 9, 7),
            // TODO: fix test
            //new Work(arctic, 6, 4)
        };

        for (Work entry : cost) {
            Tile tile = new Tile(game, entry.type, 0, 0);
            assertTrue(tile.getType().toString(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().toString(), road.isTileAllowed(tile));
            assertFalse(tile.getType().toString(), clearForest.isTileAllowed(tile));

            assertEquals(tile.getType().toString(), entry.plow, tile.getWorkAmount(plow));
            assertEquals(tile.getType().toString(), entry.road, tile.getWorkAmount(road));
        }

        // Now check the forests
        cost = new Work[] {
            new Work(tundraForest, 6, 4),
            new Work(grasslandForest, 6, 4),
            new Work(desertForest, 6, 4),
            new Work(prairieForest, 6, 4),
            new Work(savannahForest, 8, 6),
            new Work(marshForest, 8, 6),
            new Work(swampForest, 9, 7),
            new Work(plainsForest, 6, 4)
        };

        for (Work entry : cost) {
            Tile tile = new Tile(game, entry.type, 0, 0);
            assertFalse(tile.getType().toString(), plow.isTileAllowed(tile));
            assertTrue(tile.getType().toString(), road.isTileAllowed(tile));
            assertTrue(tile.getType().toString(), clearForest.isTileAllowed(tile));

            assertEquals(tile.getType().toString(), entry.plow, tile.getWorkAmount(clearForest));
            assertEquals(tile.getType().toString(), entry.road, tile.getWorkAmount(road));
        }

    }

    public void testTileTypeChangeProduction() {
        for (TileType tileType : spec().getTileTypeList()) {
            if (tileType.isForested()) {
                AbstractGoods production = clearForest.getProduction(tileType);
                assertNotNull(tileType.getId(), production);
                int amount = (desertForest == tileType) ? 10 : 20;
                assertEquals(tileType.getId(), amount, production.getAmount());
            }
        }
    }

    public void testPrimarySecondaryGoods() {

        Game game = getStandardGame();
        game.setMap(getTestMap(true));
        Colony colony = getStandardColony();
        Tile tile = colony.getTile();
        ColonyTile center = colony.getColonyTile(tile);

        List<AbstractGoods> production = center.getProduction();
        assertEquals(2, production.size());
        AbstractGoods primaryProduction = production.get(0);
        AbstractGoods secondaryProduction = production.get(1);
        assertEquals(grain, primaryProduction.getType());
        assertEquals(5, primaryProduction.getAmount());
        assertEquals(cotton, secondaryProduction.getType());
        assertEquals(2, secondaryProduction.getAmount());

        TileImprovement ti = new TileImprovement(game, tile, spec().getTileImprovementType("model.improvement.plow"));
        ti.setTurnsToComplete(0);
        tile.add(ti);

        production = center.getProduction();
        assertEquals(2, production.size());
        primaryProduction = production.get(0);
        secondaryProduction = production.get(1);
        assertEquals(grain, primaryProduction.getType());
        assertEquals(6, primaryProduction.getAmount());
        assertEquals(cotton, secondaryProduction.getType());
        assertEquals(2, secondaryProduction.getAmount());

        tile.setType(plainsForest);
        production = center.getProduction();
        assertEquals(2, production.size());
        primaryProduction = production.get(0);
        secondaryProduction = production.get(1);
        assertEquals(grain, primaryProduction.getType());
        assertEquals(3, primaryProduction.getAmount());
        assertEquals(furs, secondaryProduction.getType());
        assertEquals(3, secondaryProduction.getAmount());

        ti = new TileImprovement(game, tile, spec().getTileImprovementType("model.improvement.road"));
        ti.setTurnsToComplete(0);
        tile.add(ti);

        production = center.getProduction();
        assertEquals(2, production.size());
        primaryProduction = production.get(0);
        secondaryProduction = production.get(1);
        assertEquals(grain, primaryProduction.getType());
        assertEquals(3, primaryProduction.getAmount());
        assertEquals(furs, secondaryProduction.getType());
        assertEquals(3, secondaryProduction.getAmount());

    }

    public void testPotential() {
        Game game = getStandardGame();
        Tile tile = new Tile(game, mountains, 0, 0);
        assertEquals(0,tile.potential(food, null));
        assertEquals(1,tile.potential(silver, null));
        tile.addResource(new Resource(game, tile, silverResource));
        assertEquals(0,tile.potential(food, null));
        assertEquals(3,tile.potential(silver, null));
    }

    public void testMaximumPotential() {
        Game game = getStandardGame();

        Tile tile1 = new Tile(game, mountains, 0, 0);
        assertEquals(0, tile1.potential(food, null));
        assertEquals(0, tile1.getMaximumPotential(food, null));
        assertEquals(1, tile1.potential(silver, null));
        assertEquals(2, tile1.getMaximumPotential(silver, null));
        tile1.addResource(new Resource(game, tile1, silverResource));
        assertEquals(0, tile1.potential(food, null));
        assertEquals(3, tile1.potential(silver, null));
        assertEquals(4, tile1.getMaximumPotential(silver, null));

        Tile tile2 = new Tile(game, plains, 0, 1);
        assertEquals(5, tile2.potential(grain, null));
        assertEquals(6, tile2.getMaximumPotential(grain, null));
        tile2.addResource(new Resource(game, tile2, grainResource));
        // potential assumes expert
        assertEquals(9, tile2.potential(grain, null));
        assertEquals(10, tile2.getMaximumPotential(grain, null));

        Tile tile3 = new Tile(game, plainsForest, 1, 1);
        assertEquals(3, tile3.potential(grain, null));
        assertEquals(6, tile3.getMaximumPotential(grain, null));
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

        tile1.setType(savannah);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        tile2.setType(hills);
        assertTrue(tile2.hasRoad());
        assertFalse(tile2.hasRiver());

        assertTrue(hasBonusFromSource(tile1.getProductionBonus(sugar, null), river1.getType()));
        assertFalse(hasBonusFromSource(tile1.getProductionBonus(lumber, null), river1.getType()));
        assertFalse(hasBonusFromSource(tile2.getProductionBonus(sugar, null), road2.getType()));
        assertTrue(hasBonusFromSource(tile2.getProductionBonus(ore, null), road2.getType()));

        tile1.addResource(new Resource(game, tile1, sugarResource));

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
        assertEquals(grain, sortedPotential.get(0).getType());
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
        assertEquals(grain, sortedPotential.get(0).getType());
        assertEquals(7, sortedPotential.get(0).getAmount());
        assertTrue(market.getSalePrice(grain, 7) > market.getSalePrice(sugar, 3));

        tile1.setType(plains);

        // plains produces more food than sugar
        assertEquals(grain, tile1.getSortedPotential().get(0).getType());
        assertEquals(grain, tile1.getSortedPotential(farmer, dutch).get(0).getType());
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
        ColonyTile colonyTile = colony.getColonyTile(tile);
        tile.addResource(new Resource(game, tile, mineralsResource));
        if (colonyTile.getUnit() != null) {
            colonyTile.getUnit().setLocation(colony.getBuilding(townHallType));
        }
        assertNull(colonyTile.getUnit());

        Unit unit = colony.getUnitList().get(0);
        assertEquals(colonistType, unit.getType());
        assertTrue(silver.isFarmed());
        assertEquals(0, tundra.getProductionOf(silver, colonistType));
        assertEquals(1, tile.potential(silver, colonistType));

        Set<Modifier> modifiers = tile.getProductionBonus(silver, unit.getType());
        assertFalse(modifiers.isEmpty());

        assertEquals(1, colonyTile.getProductionOf(unit, silver));
        assertEquals(colonyTile.getWorkTile().getOwningSettlement(), colony);
        assertTrue(colonyTile.canBeWorked());
        assertTrue(colonyTile.canAdd(unit));
        assertEquals(colonyTile, colony.getVacantColonyTileFor(unit, false, silver));
    }

    public void testZIndex() {
        assertTrue(MapViewer.OVERLAY_INDEX < MapViewer.FOREST_INDEX);
        assertTrue(MapViewer.FOREST_INDEX < TileItem.RESOURCE_ZINDEX);
        assertTrue(TileItem.RESOURCE_ZINDEX < TileItem.RUMOUR_ZINDEX);
        assertTrue(plow.getZIndex() < river.getZIndex());
        assertTrue(river.getZIndex() < road.getZIndex());
        assertTrue(MapViewer.FOREST_INDEX < road.getZIndex());
        assertTrue(road.getZIndex() < TileItem.RESOURCE_ZINDEX);
    }

}
