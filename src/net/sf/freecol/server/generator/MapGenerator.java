package net.sf.freecol.server.generator;

import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.server.model.ServerPlayer;


/**
* Creates random maps and sets the starting locations for the players.
*/
public class MapGenerator {
    private static final Logger logger = Logger.getLogger(MapGenerator.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // The directions a Unit can move to.
    protected static final int N = 0,
                               NE = 1,
                               E = 2,
                               SE = 3,
                               S = 4,
                               SW = 5,
                               W = 6,
                               NW = 7;

    // Deltas for moving to adjacent squares. Different due to the
    // isometric map. Starting north and going clockwise.
    private final static int[] ODD_DX = {0, 1, 1, 1, 0, 0, -1, 0};
    private final static int[] ODD_DY = {-2, -1, 0, 1, 2, 1, 0, -1};
    private final static int[] EVEN_DX = {0, 0, 1, 0, 0, -1, -1, -1};
    private final static int[] EVEN_DY = {-2, -1, 0, 1, 2, 1, 0, -1};
    
    private final static int NUM_STARTING_LOCATIONS = 4;

    private Random random = new Random();
    private final Game game;
    private final MapGeneratorOptions mapGeneratorOptions;

    /**
    * Creates a <code>MapGenerator</code> for generating
    * random maps for the given game.
    * @param game The <code>Game</code> this generator
    *             will be making maps for.
    * @see #createMap
    */
    public MapGenerator(Game game) {
        this.game = game;
        this.mapGeneratorOptions = new MapGeneratorOptions();
    }




    /**
     * Creates a new <code>Map</code> where the given players
     * have random starting locations. During this process,
     * <code>game.setMap(...)</code> is invoked.
     *
     * @param players The players to create <code>Settlement</code>s
     *       and starting locations for. That is; both indian and 
     *       european players.
     * @see LandGenerator
     * @see TerrainGenerator
     */
    public void createMap(Vector<Player> players) {        
        LandGenerator landGenerator = new LandGenerator(getMapGeneratorOptions().getWidth(), getMapGeneratorOptions().getHeight());        
        boolean[][] landMap = landGenerator.createLandMap();

        TerrainGenerator terrainGenerator = new TerrainGenerator(landMap);
        Map map = terrainGenerator.createMap();
        game.setMap(map);
        createMountains(map);
        createRivers(map);
        createIndianSettlements(map, players);
        createEuropeanUnits(map, getMapGeneratorOptions().getWidth(), getMapGeneratorOptions().getHeight(), players);
        createLostCityRumours(map);        
    }

    /**
     * Gets the options used when generating the map.
     * @return The <code>MapGeneratorOptions</code>.
     */
    public MapGeneratorOptions getMapGeneratorOptions() {
        return mapGeneratorOptions;
    }

    /**
     * Creates mountain ranges on the given map.  The number and size
     * of mountain ranges depends on the map size.
     *
     * @param map The map to use.
     */
    public void createMountains(Map map) {
        int maximumLength = Math.max(getMapGeneratorOptions().getWidth(), getMapGeneratorOptions().getHeight()) / 10;
        int number = getMapGeneratorOptions().getNumberOfMountainTiles();
        int counter = 0;
        logger.info("Number of land tiles is " + getMapGeneratorOptions().getLand() +
                    ", number of mountain tiles is " + number);
        logger.fine("Maximum length of mountain ranges is " + maximumLength);
        for (int tries = 0; tries < 100; tries++) {
            if (counter < number) {
                Position p = getRandomLandPosition(map);
                if (map.getTile(p).isLand()) {
                    int direction = random.nextInt(8);
                    int length = maximumLength - random.nextInt(maximumLength/2);
                    logger.info("Direction of mountain range is " + direction +
                            ", length of mountain range is " + length);
                    for (int index = 0; index < length; index++) {
                        p = getAdjacent(p, direction);
                        Tile t = map.getTile(p);
                        if (t != null && t.isLand()) {
                            t.setAddition(Tile.ADD_MOUNTAINS);
                            counter++;
                            Iterator it = map.getCircleIterator(p, false, 1);
                            while (it.hasNext()) {
                                t = map.getTile((Position) it.next());
                                if (t.isLand() &&
                                        t.getAddition() != Tile.ADD_MOUNTAINS) {
                                    int r = random.nextInt(8);
                                    if (r == 0) {
                                        t.setAddition(Tile.ADD_MOUNTAINS);
                                        counter++;
                                    } else if (r > 2) {
                                        t.setAddition(Tile.ADD_HILLS);
                                    }
                                }
                            }
                        }
                    }
                    // break;
                }
            }
        }
        logger.info("Added " + counter + " mountain tiles.");
    }   


    /**
     * Creates lost city rumours on the given map.
     * The number of rumours depends on the map size.
     *
     * @param map The map to use.
     */
    public void createLostCityRumours(Map map) {
        int number = getMapGeneratorOptions().getNumberOfRumours();
        int counter = 0;

        for (int i = 0; i < number; i++) {
            for (int tries=0; tries<100; tries++) {
                Position p = new Position(random.nextInt(getMapGeneratorOptions().getWidth()), 
                                          random.nextInt(getMapGeneratorOptions().getHeight()));
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
        

    /**
     * Creates rivers on the given map. The number of rivers depends
     * on the map size.
     *
     * @param map The map to create rivers on.
     */
    public void createRivers(Map map) {
        int number = getMapGeneratorOptions().getNumberOfRivers();
        int counter = 0;
        Hashtable riverMap = new Hashtable();

        for (int i = 0; i < number; i++) {
            River river = new River(map, riverMap);
            for (int tries = 0; tries < 100; tries++) {
                Position position = new Position(random.nextInt(getMapGeneratorOptions().getWidth()),
                                                 random.nextInt(getMapGeneratorOptions().getHeight()));

                if (riverMap.get(position) == null) {
                    if (river.flowFromSource(position)) {
                        logger.fine("Created new river with length " + river.getLength());
                        counter++;
                        break;
                    } else {
                        logger.fine("Failed to generate river.");
                    }
                }
            }
        }

        logger.info("Created " + counter + " rivers of maximum " + number + ".");
    }


    /**
     * Create the Indian settlements, at least a capital for every nation and
     * random numbers of other settlements.
     * 
     * @param map The <code>Map</code> to place the indian settlments on.
     * @param players The players to create <code>Settlement</code>s
     *       and starting locations for. That is; both indian and 
     *       european players.
     */
    protected void createIndianSettlements(Map map, Vector<Player> players) {
        Collections.sort(players, new Comparator<Player>() {
            public int compare(Player o, Player p) {
                return o.getNation() - p.getNation();
            }
        });

        Position[] territoryCenter = new Position[IndianSettlement.LAST_TRIBE+1];
        for (int tribe=0; tribe<territoryCenter.length; tribe++) {        
            int x = random.nextInt(map.getWidth());
            int y = random.nextInt(map.getHeight());
            territoryCenter[tribe] = new Position(x, y);
        }
        
        IndianSettlement[] capitalCandidate = new IndianSettlement[IndianSettlement.LAST_TRIBE+1];
        final int minSettlementDistance = 4;
        final int width = map.getWidth() / minSettlementDistance;
        final int height = map.getHeight() / (minSettlementDistance*2);
        for (int i=1; i<width; i++) {            
            for (int j=1; j<height; j++) {
                int x = i * minSettlementDistance + random.nextInt(3) - 1;
                if (j % 2 != 0) {
                    x += minSettlementDistance / 2;
                }
                int y = j * (2*minSettlementDistance) + random.nextInt(3) - 1;
                if (!map.isValid(x, y)) {        
                    continue;
                }
                Tile candidate = map.getTile(x, y);
                if (candidate.isSettleable()) {
                    int bestTribe = 0;
                    int minDistance = Integer.MAX_VALUE;
                    for (int t=0; t<territoryCenter.length; t++) {
                        if (map.getDistance(territoryCenter[t], candidate.getPosition()) < minDistance) {
                            minDistance = map.getDistance(territoryCenter[t], candidate.getPosition());
                            bestTribe = t;
                        }
                    }
                    IndianSettlement is = placeIndianSettlement((Player) players.get(bestTribe+4), bestTribe, false, candidate.getPosition(), map, players);
                    if (capitalCandidate == null
                            || random.nextInt(width+height) == 1) {
                        capitalCandidate[bestTribe] = is; 
                    }
                }
            }
        }
        for (int i=0; i<capitalCandidate.length; i++) {
            if (capitalCandidate[i] != null) {
                capitalCandidate[i].setCapital(true);
            }
        }
    }


    /**
    * Gets the Indian player of the appropriate tribe.
    * @return The Indian player of the appropriate tribe.
    */
    private ServerPlayer getIndianPlayer(int owner, Vector players) {
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = (ServerPlayer)players.elementAt(i);
            if (player.getNation() == (owner + 4)) {
                return player;
            }
        }
        logger.warning("VERY BAD: Can't find indian player for Indian tribe " + owner);
        return null;
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
                                       Position position, Map map, Vector players) {
        final int kind = IndianSettlement.getKind(tribe);
        final Tile tile = map.getTile(position);
        IndianSettlement settlement = 
            new IndianSettlement(game, player,
                    tile, tribe, kind, capital,
                    generateSkillForLocation(map, map.getTile(position)),
                    false, null);
        logger.fine("Generated skill: " + Unit.getName(settlement.getLearnableSkill()));

        tile.setSettlement(settlement);
        
        tile.setClaim(Tile.CLAIM_CLAIMED);
        tile.setOwner(settlement);
        
        Iterator circleIterator = map.getCircleIterator(position, true, IndianSettlement.getRadius(kind));
        while (circleIterator.hasNext()) {
            Position adjPos = (Position)circleIterator.next();
            map.getTile(adjPos).setClaim(Tile.CLAIM_CLAIMED);
            map.getTile(adjPos).setNationOwner(player.getNation());
        }

        for (int i = 0; i < (kind * 2) + 4; i++) {
            Unit unit = new Unit(game, player, Unit.BRAVE);
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
    private int generateSkillForLocation(Map map, Tile tile) {
        int rand = random.nextInt(2);
        int[] potentials = new int[Goods.HORSES];

        Iterator iter = map.getAdjacentIterator(tile.getPosition());
        while (iter.hasNext()) {
            Map.Position p = (Map.Position)iter.next();
            Tile t = map.getTile(p);

            if (t.hasBonus()) {
                if (t.getAddition() == Tile.ADD_HILLS) {
                    return IndianSettlement.EXPERT_ORE_MINER;
                } else if (t.getAddition() == Tile.ADD_MOUNTAINS) {
                    return IndianSettlement.EXPERT_SILVER_MINER;
                } else if (t.isForested()) {
                    switch (t.getType()) {
                    case Tile.PLAINS:
                    case Tile.PRAIRIE:
                    case Tile.TUNDRA:
                        return IndianSettlement.EXPERT_FUR_TRAPPER;
                    case Tile.GRASSLANDS:
                    case Tile.SAVANNAH:
                        return IndianSettlement.EXPERT_LUMBER_JACK;
                    case Tile.MARSH:
                        return (rand==0 ? IndianSettlement.EXPERT_SILVER_MINER : IndianSettlement.EXPERT_ORE_MINER);
                    case Tile.SWAMP:
                        return (rand==0 ? IndianSettlement.EXPERT_SILVER_MINER : IndianSettlement.EXPERT_ORE_MINER);
                    case Tile.DESERT:
                        return (rand==0 ? IndianSettlement.EXPERT_LUMBER_JACK : IndianSettlement.EXPERT_FARMER);
                    default:
                        throw new IllegalArgumentException("Invalid tile provided: Tile type is invalid");
                    }
                } else {
                    switch (t.getType()) {
                    case Tile.PLAINS:
                        return IndianSettlement.MASTER_COTTON_PLANTER;
                    case Tile.GRASSLANDS:
                        return IndianSettlement.MASTER_TOBACCO_PLANTER;
                    case Tile.PRAIRIE:
                        return IndianSettlement.MASTER_COTTON_PLANTER;
                    case Tile.SAVANNAH:
                        return IndianSettlement.MASTER_SUGAR_PLANTER;
                    case Tile.MARSH:
                        return IndianSettlement.EXPERT_ORE_MINER;
                    case Tile.SWAMP:
                        if (rand == 0) {
                            return IndianSettlement.MASTER_TOBACCO_PLANTER;
                        } else {
                            return IndianSettlement.MASTER_SUGAR_PLANTER;
                        }
                    case Tile.DESERT:
                        return IndianSettlement.SEASONED_SCOUT;
                    case Tile.TUNDRA:
                        if (rand == 0) {
                            return IndianSettlement.EXPERT_SILVER_MINER;
                        } else {
                            return IndianSettlement.EXPERT_ORE_MINER;
                        }
                    case Tile.ARCTIC:
                    case Tile.OCEAN:
                        return IndianSettlement.EXPERT_FISHERMAN;
                    default:
                        throw new IllegalArgumentException("Invalid tile provided: Tile type is invalid");
                    }
                }
            } else {
                for (int goodsType = Goods.FOOD; goodsType < Goods.HORSES; goodsType++) {
                    potentials[goodsType] += t.potential(goodsType);
                }
            }
        }

        int counter = 0;
        for (int goodsType = Goods.FOOD; goodsType < Goods.HORSES; goodsType++) {
            counter += potentials[goodsType];
            potentials[goodsType] = counter;
        }
        int newRand = random.nextInt(counter);
        for (int goodsType = Goods.FOOD; goodsType < Goods.HORSES; goodsType++) {
            if (newRand < potentials[goodsType]) {
                switch (goodsType) {
                case Goods.FOOD:
                    return IndianSettlement.EXPERT_FARMER;
                case Goods.SUGAR:
                    return IndianSettlement.MASTER_SUGAR_PLANTER;
                case Goods.TOBACCO:
                    return IndianSettlement.MASTER_TOBACCO_PLANTER;
                case Goods.COTTON:
                    return IndianSettlement.MASTER_COTTON_PLANTER;
                case Goods.FURS:
                    return IndianSettlement.EXPERT_FUR_TRAPPER;
                case Goods.LUMBER:
                    return IndianSettlement.EXPERT_LUMBER_JACK;
                case Goods.ORE:
                    return IndianSettlement.EXPERT_ORE_MINER;
                case Goods.SILVER:
                    return IndianSettlement.EXPERT_SILVER_MINER;
                default:
                    return IndianSettlement.SEASONED_SCOUT;
                }
            }

        }
        return IndianSettlement.SEASONED_SCOUT;   
    }



    /**
     * Check to see if it is possible to build an Indian settlement at a
     * given map position. A city (Incas and Aztecs) needs a free radius
     * of two tiles, a village or camp needs one tile in every direction.
     * There must be at least three productive tiles in the area including
     * the settlement tile.
     * 
     * @param map The <code>Map</code>.
     * @param position Candidate position
     * @param radius necessary radius
     * @return True if position suitable for settlement
     */
    private boolean isIndianSettlementCandidate(Position position, int radius, Map map) {
        if (map.getTile(position).getClaim() == Tile.CLAIM_NONE) {
            map.getTile(position).setClaim(Tile.CLAIM_VISITED);
            if (map.getTile(position).isSettleable()) {
                int numSettleableNeighbors = 0;
                Iterator iterator = map.getCircleIterator(position, true,
                                                                radius);
                while (iterator.hasNext()) {
                    Position adjPos = (Position)iterator.next();
                    if (map.getTile(adjPos).getClaim() == Tile.CLAIM_CLAIMED) {
                        return false;
                    }
                    if (map.getTile(adjPos).isSettleable() &&
                        map.getDistance(position.getX(), position.getY(),
                                        adjPos.getX(), adjPos.getY()) == 1) {
                        numSettleableNeighbors++;
                    }
                }
                return numSettleableNeighbors >= 2;
            }
        }
        return false;
    }


    /**
     * Select a random position on the map to use as a starting position.
     * 
     * @param map The <code>Map</code>.
     * @return Position selected
     */
    private Position getRandomLandPosition(Map map) {
        int x = random.nextInt(getMapGeneratorOptions().getWidth() - 20) + 10;
        int y = random.nextInt(getMapGeneratorOptions().getHeight() - 20) + 10;
        Position centerPosition = new Position(x, y);
        Iterator it = map.getFloodFillIterator(centerPosition);
        while (it.hasNext()) {
            Position p = (Position) it.next();
            if (map.getTile(p).isLand()) {
                return p;
            }
        }
        return null;    }
    
    /**
     * Select a random position on the map to use as a starting position.
     * 
     * @param map The <code>Map</code>.
     * @return Position selected
     */
    private Position getRandomSettleablePosition(Map map) {
        int x = random.nextInt(getMapGeneratorOptions().getWidth() - 20) + 10;
        int y = random.nextInt(getMapGeneratorOptions().getHeight() - 20) + 10;
        Position centerPosition = new Position(x, y);
        Iterator it = map.getFloodFillIterator(centerPosition);
        while (it.hasNext()) {
            Position p = (Position) it.next();
            if (map.getTile(p).isSettleable()) {
                return p;
            }
        }
        return null;
    }


    /**
     * Create two ships, one with a colonist, for each player, and
     * select suitable starting positions.
     * 
     * @param map The <code>Map</code> to place the european units on.
     * @param width The width of the map to create.
     * @param height The height of the map to create.
     * @param players The players to create <code>Settlement</code>s
     *      and starting locations for. That is; both indian and 
     *      european players.
     */
    protected void createEuropeanUnits(Map map, int width, int height, Vector<Player> players) {
        int[] shipYPos = new int[NUM_STARTING_LOCATIONS];
        for (int i = 0; i < NUM_STARTING_LOCATIONS; i++) {
            shipYPos[i] = 0;
        }

        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = (ServerPlayer)players.elementAt(i);
            if (player.isREF()) {
                player.setEntryLocation(map.getTile(width - 2, random.nextInt(height - 20) + 10));
                continue;
            }
            if (!player.isEuropean()) {
                continue;
            }

            int y = random.nextInt(height - 20) + 10;
            int x = width - 1;
            while (isAShipTooClose(y, shipYPos)) {
                y = random.nextInt(height - 20) + 10;
            }
            shipYPos[i] = y;
            while (map.getTile(x - 1, y).getType() == Tile.HIGH_SEAS) {
                x--;
            }

            Tile startTile = map.getTile(x,y);
            player.setEntryLocation(startTile);

            int navalUnitType = (player.getNation() == ServerPlayer.DUTCH) ? Unit.MERCHANTMAN : Unit.CARAVEL;
            int pioneerUnitType = (player.getNation() == ServerPlayer.FRENCH) ? Unit.HARDY_PIONEER : Unit.FREE_COLONIST;
            int soldierUnitType = (player.getNation() == ServerPlayer.SPANISH) ? Unit.VETERAN_SOLDIER : Unit.FREE_COLONIST;

            Unit unit1 = new Unit(game, startTile, player, navalUnitType, Unit.ACTIVE);
            //unit1.setName(Messages.message("shipName." + player.getNation() + ".0"));
            Unit unit2 = new Unit(game, unit1, player, pioneerUnitType, Unit.SENTRY, false, false, 100, false);
            Unit unit3 = new Unit(game, unit1, player, soldierUnitType, Unit.SENTRY, true, false, 0, false);

            // START DEBUG:
            if (net.sf.freecol.FreeCol.isInDebugMode()) {
                Unit unit4 = new Unit(game, startTile, player, Unit.GALLEON, Unit.ACTIVE);
                Unit unit5 = new Unit(game, unit4, player, Unit.FREE_COLONIST, Unit.SENTRY);
                Unit unit6 = new Unit(game, unit4, player, Unit.VETERAN_SOLDIER, Unit.SENTRY);
                Unit unit7 = new Unit(game, unit4, player, Unit.JESUIT_MISSIONARY, Unit.SENTRY);

                Tile colonyTile = null;
                Iterator cti = map.getFloodFillIterator(new Position(x, y));
                while(cti.hasNext()) {
                    Tile tempTile = map.getTile((Position) cti.next());
                    if (tempTile.isColonizeable()) {
                        colonyTile = tempTile;
                        break;
                    }
                }

                if (colonyTile != null) {
                    colonyTile.setType(Tile.PRAIRIE);
                    colonyTile.setForested(false);
                    colonyTile.setPlowed(true);
                    colonyTile.setAddition(Tile.ADD_NONE);

                    Unit buildColonyUnit = new Unit(game, colonyTile, player, Unit.EXPERT_FARMER, Unit.ACTIVE);
                    Colony colony = new Colony(game, player, "Colony for Testing", colonyTile);
                    buildColonyUnit.buildColony(colony);
                    if (buildColonyUnit.getLocation() instanceof ColonyTile) {
                        Tile ct = ((ColonyTile) buildColonyUnit.getLocation()).getWorkTile();
                        ct.setType(Tile.PLAINS);
                        ct.setForested(false);
                        ct.setPlowed(true);
                        ct.setAddition(Tile.ADD_NONE);
                    }
                    colony.getBuilding(Building.SCHOOLHOUSE).setLevel(Building.SHOP);

                    Unit carpenter = new Unit(game, colonyTile, player, Unit.MASTER_CARPENTER, Unit.ACTIVE);
                    carpenter.setLocation(colony.getBuilding(Building.CARPENTER));

                    Unit statesman = new Unit(game, colonyTile, player, Unit.ELDER_STATESMAN, Unit.ACTIVE);
                    statesman.setLocation(colony.getBuilding(Building.TOWN_HALL));

                    Unit lumberjack = new Unit(game, colony, player, Unit.EXPERT_LUMBER_JACK, Unit.ACTIVE);
                    if (lumberjack.getLocation() instanceof ColonyTile) {
                        Tile lt = ((ColonyTile) lumberjack.getLocation()).getWorkTile();
                        lt.setType(Tile.PLAINS);
                        lt.setForested(true);
                        lt.setRoad(true);
                        lt.setAddition(Tile.ADD_NONE);
                        lumberjack.setWorkType(Goods.LUMBER);
                    }

                    Unit scout = new Unit(game, colonyTile, player, Unit.SEASONED_SCOUT, Unit.ACTIVE);
                    Unit unit8 = new Unit(game, colonyTile, player, Unit.VETERAN_SOLDIER, Unit.ACTIVE);
                    Unit unit9 = new Unit(game, colonyTile, player, Unit.VETERAN_SOLDIER, Unit.ACTIVE);
                    Unit unit10 = new Unit(game, colonyTile, player, Unit.ARTILLERY, Unit.ACTIVE);
                    Unit unit11 = new Unit(game, colonyTile, player, Unit.ARTILLERY, Unit.ACTIVE);
                    Unit unit12 = new Unit(game, colonyTile, player, Unit.ARTILLERY, Unit.ACTIVE);
                    Unit unit13 = new Unit(game, colonyTile, player, Unit.TREASURE_TRAIN, Unit.ACTIVE);
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
    protected boolean isAShipTooClose(int proposedY,
                                        int[] usedYPositions) {
        for (int i = 0; i < NUM_STARTING_LOCATIONS && usedYPositions[i] != 0; i++) {
            if (Math.abs(usedYPositions[i] - proposedY) < (getMapGeneratorOptions().getHeight() / 2) / NUM_STARTING_LOCATIONS) {
                return true;
            }
        }
        return false;
    }


    /**
    * Gets the position adjacent to a given position, in a given
    * direction.
    *
    * @param position The position
    * @param direction The direction (N, NE, E, etc.)
    * @return Adjacent position
    */
    protected Position getAdjacent(Position position, int direction) {
        int x = position.getX() + ((position.getY() & 1) != 0 ?
            ODD_DX[direction] : EVEN_DX[direction]);
        int y = position.getY() + ((position.getY() & 1) != 0 ?
            ODD_DY[direction] : EVEN_DY[direction]);
        return new Position(x, y);
    }


    /**
     * Checks if the given position is valid.
     * 
     * @param x The x-coordinate of the position.
     * @param y The y-coordinate of the position.
     * @return <code>true</code> if the given position is 
     *        within the bounds of the map and <code>false</code> otherwise
     */
    protected boolean isValid(int x, int y) {
        return x>=0 && x < getMapGeneratorOptions().getWidth() && y >= 0 && y < getMapGeneratorOptions().getHeight();
    }

    
    /**
     * Checks if the given position is valid.
     * 
     * @param p The position.
     * @return <code>true</code> if the given position is 
     *        within the bounds of the map and <code>false</code> otherwise.
     */    
    protected boolean isValid(Position p) {
        return isValid(p.getX(), p.getY());
    }



    /**
    * Class for making a <code>Map</code> based upon a land map.
    */
    protected class TerrainGenerator {
                int distToLandFromHighSeas = getMapGeneratorOptions().getDistLandHighSea();
                int maxDistanceToEdge = getMapGeneratorOptions().getMaxDistToEdge();

        private boolean[][] landMap;

        private Random tileType;


        /**
        * Creates a new <code>TerrainGenerator</code>.
        * @param landMap Determines wether there should be land
        *                or ocean on a given tile. This array also
        *                specifies the size of the map that is going
        *                to be created.
        * @see #createMap
        */
        TerrainGenerator(boolean[][] landMap) {
            this.landMap = landMap;
        }


        /**
        * Gets a random tile type based on the given percentage.
        *
        * @param percent The location of the tile, where 100% is the center on
        *        the y-axis and 0% is on the top/bottom of the map.
        */
        private int getRandomTileType(int percent) {
            int thisValue = Math.max(((percent - tileType.nextInt(20) - 1)) / 10, 0);
    
            int minWoD = 0;
            int maxWoD = 99;
            int dryOrWet = tileType.nextInt(maxWoD - minWoD) + minWoD;
            dryOrWet /= 33;
            switch(thisValue) {
            case 0: return Tile.ARCTIC;
            case 1: case 2: switch (dryOrWet) {
                case 0: return Tile.TUNDRA;
                case 1: default: return Tile.TUNDRA;
                case 2: return Tile.MARSH;
            }
            case 3: case 4: case 5: default: switch (dryOrWet) {
                case 0: return Tile.DESERT;
                case 1: default: return Tile.PLAINS;
                case 2: return Tile.PRAIRIE;
            }
            case 6: case 7: case 8: case 9: switch (dryOrWet) {
                case 0: return Tile.GRASSLANDS;
                case 1: default: return Tile.SAVANNAH;
                case 2: return Tile.SWAMP;
            }
            }
        }


        /**
        * Creates the map.
        * @return The new <code>Map</code>.
        */
        public Map createMap() {
            Vector<Vector<Tile>> columns = new Vector<Vector<Tile>>(getMapGeneratorOptions().getWidth());
            tileType = new Random();
            for (int i = 0; i < getMapGeneratorOptions().getWidth(); i++) {
                Vector<Tile> v = new Vector<Tile>(getMapGeneratorOptions().getHeight());
                for (int j = 0; j < getMapGeneratorOptions().getHeight(); j++) {
                    Tile t;

                    if (landMap[i][j]) {
                        t = new Tile(game, getRandomTileType(((Math.min(j, getMapGeneratorOptions().getHeight() - j) * 200) / getMapGeneratorOptions().getHeight())), i, j);

                        if ((t.getType() != Tile.ARCTIC) && 
                            (tileType.nextInt(100) < getMapGeneratorOptions().getPercentageOfForests())) {
                            t.setForested(true);
                            //} else if ((t.getType() != Tile.ARCTIC) && (t.getType() != Tile.TUNDRA)) {
                        } else if (t.getType() != Tile.ARCTIC) {
                            int k = tileType.nextInt(16);
                            if (k < 1) {
                                t.setAddition(Tile.ADD_MOUNTAINS);
                            } else if (k < 2) {
                                t.setAddition(Tile.ADD_HILLS);
                            }
                        }

                        // Terrain bonus:
                        if (tileType.nextInt(100) < getMapGeneratorOptions().getPercentageOfBonusTiles()) {
                            t.setBonus(true);
                        }
                    } else {
                        t = new Tile(game, Tile.OCEAN, i, j);

                        // Terrain bonus:
                        int adjacentLand = 0;
                        for (int k=0; k<8; k++) {
                            Position mp = getAdjacent(t.getPosition(), k);
                            if (isValid(mp) && landMap[mp.getX()][mp.getY()]) {
                                adjacentLand++;
                            }
                        }

                        if (adjacentLand > 1 && tileType.nextInt(10 - adjacentLand) == 0) {
                            t.setBonus(true);
                        }
                    }

                    v.add(t);
                }
                columns.add(v);
            }

            Map map = new Map(game, columns);

            createHighSeas(map);

            return map;
        }


        /**
        * Places "high seas"-tiles on the border of the given map.
        * @param map The <code>Map</code> to create high seas on.
        */
        protected void createHighSeas(Map map) {
            for (int y = 0; y < getMapGeneratorOptions().getHeight(); y++) {
                for (int x=0; x<maxDistanceToEdge && !map.isLandWithinDistance(x, y, distToLandFromHighSeas); x++) {
                        map.getTile(x, y).setType(Tile.HIGH_SEAS);
                }

                for (int x=1; x<=maxDistanceToEdge && !map.isLandWithinDistance(getMapGeneratorOptions().getWidth()-x, y, distToLandFromHighSeas); x++) {
                        map.getTile(getMapGeneratorOptions().getWidth()-x, y).setType(Tile.HIGH_SEAS);
                }
            }
        }


    }


    /**
    * Class for creating a land map.
    */
    protected class LandGenerator {
                int preferredDistanceToEdge = getMapGeneratorOptions().getPrefDistToEdge();
        private static final int C = 2;

        private boolean[][] map;
        private boolean[][] visited;

        private int numberOfLandTiles = 0;


        /**
         * Creates a new <code>LandGenerator</code>.
         * 
         * @param width The width of the map to be generated.
         * @param height The height of the map to be generated.
         * @see #createLandMap
         */
        LandGenerator(int width, int height) {
            map = new boolean[width][height];
            visited = new boolean[width][height];
        }



        /**
         * Creates a new land map.
         * 
         * @return An array where <i>true</i> means land 
         * and <i>false</i> means ocean.
         */
        boolean[][] createLandMap() {
            int x;
            int y;

            int minLandMass = getMapGeneratorOptions().getLandMass();

            int minimumNumberOfTiles = (map.length * map[0].length * minLandMass - 2 * preferredDistanceToEdge) / 100;
            while (numberOfLandTiles < minimumNumberOfTiles) {
                do {
                    x=((int) (Math.random() * (map.length-preferredDistanceToEdge*2))) + preferredDistanceToEdge;
                    y=((int) (Math.random() * (map[0].length-preferredDistanceToEdge*4))) + preferredDistanceToEdge * 2;
                } while (map[x][y]);

                map[x][y] = true;
                setLand(x,y);
            }

            cleanMap();
            createPolarRegions();

            return map;
        }


        private void createPolarRegions() {
            for (int x=0; x<map.length; x++) {
                map[x][0] = true;
                map[x][1] = true;
                map[x][map[0].length-1] = true;
                map[x][map[0].length-2] = true;
            }
        }


        private boolean hasLandMass(int minLandMass) {
            return (numberOfLandTiles * 100) / (map.length * map[0].length) >= minLandMass;
        }


        /**
        * Removes any 1x1 islands on the map.
        */
        private void cleanMap() {
            for (int y=0; y<map[0].length; y++) {
                for (int x=0; x<map.length; x++) {
                    if (isSingleTile(x, y)) {
                        map[x][y] = false;
                    }
                }
            }
        }


        /**
        * Returns <i>true</i> if there are no adjacent land
        * to the given coordinates.
        */
        private boolean isSingleTile(int x, int y) {
            Position p = new Position(x, y);

            for (int i=1; i<8;i+=2) {
                Position n = getAdjacent(p, i);
                if (isValid(n) && map[n.getX()][n.getY()]) {
                    return false;
                }
            }

            return true;
        }


        private int getEdge(int x, int y) {
            int u = Math.max(preferredDistanceToEdge-Math.min(x, map.length-x),
                             2*preferredDistanceToEdge-Math.min(y, map[0].length-y))
                    + C;

            return (u>0 ? u : 0);
        }


        private void setLand(int x, int y) {
            if (visited[x][y]) {
                return;
            }

            visited[x][y] = true;
            numberOfLandTiles++;

            Position p = new Position(x, y);

            for (int i=1; i<8;i+=2) {
                Position n = getAdjacent(p, i);
                if (isValid(n)) {
                    gl(n.getX(), n.getY());
                }
            }
        }


        private void gl(int i, int j) {
            if (visited[i][j]) {
                return;
            }

            int r = ((int) (Math.random() * 8)) + 1 - C + getEdge(i,j);

            int sum = -1;

            Position p = new Position(i, j);

            for (int k=0; k<8;k++) {
                Position n = getAdjacent(p, k);
                if (isValid(n) && map[n.getX()][n.getY()]) {
                    sum++;
                }
            }

            if (sum >= r) {
                if (!map[i][j]) {
                    map[i][j] = true;
                    setLand(i,j);
                }
            }
        }

    }

}
