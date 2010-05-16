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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
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
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
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
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.XMLStream;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerRegion;


/**
 * Creates random maps and sets the starting locations for the players.
 */
public class MapGenerator implements IMapGenerator {

    private static final Logger logger = Logger.getLogger(MapGenerator.class.getName());
    
    private final Random random;
    private final MapGeneratorOptions mapGeneratorOptions;
    
    private final LandGenerator landGenerator;
    private final TerrainGenerator terrainGenerator;
    
    // To avoid starting positions to be too close to the poles
    // percentage indicating how much of the half map close to the pole cannot be spawned on
    private static final float MIN_DISTANCE_FROM_POLE = 0.30f;

    
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


    /* (non-Javadoc)
	 * @see net.sf.freecol.server.generator.IMapGenerator#createMap(net.sf.freecol.common.model.Game)
	 */
    /* (non-Javadoc)
	 * @see net.sf.freecol.server.generator.IMapGenerator#createMap(net.sf.freecol.common.model.Game)
	 */
    public void createMap(Game game) throws FreeColException {        
        
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
     * Loads a <code>Game</code> from the given <code>File</code>.
     * 
     * @param importFile The <code>File</code> to be loading the
     *      <code>Game</code> from.
     * @return The <code>Game</code>.
     */
    private Game loadSaveGame(File importFile) throws FreeColException {        
        /* 
         * TODO-LATER: We are using same method in FreeColServer.
         *       Create a framework for loading games/maps.
         */
        XMLStream xs = null;
        Game game = null;
        try {
            final FreeColSavegameFile fis = new FreeColSavegameFile(importFile);
            xs = FreeColServer.createXMLStreamReader(fis);
            final XMLStreamReader xsr = xs.getXMLStreamReader();
            xsr.nextTag();
            
            FreeColServer.checkSavegameVersion(xsr);
            
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
                    game = new ServerGame(null, null, xsr, serverObjects
                                    .toArray(new FreeColGameObject[serverObjects.size()]));
                    game.setCurrentPlayer(null);
                    game.checkIntegrity();
                }
            }
            xs.close();
        } catch (XMLStreamException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new FreeColException(e.toString());
        } catch (FreeColException fe) {
            StringWriter sw = new StringWriter();
            fe.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new FreeColException(fe.toString());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new FreeColException(e.toString());
        } finally {
            xs.close();
        }
        return game;
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
    public MapGeneratorOptions getMapGeneratorOptions() {
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
                        t.add(new LostCityRumour(t.getGame(), t));
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
    private void createIndianSettlements(final Map map, List<Player> players) {

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
                        ServerRegion region = (ServerRegion) map.getRegion(name);
                        if (region == null) {
                            territory = new Territory(player, map.getRandomLandPosition());
                        } else {
                            territory = new Territory(player, region);
                        }
                        territoryMap.put(name, territory);
                        logger.fine("Allocated region " + name + " for " +
                                    player + ". Center is " +
                                    territory.getCenter() + ".");
                        break;
                    }
                }
                if (territory == null) {
                    logger.warning("Failed to allocate preferred region " + regionNames.get(0) +
                                   " for " + player.getNation());
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
        
        // Default value for map editor
        int nativeSettlementDensity = 50;
        boolean isNativeSettlementDensitySet = Specification.getSpecification().hasOption("model.option.nativeSettlementDensity");
        // A difficulty level was set, use the level value instead
        if(isNativeSettlementDensitySet){
        	nativeSettlementDensity = Specification.getSpecification().getIntegerOption("model.option.nativeSettlementDensity").getValue();
        }
        
        int number = mapGeneratorOptions.getNumberOfSettlements() * nativeSettlementDensity / 100;

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
        int counter = 0;
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
                String name = "default region";
                if (territory.region != null) {
                    name = territory.region.getNameKey();
                }
                logger.fine("Placing the " + territory.player + 
                        " capital in region: " + name +
                        " at Tile: "+ tile.getPosition());
                placeIndianSettlement(territory.player, true, tile.getPosition(), map);
                territory.numberOfSettlements--;
                territory.position = tile.getPosition();
                settlementTiles.remove(tile);
                counter++;
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
        for (Tile tile : settlementTiles) {
            Territory territory = getClosestTerritory(map, tile, territories);
            if (territory == null) {
                // no more territories
                break;
            } else {
                String name = "default region";
                if (territory.region != null) {
                    name = territory.region.getNameKey();
                }
                logger.fine("Placing a " + territory.player + 
                        " camp in region: " + name +
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
        IndianSettlement settlement = 
            new IndianSettlement(map.getGame(), player, tile,
                                 Messages.getDefaultSettlementName(player, capital), capital,
                                 generateSkillForLocation(map, tile, player.getNationType()),
                                 new HashSet<Player>(), null);
        logger.fine("Generated skill: " + settlement.getLearnableSkill());

        int unitCount = settlement.getGeneratedUnitCount();
        for (int i = 0; i < unitCount; i++) {
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
        settlement.placeSettlement();
        Map.CircleIterator iterator = map.getCircleIterator(position, false, settlement.getRadius() + 1);
        while (iterator.hasNext()) {
            Position p = iterator.next();
            if (map.getTile(p).isLand() && random.nextInt(2) == 0) {
                settlement.claimTile(map.getTile(p));
            }
        }

        iterator = map.getCircleIterator(position, false, settlement.getRadius() + 2);
        while (iterator.hasNext()) {
            Position p = iterator.next();
            if (map.getTile(p).isLand() && random.nextInt(4) == 0) {
                settlement.claimTile(map.getTile(p));
            }
        }

        // START DEBUG:
        if (FreeCol.isInDebugMode()) {
            for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
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

        Iterator<Position> iter = map.getAdjacentIterator(tile.getPosition());
        while (iter.hasNext()) {
            Map.Position p = iter.next();
            Tile t = map.getTile(p);
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
        UnitType skill = RandomChoice.getWeightedRandom(random, scaledSkills);
        if (skill == null) {
            // Seasoned Scout
            List<UnitType> unitList = FreeCol.getSpecification().getUnitTypesWithAbility("model.ability.expertScout");
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
        final int width = map.getWidth();
        final int height = map.getHeight();
        final int poleDistance = (int)(MIN_DISTANCE_FROM_POLE*height/2);
        Game game = map.getGame();

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
        int startingPositions = europeanPlayers.size();
        List<Integer> startingYPositions = new ArrayList<Integer>();

        for (Player player : europeanPlayers) {
            logger.fine("generating units for player " + player);

            List<Unit> carriers = new ArrayList<Unit>();
            List<Unit> passengers = new ArrayList<Unit>();
            List<AbstractUnit> unitList = ((EuropeanNationType) player.getNationType())
                .getStartingUnits();
            for (AbstractUnit startingUnit : unitList) {
                Unit newUnit = new Unit(game, null, player, startingUnit.getUnitType(),
                                        UnitState.SENTRY, startingUnit.getEquipment());
                if (newUnit.canCarryUnits() && newUnit.isNaval()) {
                    newUnit.setState(UnitState.ACTIVE);
                    carriers.add(newUnit);
                } else {
                    passengers.add(newUnit);
                }
                
            }

            boolean startAtSea = true;
            if (carriers.isEmpty()) {
                logger.warning("No carriers defined for player " + player);
                startAtSea = false;
            }

            // find an appropriate starting latitude
            int x = width - 1;  // eastern edge of map
            int y;
            do {
                 y = random.nextInt(height - poleDistance*2) + poleDistance;
            } while (map.getTile(x, y).isLand() == startAtSea ||
                     isStartingPositionTooClose(map, y, startingPositions, startingYPositions));
            startingYPositions.add(new Integer(y));
            
            if (startAtSea) {
                // move westward to find the limit between high seas and coastal waters
                while (map.getTile(x - 1, y).canMoveToEurope()) {
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
            if (FreeCol.isInFullDebugMode()) {
                // in debug mode give each player a few more units and a colony
                UnitType unitType = FreeCol.getSpecification().getUnitType("model.unit.galleon");
                Unit unit4 = new Unit(game, startTile, player, unitType, UnitState.ACTIVE);
                
                unitType = FreeCol.getSpecification().getUnitType("model.unit.privateer");
                @SuppressWarnings("unused") Unit privateer = new Unit(game, startTile, player, unitType, UnitState.ACTIVE);
                
                unitType = FreeCol.getSpecification().getUnitType("model.unit.freeColonist");
                @SuppressWarnings("unused") Unit unit5 = new Unit(game, unit4, player, unitType, UnitState.SENTRY);
                unitType = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");
                @SuppressWarnings("unused") Unit unit6 = new Unit(game, unit4, player, unitType, UnitState.SENTRY);
                unitType = FreeCol.getSpecification().getUnitType("model.unit.jesuitMissionary");
                @SuppressWarnings("unused") Unit unit7 = new Unit(game, unit4, player, unitType, UnitState.SENTRY);

                Tile colonyTile = null;
                Iterator<Position> cti = map.getFloodFillIterator(new Position(x, y));
                while(cti.hasNext()) {
                    Tile tempTile = map.getTile(cti.next());
                    if (tempTile.getY() <= LandGenerator.POLAR_HEIGHT ||
                        tempTile.getY() >= map.getHeight() - LandGenerator.POLAR_HEIGHT - 1) {
                        // do not place the initial colony at the pole
                        continue;
                    }
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
                    Unit buildColonyUnit = new Unit(game, colonyTile, player, unitType, UnitState.ACTIVE);
                    String colonyName = Messages.message(player.getNationName()) + " Colony";
                    Colony colony = new Colony(game, player, colonyName, colonyTile);
                    colony.placeSettlement();
                    buildColonyUnit.setState(UnitState.IN_COLONY);
                    buildColonyUnit.setLocation(colony);
                    if (buildColonyUnit.getLocation() instanceof ColonyTile) {
                        Tile ct = ((ColonyTile) buildColonyUnit.getLocation()).getWorkTile();
                        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
                            if (!t.isWater()) {
                                ct.setType(t);
                                TileImprovementType plowType = FreeCol.getSpecification()
                                    .getTileImprovementType("model.improvement.plow");
                                TileImprovementType roadType = FreeCol.getSpecification()
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
                    BuildingType schoolType = FreeCol.getSpecification().getBuildingType("model.building.schoolhouse");
                    Building schoolhouse = new Building(game, colony, schoolType);
                    colony.addBuilding(schoolhouse);
                    unitType = FreeCol.getSpecification().getUnitType("model.unit.masterCarpenter");
                    while (!schoolhouse.canAdd(unitType)) {
                        schoolhouse.upgrade();
                    }
                    Unit carpenter = new Unit(game, colonyTile, player, unitType, UnitState.ACTIVE);
                    carpenter.setLocation(colony.getBuildingForProducing(unitType.getExpertProduction()));

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.elderStatesman");
                    Unit statesman = new Unit(game, colonyTile, player, unitType, UnitState.ACTIVE);
                    statesman.setLocation(colony.getBuildingForProducing(unitType.getExpertProduction()));

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.expertLumberJack");
                    Unit lumberjack = new Unit(game, colony, player, unitType, UnitState.ACTIVE);
                    if (lumberjack.getLocation() instanceof ColonyTile) {
                        Tile lt = ((ColonyTile) lumberjack.getLocation()).getWorkTile();
                        for (TileType t : FreeCol.getSpecification().getTileTypeList()) {
                            if (t.isForested()) {
                                lt.setType(t);
                                break;
                            }
                        }
                        lumberjack.setWorkType(lumberjack.getType().getExpertProduction());
                    }

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.seasonedScout");
                    @SuppressWarnings("unused")
                    Unit scout = new Unit(game, colonyTile, player, 
                                          unitType, UnitState.ACTIVE);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.veteranSoldier");
                    @SuppressWarnings("unused")
                    Unit unit8 = new Unit(game, colonyTile, player, 
                                          unitType, UnitState.ACTIVE);

                    @SuppressWarnings("unused")
                    Unit unit9 = new Unit(game, colonyTile, player, 
                                          unitType, UnitState.ACTIVE);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.artillery");
                    @SuppressWarnings("unused")
                    Unit unit10 = new Unit(game, colonyTile, player,
                                           unitType, UnitState.ACTIVE);

                    @SuppressWarnings("unused")
                    Unit unit11 = new Unit(game, colonyTile, player,
                                           unitType, UnitState.ACTIVE);

                    @SuppressWarnings("unused")
                    Unit unit12 = new Unit(game, colonyTile, player,
                                           unitType, UnitState.ACTIVE);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.treasureTrain");
                    Unit unit13 = new Unit(game, colonyTile, player, unitType, UnitState.ACTIVE);
                    unit13.setTreasureAmount(10000);
                    
                    unitType = FreeCol.getSpecification().getUnitType("model.unit.wagonTrain");
                    Unit unit14 = new Unit(game, colonyTile, player, unitType, UnitState.ACTIVE);
                    GoodsType cigarsType = FreeCol.getSpecification().getGoodsType("model.goods.cigars");
                    Goods cigards = new Goods(game, unit14, cigarsType, 5);
                    unit14.add(cigards);

                    unitType = FreeCol.getSpecification().getUnitType("model.unit.jesuitMissionary");
                    @SuppressWarnings("unused") Unit unit15 = new Unit(game, colonyTile, player,
                                                                       unitType, UnitState.ACTIVE);
                    @SuppressWarnings("unused") Unit unit16 = new Unit(game, colonyTile, player,
                                                                       unitType, UnitState.ACTIVE);

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
    private boolean isStartingPositionTooClose(Map map, int proposedY, int startingPositions,
                                                 List<Integer> usedYPositions) {
        final int poleDistance = (int)(MIN_DISTANCE_FROM_POLE*map.getHeight()/2);
        final int spawnableRange = map.getHeight() - poleDistance*2;
        final int minimumDistance = spawnableRange / (startingPositions * 2);
        for (Integer yPosition : usedYPositions) {
            if (Math.abs(yPosition.intValue() - proposedY) < minimumDistance) {
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
            return player + " territory at " + region.toString();
        }
    }


}
