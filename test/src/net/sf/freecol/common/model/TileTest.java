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
    private static final ResourceType lumberResource
        = spec().getResourceType("model.resource.lumber");
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
    private static final TileType coniferForest
        = spec().getTileType("model.tile.coniferForest");
    private static final TileType desert
        = spec().getTileType("model.tile.desert");
    private static final TileType desertForest
        = spec().getTileType("model.tile.scrubForest");
    private static final TileType grassland
        = spec().getTileType("model.tile.grassland");
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
    private static final UnitType expertFarmerType
        = spec().getUnitType("model.unit.expertFarmer");
    private static final UnitType expertLumberJack
        = spec().getUnitType("model.unit.expertLumberJack");


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
        assertNotNull( coniferForest );
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
            // FIXME: fix test
            //new Work(arctic, 6, 4)
        };

        for (Work entry : cost) {
            Tile tile = new Tile(game, entry.type, 0, 0);
            assertTrue(tile.getType().toString(),
                       tile.isImprovementTypeAllowed(plow));
            assertTrue(tile.getType().toString(),
                       tile.isImprovementTypeAllowed(road));
            assertFalse(tile.getType().toString(),
                        tile.isImprovementTypeAllowed(clearForest));

            assertEquals(tile.getType().toString(), entry.plow,
                         tile.getWorkAmount(plow));
            assertEquals(tile.getType().toString(), entry.road,
                         tile.getWorkAmount(road));
        }

        // Now check the forests
        cost = new Work[] {
            new Work(tundraForest, 6, 4),
            new Work(coniferForest, 6, 4),
            new Work(desertForest, 6, 4),
            new Work(prairieForest, 6, 4),
            new Work(savannahForest, 8, 6),
            new Work(marshForest, 8, 6),
            new Work(swampForest, 9, 7),
            new Work(plainsForest, 6, 4)
        };

        for (Work entry : cost) {
            Tile tile = new Tile(game, entry.type, 0, 0);
            assertFalse(tile.getType().toString(),
                        tile.isImprovementTypeAllowed(plow));
            assertTrue(tile.getType().toString(),
                       tile.isImprovementTypeAllowed(road));
            assertTrue(tile.getType().toString(),
                       tile.isImprovementTypeAllowed(clearForest));

            assertEquals(tile.getType().toString(), entry.plow,
                         tile.getWorkAmount(clearForest));
            assertEquals(tile.getType().toString(), entry.road,
                         tile.getWorkAmount(road));
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

        TileImprovement ti = new TileImprovement(game, tile, plow);
        ti.setTurnsToComplete(0);
        tile.add(ti);
        colony.invalidateCache();

        production = center.getProduction();
        assertEquals(2, production.size());
        primaryProduction = production.get(0);
        secondaryProduction = production.get(1);
        assertEquals(grain, primaryProduction.getType());
        assertEquals(6, primaryProduction.getAmount());
        assertEquals(cotton, secondaryProduction.getType());
        assertEquals(2, secondaryProduction.getAmount());

        tile.changeType(plainsForest);
        colony.invalidateCache();

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
        assertEquals(0, mountains.getPotentialProduction(silver, null));
        assertEquals(0, tile.getPotentialProduction(food, null));
        assertEquals(1, mountains.getPotentialProduction(silver, colonistType));
        assertEquals(1, tile.getPotentialProduction(silver, colonistType));
        tile.addResource(new Resource(game, tile, silverResource));
        assertEquals(0, tile.getPotentialProduction(food, colonistType));
        assertEquals(3, tile.getPotentialProduction(silver, colonistType));
    }

    public void testMaximumPotential() {
        Game game = getStandardGame();

        Tile tile1 = new Tile(game, mountains, 0, 0);
        assertEquals("Mountain/food", 0,
                     tile1.getPotentialProduction(food, colonistType));
        assertEquals("Mountain/food max", 0,
                     tile1.getMaximumPotential(food, colonistType));
        assertEquals("Mountain/silver", 1,
                     tile1.getPotentialProduction(silver, colonistType));
        assertEquals("Mountain/silver max", 2,
                     tile1.getMaximumPotential(silver, colonistType));
        tile1.addResource(new Resource(game, tile1, silverResource));
        assertEquals("Mountain+Resource/food", 0,
                     tile1.getPotentialProduction(food, colonistType));
        assertEquals("Mountain+Resource/silver", 3,
                     tile1.getPotentialProduction(silver, colonistType));
        assertEquals("Mountain+Resource/silver max", 4,
                     tile1.getMaximumPotential(silver, colonistType));

        // grain-max should equal grain-potential + 1 (ploughing improvement)
        Tile tile2 = new Tile(game, plains, 0, 1);
        assertEquals("Plains/grain", 5,
                     tile2.getPotentialProduction(grain, null));
        assertEquals("Plains/grain max", 6,
                     tile2.getMaximumPotential(grain, null));
        tile2.addResource(new Resource(game, tile2, grainResource));
        assertEquals("Plains+Resource/grain", 7,
                     tile2.getPotentialProduction(grain, null));
        assertEquals("Plains+Resource/grain max", 8,
                     tile2.getMaximumPotential(grain, null));
        assertEquals("Plains+Resource/grain/expertFarmer", 9,
                     tile2.getPotentialProduction(grain, expertFarmerType));
        assertEquals("Plains+Resource/grain/expertFarmer max", 10,
                     tile2.getMaximumPotential(grain, expertFarmerType));

        Tile tile3 = new Tile(game, plainsForest, 1, 1);
        assertEquals("Forest/grain", 3,
                     tile3.getPotentialProduction(grain, null));
        assertEquals("Forest/grain max", 6,
                     tile3.getMaximumPotential(grain, null));
    }

    public void testIsTileTypeAllowed() {
        for (TileType tileType : spec().getTileTypeList()) {

            if (tileType.isWater()) {
                if (highSeas.equals(tileType)) {
                    assertFalse(fishBonusLand.isTileTypeAllowed(tileType));
                    assertFalse(fishBonusRiver.isTileTypeAllowed(tileType));
                } else {
                    assertTrue(fishBonusLand.isTileTypeAllowed(tileType));
                    assertTrue(fishBonusRiver.isTileTypeAllowed(tileType));
                }
                assertFalse(river.isTileTypeAllowed(tileType));
                assertFalse(road.isTileTypeAllowed(tileType));
                assertFalse(plow.isTileTypeAllowed(tileType));
                assertFalse(clearForest.isTileTypeAllowed(tileType));
            } else {
                if (tileType.isForested()) {
                    assertTrue(clearForest.isTileTypeAllowed(tileType));
                } else {
                    assertFalse(clearForest.isTileTypeAllowed(tileType));
                }
                if (arctic.equals(tileType) || hills.equals(tileType)
                    || mountains.equals(tileType)) {
                    assertFalse(river.isTileTypeAllowed(tileType));
                    assertFalse(plow.isTileTypeAllowed(tileType));
                } else {
                    assertTrue(river.isTileTypeAllowed(tileType));
                    if (tileType.isForested()) {
                        assertFalse(plow.isTileTypeAllowed(tileType));
                    } else {
                        assertTrue(plow.isTileTypeAllowed(tileType));
                    }
                }

                assertTrue(road.isTileTypeAllowed(tileType));
            }
        }
    }


    public void testImprovements() throws Exception {
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.setMap(map);
        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);

        TileImprovement road1 = tile1.addRoad();
        assertFalse(road1.isComplete());
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        assertTrue(tile1.hasRoad());
        TileImprovement river1 = tile1.addRiver(1, "0101");
        assertTrue(river1.isComplete());
        assertTrue(tile1.hasRiver());

        TileImprovement road2 = tile2.addRoad();
        road2.setTurnsToComplete(0);
        assertTrue(road2.isComplete());
        TileImprovement river2 = tile2.addRiver(1, "0101");
        assertTrue(tile2.hasRoad());
        assertTrue(tile2.hasRiver());

        tile1.changeType(savannah);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        tile2.changeType(hills);
        assertTrue(tile2.hasRoad());
        assertFalse(tile2.hasRiver());
    }

    public void testProductionModifiers() throws Exception {
        Game game = getGame();
        game.setMap(getTestMap(true));

        Colony colony = getStandardColony();

        List<ColonyTile> colonyTiles = colony.getColonyTiles();
        ColonyTile colonyTile1 = null;
        ColonyTile colonyTile2 = null;
        for (ColonyTile ct : colonyTiles) {
            if (!ct.getWorkTile().hasRoad()) {
                if (colonyTile1 == null) {
                    colonyTile1 = ct;
                } else if (colonyTile2 == null) {
                    colonyTile2 = ct;
                    break;
                }
            }
        }

        Tile tile1 = colonyTile1.getWorkTile();
        Tile tile2 = colonyTile2.getWorkTile();
        assertFalse(tile1.hasRoad());
        assertFalse(tile2.hasRoad());

        TileImprovement road1 = tile1.addRoad();
        road1.setTurnsToComplete(0);
        assertTrue(road1.isComplete());
        TileImprovement river1 = tile1.addRiver(1, "0101");
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        TileImprovement road2 = tile2.addRoad();
        road2.setTurnsToComplete(0);
        assertTrue(road2.isComplete());
        TileImprovement river2 = tile2.addRiver(1, "0101");
        assertTrue(tile2.hasRoad());
        assertTrue(tile2.hasRiver());

        tile1.changeType(savannah);
        assertTrue(tile1.hasRoad());
        assertTrue(tile1.hasRiver());

        tile2.changeType(hills);
        assertTrue(tile2.hasRoad());
        assertFalse(tile2.hasRiver());

        // Savannah can produce sugar, but not lumber.  Therefore the
        // river provides a bonus for sugar but not lumber.
        assertTrue(tile1.canProduce(sugar, null));
        assertTrue(hasBonusFrom(tile1.getProductionModifiers(sugar, null),
                                river1.getType()));
        assertFalse(tile1.canProduce(lumber, null));
        assertFalse(hasBonusFrom(tile1.getProductionModifiers(lumber, null),
                                 river1.getType()));
        // Hills can not produce sugar, but can produce ore.  They do not
        // get a road bonus for unattended ore production, but do get if
        // if attended.
        assertFalse(tile2.canProduce(sugar, null));
        assertFalse(hasBonusFrom(tile2.getProductionModifiers(sugar, null),
                                 road2.getType()));
        assertTrue(tile2.canProduce(ore, null));
        assertFalse(hasBonusFrom(tile2.getProductionModifiers(ore, null),
                                 road2.getType()));
        assertTrue(hasBonusFrom(tile2.getProductionModifiers(ore, colonistType),
                                road2.getType()));

        // Add a sugar resource, there should now be two sugar bonuses
        // on tile1.
        final Turn turn = getGame().getTurn();
        assertTrue(tile1.canProduce(sugar, null));
        int oldBase = tile1.getBaseProduction(null, sugar, null);
        Resource addedSugar = new Resource(game, tile1, sugarResource);
        tile1.addResource(addedSugar);
        int newBase = tile1.getBaseProduction(null, sugar, null);
        assertEquals(oldBase, newBase);
        assertEquals(
            (int)FeatureContainer.applyModifiers(newBase, turn,
                tile1.getProductionModifiers(sugar, null)),
            (int)FeatureContainer.applyModifiers(oldBase, turn,
                addedSugar.getProductionModifiers(sugar, null))
            + (int)FeatureContainer.applyModifiers(0f, turn,
                river1.getProductionModifiers(sugar, null)));
        assertTrue(hasBonusFrom(tile1.getProductionModifiers(sugar, null),
                                river1.getType()));
        assertTrue(hasBonusFrom(tile1.getProductionModifiers(sugar, null),
                                sugarResource));

        // Add a minerals resource, and tile2 should now produce silver.
        assertFalse(tile2.canProduce(silver, null));
        oldBase = tile2.getBaseProduction(null, silver, null);
        Resource addedSilver = new Resource(game, tile2, mineralsResource);
        tile2.addResource(addedSilver);
        newBase = tile2.getBaseProduction(null, silver, null);
        assertTrue(tile2.canProduce(silver, null));
        assertEquals(oldBase, newBase);
        assertEquals(
            (int)FeatureContainer.applyModifiers(newBase, turn,
                tile2.getProductionModifiers(silver, null)),
            (int)FeatureContainer.applyModifiers(oldBase, turn,
                addedSilver.getProductionModifiers(silver, null))
            + (int)FeatureContainer.applyModifiers(0f, turn,
                road2.getProductionModifiers(silver, null)));
        assertTrue(tile2.canProduce(silver, null));
        assertFalse(hasBonusFrom(tile2.getProductionModifiers(silver, null),
                                 road2.getType()));
        assertTrue(hasBonusFrom(tile2.getProductionModifiers(silver, colonistType),
                                road2.getType()));
        assertTrue(hasBonusFrom(tile2.getProductionModifiers(silver, null),
                                mineralsResource));
    }

    private boolean hasBonusFrom(List<Modifier> modifierSet,
                                 FreeColGameObjectType source) {
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

        colony.exciseSettlement();

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
        assertTrue(tile1.getPotentialProduction(sugar, sugarPlanter) > tile1.getPotentialProduction(sugar, null));
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

    public void testConiferForest() {
        Map map = getTestMap(coniferForest);
        Game game = getGame();
        game.setMap(map);

        Colony colony = getStandardColony(1);
        final Tile tile = colony.getTile();
        tile.addRiver(1, "1111"); // allow rivers to join
        final Iterable<Tile> tiles = tile.getSurroundingTiles(1);
        Tile firstTile = null;
        int i = 0;
        for (Tile t : tiles) {
            if (firstTile == null) firstTile = t;
            if ((i & 0b0001) == 0b0001) t.addRiver(1, "1111");// must be first!
            if ((i & 0b0010) == 0b0010) {
                TileImprovement road = t.addRoad();
                road.setTurnsToComplete(0);
            }
            if ((i & 0b0100) == 0b0100) t.addResource(new Resource(game, t, lumberResource, 99));
            i++;
        }
        ColonyTile firstColonyTile = colony.getColonyTile(firstTile);

        Unit unit = colony.getUnitList().get(0);
        assertEquals(colonistType, unit.getType());
        unit.setLocation(firstColonyTile);
        unit.changeWorkType(lumber);
        assertEquals("Added unit producing lumber", lumber,
            unit.getWorkType());

        // production = (BASE + RESOURCE) x EXPERT + RIVER + ROAD
        //   (+ untested)
        final int base = 6;
        final int riverBonus = 2;
        final int roadBonus = 2;
        final int resourceBonus = 4;
        final int expertBonus = 2;
        assertEquals("Base lumber production", base,
            coniferForest.getBaseProduction(null, lumber, colonistType));

        // Check all tiles with colonist unit
        i = 0;
        for (Tile t : tiles) {
            ColonyTile ct = colony.getColonyTile(t);
            unit.setLocation(ct);
            unit.changeWorkType(lumber);
            int result = base;
            if (t.hasRiver()) result += riverBonus;
            if (t.hasRoad()) result += roadBonus;
            if (t.hasResource()) result += resourceBonus;
            assertEquals("FreeColonist lumber production at tile " + i, result,
                ct.getTotalProductionOf(lumber));
            i++;
        }

        // Try again with expert unit
        assertEquals("Expert unit", expertLumberJack,
            firstColonyTile.getExpertUnitType());
        unit.setType(expertLumberJack);
        colony.invalidateCache();
        i = 0;
        for (Tile t : tiles) {
            ColonyTile ct = colony.getColonyTile(t);
            unit.setLocation(ct);
            unit.changeWorkType(lumber);
            int result = base * expertBonus;
            if (t.hasRiver()) result += riverBonus;
            if (t.hasRoad()) result += roadBonus;
            if (t.hasResource()) result += resourceBonus * expertBonus;
            assertEquals("Expert lumber production at tile " + i, result,
                ct.getTotalProductionOf(lumber));
            i++;
        }
    }

    public void testMinerals() {
        Game game = getGame();
        Map map = getTestMap(tundra);
        game.setMap(map);

        Colony colony = getStandardColony();
        Tile tile = colony.getTile().getNeighbourOrNull(Direction.N);
        ColonyTile colonyTile = colony.getColonyTile(tile);
        tile.addResource(new Resource(game, tile, mineralsResource));
        for (Unit u : colonyTile.getUnitList()) {
            u.setLocation(colony.getBuilding(townHallType));
        }
        assertTrue(colonyTile.isEmpty());
        assertEquals(colonyTile.getWorkTile().getOwningSettlement(), colony);

        Unit unit = colony.getUnitList().get(0);
        assertEquals(colonistType, unit.getType());
        assertTrue(silver.isFarmed());
        assertEquals(0, tundra.getPotentialProduction(silver, colonistType));
        assertEquals(1, tile.getPotentialProduction(silver, colonistType));
        assertEquals(1, tile.getProductionModifiers(silver, colonistType).size());
        assertEquals(1, colonyTile.getPotentialProduction(silver, unit.getType()));
        assertTrue(colonyTile.canBeWorked());
        assertTrue(colonyTile.canAdd(unit));
        assertEquals(colonyTile, colony.getWorkLocationFor(unit, silver));
    }

    public void testDefenceModifiers() {
        for (TileType tileType : spec().getTileTypeList()) {
            boolean present = tileType.isForested()
                || "model.tile.hills".equals(tileType.getId())
                || "model.tile.marsh".equals(tileType.getId())
                || "model.tile.mountains".equals(tileType.getId())
                || "model.tile.swamp".equals(tileType.getId());
            assertEquals("Defence for " + tileType.getId(), present,
                !tileType.getDefenceModifiers().isEmpty());
        }
    }

    public void testZIndex() {
        assertTrue(Tile.OVERLAY_ZINDEX < Tile.FOREST_ZINDEX);
        assertTrue(Tile.FOREST_ZINDEX < Tile.RESOURCE_ZINDEX);
        assertTrue(Tile.RESOURCE_ZINDEX < Tile.RUMOUR_ZINDEX);
        assertTrue(plow.getZIndex() < river.getZIndex());
        assertTrue(river.getZIndex() < road.getZIndex());
        assertTrue(Tile.FOREST_ZINDEX < road.getZIndex());
        assertTrue(road.getZIndex() < Tile.RESOURCE_ZINDEX);
    }

    public void testCopy() {
        Game game = getStandardGame();
        game.setMap(getTestMap(plains));
        Colony colony = getStandardColony();
        Tile tile = colony.getTile();

        Tile otherTile = tile.copy(game, tile.getClass());
        assertNotNull(otherTile);
        assertFalse(otherTile == tile);
        assertEquals(tile.getId(), otherTile.getId());
        assertEquals(tile.getType(), otherTile.getType());

        Colony otherColony = otherTile.getColony();
        assertEquals(otherTile, otherColony.getTile());
        assertEquals(otherTile.getOwningSettlement(), otherColony);
        assertFalse(colony == otherColony);
        assertEquals(colony.getId(), otherColony.getId());

        // Do not test units, colony owned tiles are not correctly
        // recognized as belonging to the colony which stops those
        // work locations from contributing their units.
    }

    public void testGetBestDisembarkTile() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plains, true);
        game.setMap(map);

        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Tile settlementTile = map.getTile(9, 2);
        FreeColTestUtils.getColonyBuilder().player(dutch)
            .colonyTile(settlementTile).build();
        Tile tileN = map.getTile(9, 1);
        assertTrue(tileN.isLand());
        Tile tileS = map.getTile(9, 3);
        assertTrue(tileS.isLand());
        Tile tileE = map.getTile(8, 2);
        assertTrue(tileE.isLand());
        tileS.setType(tundraForest);
        tileE.setType(mountains);
        
        List<Tile> tiles = settlementTile.getSafestSurroundingLandTiles(dutch);
        assertFalse("Surrounding tiles should be found", tiles.isEmpty());
        assertEquals("Best tile is mountainous", tileE, tiles.get(0));

        assertEquals("Best landing tile is forest", tileS, 
            settlementTile.getBestDisembarkTile(dutch));
        
        tileN.setType(hills);
        assertEquals("Best landing tile is now hills", tileN,
            settlementTile.getBestDisembarkTile(dutch));
    }
}
