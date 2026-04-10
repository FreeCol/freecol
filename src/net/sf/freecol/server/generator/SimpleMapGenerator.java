/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.alwaysTrue;
import static net.sf.freecol.common.util.CollectionUtils.any;
import static net.sf.freecol.common.util.CollectionUtils.descendingListLengthComparator;
import static net.sf.freecol.common.util.CollectionUtils.find;
import static net.sf.freecol.common.util.CollectionUtils.forEachMapEntry;
import static net.sf.freecol.common.util.CollectionUtils.isNotNull;
import static net.sf.freecol.common.util.CollectionUtils.isNull;
import static net.sf.freecol.common.util.CollectionUtils.map;
import static net.sf.freecol.common.util.CollectionUtils.matchKeyEquals;
import static net.sf.freecol.common.util.CollectionUtils.toListNoNulls;
import static net.sf.freecol.common.util.CollectionUtils.transform;
import static net.sf.freecol.common.util.RandomUtils.getRandomMember;
import static net.sf.freecol.common.util.RandomUtils.randomInt;
import static net.sf.freecol.common.util.RandomUtils.randomShuffle;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Area;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LandMap;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.RandomUtils.RandomIntCache;
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

    /** The random number source. */
    private final Random random;

    /** A cached random integer source. */
    private final RandomIntCache cache;


    /**
     * Creates a {@code MapGenerator}
     *
     * @param random The {@code Random} number source to use.
     * @see #generateMap
     */
    public SimpleMapGenerator(Random random) {
        this.random = random;
        this.cache = new RandomIntCache(logger, "simpleMap", random,
                                        1 << 16, 512);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map generateEmptyMap(Game game, int width, int height, LogBuilder lb) {
        final LandMap landMap = new LandMap(width, height, this.cache);
        return new TerrainGenerator(this.random).generateMap(game, null, landMap, lb);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map generateMap(Game game, Map importMap, boolean generateEuropeanPlayerUnits, LogBuilder lb) {
        final LandMap landMap = (importMap != null)
            ? new LandMap(importMap, this.cache)
            : new LandMap(game.getMapGeneratorOptions(), this.cache);

        final Map map = new TerrainGenerator(this.random).generateMap(game, importMap, landMap, lb);
        
        makeNativeSettlements(map, importMap, lb);
        makeLostCityRumours(map, importMap, lb);
        if (generateEuropeanPlayerUnits) {
            createEuropeanUnits(map, game.getLiveEuropeanPlayerList());
        }
        lb.shrink("\n");
        return map;
    }


    /**
     * Gets the approximate number of land tiles.
     *
     * @param game The {@code Game} to look up options in.
     * @return The approximate number of land tiles
     */
    private int getApproximateLandCount(Game game) {
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        return mapOptions.getInteger(MapGeneratorOptions.MAP_WIDTH)
            * mapOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT)
            * mapOptions.getInteger(MapGeneratorOptions.LAND_MASS) / 100;
    }

    /**
     * Make lost city rumours on the given map.
     *
     * The number of rumours depends on the map size.
     *
     * @param map The {@code Map} to use.
     * @param importMap An optional {@code Map} to import from.
     * @param lb A {@code LogBuilder} to log to.
     */
    private void makeLostCityRumours(Map map, Map importMap, LogBuilder lb) {
        final Game game = map.getGame();
        final boolean importRumours = game.getMapGeneratorOptions()
            .getBoolean(MapGeneratorOptions.IMPORT_RUMOURS);
        if (importMap != null && importRumours) {
            // Rumours were read from the import game, no need to do more
            return;
        }

        final int rumourNumber = game.getMapGeneratorOptions()
            .getRange(MapGeneratorOptions.RUMOUR_NUMBER);
        int number = getApproximateLandCount(game) / rumourNumber;
        int counter = 0;

        // FIXME: Remove temporary fix:
        if (importMap != null) {
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

    /**
     * Import the native settlements from a game.
     *
     * @param map The {@code Map} to import settlements to.
     * @param importMap The {@code Map} to import from.
     * @param lb The {@code LogBuilder} to log to.
     * @return True if the settlements were imported.
     */
    private boolean importIndianSettlements(Map map, Map importMap,
                                            LogBuilder lb) {
        final Game game = map.getGame();
        final Specification spec = game.getSpecification();
        // First make sure all the players are present.
        for (Player iPlayer : importMap.getGame().getLiveNativePlayerList()) {
            Player indian = game.getPlayerByNationId(iPlayer.getNationId());
            if (indian == null) {
                Nation nation = spec.getNation(iPlayer.getNationId());
                if (nation == null) {
                    lb.add("Native nation ", iPlayer.getNationId(),
                        " not found in spec.\n");
                } else {
                    indian = new ServerPlayer(game, false, nation);
                    lb.add("Imported new native nation ",
                        iPlayer.getNationId(), ": ", indian.getId(), "\n");
                    game.addPlayer(indian);
                }
            } else {
                lb.add("Found native nation ", iPlayer.getNationId(),
                    " for import: ", indian.getId(), "\n");
            }
        }

        // Make new settlements.
        List<Tile> iTiles = importMap.getTileList(isNotNull(Tile::getIndianSettlement)); 
        Set<IndianSettlement> newSettlements = new HashSet<>(iTiles.size());
        for (Tile iTile : iTiles) {
            final IndianSettlement is = iTile.getIndianSettlement();
            if (is.getOwner() == null) continue;
            final Player owner = game.getPlayerByNationId(is.getOwner()
                .getNationId());
            if (owner == null) continue;
            
            final UnitType iSkill = is.getLearnableSkill();
            ServerIndianSettlement sis = new ServerIndianSettlement(game,
                owner, is.getName(), map.getTile(iTile.getX(), iTile.getY()),
                is.isCapital(),
                ((iSkill == null) ? null : spec.getUnitType(iSkill.getId())),
                null);
            sis.placeSettlement(false);
            for (Tile it : is.getOwnedTiles()) {
                map.getTile(it.getX(), it.getY()).changeOwnership(owner, sis);
            }

            List<Unit> iUnits = is.getUnitList();
            if (iUnits.isEmpty()) {
                sis.addUnits(random);
            } else {
                for (Unit iu : iUnits) {
                    UnitType it = spec.getUnitType(iu.getType().getId());
                    if (it != null) {
                        Unit su = new ServerUnit(game, sis, owner, it);
                        sis.add(su);
                        sis.addOwnedUnit(su);
                    }
                }
            }
            
            List<Goods> iGoods = is.getCompactGoodsList();
            if (iGoods.isEmpty()) {
                sis.addRandomGoods(random);
            } else {
                for (Goods ig : iGoods) {
                    GoodsType it = spec.getGoodsType(ig.getType().getId());
                    if (it != null) {
                        sis.addGoods(it, ig.getAmount());
                    }
                }
            }
            
            sis.setWantedGoods(transform(is.getWantedGoods(), alwaysTrue(),
                    ig -> (ig == null) ? null : spec.getGoodsType(ig.getId()),
                    toListNoNulls()));

            owner.addSettlement(sis);
            newSettlements.add(sis);
        }

        lb.add("Imported ", newSettlements.size(), " native settlements.\n");
        return !newSettlements.isEmpty();
    }

    /**
     * Make the native settlements, at least a capital for every
     * nation and random numbers of other settlements.
     *
     * @param map The {@code Map} to place the indian settlements on.
     * @param importMap An optional {@code Map} to import from.
     * @param lb A {@code LogBuilder} to log to.
     */
    private void makeNativeSettlements(final Map map, Map importMap,
                                       LogBuilder lb) {
        final Game game = map.getGame();
        final Specification spec = game.getSpecification();
        final boolean importSettlements = game.getMapGeneratorOptions() .getBoolean(MapGeneratorOptions.IMPORT_SETTLEMENTS);
        if (importSettlements && importMap != null && importIndianSettlements(map, importMap, lb)) {
            return;
        }
        
        List<IndianSettlement> settlements = new ArrayList<>();
        final List<Player> nativePlayers = game.getLiveNativePlayerList();
        if (nativePlayers.isEmpty()) {
            return;
        }
        
        // Examine all the non-polar settleable tiles in a random
        // order picking out as many as possible suitable tiles for
        // native settlements such that can be guaranteed at least one
        // layer of surrounding tiles to own.
        final List<Tile> allTiles = map.getShuffledTiles(random);
        final int minDistance = spec.getRange(GameOptions.SETTLEMENT_NUMBER);
        
        final Set<Tile> settlementTiles = new LinkedHashSet<>();
        for (Tile tile : allTiles) {
            if (tile.isPolar()) {
                continue;
            }
            if (!suitableForNativeSettlement(tile)) {
                continue;
            }
            if (any(settlementTiles, t -> t.getDistanceTo(tile) < minDistance)) {
                continue;
            }
            settlementTiles.add(tile);
        }
        
        final java.util.Map<Player, Set<Tile>> designatedArea = new HashMap<>();
        for (Player player : nativePlayers) {
            final Area area = map.getGame().getNationStartingArea(player.getNation());
            if (area != null && !area.isEmpty()) {
                final Set<Tile> settlementTilesForNation = area.getTiles().stream()
                        .filter(t -> settlementTiles.contains(t))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                if (settlementTilesForNation.isEmpty()) {
                    logger.warning("No settlements for nationType=" + player.getNationId());
                }
                designatedArea.put(player, settlementTilesForNation);
            }
        }
        for (Player player : nativePlayers) {
            if (designatedArea.containsKey(player)) {
                continue;
            }

            final IndianNationType indianNationType = (IndianNationType) player.getNationType();
            final List<Rectangle> otherRegions = indianNationType.getRegions().stream()
                    .map(key -> ((ServerRegion) map.getRegionByKey(key)).getBounds())
                    .filter(bounds -> !bounds.isEmpty())
                    .collect(Collectors.toList());
                        
            if (otherRegions.isEmpty()) {
                logger.warning("No area or regions found for nationType=" + player.getNationId());
                continue;
            }
            
            final Set<Tile> settlementTilesForNation = settlementTiles.stream()
                    .filter(t -> otherRegions.stream().anyMatch(bounds -> bounds.contains(t.getX(), t.getY())))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            
            if (settlementTilesForNation.isEmpty()) {
                logger.warning("No settlements in region for nationType=" + player.getNationId());
            }
            designatedArea.put(player, settlementTilesForNation);
        }
        
        // Place the capitals:
        for (Player player : nativePlayers) {
            final Set<Tile> allowedPlacements = designatedArea.get(player);
            if (allowedPlacements == null) {
                continue;
            }
            for (Tile settlementTile : new ArrayList<>(settlementTiles)) {
                if (!allowedPlacements.contains(settlementTile)) {
                    continue;
                }
                
                final IndianSettlement capital = placeIndianSettlement(player, true, settlementTile, map, lb);
                settlements.add(capital);
                settlementTiles.remove(settlementTile);
                break;
            }
        }
        
        for (Tile settlementTile : settlementTiles) {
            final List<Player> players = new ArrayList<>(designatedArea.keySet());
            Collections.shuffle(players, random);
            
            final Player owner = players.stream().filter(p -> {
                    final Set<Tile> tiles = designatedArea.get(p);
                    if (tiles == null) {
                        return false;
                    }
                    return tiles.contains(settlementTile);
                })
                .findFirst()
                .orElse(null);
            
            if (owner == null) {
                continue;
            }
            
            final IndianSettlement settlement = placeIndianSettlement(owner, false, settlementTile, map, lb);
            settlements.add(settlement);
        }
        
        // Grow some more tiles.
        // FIXME: move the magic numbers below to the spec
        // Also collect the skills provided
        HashMap<UnitType, List<IndianSettlement>> skills = new HashMap<>();
        randomShuffle(logger, "Settlements", settlements, random);
        for (IndianSettlement is : settlements) {
            List<Tile> tiles = transform(is.getOwnedTiles(),
                t -> any(t.getSurroundingTiles(1, 1),
                         isNull(Tile::getOwningSettlement)));
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
            isList.sort(descendingListLengthComparator);
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
                RandomChoice<UnitType> rc
                    = find(nation.generateSkillsForTile(is.getTile()),
                        matchKeyEquals(neededSkill, RandomChoice::getObject));
                choices.add(new RandomChoice<>(is,
                        (rc == null) ? 1 : rc.getProbability() * cm));
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
    }

    /**
     * Is a tile suitable for a native settlement?
     * Require the tile be settleable, and at least half its neighbours
     * also be settleable.
     *
     * FIXME: degrade the second test to usability, but wait until the
     * natives-use-water situation is sorted.
     *
     * @param tile The {@code Tile} to examine.
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

    /**
     * Find a free neighbouring tile to a settlement from a list of choices.
     *
     * @param is The {@code IndianSettlement} that might claim the tile.
     * @param tiles A list of {@code Tile}s to start searching from.
     * @return The first suitable tile found, or null if none present.
     */
    private Tile findFreeNeighbouringTile(IndianSettlement is,
                                          List<Tile> tiles) {
        final Player owner = is.getOwner();
        final Predicate<Tile> freeTilePred = t -> t != null
            && t.getOwningSettlement() == null
            && owner.canClaimForSettlement(t);
        final Direction[] dirns = Direction.getRandomDirections("freeTile",
                                                                logger, random);
        for (Tile t : tiles) {
            Tile ret = find(map(dirns, d -> t.getNeighbourOrNull(d)),
                            freeTilePred);
            if (ret != null) return ret;
        }
        return null;
    }

    /**
     * Builds a {@code IndianSettlement} at the given position.
     *
     * @param player The player owning the new settlement.
     * @param capital {@code true} if the settlement should be a
     *      {@link IndianSettlement#isCapital() capital}.
     * @param tile The {@code Tile} to place the settlement.
     * @param map The map that should get a new settlement.
     * @param lb A {@code LogBuilder} to log to.
     * @return The {@code IndianSettlement} just being placed
     *      on the map.
     */
    private IndianSettlement placeIndianSettlement(Player player,
        boolean capital, Tile tile, Map map, LogBuilder lb) {
        String name = (capital) ? player.getCapitalName(random)
            : player.getSettlementName(random);
        UnitType skill
            = generateSkillForLocation(map, tile, player.getNationType());
        ServerIndianSettlement sis
            = new ServerIndianSettlement(map.getGame(), player, name, tile,
                                         capital, skill, null);
        player.addSettlement(sis);
        lb.add("Generated skill for ", sis.getName(),
            ": ", sis.getLearnableSkill().getSuffix(), "\n");

        sis.placeSettlement(true);
        sis.addRandomGoods(random);
        sis.addUnits(random);
        return sis;
    }

    /**
     * Generates a skill that could be taught from a settlement on the
     * given tile.
     *
     * @param map The {@code Map}.
     * @param tile The {@code Tile} where the settlement will be located.
     * @param nationType The {@code NationType} to generate a skill for.
     * @return A skill that can be taught to Europeans.
     */
    private UnitType generateSkillForLocation(Map map, Tile tile,
                                              NationType nationType) {
        List<RandomChoice<UnitType>> skills
            = ((IndianNationType)nationType).getSkills();
        java.util.Map<GoodsType, Integer> scale
            = transform(skills, alwaysTrue(),
                        Function.<RandomChoice<UnitType>>identity(),
                        Collectors.toMap(rc ->
                            rc.getObject().getExpertProduction(), rc -> 1));

        for (Tile t: tile.getSurroundingTiles(1)) {
            forEachMapEntry(scale, e -> {
                    GoodsType goodsType = e.getKey();
                    scale.put(goodsType, e.getValue()
                        + t.getPotentialProduction(goodsType, null));
                });
        }

        final Function<RandomChoice<UnitType>, RandomChoice<UnitType>> mapper
            = rc -> {
                UnitType unitType = rc.getObject();
                return new RandomChoice<>(unitType, rc.getProbability()
                    * scale.get(unitType.getExpertProduction()));
            };
        UnitType skill = RandomChoice.getWeightedRandom(null, null,
            transform(skills, alwaysTrue(), mapper), random);
        final Specification spec = map.getSpecification();
        return (skill != null) ? skill
            : getRandomMember(logger, "Scout",
                spec.getUnitTypesWithAbility(Ability.EXPERT_SCOUT), random);
    }

    private void createEuropeanUnits(Map map, List<Player> liveEuropeanPlayerList) {
        final EuropeanStartingPositionsGenerator startPosGenerator = new EuropeanStartingPositionsGenerator(random);
        startPosGenerator.createEuropeanUnits(map, liveEuropeanPlayerList);
    }
}
