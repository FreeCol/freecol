/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.LandMap;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Region;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.Resource;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.RandomChoice;
import static net.sf.freecol.common.util.RandomUtils.*;
import net.sf.freecol.server.model.ServerRegion;


/**
 * Class for making a {@code Map} based upon a land map.
 *
 * FIXME: dynamic lakes, mountains and hills
 */
public class TerrainGenerator {

    private static final Logger logger = Logger.getLogger(TerrainGenerator.class.getName());

    public static final int LAND_REGIONS_SCORE_VALUE = 1000;
    public static final int LAND_REGION_MIN_SCORE = 5;
    public static final int LAND_REGION_MAX_SIZE = 75;

    /**
     * The pseudo-random number source to use.
     *
     * Uses of the PRNG are usually logged in FreeCol, but the use is
     * so intense here and this code is called pre-game, so we
     * intentionally skip the logging here.
     */
    private final Random random;

    /** The cached land and ocean tile types. */
    private List<TileType> landTileTypes = null;
    private List<TileType> oceanTileTypes = null;


    /**
     * Creates a new {@code TerrainGenerator}.
     *
     * @param random A {@code Random} number source.
     * @see #generateMap
     */
    public TerrainGenerator(Random random) {
        this.random = random;
    }


    // Utilities

    // FIXME: this might be useful elsewhere, too
    private int limitToRange(int value, int lower, int upper) {
        return Math.max(lower, Math.min(value, upper));
    }

