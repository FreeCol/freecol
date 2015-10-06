/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.server.generator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LandMap;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Creates random maps and sets the starting locations for the players.
 *
 * No visibility implications here as this all happens pre-game,
 * so no +/-vis annotations are needed.
 */
public class SimpleMapGenerator implements MapGenerator {

    private static final Logger logger = Logger.getLogger(SimpleMapGenerator.class.getName());

    /**
     * To avoid starting positions too close to the poles, this
     * percentage indicating how much of the half map close to the
     * pole cannot be spawned on.
     */
    private static final float MIN_DISTANCE_FROM_POLE = 0.30f;

   
    private static class Territory {
        public ServerRegion region;
        public Tile tile;
        public final Player player;
        public int numberOfSettlements;

        public Territory(Player player, Tile tile) {
            this.player = player;
            this.tile = tile;
        }

        public Territory(Player player, ServerRegion region) {
            this.player = player;
            this.region = region;
        }

        public Tile getCenterTile(Map map) {
            if (tile != null) return tile;
            int[] xy = region.getCenter();
            return map.getTile(xy[0], xy[1]);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return player + " territory at " + region;
        }
    }

    /** The game to generate for. */
    private final Game game;

    /** The random number source. */
    private final Random random;

    /** The cached map generator options. */
    private OptionGroup mapOptions;

    /** The cached specification to use. */
    private Specification spec;

    /** The game to selectively import from. */
    private Game importGame;


    /**
     * Creates a <code>MapGenerator</code>
     *
     * @param game The <code>Game</code> to generate for.
     * @param random The <code>Random</code> number source to use.
     * @see #createMap
     */
    public SimpleMapGenerator(Game game, Random random) {
        this.game = game;
        this.random = random;
    }


    /**
     * Update the cached variables.
     *
     * @param checkImport Check for an import file or not.
     */
    private void recache(boolean checkImport) {
        this.mapOptions = game.getMapGeneratorOptions();
        this.spec = game.getSpecification();
        File importFile = (checkImport) ? ((FileOption)this.mapOptions
            .getOption(MapGeneratorOptions.IMPORT_FILE)).getValue()
            : null;
        this.importGame = (importFile == null) ? null
            : FreeColServer.readGame(importFile, this.spec, null);
    }

