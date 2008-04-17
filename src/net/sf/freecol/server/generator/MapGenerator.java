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

package net.sf.freecol.server.generator;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;


/**
 * Creates random maps and sets the starting locations for the players.
 */
public class MapGenerator {

    private static final Logger logger = Logger.getLogger(MapGenerator.class.getName());
    
    private final Random random;
    private final MapGeneratorOptions mapGeneratorOptions;
    
    private final LandGenerator landGenerator;
    private final TerrainGenerator terrainGenerator;

    
    /**
     * Creates a <code>MapGenerator</code>
     * @see #createMap
     */
    public MapGenerator() {
        this.mapGeneratorOptions = new MapGeneratorOptions();
        this.random = new Random();
                
        landGenerator = new LandGenerator(mapGeneratorOptions);
        terrainGenerator = new TerrainGenerator(mapGeneratorOptions);
    }


    /**
     * Creates a new <code>Map</code> for the given
     * <code>Game</code>. The <code>Map</code> is
     * {@link Game#setMap(Map) added to the game} in this process.
     * 
     * <br><br>
     * 
     * The <code>Map</code> will be created using the assigned
     * options (see {@link #getMapGeneratorOptions()}). 
     * 
     * @param game The <code>Game</code> that will be getting a map.
     * @see LandGenerator
     * @see TerrainGenerator
     */
    public void createMap(Game game) {        
        
        // Prepare imports:
        final File importFile = getMapGeneratorOptions().getFile(MapGeneratorOptions.IMPORT_FILE);
        final Game importGame;
        if (importFile != null) {                 
            importGame = loadSaveGame(importFile);
        } else {
            importGame = null;
        }
        
        // Create land map:
        boolean[][] landMap;
        if (importGame != null
                && getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_LAND_MAP)) {
            landMap = landGenerator.importLandMap(importGame);
        } else {
            landMap = landGenerator.createLandMap();
        }
        
        // Create terrain:
        terrainGenerator.createMap(game, importGame, landMap);