    /**
     * Gets the approximate number of land tiles.
     *
     * @param game The {@code Game} to generate for.
     * @return The approximate number of land tiles
     */
    private int getApproximateLandCount(Game game) {
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        return mapOptions.getInteger(MapGeneratorOptions.MAP_WIDTH)
            * mapOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT)
            * mapOptions.getInteger(MapGeneratorOptions.LAND_MASS)
            / 100;
    }

    /**
     * Gets a random land tile type based on the latitude.
     *
     * @param game The {@code Game} to generate for.
     * @param latitude The location of the tile relative to the north/south
     *     poles and equator:
     *     0 is the mid-section of the map (equator)
     *     +/-90 is on the bottom/top of the map (poles).
     * @return A suitable random land tile type.
     */
    private TileType getRandomLandTileType(Game game, int latitude) {
        final Specification spec = game.getSpecification();
        if (landTileTypes == null) {
            // Do not generate elevated and water tiles at this time
            // they are created elsewhere.
            landTileTypes = transform(spec.getTileTypeList(),
                                      t -> !t.isElevation() && !t.isWater());
        }
        return getRandomTileType(game, landTileTypes, latitude);
    }

    /**
     * Gets a random ocean tile type.
     *
     * @param game The {@code Game} to generate for.
     * @param latitude The latitude of the proposed tile.
     * @return A suitable random ocean tile type.
     */
    private TileType getRandomOceanTileType(Game game, int latitude) {
        final Specification spec = game.getSpecification();
        if (oceanTileTypes == null) {
            oceanTileTypes = transform(spec.getTileTypeList(),
                                       t -> t.isWater() && t.isHighSeasConnected()
                                           && !t.isDirectlyHighSeasConnected());
        }
        return getRandomTileType(game, oceanTileTypes, latitude);
    }

    /**
     * Gets a tile type fitted to the regional requirements.
     *
     * FIXME: Can be used for mountains and rivers too.
     *
     * @param game The {@code Game} to generate for.
     * @param candidates A list of {@code TileType}s to use for
     *     calculations.
     * @param latitude The tile latitude.
     * @return A suitable {@code TileType}.
     */
    private TileType getRandomTileType(Game game, List<TileType> candidates,
                                       int latitude) {
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        final Specification spec = game.getSpecification();
        // decode options
        final int forestChance
            = mapOptions.getRange(MapGeneratorOptions.FOREST_NUMBER);
        final int temperaturePreference
            = mapOptions.getRange(MapGeneratorOptions.TEMPERATURE);

        // temperature calculation
        int poleTemperature = -20;
        int equatorTemperature= 40;
        switch (temperaturePreference) {
        case MapGeneratorOptions.TEMPERATURE_COLD:
            poleTemperature = -20;
            equatorTemperature = 25;
            break;
        case MapGeneratorOptions.TEMPERATURE_CHILLY:
            poleTemperature = -20;
            equatorTemperature = 30;
            break;
        case MapGeneratorOptions.TEMPERATURE_TEMPERATE:
            poleTemperature = -10;
            equatorTemperature = 35;
            break;
        case MapGeneratorOptions.TEMPERATURE_WARM:
            poleTemperature = -5;
            equatorTemperature = 40;
            break;
        case MapGeneratorOptions.TEMPERATURE_HOT:
            poleTemperature = 0;
            equatorTemperature = 40;
            break;
        default:
            break;
        }

        int temperatureRange = equatorTemperature-poleTemperature;
        int localeTemperature = poleTemperature + (90 - Math.abs(latitude))
            * temperatureRange/90;
        int temperatureDeviation = 7; // +/- 7 degrees randomization
        localeTemperature += randomInt(logger, "Temperature", random,
                                       temperatureDeviation * 2)
            - temperatureDeviation;
        localeTemperature = limitToRange(localeTemperature, -20, 40);

        // humidity calculation
        int localeHumidity = spec.getRange(MapGeneratorOptions.HUMIDITY);
        int humidityDeviation = 20; // +/- 20% randomization
        localeHumidity += randomInt(logger, "Humidity", random,
                                    humidityDeviation * 2)
            - humidityDeviation;
        localeHumidity = limitToRange(localeHumidity, 0, 100);

        List<TileType> candidateTileTypes = new ArrayList<>(candidates);

        // Filter the candidates by temperature.
        int i = 0;
        while (i < candidateTileTypes.size()) {
            TileType type = candidateTileTypes.get(i);
            if (!type.withinRange(TileType.RangeType.TEMPERATURE,
                                  localeTemperature)) {
                candidateTileTypes.remove(i);
                continue;
            }
            i++;
        }

        // Need to continue?
        switch (candidateTileTypes.size()) {
        case 0:
            throw new RuntimeException("No TileType for"
                + " temperature==" + localeTemperature);
        case 1:
            return first(candidateTileTypes);
        default:
            break;
        }

        // Filter the candidates by humidity.
        i = 0;
        while (i < candidateTileTypes.size()) {
            TileType type = candidateTileTypes.get(i);
            if (!type.withinRange(TileType.RangeType.HUMIDITY,
                                  localeHumidity)) {
                candidateTileTypes.remove(i);
                continue;
            }
            i++;
        }

        // Need to continue?
        switch (candidateTileTypes.size()) {
        case 0:
            throw new RuntimeException("No TileType for"
                + " humidity==" + localeHumidity);
        case 1:
            return first(candidateTileTypes);
        default:
            break;
        }

        // Filter the candidates by forest presence.
        boolean forested = randomInt(logger, "Forest", random, 100) < forestChance;
        i = 0;
        while (i < candidateTileTypes.size()) {
            TileType type = candidateTileTypes.get(i);
            if (type.isForested() != forested) {
                candidateTileTypes.remove(i);
                continue;
            }
            i++;
        }

        // Done
        switch (i = candidateTileTypes.size()) {
        case 0:
            throw new RuntimeException("No TileType for"
                + " forested==" + forested);
        case 1:
            return first(candidateTileTypes);
        default:
            return candidateTileTypes.get(randomInt(logger, "Forest tile",
                                                    random, i));
        }
    }


    // Create map entities

    /**
     * Creates land map regions in the given Map.
     *
     * First, the arctic/antarctic regions are defined, based on
     * {@code Map.POLAR_HEIGHT}.
     *
     * For the remaining land tiles, one region per contiguous
     * landmass is created.
     *
     * @param map The {@code Map} to work on.
     * @param lb A {@code LogBuilder} to log to.
     * @return A list of created {@code ServerRegion}s.
     */
    private List<ServerRegion> createLandRegions(Map map, LogBuilder lb) {
        // Create "explorable" land regions
        final Game game = map.getGame();
        int continents = 0;
        boolean[][] landmap = new boolean[map.getWidth()][map.getHeight()];
        int[][] continentmap = new int[map.getWidth()][map.getHeight()];
        int landsize = 0;

        // Initialize both maps
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                continentmap[x][y] = 0;
                landmap[x][y] = false;
                if (map.isValid(x, y)) {
                    Tile tile = map.getTile(x, y);
                    // Exclude existing regions (arctic/antarctic, mountains,
                    // rivers).
                    landmap[x][y] = tile.isLand()
                        && tile.getRegion() == null;
                    if (tile.isLand()) landsize++;
                }
            }
        }

        // Flood fill, so that we end up with individual landmasses
        // numbered in continentmap[][]
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (landmap[x][y]) { // Found a new region.
                    continents++;
                    boolean[][] continent = Map.floodFillBool(landmap, x, y);

                    for (int yy = 0; yy < map.getHeight(); yy++) {
                        for (int xx = 0; xx < map.getWidth(); xx++) {
                            if (continent[xx][yy]) {
                                continentmap[xx][yy] = continents;
                                landmap[xx][yy] = false;
                            }
                        }
                    }
                }
            }
        }
        lb.add("Number of individual landmasses is ", continents, "\n");

        // Get landmass sizes
        int[] continentsize = new int[continents+1];
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                continentsize[continentmap[x][y]]++;
            }
        }

        // Go through landmasses, split up those too big
        int oldcontinents = continents;
        for (int c = 1; c <= oldcontinents; c++) {
            // c starting at 1, c=0 is all excluded tiles
            if (continentsize[c] > LAND_REGION_MAX_SIZE) {
                boolean[][] splitcontinent
                    = new boolean[map.getWidth()][map.getHeight()];
                int splitX = 0, splitY = 0;

                for (int x = 0; x < map.getWidth(); x++) {
                    for (int y = 0; y < map.getHeight(); y++) {
                        if (continentmap[x][y] == c) {
                            splitcontinent[x][y] = true;
                            splitX = x; splitY = y;
                        } else {
                            splitcontinent[x][y] = false;
                        }
                    }
                }

                while (continentsize[c] > LAND_REGION_MAX_SIZE) {
                    int targetsize = LAND_REGION_MAX_SIZE;
                    if (continentsize[c] < 2*LAND_REGION_MAX_SIZE) {
                        targetsize = continentsize[c]/2;
                    }
                    continents++; //index of the new region in continentmap[][]
                    boolean[][] newregion = Map.floodFillBool(splitcontinent,
                        splitX, splitY, targetsize);
                    for (int x = 0; x < map.getWidth(); x++) {
                        for (int y = 0; y < map.getHeight(); y++) {
                            if (newregion[x][y]) {
                                continentmap[x][y] = continents;
                                splitcontinent[x][y] = false;
                                continentsize[c]--;
                            }
                            if (splitcontinent[x][y]) {
                                splitX = x; splitY = y;
                            }
                        }
                    }
                }
            }
        }
        lb.add("Number of land regions being created: ", continents, "\n");

        // Create ServerRegions for all land regions
        ServerRegion[] landregions = new ServerRegion[continents+1];
        int landIndex = 1;
        for (int c = 1; c <= continents; c++) {
            // c starting at 1, c=0 is all water tiles
            landregions[c] = new ServerRegion(game, RegionType.LAND);
        }

        // Add tiles to ServerRegions
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (continentmap[x][y] > 0) {
                    Tile tile = map.getTile(x, y);
                    landregions[continentmap[x][y]].addTile(tile);
                }
            }
        }

        for (int c = 1; c <= continents; c++) {
            ServerRegion sr = landregions[c];

            // Set exploration points for land regions based on size
            int score = Math.max((int)(((float)sr.getSize() / landsize)
                                       * LAND_REGIONS_SCORE_VALUE),
                                 LAND_REGION_MIN_SCORE);
            sr.setScoreValue(score);
            lb.add("Created land region ", sr.toString(),
                " (size ", sr.getSize(),
                ", score ", sr.getScoreValue(),
                ", parent ", ((sr.getParent() == null) ? "(null)"
                    : sr.getParent().toString()), ")\n");
        }
        return Arrays.asList(Arrays.copyOfRange(landregions, 1, continents+1));
    }

    /**
     * Creates mountain ranges on the given map.  The number and size
     * of mountain ranges depends on the map size.
     *
     * @param map The map to use.
     * @param lb A {@code LogBuilder} to log to.
     * @return A list of created {@code ServerRegion}s.
     */
    private List<ServerRegion> createMountains(Map map, LogBuilder lb) {
        final Game game = map.getGame();
        final Specification spec = game.getSpecification();
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        float randomHillsRatio = 0.5f;
        // 50% of user settings will be allocated for random hills
        // here and there the rest will be allocated for large
        // mountain ranges
        int maximumLength
            = Math.max(mapOptions.getInteger(MapGeneratorOptions.MAP_WIDTH),
                mapOptions.getInteger(MapGeneratorOptions.MAP_HEIGHT)) / 10;
        int number = Math.round((1.0f - randomHillsRatio)
            * ((float)getApproximateLandCount(game)
                / mapOptions.getRange(MapGeneratorOptions.MOUNTAIN_NUMBER)));
        lb.add("Number of mountain tiles is ", number, "\n",
            "Maximum length of mountain ranges is ", maximumLength, "\n");
        List<ServerRegion> result = new ArrayList<>(number);

        // lookup the resources from specification
        final TileType hills = spec.getTileType("model.tile.hills");
        final TileType mountains = spec.getTileType("model.tile.mountains");
        if (hills == null || mountains == null) {
            throw new RuntimeException("Both Hills and Mountains TileTypes must be defined: " + spec);
        }

        // Generate the mountain ranges
        List<Tile> tiles = new ArrayList<>();
        map.forEachTile(t -> t.isGoodMountainTile(mountains),
                        t -> tiles.add(t));
        randomShuffle(logger, "Randomize mountain tiles", tiles, random);

        int counter = 0;
        for (Tile startTile : tiles) {
            // isGoodMountainTile can change when new mountains are added
            if (!startTile.isGoodMountainTile(mountains)) continue;
            
            ServerRegion mountainRegion
                = new ServerRegion(game, RegionType.MOUNTAIN);
            Direction direction = Direction.getRandomDirection("getLand",
                logger, random);
            int length = maximumLength
                - randomInt(logger, "MLen", random, maximumLength/2);
            Tile tile = startTile;
            for (int index = 0; index < length; index++) {
                // Raise current tile up as mountain
                if (tile.getType() != mountains) {
                    tile.setType(mountains);
                    mountainRegion.addTile(tile);
                    counter++;
                }
                // Add nothing (1/8), mountains (2/8) or hills (5/8)
                // to surrounding tiles if suitable and not already a
                // mountain
                for (Tile t : tile.getSurroundingTiles(1)) {
                    if (!t.isGoodHillTile()
                        || t.getType() == mountains) continue;
                    int r = randomInt(logger, "MSiz", random, 8);
                    if (r < 2) {
                        t.setType(mountains);
                        mountainRegion.addTile(t);
                        counter++;
                    } else if (r < 7) {
                        t.setType(hills);
                        mountainRegion.addTile(t);
                    }
                }
                tile = tile.getNeighbourOrNull(direction);
                if (tile == null || !tile.isLand()) break;
            }
            int scoreValue = 2 * mountainRegion.getSize();
            mountainRegion.setScoreValue(scoreValue);
            result.add(mountainRegion);
            lb.add("Created mountain region (direction ", direction,
                ", length ", length,
                ", size ", mountainRegion.getSize(),
                ", score value ", scoreValue, ").\n");
            if (counter >= number) break;
        }
        lb.add("Added ", counter, " mountain range tiles.\n");

        // and sprinkle a few random hills/mountains here and there
        tiles.clear();
        map.forEachTile(Tile::isGoodHillTile, t -> tiles.add(t));
        randomShuffle(logger, "Randomize hill tiles", tiles, random);

        number = (int) (getApproximateLandCount(game) * randomHillsRatio)
            / mapOptions.getRange(MapGeneratorOptions.MOUNTAIN_NUMBER);
        counter = 0;
        for (Tile t : tiles) {
            // 25% mountains, 75% hills
            boolean m = randomInt(logger, "MorH", random, 4) == 0;
            t.setType((m) ? mountains : hills);
            if (++counter >= number) break;
        }
        lb.add("Added ", counter, " random hilly tiles.\n");
        return result;
    }

    /**
     * Creates rivers on the given map. The number of rivers depends
     * on the map size.
     *
     * @param map The {@code Map} to create rivers on.
     * @param lb A {@code LogBuilder} to log to.
     * @return A list of created {@code ServerRegion}s.
     */
    private List<ServerRegion> createRivers(Map map, LogBuilder lb) {
        final Game game = map.getGame();
        final Specification spec = game.getSpecification();
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        List<ServerRegion> result = new ArrayList<>();
        final TileImprovementType riverType
            = spec.getTileImprovementType("model.improvement.river");
        final int number = getApproximateLandCount(game)
            / mapOptions.getRange(MapGeneratorOptions.RIVER_NUMBER);
        HashMap<Tile, River> riverMap = new HashMap<>();
        List<River> rivers = new ArrayList<>();

        List<Tile> tiles = new ArrayList<>();
        map.forEachTile(t -> t.isGoodRiverTile(riverType),
                        t -> tiles.add(t));
        randomShuffle(logger, "Randomize river tiles", tiles, random);
        
        int counter = 0;
        for (Tile tile : tiles) {
            // Any river here yet?
            if (riverMap.get(tile) != null)
                continue;

            ServerRegion riverRegion = new ServerRegion(game, RegionType.RIVER);
            River river = new River(map, riverMap, riverRegion, random);
            if (river.flowFromSource(tile)) {
                lb.add("Created new river with length ",
                    river.getLength(), "\n");
                result.add(riverRegion);
                rivers.add(river);
                if (++counter >= number) break;
            } else {
                lb.add("Failed to generate river at " + tile + ".\n");
            }
        }
        lb.add("Created ", counter, " rivers of maximum ", number, "\n");

        for (River river : rivers) {
            ServerRegion region = river.getRegion();
            int score = 2 * sum(river.getSections(), RiverSection::getSize);
            region.setScoreValue(score);
            lb.add("Created river region (length ", river.getLength(),
                ", score value ", score, ").\n");
        }
        return result;
    }

    /**
     * Finds all the lake regions.
     *
     * @param map The {@code Map} to work on.
     * @param lb A {@code LogBuilder} to log to.
     * @return A list of created {@code ServerRegion}s.
     */
    private List<ServerRegion> createLakeRegions(Map map, LogBuilder lb) {
        // Create the water map, and find any tiles that are water but
        // not part of any region (such as the oceans).  These are
        // lake tiles.
        List<Tile> lakes = new ArrayList<>();
        lb.add("Lakes at:");
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile;
                if (map.isValid(x, y)
                    && !(tile = map.getTile(x, y)).isLand()
                    && map.getTile(x, y).getRegion() == null) {
                    lakes.add(tile);
                    lb.add(" ", x, ",", y);
                }
            }
        }
        lb.add("\n");
        
        return makeLakes(map, lakes);
    }

    /**
     * Make lake regions from unassigned lake tiles.
     *
     * @param map The {@code Map} to add to.
     * @param lakes A list of lake {@code Tile}s.
     * @return A list of new {@code ServerRegion}s.
     */
    private List<ServerRegion> makeLakes(Map map, List<Tile> lakes) {
        Game game = map.getGame();
        final TileType lakeType = map.getSpecification()
            .getTileType("model.tile.lake");
        List<Tile> todo = new ArrayList<>(lakes.size()/10);
        List<ServerRegion> result = new ArrayList<>(lakes.size()/10);
        int lakeCount = 0;
        while (!lakes.isEmpty()) {
            Tile tile = first(lakes);
            if (tile.getRegion() != null) continue;

            ServerRegion lakeRegion = new ServerRegion(game, RegionType.LAKE);
            // Pretend lakes are discovered with the surrounding terrain?
            todo.clear();
            todo.add(tile);
            while (!todo.isEmpty()) {
                Tile t = todo.remove(0);
                if (lakes.contains(t)) {
                    t.setRegion(lakeRegion);
                    t.setType(lakeType);
                    lakes.remove(t);
                    // It would be better to do:
                    //   todo.addAll(t.getSurroundingTiles(1, 1));
                    // but this routine can be called from Map.readChildren
                    // before game.getMap() works.  When that use goes away,
                    // use the above code.
                    for (Direction d : Direction.allDirections) {
                        Tile t0 = map.getAdjacentTile(t, d);
                        if (t0 != null) todo.add(t0);
                    }
                }
            }
            result.add(lakeRegion);
        }
        return result;
    }

    /**
     * Adds a terrain bonus with a probability determined by the
     * {@code MapGeneratorOptions}.
     *
     * @param t The {@code Tile} to add bonuses to.
     * @param generateBonus Generate the bonus or not.
     */
    private void perhapsAddBonus(Tile t, boolean generateBonus) {
        final Game game = t.getGame();
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        final Specification spec = game.getSpecification();
        TileImprovementType fishBonusLandType
            = spec.getTileImprovementType("model.improvement.fishBonusLand");
        TileImprovementType fishBonusRiverType
            = spec.getTileImprovementType("model.improvement.fishBonusRiver");
        final int bonusNumber
            = mapOptions.getRange(MapGeneratorOptions.BONUS_NUMBER);
        if (t.isLand()) {
            if (generateBonus
                && randomInt(logger, "Land Resource", random, 100) < bonusNumber) {
                // Create random Bonus Resource
                t.addResource(createResource(t));
            }
        } else {
            int adjacentLand = 0;
            boolean adjacentRiver = false;
            for (Direction direction : Direction.values()) {
                Tile otherTile = t.getNeighbourOrNull(direction);
                if (otherTile != null && otherTile.isLand()) {
                    adjacentLand++;
                    if (otherTile.hasRiver()) {
                        adjacentRiver = true;
                    }
                }
            }

            // In Col1, ocean tiles with less than 3 land neighbours
            // produce 2 fish, all others produce 4 fish
            if (adjacentLand > 2) {
                t.add(new TileImprovement(game, t, fishBonusLandType, null));
            }

            // In Col1, the ocean tile in front of a river mouth would
            // get an additional +1 bonus
            // FIXME: This probably has some false positives, means
            // river tiles that are NOT a river mouth next to this tile!
            if (adjacentRiver && !t.hasRiver()) {
                t.add(new TileImprovement(game, t, fishBonusRiverType, null));
            }

            if (t.getType().isHighSeasConnected()) {
                if (generateBonus && adjacentLand > 1
                    && randomInt(logger, "Sea resource", random,
                                 10 - adjacentLand) == 0) {
                    t.addResource(createResource(t));
                }
            } else {
                if (randomInt(logger, "Water resource", random, 100) < bonusNumber) {
                    // Create random Bonus Resource
                    t.addResource(createResource(t));
                }
            }
        }
    }

    /**
     * Create a random resource on a tile.
     *
     * @param tile The {@code Tile} to create the resource on.
     * @return The created resource, or null if it is not possible.
     */
    private Resource createResource(Tile tile) {
        if (tile == null) return null;
        ResourceType resourceType = RandomChoice.getWeightedRandom(null, null,
            tile.getType().getResourceTypes(), random);
        if (resourceType == null) return null;
        int minValue = resourceType.getMinValue();
        int maxValue = resourceType.getMaxValue();
        int quantity = (minValue == maxValue) ? maxValue
            : (minValue + randomInt(logger, "Rsiz", random, 
                                    maxValue - minValue + 1));
        return new Resource(tile.getGame(), tile, resourceType, quantity);
    }

    /**
     * Sets the style of the tiles.
     * Only relevant to water tiles for now.
     * Public because it is used in the river generator.
     *
     * @param tile The {@code Tile} to set the style of.
     */
    public static void encodeStyle(Tile tile) {
        EnumMap<Direction, Boolean> connections
            = new EnumMap<>(Direction.class);

        // corners
        for (Direction d : Direction.corners) {
            Tile t = tile.getNeighbourOrNull(d);
            connections.put(d, t != null && t.isLand());
        }
        // edges
        for (Direction d : Direction.longSides) {
            Tile t = tile.getNeighbourOrNull(d);
            if (t != null && t.isLand()) {
                connections.put(d, Boolean.TRUE);
                // ignore adjacent corners
                connections.put(d.getNextDirection(), Boolean.FALSE);
                connections.put(d.getPreviousDirection(), Boolean.FALSE);
            } else {
                connections.put(d, Boolean.FALSE);
            }
        }
        int result = 0;
        int index = 0;
        for (Direction d : Direction.corners) {
            if (connections.get(d)) result += (int)Math.pow(2, index);
            index++;
        }
        for (Direction d : Direction.longSides) {
            if (connections.get(d)) result += (int)Math.pow(2, index);
            index++;
        }
        tile.setStyle(result);
    }

    // Main functionality, create the map.

    /**
     * Make a {@code Map}.
     *
     * @param game The {@code Game} to generate a map for.
     * @param importMap An optional {@code Map} to import.
     * @param landMap The {@code LandMap} to use as a template.
     * @param lb A {@code LogBuilder} to log to.
     * @return The new {@code Map}.
     */
    public Map generateMap(Game game, Map importMap, LandMap landMap,
                           LogBuilder lb) {
        final OptionGroup mapOptions = game.getMapGeneratorOptions();
        final int width = landMap.getWidth();
        final int height = landMap.getHeight();
        final boolean importBonuses = (importMap != null)
            && mapOptions.getBoolean(MapGeneratorOptions.IMPORT_BONUSES);
        final boolean importRumours = (importMap != null)
            && mapOptions.getBoolean(MapGeneratorOptions.IMPORT_RUMOURS);
        final boolean importTerrain = (importMap != null)
            && mapOptions.getBoolean(MapGeneratorOptions.IMPORT_TERRAIN);

        Map map = new Map(game, width, height);
        game.setMap(map);
        
        int minimumLatitude = mapOptions
            .getInteger(MapGeneratorOptions.MINIMUM_LATITUDE);
        int maximumLatitude = mapOptions
            .getInteger(MapGeneratorOptions.MAXIMUM_LATITUDE);
        // make sure the values are in range
        minimumLatitude = limitToRange(minimumLatitude, -90, 90);
        maximumLatitude = limitToRange(maximumLatitude, -90, 90);
        map.setMinimumLatitude(Math.min(minimumLatitude, maximumLatitude));
        map.setMaximumLatitude(Math.max(minimumLatitude, maximumLatitude));

        java.util.Map<String, ServerRegion> regionMap = new HashMap<>();
        if (importTerrain) { // Import the regions
            lb.add("Imported regions: ");
            for (Region r : importMap.getRegions()) {
                ServerRegion region = new ServerRegion(game, r);
                map.addRegion(region);
                regionMap.put(r.getId(), region);
                lb.add(" ", region.toString());
            }
            for (Region r : importMap.getRegions()) {
                ServerRegion region = regionMap.get(r.getId());
                Region x = r.getParent();
                if (x != null) x = regionMap.get(x.getId());
                region.setParent(x);
                for (Region c : r.getChildren()) {
                    x = regionMap.get(c.getId());
                    if (x != null) region.addChild(x);
                }
            }
            lb.add("\n");
        }

        final Map.Layer layer = (importRumours) ? Map.Layer.RUMOURS
            : (importBonuses) ? Map.Layer.RESOURCES
            : Map.Layer.RIVERS;
        List<Tile> fixRegions = new ArrayList<>();
        map.populateTiles((x, y) -> {
                Tile t, otherTile = null;
                if (importTerrain
                    && importMap.isValid(x, y)
                    && (otherTile = importMap.getTile(x, y)) != null
                    && otherTile.isLand() == landMap.isLand(x, y)) {
                    t = map.importTile(otherTile, x, y, layer);
                    Region r = otherTile.getRegion();
                    if (r == null) {
                        fixRegions.add(t);
                    } else {
                        ServerRegion ours = regionMap.get(r.getId());
                        if (ours == null) {
                            lb.add("Could not set tile region ", r.getId(),
                                " for tile: ", t, "\n");
                            fixRegions.add(t);
                        } else {
                            ours.addTile(t);
                        }
                    }
                } else {
                    final int latitude = map.getLatitude(y);
                    TileType tt = (landMap.isLand(x, y))
                        ? getRandomLandTileType(game, latitude)
                        : getRandomOceanTileType(game, latitude);
                    t = new Tile(game, tt, x, y);
                }
                return t;
            });

        // Build the regions.
        List<ServerRegion> fixed = ServerRegion.requireFixedRegions(map, lb);
        List<ServerRegion> newRegions = new ArrayList<>();
        if (importTerrain) {
            if (!fixRegions.isEmpty()) { // Fix the tiles missing regions.
                newRegions.addAll(createLakeRegions(map, lb));
                newRegions.addAll(createLandRegions(map, lb));
            }
        } else {
            map.resetHighSeas(
                mapOptions.getInteger(MapGeneratorOptions.DISTANCE_TO_HIGH_SEA),
                mapOptions.getInteger(MapGeneratorOptions.MAXIMUM_DISTANCE_TO_EDGE));
            if (landMap.hasLand()) {
                newRegions.addAll(createMountains(map, lb));
                newRegions.addAll(createRivers(map, lb));
                newRegions.addAll(createLakeRegions(map, lb));
                newRegions.addAll(createLandRegions(map, lb));
            }
        }
        lb.shrink("\n");

        // Connect all new regions to their geographic parent and add to
        // the map.
        List<ServerRegion> geographic
            = transform(fixed, ServerRegion::isGeographic);
        for (ServerRegion sr : newRegions) {
            ServerRegion gr = find(geographic, g -> g.containsCenter(sr));
            if (gr != null) {
                sr.setParent(gr);
                gr.addChild(sr);
                gr.setSize(gr.getSize() + sr.getSize());
            }
            map.addRegion(sr);
        }

        // Probably only needed on import of old maps.
        map.fixupRegions();

        // Add the bonuses only after the map is completed.
        // Otherwise we risk creating resources on fields where they
        // do not belong (like sugar in large rivers or tobacco on hills).
        map.forEachTile(t -> {
                perhapsAddBonus(t, !importBonuses);
                if (!t.isLand()) encodeStyle(t);
            });

        // Final cleanups
        map.resetContiguity();
        map.resetHighSeasCount();
        return map;
    }
}