    /**
     * Gets the approximate number of land tiles.
     *
     * @return The approximate number of land tiles
     */
    private int getApproximateLandCount() {
        return mapOptions.getInteger(MapGeneratorOptions.MAP_WIDTH)
            * mapOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT)
            * mapOptions.getInteger(MapGeneratorOptions.LAND_MASS) / 100;
    }

    /**
     * Make lost city rumours on the given map.
     *
     * The number of rumours depends on the map size.
     *
     * @param map The <code>Map</code> to use.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void makeLostCityRumours(Map map, LogBuilder lb) {
        final boolean importRumours
            = mapOptions.getBoolean(MapGeneratorOptions.IMPORT_RUMOURS);
        if (importGame != null && importRumours) {
            int nLCRs = 0;
            for (Tile importTile : importGame.getMap().getAllTiles()) {
                LostCityRumour rumour = importTile.getLostCityRumour();
                // no rumor
                if (rumour == null) continue;
                int x = importTile.getX();
                int y = importTile.getY();
                if (map.isValid(x, y)) {
                    final Tile t = map.getTile(x, y);
                    rumour.setLocation(t);
                    t.addLostCityRumour(rumour);
                    nLCRs++;
                }
            }
            if (nLCRs > 0) {
                lb.add("Imported ", nLCRs, " lost city rumours.\n");
                return;
            }
            // Otherwise fall through and create them
        }

        final int rumourNumber
            = mapOptions.getInteger(MapGeneratorOptions.RUMOUR_NUMBER);
        int number = getApproximateLandCount() / rumourNumber;
        int counter = 0;

        // FIXME: Remove temporary fix:
        if (importGame != null) {
            number = map.getWidth() * map.getHeight() * 25 / (100 * 35);
        }

        for (int i = 0; i < number; i++) {
            for (int tries = 0; tries < 100; tries++) {
                Tile t = map.getRandomLandTile(random);
                if (t.isPolar()) continue; // No polar lost cities
                if (t.isLand() && !t.hasLostCityRumour()
                    && !t.hasSettlement() && t.getUnitCount() == 0) {
                    LostCityRumour r = new LostCityRumour(t.getGame(), t);
                    if (r.chooseType(null, random)
                        == LostCityRumour.RumourType.MOUNDS
                        && t.getOwningSettlement() != null) {
                        r.setType(LostCityRumour.RumourType.MOUNDS);
                    }
                    t.addLostCityRumour(r);
                    counter++;
                    break;
                }
            }
        }
        lb.add("Created ", counter,
            " lost city rumours of maximum ", number, ".\n");
    }

    private boolean importIndianSettlements(Map map, LogBuilder lb) {
        int nSettlements = 0;
        
        for (Player player : importGame.getLiveNativePlayers(null)) {
            Player indian = game.getPlayerByNationId(player.getNationId());
            if (indian == null) {
                Nation nation = spec.getNation(player.getNationId());
                if (nation == null) {
                    lb.add("Native nation ", player.getNationId(),
                        " not found in spec.\n");
                    continue;
                }
                indian = new ServerPlayer(game, false, nation, null, null);
                lb.add("Imported new native nation ", player.getNationId(),
                    ": ", indian.getId(), "\n");
                game.addPlayer(indian);
            } else {
                lb.add("Found native nation ", player.getNationId(),
                    " for import: ", indian.getId(), "\n");
            }
        }
        for (Tile tile : importGame.getMap().getAllTiles()) {
            IndianSettlement is = tile.getIndianSettlement();
            if (is == null) continue;
            Player indian = game.getPlayerByNationId(is.getOwner().getNationId());
            ServerIndianSettlement settlement
                = new ServerIndianSettlement(game, indian, is.getName(),
                    map.getTile(tile.getX(), tile.getY()), is.isCapital(),
                    is.getLearnableSkill(), null);
            settlement.placeSettlement(false);
            for (Tile t : is.getOwnedTiles()) {
                map.getTile(t.getX(), t.getY())
                    .changeOwnership(indian, settlement);
            }
                
            List<Unit> units = is.getUnitList();
            if (units.isEmpty()) {
                settlement.addUnits(random);
            } else {
                for (Unit unit : units) {
                    UnitType t = spec.getUnitType(unit.getType().getId());
                    if (t != null) {
                        Unit u = new ServerUnit(game, settlement, indian, t);
                        settlement.add(u);
                        settlement.addOwnedUnit(u);
                    }
                }
            }
            
            List<Goods> goods = is.getCompactGoods();
            if (goods.isEmpty()) {
                settlement.addRandomGoods(random);
            } else {
                for (Goods g : goods) {
                    GoodsType t = spec.getGoodsType(g.getType().getId());
                    if (t != null) {
                        settlement.addGoods(t, g.getAmount());
                    }
                }
            }
            settlement.setWantedGoods(is.getWantedGoods());
            indian.addSettlement(settlement);
            nSettlements++;
        }

        if (nSettlements > 0) {
            for (Tile t : importGame.getMap().getAllTiles()) {
                if (t.getOwner() == null) continue;
                Player owner = game.getPlayerByNationId(t.getOwner()
                    .getNationId());
                if (owner == null) continue;
                Tile tile = map.getTile(t.getX(), t.getY());
                if (tile == null) continue;
                tile.setOwner(owner);
                if (tile.getOwningSettlement() != null) {
                    String name = tile.getOwningSettlement().getName();
                    Settlement is = game.getSettlementByName(name);
                    tile.setOwningSettlement(is);
                }
            }
        }
        lb.add("Imported ", nSettlements, " native settlements.\n");
        return nSettlements > 0;
    }

    /**
     * Make the native settlements, at least a capital for every
     * nation and random numbers of other settlements.
     *
     * @param map The <code>Map</code> to place the indian settlements on.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void makeNativeSettlements(final Map map, LogBuilder lb) {
        final boolean importSettlements
            = mapOptions.getBoolean(MapGeneratorOptions.IMPORT_SETTLEMENTS);
        if (importSettlements && importGame != null) {
            if (importIndianSettlements(map, lb)) return;
            // Fall through and create them
        }
        
        final Game game = map.getGame();
        float shares = 0f;
        List<IndianSettlement> settlements = new ArrayList<>();
        List<Player> indians = new ArrayList<>();
        HashMap<String, Territory> territoryMap = new HashMap<>();

        for (Player player : game.getLiveNativePlayers(null)) {
            switch (player.getNationType()
                    .getNumberOfSettlements()) {
            case HIGH:
                shares += 4;
                break;
            case AVERAGE:
                shares += 3;
                break;
            case LOW:
                shares += 2;
                break;
            }
            indians.add(player);
            List<String> regionKeys
                = ((IndianNationType)player.getNationType()).getRegionNames();
            Territory territory = null;
            if (regionKeys == null || regionKeys.isEmpty()) {
                territory = new Territory(player, map.getRandomLandTile(random));
                territoryMap.put(player.getId(), territory);
            } else {
                for (String key : regionKeys) {
                    if (territoryMap.get(key) == null) {
                        ServerRegion region = (ServerRegion)map.getRegionByKey(key);
                        if (region == null) {
                            territory = new Territory(player, map.getRandomLandTile(random));
                        } else {
                            territory = new Territory(player, region);
                        }
                        territoryMap.put(key, territory);
                        lb.add("Allocated region ", key,
                            " for ", player, ".\n");
                        break;
                    }
                }
                if (territory == null) {
                    lb.add("Failed to allocate preferred region ",
                        regionKeys.get(0), " for ", player.getNation(), "\n");
                    outer: for (String key : regionKeys) {
                        Territory otherTerritory = territoryMap.get(key);
                        for (String otherKey : ((IndianNationType) otherTerritory.player.getNationType())
                                 .getRegionNames()) {
                            if (territoryMap.get(otherKey) == null) {
                                ServerRegion foundRegion = otherTerritory.region;
                                otherTerritory.region = (ServerRegion)map.getRegionByKey(otherKey);
                                territoryMap.put(otherKey, otherTerritory);
                                territory = new Territory(player, foundRegion);
                                territoryMap.put(key, territory);
                                break outer;
                            }
                        }
                    }
                    if (territory == null) {
                        lb.add("Unable to find free region for ",
                            player.getName(), "\n");
                        territory = new Territory(player, map.getRandomLandTile(random));
                        territoryMap.put(player.getId(), territory);
                    }
                }
            }
        }
        if (indians.isEmpty()) return;

        // Examine all the non-polar settleable tiles in a random
        // order picking out as many as possible suitable tiles for
        // native settlements such that can be guaranteed at least one
        // layer of surrounding tiles to own.
        List<Tile> allTiles = new ArrayList<>();
        for (Tile t : map.getAllTiles()) allTiles.add(t);
        randomShuffle(logger, "All tile shuffle", allTiles, random);
        final int minDistance
            = spec.getRangeOption(GameOptions.SETTLEMENT_NUMBER).getValue();
        List<Tile> settlementTiles = new ArrayList<>();
        for (Tile tile : allTiles) {
            if (!tile.isPolar()
                && suitableForNativeSettlement(tile)
                && none(settlementTiles, t -> t.getDistanceTo(tile) < minDistance))
                settlementTiles.add(tile);
        }
        randomShuffle(logger, "Settlement tiles", settlementTiles, random);

        // Check number of settlements.
        int settlementsToPlace = settlementTiles.size();
        float share = settlementsToPlace / shares;
        if (settlementTiles.size() < indians.size()) {
            // FIXME: something drastic to boost the settlement number
            lb.add("There are only ", settlementTiles.size(),
                " settlement sites.\n",
                " This is smaller than ", indians.size(),
                " the number of tribes.\n");
        }

        // Find the capitals
        List<Territory> territories
            = new ArrayList<>(territoryMap.values());
        int settlementsPlaced = 0;
        for (Territory territory : territories) {
            switch (territory.player.getNationType()
                    .getNumberOfSettlements()) {
            case HIGH:
                territory.numberOfSettlements = Math.round(4 * share);
                break;
            case AVERAGE:
                territory.numberOfSettlements = Math.round(3 * share);
                break;
            case LOW:
                territory.numberOfSettlements = Math.round(2 * share);
                break;
            }
            int radius = territory.player.getNationType().getCapitalType()
                .getClaimableRadius();
            IndianSettlement is = placeCapital(map, territory, radius,
                new ArrayList<>(settlementTiles), lb);
            if (is != null) {
                settlements.add(is);
                settlementsPlaced++;
                settlementTiles.remove(is.getTile());
            }
        }

        // Sort tiles from the edges of the map inward
        Collections.sort(settlementTiles, Tile.edgeDistanceComparator);

        // Now place other settlements
        while (!settlementTiles.isEmpty() && !territories.isEmpty()) {
            Tile tile = settlementTiles.remove(0);
            if (tile.getOwner() != null) continue; // No close overlap

            Territory territory = getClosestTerritory(map, tile, territories);
            int radius = territory.player.getNationType().getSettlementType(false)
                .getClaimableRadius();
            // Insist that the settlement can not be linear
            if (territory.player.getClaimableTiles(tile, radius).size()
                > 2 * radius + 1) {
                String name = (territory.region == null) ? "default region"
                    : territory.region.toString();
                lb.add("Placing a ", territory.player,
                    " camp in region: ", name,
                    " at tile: ", tile, "\n");
                settlements.add(placeIndianSettlement(territory.player,
                                                      false, tile, map, lb));
                settlementsPlaced++;
                territory.numberOfSettlements--;
                if (territory.numberOfSettlements <= 0) {
                    territories.remove(territory);
                }

            }
        }

        // Grow some more tiles.
        // FIXME: move the magic numbers below to the spec
        // Also collect the skills provided
        HashMap<UnitType, List<IndianSettlement>> skills = new HashMap<>();
        randomShuffle(logger, "Settlements", settlements, random);
        for (IndianSettlement is : settlements) {
            List<Tile> tiles = new ArrayList<>();
            for (Tile tile : is.getOwnedTiles()) {
                for (Tile t : tile.getSurroundingTiles(1)) {
                    if (t.getOwningSettlement() == null) {
                        tiles.add(tile);
                        break;
                    }
                }
            }
            randomShuffle(logger, "Settlement tiles", tiles, random);
            int minGrow = is.getType().getMinimumGrowth();
            int maxGrow = is.getType().getMaximumGrowth();
            if (maxGrow > minGrow) {
                for (int i = randomInt(logger, "Gdiff", random,
                                       maxGrow - minGrow) + minGrow;
                     i > 0; i--) {
                    Tile tile = findFreeNeighbouringTile(is, tiles);
                    if (tile == null) break;
                    tile.changeOwnership(is.getOwner(), is);
                    tiles.add(tile);
                }
            }

            // Collect settlements by skill
            UnitType skill = is.getLearnableSkill();
            List<IndianSettlement> isList = skills.get(skill);
            if (isList == null) {
                isList = new ArrayList<>();
                isList.add(is);
                skills.put(skill, isList);
            } else {
                isList.add(is);
            }
        }

        // Require that there be experts for all the new world goods types.
        // Collect the list of needed experts
        List<UnitType> expertsNeeded = new ArrayList<>();
        for (GoodsType goodsType : spec.getNewWorldGoodsTypeList()) {
            UnitType expert = spec.getExpertForProducing(goodsType);
            if (!skills.containsKey(expert)) expertsNeeded.add(expert);
        }
        // Extract just the settlement lists.
        List<List<IndianSettlement>> isList = new ArrayList<>(skills.values());
        // For each missing skill...
        while (!expertsNeeded.isEmpty()) {
            UnitType neededSkill = expertsNeeded.remove(0);
            Collections.sort(isList, descendingListLengthComparator);
            List<IndianSettlement> extras = isList.remove(0);
            UnitType extraSkill = extras.get(0).getLearnableSkill();
            List<RandomChoice<IndianSettlement>> choices = new ArrayList<>();
            // ...look at the settlements with the most common skill
            // with a bit of favoritism to capitals as the needed skill
            // is so rare,...
            for (IndianSettlement is : extras) {
                IndianNationType nation
                    = (IndianNationType) is.getOwner().getNationType();
                int cm = (is.isCapital()) ? 2 : 1;
                RandomChoice<IndianSettlement> rc = null;
                for (RandomChoice<UnitType> c : nation.generateSkillsForTile(is.getTile())) {
                    if (c.getObject() == neededSkill) {
                        rc = new RandomChoice<>(is, c.getProbability() * cm);
                        break;
                    }
                }
                choices.add((rc != null) ? rc
                            : new RandomChoice<>(is, 1));
            }
            if (!choices.isEmpty()) {
                // ...and pick one that could do the missing job.
                IndianSettlement chose = RandomChoice
                    .getWeightedRandom(logger, "expert", choices, random);
                lb.add("At ", chose.getName(),
                    " replaced ", extraSkill,
                    " (one of ", extras.size(), ")",
                    " by missing ", neededSkill, "\n");
                chose.setLearnableSkill(neededSkill);
                extras.remove(chose);
                isList.add(0, extras); // Try to stay well sorted
                List<IndianSettlement> neededList = new ArrayList<>();
                neededList.add(chose);
                isList.add(neededList);
            } else { // `can not happen'
                lb.add("Game is missing skill: ", neededSkill, "\n");
            }
        }
        lb.add("Settlement skills:");
        for (List<IndianSettlement> iss : isList) {
            if (iss.isEmpty()) {
                lb.add("  0 x <none>");
            } else {
                lb.add("  ", iss.size(),
                    " x ", iss.get(0).getLearnableSkill().getSuffix());
            }
        }
        lb.add("\nCreated ", settlementsPlaced,
            " Indian settlements of maximum ", settlementsToPlace, ".\n");
    }

    /**
     * Is a tile suitable for a native settlement?
     * Require the tile be settleable, and at least half its neighbours
     * also be settleable.
     *
     * FIXME: degrade the second test to usability, but wait until the
     * natives-use-water situation is sorted.
     *
     * @param tile The <code>Tile</code> to examine.
     * @return True if this tile is suitable.
     */
    private boolean suitableForNativeSettlement(Tile tile) {
        if (!tile.getType().canSettle()) return false;
        int good = 0, n = 0;
        for (Tile t : tile.getSurroundingTiles(1)) {
            if (t.getType().canSettle()) good++;
            n++;
        }
        return good >= n / 2;
    }

    private Tile findFreeNeighbouringTile(IndianSettlement is,
                                          List<Tile> tiles) {
        for (Tile tile : tiles) {
            for (Direction d : Direction.getRandomDirections("freeTile",
                    logger, random)) {
                Tile t = tile.getNeighbourOrNull(d);
                if ((t != null)
                    && (t.getOwningSettlement() == null)
                    && (is.getOwner().canClaimForSettlement(t))) return t;
            }
        }
        return null;
    }

    private Territory getClosestTerritory(Map map, Tile tile,
                                          List<Territory> territories) {
        Territory result = null;
        int minimumDistance = Integer.MAX_VALUE;
        for (Territory territory : territories) {
            int distance = map.getDistance(tile, territory.getCenterTile(map));
            if (distance < minimumDistance) {
                minimumDistance = distance;
                result = territory;
            }
        }
        return result;
    }

    /**
     * Place a native capital in a territory.
     *
     * @param map The <code>Map</code> to place the settlement in.
     * @param territory The <code>Territory</code> within the map.
     * @param radius The settlement radius.
     * @param tiles A list of <code>Tile</code>s to select from.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The <code>IndianSettlement</code> placed, or null if
     *     none placed.
     */
    private IndianSettlement placeCapital(final Map map, Territory territory,
                                          int radius, List<Tile> tiles,
                                          LogBuilder lb) {
        final Tile center = territory.getCenterTile(map);
        Collections.sort(tiles, new Comparator<Tile>() {
                @Override
                public int compare(Tile t1, Tile t2) {
                    return t1.getDistanceTo(center) - t2.getDistanceTo(center);
                }
            });
        for (Tile t : tiles) {
            // Choose this tile if it is free and half the expected tile
            // claim can succeed (preventing capitals on small islands).
            if (territory.player.getClaimableTiles(t, radius).size()
                >= (2 * radius + 1) * (2 * radius + 1) / 2) {
                String name = (territory.region == null) ? "default region"
                    : territory.region.toString();
                lb.add("Placing the ", territory.player,
                    " capital in region: ", name, " at tile: ", t, "\n");
                IndianSettlement is = placeIndianSettlement(territory.player,
                    true, t, map, lb);
                territory.numberOfSettlements--;
                territory.tile = t;
                return is;
            }
        }
        return null;
    }

    /**
     * Builds a <code>IndianSettlement</code> at the given position.
     *
     * @param player The player owning the new settlement.
     * @param capital <code>true</code> if the settlement should be a
     *      {@link IndianSettlement#isCapital() capital}.
     * @param tile The <code>Tile</code> to place the settlement.
     * @param map The map that should get a new settlement.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return The <code>IndianSettlement</code> just being placed
     *      on the map.
     */
    private IndianSettlement placeIndianSettlement(Player player,
        boolean capital, Tile tile, Map map, LogBuilder lb) {
        String name = (capital) ? player.getCapitalName(random)
            : player.getSettlementName(random);
        UnitType skill
            = generateSkillForLocation(map, tile, player.getNationType());
        ServerIndianSettlement settlement
            = new ServerIndianSettlement(map.getGame(), player, name, tile,
                                         capital, skill, null);
        player.addSettlement(settlement);
        lb.add("Generated skill for ", settlement.getName(),
            ": ", settlement.getLearnableSkill().getSuffix(), "\n");

        settlement.placeSettlement(true);
        settlement.addRandomGoods(random);
        settlement.addUnits(random);

        return settlement;
    }

    /**
     * Generates a skill that could be taught from a settlement on the
     * given tile.
     *
     * @param map The <code>Map</code>.
     * @param tile The <code>Tile</code> where the settlement will be located.
     * @param nationType The <code>NationType</code> to generate a skill for.
     * @return A skill that can be taught to Europeans.
     */
    private UnitType generateSkillForLocation(Map map, Tile tile,
                                              NationType nationType) {
        List<RandomChoice<UnitType>> skills
            = ((IndianNationType)nationType).getSkills();
        java.util.Map<GoodsType, Integer> scale = new HashMap<>();
        for (RandomChoice<UnitType> skill : skills) {
            scale.put(skill.getObject().getExpertProduction(), 1);
        }

        for (Tile t: tile.getSurroundingTiles(1)) {
            for (Entry<GoodsType, Integer> entry : scale.entrySet()) {
                GoodsType goodsType = entry.getKey();
                scale.put(goodsType, entry.getValue()
                          + t.getPotentialProduction(goodsType, null));
            }
        }

        List<RandomChoice<UnitType>> scaledSkills = new ArrayList<>();
        for (RandomChoice<UnitType> skill : skills) {
            UnitType unitType = skill.getObject();
            int scaleValue = scale.get(unitType.getExpertProduction());
            scaledSkills.add(new RandomChoice<>(unitType,
                    skill.getProbability() * scaleValue));
        }
        UnitType skill = RandomChoice.getWeightedRandom(null, null,
                                                        scaledSkills, random);
        if (skill == null) {
            // Seasoned Scout
            List<UnitType> unitList = map.getSpecification().getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
            return getRandomMember(logger, "Scout", unitList, random);
        } else {
            return skill;
        }
    }

    /**
     * Create two ships, one with a colonist, for each player, and
     * select suitable starting positions.
     *
     * @param map The <code>Map</code> to place the european units on.
     * @param players The players to create <code>Settlement</code>s
     *      and starting locations for. That is; both indian and
     *      european players.
     * @param lb A <code>LogBuilder</code> to log to.
     */
    private void createEuropeanUnits(Map map, List<Player> players,
                                     LogBuilder lb) {
        final int width = map.getWidth();
        final int height = map.getHeight();
        final int poleDistance = (int)(MIN_DISTANCE_FROM_POLE*height/2);

        List<Player> europeanPlayers = new ArrayList<>();
        for (Player player : players) {
            if (player.isREF()) {
                // eastern edge of the map
                int x = width - 2;
                // random latitude, not too close to the pole
                int y = randomInt(logger, "Pole", random,
                                  height - 2*poleDistance) + poleDistance;
                player.setEntryLocation(map.getTile(x, y));
                continue;
            }
            if (player.isEuropean()) europeanPlayers.add(player);
        }

        List<Position> positions = generateStartingPositions(map, europeanPlayers);
        List<Tile> startingTiles = new ArrayList<>();
        List<Unit> carriers = new ArrayList<>();
        List<Unit> passengers = new ArrayList<>();

        for (int index = 0; index < europeanPlayers.size(); index++) {
            Player player = europeanPlayers.get(index);
            Position position = positions.get(index);
            lb.add("Generating units for player ", player, ".\n");

            carriers.clear();
            passengers.clear();
            List<AbstractUnit> unitList = ((EuropeanNationType) player.getNationType())
                .getStartingUnits();
            for (AbstractUnit startingUnit : unitList) {
                UnitType type = startingUnit.getType(spec);
                Role role = startingUnit.getRole(spec);
                Unit newUnit = new ServerUnit(game, null, player, type, role);
                newUnit.setName(player.getNameForUnit(type, random));
                if (newUnit.isNaval()) {
                    if (newUnit.canCarryUnits()) {
                        newUnit.setState(Unit.UnitState.ACTIVE);
                        carriers.add(newUnit);
                    }
                } else {
                    newUnit.setState(Unit.UnitState.SENTRY);
                    passengers.add(newUnit);
                }

            }

            boolean startAtSea = true;
            if (carriers.isEmpty()) {
                lb.add("No carriers defined for player ", player, ".\n");
                startAtSea = false;
            }

            Tile startTile = null;
            int x = position.getX();
            int y = position.getY();
            for (int i = 0; i < 2 * map.getHeight(); i++) {
                int offset = (i % 2 == 0) ? i / 2 : -(1 + i / 2);
                int row = y + offset;
                if (row < 0 || row >= map.getHeight()) continue;
                startTile = findTileFor(map, row, x, startAtSea, lb);
                if (startTile != null) {
                    if (startingTiles.contains(startTile)) {
                        startTile = null;
                    } else {
                        startingTiles.add(startTile);
                        break;
                    }
                }
            }
            if (startTile == null) {
                LogBuilder lb2 = new LogBuilder(64);
                lb2.add("Failed to find start tile ",
                    ((startAtSea) ? "at sea" : "on land"),
                    " for player ", player,
                    " from (", x, ",", y, ") avoiding:");
                for (Tile t : startingTiles) lb2.add(" ", t);
                lb2.add(" with map: ");
                for (int xx = 0; xx < map.getWidth(); xx++) {
                    lb2.add(" ", map.getTile(xx, y));
                }
                throw new RuntimeException(lb2.toString());
            }

            player.setEntryLocation(startTile);

            if (startAtSea) {
                for (Unit carrier : carriers) {
                    carrier.setLocation(startTile);
                    ((ServerPlayer)player).exploreForUnit(carrier);
                }
                passengers: for (Unit unit : passengers) {
                    for (Unit carrier : carriers) {
                        if (carrier.canAdd(unit)) {
                            unit.setLocation(carrier);
                            continue passengers;
                        }
                    }
                    // no space left on carriers
                    unit.setLocation(player.getEurope());
                }
            } else {
                for (Unit unit : passengers) {
                    unit.setLocation(startTile);
                    ((ServerPlayer)player).exploreForUnit(unit);
                }
            }

            if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.INIT)) {
                createDebugUnits(map, player, startTile, lb);
                IntegerOption op = spec.getIntegerOption(GameOptions.STARTING_MONEY);
                if (op != null) op.setValue(10000);
            }
        }
    }

    private Tile findTileFor(Map map, int row, int start, boolean startAtSea,
                             LogBuilder lb) {
        Tile tile = null;
        Tile seas = null;
        int offset = (start == 0) ? 1 : -1;
        for (int x = start; 0 <= x && x < map.getWidth(); x += offset) {
            tile = map.getTile(x, row);
            if (tile.isDirectlyHighSeasConnected()) {
                seas = tile;
            } else if (tile.isLand()) {
                return (startAtSea) ? seas : tile;
            } 
        }
        lb.add("No land in row ", row, ".\n");
        return null;
    }

    private void createDebugUnits(Map map, Player player, Tile startTile,
                                  LogBuilder lb) {
        // In debug mode give each player a few more units and a colony.
        UnitType unitType = spec.getUnitType("model.unit.galleon");
        Unit unit4 = new ServerUnit(game, startTile, player, unitType);
        unitType = spec.getUnitType("model.unit.privateer");
        Unit privateer = new ServerUnit(game, startTile, player, unitType);
        ((ServerPlayer)player).exploreForUnit(privateer);
        unitType = spec.getUnitType("model.unit.freeColonist");
        Unit unit5 = new ServerUnit(game, unit4, player, unitType);
        unitType = spec.getUnitType("model.unit.veteranSoldier");
        Unit unit6 = new ServerUnit(game, unit4, player, unitType);
        unitType = spec.getUnitType("model.unit.jesuitMissionary");
        Unit unit7 = new ServerUnit(game, unit4, player, unitType);

        Tile colonyTile = null;
        for (Tile tempTile : map.getCircleTiles(startTile, true, 
                                                FreeColObject.INFINITY)) {
            if (tempTile.isPolar()) continue; // No initial polar colonies
            if (player.canClaimToFoundSettlement(tempTile)) {
                colonyTile = tempTile;
                break;
            }
        }

        if (colonyTile == null) {
            lb.add("Could not find a debug colony site.\n");
            return;
        }
        colonyTile.setType(find(spec.getTileTypeList(), t -> !t.isWater()));
        unitType = spec.getUnitType("model.unit.expertFarmer");
        Unit buildColonyUnit = new ServerUnit(game, colonyTile,
                                              player, unitType);
        String colonyName = Messages.message(player.getNationLabel())
            + " " + Messages.message("Colony");
        Colony colony = new ServerColony(game, player, colonyName, colonyTile);
        player.addSettlement(colony);
        colony.placeSettlement(true);
        for (Tile tile : colonyTile.getSurroundingTiles(1)) {
            if (!tile.hasSettlement()
                && (tile.getOwner() == null
                    || !tile.getOwner().isEuropean())) {
                tile.changeOwnership(player, colony);
                if (tile.hasLostCityRumour()) {
                    tile.removeLostCityRumour();
                }
            }
        }
        buildColonyUnit.setLocation(colony);
        if (buildColonyUnit.getLocation() instanceof ColonyTile) {
            Tile ct = ((ColonyTile) buildColonyUnit.getLocation()).getWorkTile();
            for (TileType t : spec.getTileTypeList()) {
                if (!t.isWater()) {
                    ct.setType(t);
                    TileImprovementType plowType = map.getSpecification()
                        .getTileImprovementType("model.improvement.plow");
                    TileImprovement plow = new TileImprovement(game, ct, plowType);
                    plow.setTurnsToComplete(0);
                    ct.add(plow);
                    break;
                }
            }
        }
        BuildingType schoolType = spec.getBuildingType("model.building.schoolhouse");
        Building schoolhouse = new ServerBuilding(game, colony, schoolType);
        colony.addBuilding(schoolhouse);

        unitType = spec.getUnitType("model.unit.elderStatesman");
        Unit statesman = new ServerUnit(game, colonyTile, player, unitType);
        statesman.setLocation(colony.getWorkLocationFor(statesman,
                statesman.getType().getExpertProduction()));

        unitType = spec.getUnitType("model.unit.expertLumberJack");
        Unit lumberjack = new ServerUnit(game, colony, player, unitType);
        if (lumberjack.getLocation() instanceof ColonyTile) {
            Tile lt = ((ColonyTile) lumberjack.getLocation()).getWorkTile();
            for (TileType t : spec.getTileTypeList()) {
                if (t.isForested()) {
                    lt.setType(t);
                    break;
                }
            }
            lumberjack.changeWorkType(lumberjack.getType()
                .getExpertProduction());
        }

        unitType = spec.getUnitType("model.unit.masterCarpenter");
        Unit carpenter = new ServerUnit(game, colony, player, unitType);

        unitType = spec.getUnitType("model.unit.seasonedScout");
        Unit scout = new ServerUnit(game, colonyTile, player, unitType);
        ((ServerPlayer)player).exploreForUnit(scout);

        unitType = spec.getUnitType("model.unit.veteranSoldier");
        Unit unit8 = new ServerUnit(game, colonyTile, player, unitType);
        Unit unit9 = new ServerUnit(game, colonyTile, player, unitType);
        unitType = spec.getUnitType("model.unit.artillery");
        Unit unit10 = new ServerUnit(game, colonyTile, player, unitType);
        Unit unit11 = new ServerUnit(game, colonyTile, player, unitType);
        Unit unit12 = new ServerUnit(game, colonyTile, player, unitType);
        unitType = spec.getUnitType("model.unit.treasureTrain");
        Unit unit13 = new ServerUnit(game, colonyTile, player, unitType);
        unit13.setTreasureAmount(10000);
        unitType = spec.getUnitType("model.unit.wagonTrain");
        Unit unit14 = new ServerUnit(game, colonyTile, player, unitType);
        GoodsType cigarsType = spec.getGoodsType("model.goods.cigars");
        Goods cigards = new Goods(game, unit14, cigarsType, 5);
        unit14.add(cigards);
        unitType = spec.getUnitType("model.unit.jesuitMissionary");
        Unit unit15 = new ServerUnit(game, colonyTile, player, unitType);
        Unit unit16 = new ServerUnit(game, colonyTile, player, unitType);

        ((ServerPlayer)player).exploreForSettlement(colony);
    }

    private List<Position> generateStartingPositions(Map map,
                                                     List<Player> players) {
        int number = players.size();
        List<Position> positions = new ArrayList<>(number);
        if (number > 0) {
            int west = 0;
            int east = map.getWidth() - 1;
            switch (spec.getInteger(GameOptions.STARTING_POSITIONS)) {
            case GameOptions.STARTING_POSITIONS_CLASSIC:
                int distance = map.getHeight() / number;
                int row = distance/2;
                for (int index = 0; index < number; index++) {
                    positions.add(new Position(east, row));
                    row += distance;
                }
                randomShuffle(logger, "Classic starting positions",
                              positions, random);
                break;
            case GameOptions.STARTING_POSITIONS_RANDOM:
                distance = 2 * map.getHeight() / number;
                row = distance/2;
                for (int index = 0; index < number; index++) {
                    if (index % 2 == 0) {
                        positions.add(new Position(east, row));
                    } else {
                        positions.add(new Position(west, row));
                        row += distance;
                    }
                }
                randomShuffle(logger, "Random starting positions",
                              positions, random);
                break;
            case GameOptions.STARTING_POSITIONS_HISTORICAL:
                for (Player player : players) {
                    Nation nation = player.getNation();
                    positions.add(new Position(nation.startsOnEastCoast() ? east : west,
                                               map.getRow(nation.getPreferredLatitude())));
                }
                break;
            }
        }
        return positions;
    }


    // Implement MapGenerator

    /**
     * {@inheritDoc}
     */
    @Override
    public Map createEmptyMap(int width, int height, LogBuilder lb) {
        recache(false); // Reload the options and specification

        return new TerrainGenerator(game, null, random)
            .createMap(new LandMap(width, height), lb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map createMap(LogBuilder lb) {
        recache(true); // Reload the options and specification

        // Create land map.
        LandMap landMap = (importGame != null) ? new LandMap(importGame)
            : new LandMap(mapOptions, random);

        // Create terrain.
        Map map = new TerrainGenerator(game, importGame, random)
            .createMap(landMap, lb);

        // Decorate the map.
        makeNativeSettlements(map, lb);
        makeLostCityRumours(map, lb);
        createEuropeanUnits(map, game.getLiveEuropeanPlayers(null), lb);
        return map;
    }
}
