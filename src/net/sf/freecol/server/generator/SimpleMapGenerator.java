/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GameOptions;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerBuilding;
import net.sf.freecol.server.model.ServerColony;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerRegion;
import net.sf.freecol.server.model.ServerUnit;


/**
 * Creates random maps and sets the starting locations for the players.
 */
public class SimpleMapGenerator implements MapGenerator {

    private static final Logger logger = Logger.getLogger(SimpleMapGenerator.class.getName());

    private final Random random;
    private final OptionGroup mapGeneratorOptions;

    private final LandGenerator landGenerator;
    private final TerrainGenerator terrainGenerator;

    // To avoid starting positions to be too close to the poles
    // percentage indicating how much of the half map close to the pole cannot be spawned on
    private static final float MIN_DISTANCE_FROM_POLE = 0.30f;


    /**
     * Creates a <code>MapGenerator</code>
     *
     * @param random The <code>Random</code> number source to use.
     * @param specification a <code>Specification</code> value
     * @see #createMap
     */
    public SimpleMapGenerator(Random random, Specification specification) {
        this.random = random;
        this.mapGeneratorOptions = specification.getOptionGroup("mapGeneratorOptions");
        landGenerator = new LandGenerator(mapGeneratorOptions, random);
        terrainGenerator = new TerrainGenerator(mapGeneratorOptions, random);
    }

    /**
     * Returns the approximate number of land tiles.
     *
     * @return the approximate number of land tiles
     */
    private int getLand() {
        return mapGeneratorOptions.getInteger("model.option.mapWidth")
            * mapGeneratorOptions.getInteger("model.option.mapHeight")
            * mapGeneratorOptions.getInteger("model.option.landMass")
            / 100;
    }

    /**
     * Creates a map given for a game.
     *
     * @param game The <code>Game</code> to use.
     * @see net.sf.freecol.server.generator.IMapGenerator#createMap(net.sf.freecol.common.model.Game)
     * @see net.sf.freecol.server.generator.IMapGenerator#createMap(net.sf.freecol.common.model.Game)
     */
    public void createMap(Game game) throws FreeColException {
        // Prepare imports:
        final File importFile = ((FileOption) getMapGeneratorOptions()
            .getOption(MapGeneratorOptions.IMPORT_FILE)).getValue();
        final Game importGame;
        if (importFile != null) {
            Game g = null;
            try {
                logger.info("Importing file " + importFile.getPath());
                g = FreeColServer
                    .readGame(new FreeColSavegameFile(importFile),
                        game.getSpecification(), null);
            } catch (IOException ioe) {
                g = null;
            }
            importGame = g;
        } else {
            importGame = null;
        }

        // Create land map:
        boolean[][] landMap;
        if (importGame != null) {
            landMap = LandGenerator.importLandMap(importGame);
        } else {
            landMap = landGenerator.createLandMap();
        }

        // Create terrain:
        terrainGenerator.createMap(game, importGame, landMap);

        Map map = game.getMap();
        if (map.getRegions() == null || map.getRegions().isEmpty()) {
            terrainGenerator.createOceanRegions(map);
            terrainGenerator.createLandRegions(map);
        }
        createIndianSettlements(map, game.getPlayers());
        createEuropeanUnits(map, game.getPlayers());
        createLostCityRumours(map, importGame);

    }

    /**
     * Creates a <code>Map</code> for the given <code>Game</code>.
     *
     * The <code>Map</code> is added to the <code>Game</code> after
     * it is created.
     *
     * @param game The game.
     * @param landMap Determines whether there should be land
     *                or ocean on a given tile. This array also
     *                specifies the size of the map that is going
     *                to be created.
     * @see Map
     * @see TerrainGenerator#createMap
     */
    public void createEmptyMap(Game game, boolean[][] landMap) {
        terrainGenerator.createMap(game, null, landMap);
    }