        Map map = game.getMap();        
        createIndianSettlements(map, game.getPlayers());
        createEuropeanUnits(map, game.getPlayers());
        createLostCityRumours(map, importGame);        
    }
    
    /**
     * Loads a <code>Game</code> from the given <code>File</code>.
     * 
     * @param importFile The <code>File</code> to be loading the
     *      <code>Game</code> from.
     * @return The <code>Game</code>.
     */
    private Game loadSaveGame(File importFile) {        
        /* 
         * TODO-LATER: We are using same method in FreeColServer.
         *       Create a framework for loading games/maps.
         */
        
        try {
            Game game = null;
            XMLStreamReader xsr = FreeColServer.createXMLStreamReader(importFile);
            xsr.nextTag();
            final String version = xsr.getAttributeValue(null, "version");
            if (!Message.getFreeColProtocolVersion().equals(version)) {
                throw new FreeColException("incompatibleVersions");
            }
            ArrayList<Object> serverObjects = null;
            while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (xsr.getLocalName().equals("serverObjects")) {
                    // Reads the ServerAdditionObjects:
                    serverObjects = new ArrayList<Object>();
                    while (xsr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        if (xsr.getLocalName().equals(ServerPlayer.getServerAdditionXMLElementTagName())) {
                            serverObjects.add(new ServerPlayer(xsr));
                        } else {
                            throw new XMLStreamException("Unknown tag: " + xsr.getLocalName());
                        }
                    }
                } else if (xsr.getLocalName().equals(Game.getXMLElementTagName())) {
                    // Read the game model:
                    game = new Game(null, null, xsr, serverObjects
                                    .toArray(new FreeColGameObject[serverObjects.size()]));
                    game.setCurrentPlayer(null);
                    game.checkIntegrity();
                }
            }
            xsr.close();
            return game ;
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            return null;
        } catch (FreeColException fe) {
            StringWriter sw = new StringWriter();
            fe.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            return null;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            return null;
        }
    }

    public LandGenerator getLandGenerator() {
        return landGenerator;
    }

    public TerrainGenerator getTerrainGenerator() {
        return terrainGenerator;
    }

    /**
     * Gets the options used when generating the map.
     * @return The <code>MapGeneratorOptions</code>.
     */
    public MapGeneratorOptions getMapGeneratorOptions() {
        return mapGeneratorOptions;
    }

    /**
     * Creates lost city rumours on the given map.
     * The number of rumours depends on the map size.
     *
     * @param map The map to use.
     */
    public void createLostCityRumours(Map map) {
        createLostCityRumours(map, null);
    }
    
    /**
     * Creates lost city rumours on the given map.
     * The number of rumours depends on the map size.
     *
     * @param map The map to use.
     * @param importGame The game to lost city rumours from.
     */
    public void createLostCityRumours(Map map, Game importGame) {
        final boolean importRumours = getMapGeneratorOptions().getBoolean(MapGeneratorOptions.IMPORT_RUMOURS);
        
        if (importGame != null && importRumours) {
            for (Tile importTile : importGame.getMap().getAllTiles()) {
                final Position p = importTile.getPosition();
                if (map.isValid(p)) {
                    final Tile t = map.getTile(p);
                    t.setLostCityRumour(importTile.hasLostCityRumour());
                }
            }
        } else {
            int number = getMapGeneratorOptions().getNumberOfRumours();
            int counter = 0;

            // TODO: Remove temporary fix:
            if (importGame != null) {
                number = map.getWidth() * map.getHeight() * 25 / (100 * 35);
            }
            // END TODO

            for (int i = 0; i < number; i++) {
                for (int tries=0; tries<100; tries++) {
                    Position p = new Position(random.nextInt(map.getWidth()), 
                            random.nextInt(map.getHeight()));
                    if (p.y <= LandGenerator.POLAR_HEIGHT ||
                        p.y >= map.getHeight() - LandGenerator.POLAR_HEIGHT - 1) {
                        // please no lost city on the poles, 
                        // as they are too difficult to go visit, and not realistic
                        continue;
                    }
                    Tile t = map.getTile(p);
                    if (map.getTile(p).isLand()
                            && !t.hasLostCityRumour()
                            && t.getSettlement() == null
                            && t.getUnitCount() == 0) { 
                        counter++;
                        t.setLostCityRumour(true);
                        break;
                    }
                }
            }

            logger.info("Created " + counter + " lost city rumours of maximum " + number + ".");
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
    protected void createIndianSettlements(final Map map, List<Player> players) {
        /** TODO: why do we need to sort the players?
        Collections.sort(players, new Comparator<Player>() {
            public int compare(Player o, Player p) {
                return o.getIndex() - p.getIndex();
            }
        });
        */

        float shares = 0f;

        List<Player> indians = new ArrayList<Player>();
        HashMap<String, Territory> territoryMap = new HashMap<String, Territory>();
        for (Player player : players) {
            if (!player.isIndian()) {
                continue;
            }
            switch (((IndianNationType) player.getNationType()).getNumberOfSettlements()) {
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
            List<String> regionNames = ((IndianNationType) player.getNationType()).getRegionNames();
            Territory territory = null;
            if (regionNames == null || regionNames.isEmpty()) {
                territory = new Territory(player, map.getRandomLandPosition());
                territoryMap.put(player.getId(), territory);
            } else {
                for (String name : regionNames) {
                    if (territoryMap.get(name) == null) {
                        territory = new Territory(player, (ServerRegion) map.getRegion(name));
                        territoryMap.put(name, territory);
                        logger.fine("Allocated region " + name + " for " +
                                    player.getNationAsString() + ". Center is " +
                                    territory.getCenter() + ".");
                        break;
                    }
                }
                if (territory == null) {
                    logger.warning("Failed to allocate preferred region " + regionNames.get(0) +
                                   " for " + player.getNation().getName());
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
                        logger.warning("Unable to find free region for " + player.getName());
                        territory = new Territory(player, map.getRandomLandPosition());
                        territoryMap.put(player.getId(), territory);
                    }
                }
            }
        }

        if (indians.isEmpty()) {
            return;
        }

        List<Territory> territories = new ArrayList<Territory>(territoryMap.values());
        List<Tile> settlementTiles = new ArrayList<Tile>();

        final int minSettlementDistance = 3;
        int number = map.getWidth() * map.getHeight() / 30;

        for (int i = 0; i < number; i++) {
            nextTry: for (int tries = 0; tries < 100; tries++) {
                Position position = map.getRandomLandPosition();
                if (position.getY() <= LandGenerator.POLAR_HEIGHT ||
                    position.getY() >= map.getHeight() - LandGenerator.POLAR_HEIGHT - 1) {
                    continue;
                }
                Tile candidate = map.getTile(position);
                if (candidate.isSettleable()) {
                    for (Tile tile : settlementTiles) {
                        if (map.getDistance(position, tile.getPosition()) < minSettlementDistance) {
                            continue nextTry;
                        }
                    }                            
                    settlementTiles.add(candidate);
                    break;
                }
            }
        }
        int potential = settlementTiles.size();

        int capitals = indians.size();
        if (potential < capitals) {
            logger.warning("Number of potential settlements is smaller than number of tribes.");
            capitals = potential;
        }

        // determines how many settlements each tribe gets
        float share = settlementTiles.size() / shares;

        // first, find capitals
        for (Territory territory : territories) {
            switch (((IndianNationType) territory.player.getNationType()).getNumberOfSettlements()) {
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
            Tile tile = getClosestTile(map, territory.getCenter(), settlementTiles);
            if (tile == null) {
                // no more tiles
                break;
            } else {
                logger.fine("Placing the " + territory.player.getNationAsString() + 
                        " capital in region: " + territory.region.getNameKey() +
                        " at Tile: "+ tile.getPosition());
                placeIndianSettlement(territory.player, true, tile.getPosition(), map);
                territory.numberOfSettlements--;
                territory.position = tile.getPosition();
                settlementTiles.remove(tile);
            }
        }

        // sort tiles from the edges of the map inward
        Collections.sort(settlementTiles, new Comparator<Tile>() {
            public int compare(Tile tile1, Tile tile2) {
                int distance1 = Math.min(Math.min(tile1.getX(), map.getWidth() - tile1.getX()),
                                         Math.min(tile1.getY(), map.getHeight() - tile1.getY()));
                int distance2 = Math.min(Math.min(tile2.getX(), map.getWidth() - tile2.getX()),
                                         Math.min(tile2.getY(), map.getHeight() - tile2.getY()));
                return (distance1 - distance2);
            }
        });

        // next, other settlements
        int counter = 0;
        for (Tile tile : settlementTiles) {
            Territory territory = getClosestTerritory(map, tile, territories);
            if (territory == null) {
                // no more territories
                break;
            } else {
                logger.fine("Placing a " + territory.player.getNationAsString() + 
                        " camp in region: " + territory.region.getNameKey() +
                        " at Tile: "+ tile.getPosition());
                placeIndianSettlement(territory.player, false, tile.getPosition(), map);
                counter++;
                if (territory.numberOfSettlements < 2) {
                    territories.remove(territory);
                } else {
                    territory.numberOfSettlements--;
                }
            }
        }

        logger.info("Created " + counter + " Indian settlements of maximum " + potential);
    }

    private Tile getClosestTile(Map map, Position center, List<Tile> tiles) {
        Tile result = null;
        int minimumDistance = Integer.MAX_VALUE;
        for (Tile tile : tiles) {
            int distance = map.getDistance(tile.getPosition(), center);
            if (distance < minimumDistance) {
                minimumDistance = distance;
                result = tile;
            }
        }
        return result;
    }

    private Territory getClosestTerritory(Map map, Tile tile, List<Territory> territories) {
        Territory result = null;
        int minimumDistance = Integer.MAX_VALUE;
        for (Territory territory : territories) {
            int distance = map.getDistance(tile.getPosition(), territory.getCenter());
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
     * @param tribe The tribe owning the new settlement.
     * @param capital <code>true</code> if the settlement should be a
     *      {@link IndianSettlement#isCapital() capital}.
     * @param position The position to place the settlement.
     * @param map The map that should get a new settlement.
     * @param players The list of the {@link Player players}.
     * @return The <code>IndianSettlement</code> just being placed
     *      on the map.
     */
    private IndianSettlement placeIndianSettlement(Player player, boolean capital,
                                       Position position, Map map) {
        final Tile tile = map.getTile(position);
        IndianSettlement settlement = new IndianSettlement(map.getGame(), player,
                    tile, capital,
                    generateSkillForLocation(map, tile),
                    false, null);
        SettlementType kind = settlement.getTypeOfSettlement();
        logger.fine("Generated skill: " + settlement.getLearnableSkill().getName());

        tile.setSettlement(settlement);
        
        tile.setClaim(Tile.CLAIM_CLAIMED);
        tile.setOwningSettlement(settlement);
        
        Iterator<Position> circleIterator = map.getCircleIterator(position, true, settlement.getRadius());
        while (circleIterator.hasNext()) {
            Tile newTile = map.getTile(circleIterator.next());
            newTile.setClaim(Tile.CLAIM_CLAIMED);
            newTile.setOwningSettlement(settlement);
            newTile.setOwner(player);
        }

        for (int i = 0; i < (kind.ordinal() * 2) + 4; i++) {
            UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.brave");
            Unit unit = new Unit(map.getGame(), settlement, player, unitType, UnitState.ACTIVE,
                                 unitType.getDefaultEquipment());
            unit.setIndianSettlement(settlement);

            if (i == 0) {
                unit.setLocation(tile);
            } else {
                unit.setLocation(settlement);
            }
        }
        
        return settlement;
    }
    
    /**
    * Generates a skill that could be taught from a settlement on the given Tile.
    * TODO: This method should be properly implemented. The surrounding terrain
    *       should be taken into account and it should be partially randomized.
    *       
    * @param map The <code>Map</code>.
    * @param tile The tile where the settlement will be located.
    * @return A skill that can be taught to Europeans.
    */
    private UnitType generateSkillForLocation(Map map, Tile tile) {
        List<GoodsType> farmedList = FreeCol.getSpecification().getFarmedGoodsTypeList();
        int[] potentials = new int[farmedList.size()];
        int[] bonuses = new int[farmedList.size()];
        int bonusMultiplier = 3;

        Iterator<Position> iter = map.getAdjacentIterator(tile.getPosition());
        while (iter.hasNext()) {
            Map.Position p = iter.next();
            Tile t = map.getTile(p);
            // If it has a resource, take the resource, and ignore the other things produced there
            if (t.hasResource()) {
                ResourceType r = t.getTileItemContainer().getResource().getType();
                for (GoodsType g : r.getModifiers().keySet()) {
                    int index = farmedList.indexOf(g);
                    if (index >= 0) {
                        potentials[index]++;
                        bonuses[index]++;
                    }
                }
            } else {
                TileType tileType = t.getType();
                for (AbstractGoods goods : tileType.getProduction()) {
                    int index = farmedList.indexOf(goods.getType());
                    if (index >= 0) {
                        potentials[index]++;
                    }
                }
            }
        }

        int counter = 1;
        for (int index = 0; index < farmedList.size(); index++) {
            if (bonuses[index] > 0) {
                potentials[index] *= bonuses[index] * bonusMultiplier;
            }
            counter += potentials[index];
            potentials[index] = counter;
        }
        int newRand = random.nextInt(counter);
        for (int index = 0; index < farmedList.size(); index++) {
            if (newRand < potentials[index]) {
                UnitType expert = FreeCol.getSpecification().getExpertForProducing(farmedList.get(index));
                if (expert == null) {
                    // Seasoned Scout
                    List<UnitType> unitList = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.expertScout");
                    expert = unitList.get(random.nextInt(unitList.size()));
                }
                return expert;
            }
        }
        List<UnitType> unitList = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.expertScout");
        return unitList.get(random.nextInt(unitList.size()));
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
    protected void createEuropeanUnits(Map map, List<Player> players) {
        final int width = map.getWidth();
        final int height = map.getHeight();

        List<Player> europeanPlayers = new ArrayList<Player>();
        for (Player player : players) {
            if (player.isREF()) {
                player.setEntryLocation(map.getTile(width - 2, random.nextInt(height - 20) + 10));
                continue;
            }
            if (player.isEuropean()) {
                europeanPlayers.add(player);
                logger.finest("found European player " + player.getNationAsString());
            }
        }
        int startingPositions = europeanPlayers.size();
        List<Integer> startingYPositions = new ArrayList<Integer>();

        for (Player player : europeanPlayers) {
            logger.fine("generating units for player " + player.getNationAsString());

            List<Unit> carriers = new ArrayList<Unit>();
            List<Unit> passengers = new ArrayList<Unit>();
            List<AbstractUnit> unitList = ((EuropeanNationType) player.getNationType())
                .getStartingUnits();
            for (AbstractUnit startingUnit : unitList) {
                Unit newUnit = new Unit(map.getGame(), null, player, startingUnit.getUnitType(),
                                        UnitState.SENTRY, startingUnit.getEquipment());
                if (newUnit.hasAbility("model.ability.carryUnits") && newUnit.isNaval()) {
                    newUnit.setState(UnitState.ACTIVE);
                    carriers.add(newUnit);
                } else {
                    passengers.add(newUnit);
                }
                
            }

            boolean startAtSea = true;
            if (carriers.isEmpty()) {
                logger.warning("No carriers defined for player " + player.getNationAsString());
                startAtSea = false;
            }

            int x, y;
            do {
                x = width - 1;
                y = random.nextInt(height - 20) + 10;
            } while (map.getTile(x, y).isLand() == startAtSea);
            while (isStartingPositionTooClose(map, y, startingPositions, startingYPositions)) {
                y = random.nextInt(height - 20) + 10;
            }
            startingYPositions.add(new Integer(y));
            if (startAtSea) {
                while (map.getTile(x - 1, y).getType().canSailToEurope()) {
                    x--;
                }
            }

            Tile startTile = map.getTile(x,y);
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
            
            // START DEBUG:
            if (FreeCol.isInDebugMode()) {
                // in debug mode give each player a few more units and a colony
                UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.galleon");
                Unit unit4 = new Unit(map.getGame(), startTile, player, unitType, UnitState.ACTIVE);
                
                unitType = FreeCol.getSpecification().getUnitType("model.unit.privateer");
                @SuppressWarnings("unused") Unit privateer = new Unit(map.getGame(), startTile, player, unitType, UnitState.ACTIVE);
                
                unitType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
                @SuppressWarnings("unused") Unit unit5 = new Unit(map.getGame(), unit4, player, unitType, UnitState.SENTRY);
                unitType = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");
                @SuppressWarnings("unused") Unit unit6 = new Unit(map.getGame(), unit4, player, unitType, UnitState.SENTRY);
                unitType = FreeCol.getSpecification().getUnitType("model.unit.jesuitMissionary");
                @SuppressWarnings("unused") Unit unit7 = new Unit(map.getGame(), unit4, player, unitType, UnitState.SENTRY);

                Tile colonyTile = null;
                Iterator<Position> cti = map.getFloodFillIterator(new Position(x, y));
                while(cti.hasNext()) {
                    Tile tempTile = map.getTile(cti.next());
                    if (tempTile.isColonizeable()) {
                        colonyTile = tempTile;
                        break;
                    }
                }

                if (colonyTile != null) {
                    for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
                        if (!t.isWater()) {
                            colonyTile.setType(t);
                            break;
                        }
                    }
                    unitType = FreeCol.getSpecification().getUnitType("model.unit.expertFarmer");
                    Unit buildColonyUnit = new Unit(map.getGame(), colonyTile, player, unitType, UnitState.ACTIVE);
                    String colonyName = player.getNationAsString()+" Colony";
                    Colony colony = new Colony(map.getGame(), player, colonyName, colonyTile);
                    buildColonyUnit.buildColony(colony);
                    if (buildColonyUnit.getLocation() instanceof ColonyTile) {
                        Tile ct = ((ColonyTile) buildColonyUnit.getLocation()).getWorkTile();
                        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
                            if (!t.isWater()) {
                                ct.setType(t);
                                break;
                            }
                        }
                    }
                    BuildingType schoolType = FreeCol.getSpecification().getBuildingType("model.building.Schoolhouse");
                    Building schoolhouse = new Building(map.getGame(), colony, schoolType);
                    colony.addBuilding(schoolhouse);
                    unitType = FreeCol.getSpecification().getUnitType("model.unit.masterCarpenter");
                    while (!schoolhouse.canAdd(unitType)) {
                        schoolhouse.upgrade();
                    }
                    Unit carpenter = new Unit(map.getGame(), colonyTile, player, unitType, UnitState.ACTIVE);
                    carpenter.setLocation(colony.getBuildingForProducing(unitType.getExpertProduction()));

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.elderStatesman");
                    Unit statesman = new Unit(map.getGame(), colonyTile, player, unitType, UnitState.ACTIVE);
                    statesman.setLocation(colony.getBuildingForProducing(unitType.getExpertProduction()));

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.expertLumberJack");
                    Unit lumberjack = new Unit(map.getGame(), colony, player, unitType, UnitState.ACTIVE);
                    if (lumberjack.getLocation() instanceof ColonyTile) {
                        Tile lt = ((ColonyTile) lumberjack.getLocation()).getWorkTile();
                        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
                            if (t.isForested()) {
                                lt.setType(t);
                                break;
                            }
                        }
                        lumberjack.setWorkType(Goods.LUMBER);
                    }

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.seasonedScout");
                    @SuppressWarnings("unused") Unit scout = new Unit(map.getGame(), colonyTile, player, 
                                                                      unitType, UnitState.ACTIVE);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");
                    @SuppressWarnings("unused") Unit unit8 = new Unit(map.getGame(), colonyTile, player, 
                                                                      unitType, UnitState.ACTIVE);

                    @SuppressWarnings("unused") Unit unit9 = new Unit(map.getGame(), colonyTile, player, 
                                                                      unitType, UnitState.ACTIVE);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.artillery");
                    @SuppressWarnings("unused") Unit unit10 = new Unit(map.getGame(), colonyTile, player,
                                                                       unitType, UnitState.ACTIVE);

                    @SuppressWarnings("unused") Unit unit11 = new Unit(map.getGame(), colonyTile, player,
                                                                       unitType, UnitState.ACTIVE);

                    @SuppressWarnings("unused") Unit unit12 = new Unit(map.getGame(), colonyTile, player,
                                                                       unitType, UnitState.ACTIVE);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.treasureTrain");
                    Unit unit13 = new Unit(map.getGame(), colonyTile, player, unitType, UnitState.ACTIVE);
                    unit13.setTreasureAmount(10000);

                    /* DEBUGGING LINES FOR AI (0.4.1):
                    for (int j=0; j<10; j++) {
                        Unit u = new Unit(game, null, player, Unit.FREE_COLONIST, Unit.ACTIVE);
                        colony.add(u);
                    }
                    for (int j=0; j<3; j++) {
                        Unit u = new Unit(game, null, player, Unit.PETTY_CRIMINAL, Unit.ACTIVE);
                        colony.add(u);
                    }
                    */
                }
            }
            // END DEBUG
        }
    }

    /**
     * Determine whether a proposed ship starting Y position is "too close"
     * to those already used.
     * @param map a <code>Map</code> value
     * @param proposedY Proposed ship starting Y position
     * @param startingPositions The number of starting positions
     * @return True if the proposed position is too close
     */
    protected boolean isStartingPositionTooClose(Map map, int proposedY, int startingPositions,
                                                 List<Integer> usedYPositions) {
        int distance = (map.getHeight() / 2) / startingPositions;
        for (Integer yPosition : usedYPositions) {
            if (Math.abs(yPosition.intValue() - proposedY) < distance) {
                return true;
            }
        }
        return false;
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
            return player.getNationAsString() + " territory at " + region.toString();
        }
    }


}
