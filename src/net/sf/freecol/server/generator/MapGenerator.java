package net.sf.freecol.server.generator;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Creates random maps and sets the starting locations for the players.
 */
public class MapGenerator {
    private static final Logger logger = Logger.getLogger(MapGenerator.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final static int NUM_STARTING_LOCATIONS = 4;

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
                            .toArray(new FreeColGameObject[0]));
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
     * @param map The <code>Map</code> to place the indian settlments on.
     * @param players The players to create <code>Settlement</code>s
     *       and starting locations for. That is; both indian and 
     *       european players. If players does not contain any indian players, 
     *       no settlements are added.
     */
    protected void createIndianSettlements(Map map, Vector<Player> players) {
        Collections.sort(players, new Comparator<Player>() {
            public int compare(Player o, Player p) {
                return o.getNation() - p.getNation();
            }
        });

        Vector<Player> indians = new Vector<Player>();

        for (Player player : players) {
            if (player.isIndian())
                indians.add(player);
        }

        if (indians.size() == 0)
            return;

        Position[] territoryCenter = new Position[indians.size()];
        for (int tribe = 0; tribe < territoryCenter.length; tribe++) {
            int x = random.nextInt(map.getWidth());
            int y = random.nextInt(map.getHeight());
            territoryCenter[tribe] = new Position(x, y);
        }

        IndianSettlement[] capitalCandidate = new IndianSettlement[indians.size()];
        final int minSettlementDistance = 4;
        final int width = map.getWidth() / minSettlementDistance;
        final int height = map.getHeight() / (minSettlementDistance * 2);
        for (int i = 1; i < width; i++) {
            for (int j = 1; j < height; j++) {
                int x = i * minSettlementDistance + random.nextInt(3) - 1;
                if (j % 2 != 0) {
                    x += minSettlementDistance / 2;
                }
                int y = j * (2 * minSettlementDistance) + random.nextInt(3) - 1;
                if (!map.isValid(x, y)) {
                    continue;
                }
                Tile candidate = map.getTile(x, y);
                if (candidate.isSettleable()) {
                    int bestTribe = 0;
                    int minDistance = Integer.MAX_VALUE;
                    for (int t = 0; t < territoryCenter.length; t++) {
                        if (map.getDistance(territoryCenter[t], candidate.getPosition()) < minDistance) {
                            minDistance = map.getDistance(territoryCenter[t], candidate
                                .getPosition());
                            bestTribe = t;
                        }
                    }
                    IndianSettlement is = placeIndianSettlement(players.get(bestTribe + 4),
                        bestTribe, false, candidate.getPosition(), map, players);

                    // CO: Fix for missing capital
                    if (capitalCandidate[bestTribe] == null) {
                        capitalCandidate[bestTribe] = is;
                    } else {
                        // If new settlement is closer to center of territory
                        // for this tribe, mark it as a better candidate
                        if (map.getDistance(territoryCenter[bestTribe], capitalCandidate[bestTribe]
                            .getTile().getPosition()) > map.getDistance(territoryCenter[bestTribe],
                            candidate.getPosition()))
                            capitalCandidate[bestTribe] = is;
                    }
                }
            }
        }
        for (int i = 0; i < capitalCandidate.length; i++) {
            if (capitalCandidate[i] != null) {
                capitalCandidate[i].setCapital(true);
            }
        }
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
    private IndianSettlement placeIndianSettlement(Player player, int tribe, boolean capital,
                                       Position position, Map map, Vector<Player> players) {
        final int kind = IndianSettlement.getKind(tribe);
        final Tile tile = map.getTile(position);
        IndianSettlement settlement = 
            new IndianSettlement(map.getGame(), player,
                    tile, tribe, kind, capital,
                    generateSkillForLocation(map, map.getTile(position)),
                    false, null);
        logger.fine("Generated skill: " + Unit.getName(settlement.getLearnableSkill()));

        tile.setSettlement(settlement);
        
        tile.setClaim(Tile.CLAIM_CLAIMED);
        tile.setOwner(settlement);
        
        Iterator<Position> circleIterator = map.getCircleIterator(position, true, IndianSettlement.getRadius(kind));
        while (circleIterator.hasNext()) {
            Position adjPos = circleIterator.next();
            map.getTile(adjPos).setClaim(Tile.CLAIM_CLAIMED);
            map.getTile(adjPos).setNationOwner(player.getNation());
        }

        for (int i = 0; i < (kind * 2) + 4; i++) {
            Unit unit = new Unit(map.getGame(), player, Unit.BRAVE);
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
        int rand = random.nextInt(2);
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
                for (GoodsType g : r.getBonusTypeList()) {
                    int index = farmedList.indexOf(g);
                    if (index >= 0) {
                        potentials[index]++;
                        bonuses[index]++;
                    }
                }
            } else {
                TileType tileType = t.getType();
                for (GoodsType g : tileType.getPotentialTypeList()) {
                    int index = farmedList.indexOf(g);
                    if (index >= 0) {
                        potentials[index]++;
                    }
                }
            }
        }

        int counter = 0;
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
    protected void createEuropeanUnits(Map map, Vector<Player> players) {
        final int width = map.getWidth();
        final int height = map.getHeight();
        int[] shipYPos = new int[NUM_STARTING_LOCATIONS];
        for (int i = 0; i < NUM_STARTING_LOCATIONS; i++) {
            shipYPos[i] = 0;
        }

        for (int i = 0; i < players.size(); i++) {
            Player player = players.elementAt(i);
            if (player.isREF()) {
                player.setEntryLocation(map.getTile(width - 2, random.nextInt(height - 20) + 10));
                continue;
            }
            if (!player.isEuropean()) {
                continue;
            }

            int y = random.nextInt(height - 20) + 10;
            int x = width - 1;
            while (isAShipTooClose(map, y, shipYPos)) {
                y = random.nextInt(height - 20) + 10;
            }
            shipYPos[i] = y;
            while (map.getTile(x - 1, y).getType() == Tile.HIGH_SEAS) {
                x--;
            }

            Tile startTile = map.getTile(x,y);
            startTile.setExploredBy(player, true);
            player.setEntryLocation(startTile);

            int navalUnitType = (player.getNation() == ServerPlayer.DUTCH) ? Unit.MERCHANTMAN : Unit.CARAVEL;
            int pioneerUnitType = (player.getNation() == ServerPlayer.FRENCH) ? Unit.HARDY_PIONEER : Unit.FREE_COLONIST;
            int soldierUnitType = (player.getNation() == ServerPlayer.SPANISH) ? Unit.VETERAN_SOLDIER : Unit.FREE_COLONIST;

            Unit unit1 = new Unit(map.getGame(), startTile, player, navalUnitType, Unit.ACTIVE);
            //unit1.setName(Messages.message("shipName." + player.getNation() + ".0"));
            @SuppressWarnings("unused") Unit unit2 = new Unit(map.getGame(), unit1, player, pioneerUnitType, Unit.SENTRY, false, false, 100, false);
            @SuppressWarnings("unused") Unit unit3 = new Unit(map.getGame(), unit1, player, soldierUnitType, Unit.SENTRY, true, false, 0, false);

            // START DEBUG:
            if (FreeCol.isInDebugMode()) {
                Unit unit4 = new Unit(map.getGame(), startTile, player, Unit.GALLEON, Unit.ACTIVE);
                @SuppressWarnings("unused") Unit unit5 = new Unit(map.getGame(), unit4, player, Unit.FREE_COLONIST, Unit.SENTRY);
                @SuppressWarnings("unused") Unit unit6 = new Unit(map.getGame(), unit4, player, Unit.VETERAN_SOLDIER, Unit.SENTRY);
                @SuppressWarnings("unused") Unit unit7 = new Unit(map.getGame(), unit4, player, Unit.JESUIT_MISSIONARY, Unit.SENTRY);

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

                    Unit buildColonyUnit = new Unit(map.getGame(), colonyTile, player, Unit.EXPERT_FARMER, Unit.ACTIVE);
                    Colony colony = new Colony(map.getGame(), player, "Colony for Testing", colonyTile);
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
                    colony.getBuilding(Building.SCHOOLHOUSE).setLevel(Building.SHOP);

                    Unit carpenter = new Unit(map.getGame(), colonyTile, player, Unit.MASTER_CARPENTER, Unit.ACTIVE);
                    carpenter.setLocation(colony.getBuilding(Building.CARPENTER));

                    Unit statesman = new Unit(map.getGame(), colonyTile, player, Unit.ELDER_STATESMAN, Unit.ACTIVE);
                    statesman.setLocation(colony.getBuilding(Building.TOWN_HALL));

                    Unit lumberjack = new Unit(map.getGame(), colony, player, Unit.EXPERT_LUMBER_JACK, Unit.ACTIVE);
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

                    @SuppressWarnings("unused") Unit scout = new Unit(map.getGame(), colonyTile, player, Unit.SEASONED_SCOUT, Unit.ACTIVE);
                    @SuppressWarnings("unused") Unit unit8 = new Unit(map.getGame(), colonyTile, player, Unit.VETERAN_SOLDIER, Unit.ACTIVE);
                    @SuppressWarnings("unused") Unit unit9 = new Unit(map.getGame(), colonyTile, player, Unit.VETERAN_SOLDIER, Unit.ACTIVE);
                    @SuppressWarnings("unused") Unit unit10 = new Unit(map.getGame(), colonyTile, player, Unit.ARTILLERY, Unit.ACTIVE);
                    @SuppressWarnings("unused") Unit unit11 = new Unit(map.getGame(), colonyTile, player, Unit.ARTILLERY, Unit.ACTIVE);
                    @SuppressWarnings("unused") Unit unit12 = new Unit(map.getGame(), colonyTile, player, Unit.ARTILLERY, Unit.ACTIVE);
                    Unit unit13 = new Unit(map.getGame(), colonyTile, player, Unit.TREASURE_TRAIN, Unit.ACTIVE);
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
     * @param proposedY Proposed ship starting Y position
     * @param usedYPositions List of already assigned positions
     * @return True if the proposed position is too close
     */
    protected boolean isAShipTooClose(Map map, int proposedY,
                                        int[] usedYPositions) {
        for (int i = 0; i < NUM_STARTING_LOCATIONS && usedYPositions[i] != 0; i++) {
            if (Math.abs(usedYPositions[i] - proposedY) < (map.getHeight() / 2) / NUM_STARTING_LOCATIONS) {
                return true;
            }
        }
        return false;
    }
}