    public LandGenerator getLandGenerator() {
        return landGenerator;
    }

    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    /* (non-Javadoc)
     * @see net.sf.freecol.server.generator.IMapGenerator#getMapGeneratorOptions()
     */
    public OptionGroup getMapGeneratorOptions() {
        return mapGeneratorOptions;
    }

    /**
     * Creates lost city rumours on the given map.
     * The number of rumours depends on the map size.
     *
     * @param map The map to use.
     * @param importGame The game to lost city rumours from.
     */
    private void createLostCityRumours(Map map, Game importGame) {
        final boolean importRumours = getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_RUMOURS);

        if (importGame != null && importRumours) {
            for (Tile importTile : importGame.getMap().getAllTiles()) {
            	LostCityRumour rumor = importTile.getLostCityRumour();
            	// no rumor
            	if(rumor == null){
            		continue;
            	}
                final Position p = importTile.getPosition();
                if (map.isValid(p)) {
                    final Tile t = map.getTile(p);
                    t.add(rumor);
                }
            }
        } else {
            int number = getLand() / getMapGeneratorOptions().getInteger("model.option.rumourNumber");
            int counter = 0;

            // TODO: Remove temporary fix:
            if (importGame != null) {
                number = map.getWidth() * map.getHeight() * 25 / (100 * 35);
            }
            // END TODO

            int difficulty = map.getGame().getSpecification()
                .getInteger("model.option.rumourDifficulty");
            for (int i = 0; i < number; i++) {
                for (int tries=0; tries<100; tries++) {
                    Position p = map.getRandomLandPosition(random);
                    Tile t = map.getTile(p);
                    if (map.isPolar(t)) continue; // No polar lost cities
                    if (t.isLand() && !t.hasLostCityRumour()
                        && t.getSettlement() == null && t.getUnitCount() == 0) {
                        LostCityRumour r = new LostCityRumour(t.getGame(), t);
                        if (r.chooseType(null, difficulty, random)
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

            logger.info("Created " + counter
                        + " lost city rumours of maximum " + number + ".");
        }
    }

    /**
     * Create the Indian settlements, at least a capital for every nation and
     * random numbers of other settlements.
     *
     * @param map The <code>Map</code> to place the indian settlements on.
     * @param players The players to create <code>Settlement</code>s
     *       and starting locations for. That is; both indian and
     *       european players. If players does not contain any indian players,
     *       no settlements are added.
     */
    private void createIndianSettlements(final Map map, List<Player> players) {
        Specification spec = map.getGame().getSpecification();
        float shares = 0f;
        List<IndianSettlement> settlements = new ArrayList<IndianSettlement>();
        List<Player> indians = new ArrayList<Player>();
        HashMap<String, Territory> territoryMap
            = new HashMap<String, Territory>();

        for (Player player : players) {
            if (!player.isIndian()) continue;
            switch (((IndianNationType) player.getNationType())
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
            List<String> regionNames
                = ((IndianNationType) player.getNationType()).getRegionNames();
            Territory territory = null;
            if (regionNames == null || regionNames.isEmpty()) {
                territory = new Territory(player, map.getRandomLandPosition(random));
                territoryMap.put(player.getId(), territory);
            } else {
                for (String name : regionNames) {
                    if (territoryMap.get(name) == null) {
                        ServerRegion region = (ServerRegion) map.getRegion(name);
                        if (region == null) {
                            territory = new Territory(player, map.getRandomLandPosition(random));
                        } else {
                            territory = new Territory(player, region);
                        }
                        territoryMap.put(name, territory);
                        logger.fine("Allocated region " + name
                                    + " for " + player
                                    + ". Center is " + territory.getCenter()
                                    + ".");
                        break;
                    }
                }
                if (territory == null) {
                    logger.warning("Failed to allocate preferred region " + regionNames.get(0)
                                   + " for " + player.getNation());
                    outer: for (String name : regionNames) {
                        Territory otherTerritory = territoryMap.get(name);
                        for (String otherName : ((IndianNationType) otherTerritory.player.getNationType())
                                 .getRegionNames()) {
                            if (territoryMap.get(otherName) == null) {
                                ServerRegion foundRegion = otherTerritory.region;
                                otherTerritory.region = (ServerRegion) map.getRegion(otherName);
                                territoryMap.put(otherName, otherTerritory);
                                territory = new Territory(player, foundRegion);
                                territoryMap.put(name, territory);
                                break outer;
                            }
                        }
                    }
                    if (territory == null) {
                        logger.warning("Unable to find free region for "
                                       + player.getName());
                        territory = new Territory(player, map.getRandomLandPosition(random));
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
        int minSettlementDistance
            = spec.getRangeOption("model.option.settlementNumber").getValue();
        List<Tile> settlementTiles = new ArrayList<Tile>();
        tiles: for (Tile tile : map.getAllTiles()) {
            if (!map.isPolar(tile) && suitableForNativeSettlement(tile)) {
                for (Tile t : settlementTiles) {
                    if (tile.getDistanceTo(t) < minSettlementDistance) {
                        continue tiles;
                    }
                }
                settlementTiles.add(tile);
            }
        }
        Collections.shuffle(settlementTiles, random);

        // Check number of settlements.
        int settlementsToPlace = settlementTiles.size();
        float share = settlementsToPlace / shares;
        if (settlementTiles.size() < indians.size()) {
            // TODO: something drastic to boost the settlement number
            logger.warning("There are only " + settlementTiles.size()
                           + " settlement sites."
                           + " This is smaller than " + indians.size()
                           + " the number of tribes.");
        }

        // Find the capitals
        List<Territory> territories
            = new ArrayList<Territory>(territoryMap.values());
        int settlementsPlaced = 0;
        for (Territory territory : territories) {
            switch (((IndianNationType) territory.player.getNationType())
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
            int radius = territory.player.getNationType().getCapitalType().getClaimableRadius();
            ArrayList<Tile> capitalTiles = new ArrayList<Tile>(settlementTiles);
            while (!capitalTiles.isEmpty()) {
                Tile tile = getClosestTile(territory.getCenter(),
                                           capitalTiles);
                capitalTiles.remove(tile);
                // Choose this tile if it is free and half the expected tile
                // claim can succeed (preventing capitals on small islands).
                if (map.getClaimableTiles(territory.player, tile, radius).size()
                    >= (2 * radius + 1) * (2 * radius + 1) / 2) {
                    String name = (territory.region == null) ? "default region"
                        : territory.region.getNameKey();
                    logger.fine("Placing the " + territory.player
                                + " capital in region: " + name
                                + " at Tile: "+ tile.getPosition());
                    settlements.add(placeIndianSettlement(territory.player,
                            true, tile.getPosition(), map));
                    territory.numberOfSettlements--;
                    territory.position = tile.getPosition();
                    settlementTiles.remove(tile);
                    settlementsPlaced++;
                    break;
                }
            }
        }

        // Sort tiles from the edges of the map inward
        Collections.sort(settlementTiles, new Comparator<Tile>() {
            public int compare(Tile tile1, Tile tile2) {
                int distance1 = Math.min(Math.min(tile1.getX(), map.getWidth() - tile1.getX()),
                                         Math.min(tile1.getY(), map.getHeight() - tile1.getY()));
                int distance2 = Math.min(Math.min(tile2.getX(), map.getWidth() - tile2.getX()),
                                         Math.min(tile2.getY(), map.getHeight() - tile2.getY()));
                return (distance1 - distance2);
            }
        });

        // Now place other settlements
        while (!settlementTiles.isEmpty() && !territories.isEmpty()) {
            Tile tile = settlementTiles.remove(0);
            if (tile.getOwner() != null) continue; // No close overlap

            Territory territory = getClosestTerritory(tile, territories);
            int radius = territory.player.getNationType().getSettlementType(false)
                .getClaimableRadius();
            // Insist that the settlement can not be linear
            if (map.getClaimableTiles(territory.player, tile, radius).size()
                > 2 * radius + 1) {
                String name = (territory.region == null) ? "default region"
                    : territory.region.getNameKey();
                logger.fine("Placing a " + territory.player
                            + " camp in region: " + name
                            + " at Tile: " + tile.getPosition());
                settlements.add(placeIndianSettlement(territory.player,
                        false, tile.getPosition(), map));
                settlementsPlaced++;
                territory.numberOfSettlements--;
                if (territory.numberOfSettlements <= 0) {
                    territories.remove(territory);
                }

            }
        }

        // Grow some more tiles.
        // TODO: move the magic numbers below to the spec RSN
        // Also collect the skills provided
        HashMap<UnitType, List<IndianSettlement>> skills
            = new HashMap<UnitType, List<IndianSettlement>>();
        Collections.shuffle(settlements, random);
        for (IndianSettlement is : settlements) {
            List<Tile> tiles = new ArrayList<Tile>();
            for (Tile tile : is.getOwnedTiles()) {
                for (Tile t : tile.getSurroundingTiles(1)) {
                    if (t.getOwningSettlement() == null) {
                        tiles.add(tile);
                        break;
                    }
                }
            }
            Collections.shuffle(tiles, random);
            int minGrow = is.getType().getMinimumGrowth();
            int maxGrow = is.getType().getMaximumGrowth();
            if (maxGrow > minGrow) {
                for (int i = random.nextInt(maxGrow - minGrow) + minGrow;
                     i > 0; i--) {
                    Tile tile = findFreeNeighbouringTile(is, tiles, random);
                    if (tile == null) break;
                    tile.setOwner(is.getOwner());
                    tile.setOwningSettlement(is);
                    tiles.add(tile);
                }
            }

            // Collect settlements by skill
            UnitType skill = is.getLearnableSkill();
            List<IndianSettlement> isList = skills.get(skill);
            if (isList == null) {
                isList = new ArrayList<IndianSettlement>();
                isList.add(is);
                skills.put(skill, isList);
            } else {
                isList.add(is);
            }
        }

        // Require that there be experts for all the new world goods types.
        // Collect the list of needed experts
        List<UnitType> expertsNeeded = new ArrayList<UnitType>();
        for (GoodsType goodsType : spec.getNewWorldGoodsTypeList()) {
            UnitType expert = spec.getExpertForProducing(goodsType);
            if (!skills.containsKey(expert)) expertsNeeded.add(expert);
        }
        // Extract just the settlement lists.
        List<List<IndianSettlement>> isList
            = new ArrayList<List<IndianSettlement>>(skills.values());
        Comparator<List<IndianSettlement>> listComparator
            = new Comparator<List<IndianSettlement>>() {
                public int compare(List<IndianSettlement> l1,
                                   List<IndianSettlement> l2) {
                    return l2.size() - l1.size();
                }
            };
        // For each missing skill...
        while (!expertsNeeded.isEmpty()) {
            UnitType neededSkill = expertsNeeded.remove(0);
            Collections.sort(isList, listComparator);
            List<IndianSettlement> extras = isList.remove(0);
            UnitType extraSkill = extras.get(0).getLearnableSkill();
            List<RandomChoice<IndianSettlement>> choices
                = new ArrayList<RandomChoice<IndianSettlement>>();
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
                        rc = new RandomChoice<IndianSettlement>(is, c.getProbability() * cm);
                        break;
                    }
                }
                choices.add((rc != null) ? rc
                            : new RandomChoice<IndianSettlement>(is, 1));
            }
            if (!choices.isEmpty()) {
                // ...and pick one that could do the missing job.
                IndianSettlement chose
                    = RandomChoice.getWeightedRandom(logger, "expert", random,
                                                     choices);
                logger.finest("At " + chose.getName()
                              + " replaced " + extraSkill
                              + " (one of " + extras.size() + ")"
                              + " by missing " + neededSkill);
                chose.setLearnableSkill(neededSkill);
                extras.remove(chose);
                isList.add(0, extras); // Try to stay well sorted
                List<IndianSettlement> neededList
                    = new ArrayList<IndianSettlement>();
                neededList.add(chose);
                isList.add(neededList);
            } else { // `can not happen'
                logger.finest("Game is missing skill: " + neededSkill);
            }
        }
        String msg = "Settlement skills:";
        for (List<IndianSettlement> iss : isList) {
            msg += "  " + iss.size() + " x " + iss.get(0).getLearnableSkill();
        }
        logger.info(msg);

        logger.info("Created " + settlementsPlaced
                    + " Indian settlements of maximum " + settlementsToPlace);
    }

    /**
     * Is a tile suitable for a native settlement?
     * Require the tile be settleable, and at least half its neighbours
     * also be settleable.  TODO: degrade the second test to usability,
     * but fix this when the natives-use-water situation is sorted.
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
                                          List<Tile> tiles, Random random) {
        for (Tile tile : tiles) {
            for (Direction d : Direction.getRandomDirectionArray(random)) {
                Tile t = tile.getNeighbourOrNull(d);
                if ((t != null)
                    && (t.getOwningSettlement() == null)
                    && (is.getOwner().canClaimForSettlement(t))) return t;
            }
        }
        return null;
    }

    private Tile getClosestTile(Position center, List<Tile> tiles) {
        Tile result = null;
        int minimumDistance = Integer.MAX_VALUE;
        for (Tile tile : tiles) {
            int distance = tile.getPosition().getDistance(center);
            if (distance < minimumDistance) {
                minimumDistance = distance;
                result = tile;
            }
        }
        return result;
    }

    private Territory getClosestTerritory(Tile tile, List<Territory> territories) {
        Territory result = null;
        int minimumDistance = Integer.MAX_VALUE;
        for (Territory territory : territories) {
            int distance = tile.getPosition().getDistance(territory.getCenter());
            if (distance < minimumDistance) {
                minimumDistance = distance;
                result = territory;
            }
        }
        return result;
    }


    /**
     * Builds a <code>IndianSettlement</code> at the given position.
     *
     * @param player The player owning the new settlement.
     * @param capital <code>true</code> if the settlement should be a
     *      {@link IndianSettlement#isCapital() capital}.
     * @param position The position to place the settlement.
     * @param map The map that should get a new settlement.
     * @return The <code>IndianSettlement</code> just being placed
     *      on the map.
     */
    private IndianSettlement placeIndianSettlement(Player player, boolean capital,
                                       Position position, Map map) {
        final Tile tile = map.getTile(position);
        String name = (capital) ? player.getCapitalName()
            : player.getSettlementName();
        if (Player.ASSIGN_SETTLEMENT_NAME.equals(name)) {
            player.installSettlementNames(Messages.getSettlementNames(player),
                                          random);
            name = (capital) ? player.getCapitalName()
                : player.getSettlementName();
        }
        UnitType skill
            = generateSkillForLocation(map, tile, player.getNationType());
        IndianSettlement settlement
            = new ServerIndianSettlement(map.getGame(), player, name, tile,
                                         capital, skill,
                                         new HashSet<Player>(), null);
        player.addSettlement(settlement);
        logger.fine("Generated skill: " + settlement.getLearnableSkill());

        int low = settlement.getType().getMinimumSize();
        int high = settlement.getType().getMaximumSize();
        int unitCount = low + random.nextInt(high - low);
        for (int i = 0; i < unitCount; i++) {
            UnitType unitType = map.getSpecification().getUnitType("model.unit.brave");
            Unit unit = new ServerUnit(map.getGame(), settlement, player,
                unitType, unitType.getDefaultEquipment());
            unit.setIndianSettlement(settlement);

            if (i == 0) {
                unit.setLocation(tile);
            } else {
                unit.setLocation(settlement);
            }
        }
        settlement.placeSettlement(true);

        // START DEBUG:
        if (FreeCol.isInDebugMode()) {
            for (GoodsType goodsType : map.getSpecification().getGoodsTypeList()) {
                if (goodsType.isNewWorldGoodsType())
                    settlement.addGoods(goodsType, 150);
            }
        }
        // END DEBUG

        return settlement;
    }

    /**
     * Generates a skill that could be taught from a settlement on the given Tile.
     *
     * @param map The <code>Map</code>.
     * @param tile The tile where the settlement will be located.
     * @return A skill that can be taught to Europeans.
     */
    private UnitType generateSkillForLocation(Map map, Tile tile, NationType nationType) {
        List<RandomChoice<UnitType>> skills = ((IndianNationType) nationType).getSkills();
        java.util.Map<GoodsType, Integer> scale = new HashMap<GoodsType, Integer>();
        for (RandomChoice<UnitType> skill : skills) {
            scale.put(skill.getObject().getExpertProduction(), 1);
        }

        for (Tile t: tile.getSurroundingTiles(1)) {
            for (GoodsType goodsType : scale.keySet()) {
                scale.put(goodsType, scale.get(goodsType).intValue() + t.potential(goodsType, null));
            }
        }

        List<RandomChoice<UnitType>> scaledSkills = new ArrayList<RandomChoice<UnitType>>();
        for (RandomChoice<UnitType> skill : skills) {
            UnitType unitType = skill.getObject();
            int scaleValue = scale.get(unitType.getExpertProduction()).intValue();
            scaledSkills.add(new RandomChoice<UnitType>(unitType, skill.getProbability() * scaleValue));
        }
        UnitType skill = RandomChoice.getWeightedRandom(null, null,
                                                        random, scaledSkills);
        if (skill == null) {
            // Seasoned Scout
            List<UnitType> unitList = map.getSpecification().getUnitTypesWithAbility(Ability.EXPERT_SCOUT);
            return unitList.get(random.nextInt(unitList.size()));
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
     */
    private void createEuropeanUnits(Map map, List<Player> players) {
        Game game = map.getGame();
        Specification spec = game.getSpecification();
        final int width = map.getWidth();
        final int height = map.getHeight();
        final int poleDistance = (int)(MIN_DISTANCE_FROM_POLE*height/2);

        List<Player> europeanPlayers = new ArrayList<Player>();
        for (Player player : players) {
            if (player.isREF()) {
                // eastern edge of the map
                int x = width - 2;
                // random latitude, not too close to the pole
                int y = random.nextInt(height - 2*poleDistance) + poleDistance;
                player.setEntryLocation(map.getTile(x, y));
                continue;
            }
            if (player.isEuropean()) {
                europeanPlayers.add(player);
                logger.finest("found European player " + player);
            }
        }

        List<Position> positions = generateStartingPositions(map, europeanPlayers);
        List<Tile> startingTiles = new ArrayList<Tile>();

        for (int index = 0; index < europeanPlayers.size(); index++) {
            Player player = europeanPlayers.get(index);
            Position position = positions.get(index);
            logger.fine("generating units for player " + player);

            List<Unit> carriers = new ArrayList<Unit>();
            List<Unit> passengers = new ArrayList<Unit>();
            List<AbstractUnit> unitList = ((EuropeanNationType) player.getNationType())
                .getStartingUnits();
            for (AbstractUnit startingUnit : unitList) {
                Unit newUnit = new ServerUnit(game, null, player,
                                              startingUnit.getUnitType(spec),
                                              startingUnit.getEquipment(spec));
                if (newUnit.canCarryUnits() && newUnit.isNaval()) {
                    newUnit.setState(Unit.UnitState.ACTIVE);
                    carriers.add(newUnit);
                } else {
                    newUnit.setState(Unit.UnitState.SENTRY);
                    passengers.add(newUnit);
                }

            }

            boolean startAtSea = true;
            if (carriers.isEmpty()) {
                logger.warning("No carriers defined for player " + player);
                startAtSea = false;
            }

            Tile startTile = null;
            int x = position.getX();
            int y = position.getY();
            for (int i = 0; i < 2 * map.getHeight(); i++) {
                int offset = (i % 2 == 0) ? i / 2 : -(1 + i / 2);
                startTile = findTileFor(map, y + offset, x, startAtSea);
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
                throw new RuntimeException("Failed to find start tile for player "
                                           + player);
            }

            startTile.setExploredBy(player, true);
            player.setEntryLocation(startTile);

            if (startAtSea) {
                for (Unit carrier : carriers) {
                    carrier.setLocation(startTile);
                }
                passengers: for (Unit unit : passengers) {
                    for (Unit carrier : carriers) {
                        if (carrier.getSpaceLeft() >= unit.getSpaceTaken()) {
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
                }
            }

            if (FreeCol.getDebugLevel() >= FreeCol.DEBUG_FULL) {
                createDebugUnits(map, player, startTile);
            }
        }
    }

    private void createDebugUnits(Map map, Player player, Tile startTile) {

        Game game = map.getGame();
        Specification spec = game.getSpecification();

        // In debug mode give each player a few more units and a colony.
        UnitType unitType = spec.getUnitType("model.unit.galleon");
        Unit unit4 = new ServerUnit(game, startTile, player, unitType);

        unitType = spec.getUnitType("model.unit.privateer");
        @SuppressWarnings("unused")
        Unit privateer = new ServerUnit(game, startTile, player, unitType);

        unitType = spec.getUnitType("model.unit.freeColonist");
        @SuppressWarnings("unused")
        Unit unit5 = new ServerUnit(game, unit4, player, unitType);
        unitType = spec.getUnitType("model.unit.veteranSoldier");
        @SuppressWarnings("unused")
        Unit unit6 = new ServerUnit(game, unit4, player, unitType);
        unitType = spec.getUnitType("model.unit.jesuitMissionary");
        @SuppressWarnings("unused")
        Unit unit7 = new ServerUnit(game, unit4, player, unitType);

        Tile colonyTile = null;
        Iterator<Position> cti
            = map.getFloodFillIterator(startTile.getPosition());
        while (cti.hasNext()) {
            Tile tempTile = map.getTile(cti.next());
            if (map.isPolar(tempTile)) {
                // do not place the initial colony at the pole
                continue;
            }
            if (player.canClaimToFoundSettlement(tempTile)) {
                colonyTile = tempTile;
                break;
            }
        }

        if (colonyTile == null) {
            logger.warning("Could not find a debug colony site.");
            return;
        }
        for (TileType t : spec.getTileTypeList()) {
            if (!t.isWater()) {
                colonyTile.setType(t);
                break;
            }
        }
        unitType = spec.getUnitType("model.unit.expertFarmer");
        Unit buildColonyUnit = new ServerUnit(game, colonyTile,
                                              player, unitType);
        String colonyName = Messages.message(player.getNationName())
            + " Colony";
        Colony colony = new ServerColony(game, player, colonyName, colonyTile);
        player.addSettlement(colony);
        colony.placeSettlement(true);
        for (Tile tile : colonyTile.getSurroundingTiles(1)) {
            if (tile.getSettlement() == null
                && (tile.getOwner() == null
                    || !tile.getOwner().isEuropean())) {
                tile.setOwner(player);
                tile.setOwningSettlement(colony);
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
                    TileImprovementType roadType = map.getSpecification()
                        .getTileImprovementType("model.improvement.road");
                    TileImprovement road = new TileImprovement(game, ct, roadType);
                    road.setTurnsToComplete(0);
                    TileImprovement plow = new TileImprovement(game, ct, plowType);
                    plow.setTurnsToComplete(0);
                    ct.setTileItemContainer(new TileItemContainer(game, ct));
                    ct.getTileItemContainer().addTileItem(road);
                    ct.getTileItemContainer().addTileItem(plow);
                    break;
                }
            }
        }
        BuildingType schoolType = spec.getBuildingType("model.building.schoolhouse");
        Building schoolhouse = new ServerBuilding(game, colony, schoolType);
        colony.addBuilding(schoolhouse);
        unitType = spec.getUnitType("model.unit.masterCarpenter");
        Unit carpenter = new ServerUnit(game, colonyTile, player, unitType);
        carpenter.setLocation(colony.getBuildingForProducing(unitType.getExpertProduction()));

        unitType = spec.getUnitType("model.unit.elderStatesman");
        Unit statesman = new ServerUnit(game, colonyTile, player, unitType);
        statesman.setLocation(colony.getBuildingForProducing(unitType.getExpertProduction()));

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
            lumberjack.setWorkType(lumberjack.getType().getExpertProduction());
        }

        unitType = spec.getUnitType("model.unit.seasonedScout");
        @SuppressWarnings("unused")
        Unit scout = new ServerUnit(game, colonyTile, player, unitType);

        unitType = spec.getUnitType("model.unit.veteranSoldier");
        @SuppressWarnings("unused")
        Unit unit8 = new ServerUnit(game, colonyTile, player, unitType);

        @SuppressWarnings("unused")
        Unit unit9 = new ServerUnit(game, colonyTile, player, unitType);

        unitType = spec.getUnitType("model.unit.artillery");
        @SuppressWarnings("unused")
        Unit unit10 = new ServerUnit(game, colonyTile, player, unitType);

        @SuppressWarnings("unused")
        Unit unit11 = new ServerUnit(game, colonyTile, player, unitType);

        @SuppressWarnings("unused")
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
        @SuppressWarnings("unused")
        Unit unit15 = new ServerUnit(game, colonyTile, player, unitType);
        @SuppressWarnings("unused")
        Unit unit16 = new ServerUnit(game, colonyTile, player, unitType);
        // END DEBUG
    }

    private List<Position> generateStartingPositions(Map map, List<Player> players) {
        int number = players.size();
        List<Position> positions = new ArrayList<Position>(number);
        if (number > 0) {
            int west = 0;
            int east = map.getWidth() - 1;
            switch(map.getSpecification().getInteger(GameOptions.STARTING_POSITIONS)) {
            case GameOptions.STARTING_POSITIONS_CLASSIC:
                int distance = map.getHeight() / number;
                int row = distance/2;
                for (int index = 0; index < number; index++) {
                    positions.add(new Position(east, row));
                    row += distance;
                }
                Collections.shuffle(positions);
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
                Collections.shuffle(positions);
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

    private Tile findTileFor(Map map, int row, int start, boolean startAtSea) {
        Tile tile = null;
        if (0 <= row && row < map.getHeight()) {
            Tile firstLand = null;
            int offset = (start == 0) ? 1 : -1;
            for (int x = start; 0 <= x && x < map.getWidth(); x += offset) {
                tile = map.getTile(x, row);
                if (tile.isLand()) {
                    if (startAtSea) {
                        firstLand = tile;
                        start = x - offset;
                        break;
                    } else {
                        return tile;
                    }
                }
            }
            if (firstLand != null) {
                offset = -offset;
                for (int x = start; 0 <= x && x < map.getWidth(); x += offset) {
                    tile = map.getTile(x, row);
                    if (tile.canMoveToEurope()) {
                        return tile;
                    }
                }
            }
        }
        return null;
    }


    private class Territory {
        public ServerRegion region;
        public Position position;
        public Player player;
        public int numberOfSettlements;

        public Territory(Player player, Position position) {
            this.player = player;
            this.position = position;
        }

        public Territory(Player player, ServerRegion region) {
            this.player = player;
            this.region = region;
        }

        public Position getCenter() {
            if (position == null) {
                return region.getCenter();
            } else {
                return position;
            }
        }

        public String toString() {
            return player + " territory at " + region.toString();
        }
    }


}
