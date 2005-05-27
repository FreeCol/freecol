package net.sf.freecol.server.generator;

import java.util.*;

import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.FreeColException;

import java.util.logging.Logger;


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
    private final int[] ODD_DX = {0, 1, 1, 1, 0, 0, -1, 0};
    private final int[] ODD_DY = {-2, -1, 0, 1, 2, 1, 0, -1};
    private final int[] EVEN_DX = {0, 0, 1, 0, 0, -1, -1, -1};
    private final int[] EVEN_DY = {-2, -1, 0, 1, 2, 1, 0, -1};

    private Random random = new Random();
    private Game game;
    private int width;
    private int height;



    /**
    * Creates a <code>MapGenerator</code> for generating
    * random maps for the given game.
    * @param game The <code>Game</code> this generator
    *             will be making maps for.
    * @see #createMap
    */
    public MapGenerator(Game game) {
        this.game = game;
    }




    /**
    * Creates a new <code>Map</code> where the given players
    * have random starting locations. During this process,
    * <code>game.setMap(...)</code> is invoked.
    *
    * @see LandGenerator
    * @see TerrainGenerator
    */
    public void createMap(Vector players, int width, int height) throws FreeColException {
        this.width = width;
        this.height = height;

        LandGenerator landGenerator = new LandGenerator(width, height);
        boolean[][] landMap = landGenerator.createLandMap();

        TerrainGenerator terrainGenerator = new TerrainGenerator(landMap);
        Map map = terrainGenerator.createMap();
        
        game.setMap(map);

        createIndianSettlements(map, players);

        createEuropeanUnits(map, width, height, players);
    }


    /**
     * Create the Indian settlements, at least a capital for every nation and
     * random numbers of other settlements.
     * @throws FreeColException if thrown by a called method
     */
    protected void createIndianSettlements(Map map, Vector players) throws FreeColException {
        Iterator incaIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator aztecIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator arawakIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator cherokeeIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator iroquoisIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator siouxIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator apacheIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));
        Iterator tupiIterator = map.getFloodFillIterator
                                                (getRandomStartingPos(map));

        placeIndianSettlement(IndianSettlement.INCA,
                              IndianSettlement.CITY,
                              true, incaIterator, map, players);
        placeIndianSettlement(IndianSettlement.AZTEC,
                              IndianSettlement.CITY,
                              true, aztecIterator, map, players);
        placeIndianSettlement(IndianSettlement.ARAWAK,
                              IndianSettlement.VILLAGE,
                              true, arawakIterator, map, players);
        placeIndianSettlement(IndianSettlement.CHEROKEE,
                              IndianSettlement.VILLAGE,
                              true, cherokeeIterator, map, players);
        placeIndianSettlement(IndianSettlement.IROQUOIS,
                              IndianSettlement.VILLAGE,
                              true, iroquoisIterator, map, players);
        placeIndianSettlement(IndianSettlement.SIOUX,
                              IndianSettlement.CAMP,
                              true, siouxIterator, map, players);
        placeIndianSettlement(IndianSettlement.APACHE,
                              IndianSettlement.CAMP,
                              true, apacheIterator, map, players);
        placeIndianSettlement(IndianSettlement.TUPI,
                              IndianSettlement.CAMP,
                              true, tupiIterator, map, players);

        while (incaIterator.hasNext() || aztecIterator.hasNext() ||
                arawakIterator.hasNext() || cherokeeIterator.hasNext() ||
                iroquoisIterator.hasNext() || siouxIterator.hasNext() ||
                apacheIterator.hasNext() || tupiIterator.hasNext()) {
            if (incaIterator.hasNext() && random.nextInt(5) != 0)
                placeIndianSettlement(IndianSettlement.INCA,
                                      IndianSettlement.CITY,
                                      false, incaIterator, map, players);
            if (aztecIterator.hasNext() && random.nextInt(5) != 0)
                placeIndianSettlement(IndianSettlement.AZTEC,
                                      IndianSettlement.CITY,
                                      false, aztecIterator, map, players);
            if (arawakIterator.hasNext() && random.nextInt(3) != 0)
                placeIndianSettlement(IndianSettlement.ARAWAK,
                                      IndianSettlement.VILLAGE,
                                      false, arawakIterator, map, players);
            if (cherokeeIterator.hasNext() && random.nextInt(4) != 0)
                placeIndianSettlement(IndianSettlement.CHEROKEE,
                                      IndianSettlement.VILLAGE,
                                      false, cherokeeIterator, map, players);
            if (iroquoisIterator.hasNext() && random.nextInt(4) != 0)
                placeIndianSettlement(IndianSettlement.IROQUOIS,
                                      IndianSettlement.VILLAGE,
                                      false, iroquoisIterator, map, players);
            if (siouxIterator.hasNext() && random.nextInt(4) != 0)
                placeIndianSettlement(IndianSettlement.SIOUX,
                                      IndianSettlement.CAMP,
                                      false, siouxIterator, map, players);
            if (apacheIterator.hasNext() && random.nextInt(3) != 0)
                placeIndianSettlement(IndianSettlement.APACHE,
                                      IndianSettlement.CAMP,
                                      false, apacheIterator, map, players);
            if (tupiIterator.hasNext() && random.nextInt(2) != 0)
                placeIndianSettlement(IndianSettlement.TUPI,
                                      IndianSettlement.CAMP,
                                      false, tupiIterator, map, players);
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
     * Finds a suitable location for a settlement and builds it there. If no
     * location can be found (the iterator is exhausted) the settlement will
     * be discarded.
     * @param settlement The settlement to place
     * @param iterator The nation's iterator to use
     * @throws FreeColException if thrown by a called method
     */
    private void placeIndianSettlement(int owner, int type,
                                       boolean capital,
                                       Iterator iterator, Map map, Vector players)
                                throws FreeColException {
        while (iterator.hasNext()) {
            Position position = (Position)iterator.next();
            int radius = (type == IndianSettlement.CITY) ? 2 : 1;
            if (isIndianSettlementCandidate(position, radius + 1, map) &&
                        random.nextInt(2) != 0) {

                ServerPlayer player = getIndianPlayer(owner, players);
                //System.out.println("Setting indian settlement at "
                //                   + position.getX() + "x" + position.getY());

                int[] wantedGoods = generateWantedGoodsForLocation(map,map.getTile(position));

                map.getTile(position).setSettlement(
                    new IndianSettlement(game, player,
                                         map.getTile(position), owner, type, capital,
                                         generateSkillForLocation(map, map.getTile(position)),
                                         wantedGoods[0],
                                         wantedGoods[1],
                                         wantedGoods[2],
                                         false, null)
                );

                map.getTile(position).setClaim(Tile.CLAIM_CLAIMED);
                map.getTile(position).setOwner(map.getTile(position).getSettlement());
                Iterator circleIterator = map.getCircleIterator
                                                (position, true, radius);
                while (circleIterator.hasNext()) {
                    Position adjPos = (Position)circleIterator.next();
                    map.getTile(adjPos).setClaim(Tile.CLAIM_CLAIMED);
                    map.getTile(adjPos).setNationOwner(player.getNation());
                }

                for (int i = 0; i < (type * 2) + 4; i++) {
                    Unit unit = new Unit(game, player, Unit.BRAVE);
                    unit.setIndianSettlement((IndianSettlement) map.getTile(position).getSettlement());
                    if (i == 0) {
                        unit.setLocation(map.getTile(position));
                    } else {
                        unit.setLocation(map.getTile(position).getSettlement());
                    }

                }

                return;
            }
        }
    }


    /**
    * Generates wanted goods for a given location (indian settlement)

    * @param tile The tile where the settlement is located.
    * @return An int array containing the 3 wanted goods.
    */

    private int[] generateWantedGoodsForLocation(Map map, Tile mainTile) {

        Iterator iter = map.getAdjacentIterator(mainTile.getPosition());
  
        //logger.info("creating want list for settlement");

        /* this probably needs finetuning. 
           The indians should prefer manufactured goods.

           TODO: take hills and mountains into consideration
        
        */

        // for every good there's a counter, some of them are randomized
        // depending on the surrounding fields, the values are decremented

        int[] goodsTable=new int[16];

        goodsTable[Goods.FOOD]=20;
        goodsTable[Goods.SUGAR]=10;
        goodsTable[Goods.TOBACCO]=10;
        goodsTable[Goods.COTTON]=10;
        goodsTable[Goods.FURS]=10;
        goodsTable[Goods.LUMBER]=10;
        goodsTable[Goods.ORE]=10;
        goodsTable[Goods.SILVER]=10;
        goodsTable[Goods.HORSES]=7+random.nextInt(10);
        goodsTable[Goods.RUM]=8+random.nextInt(10);
        goodsTable[Goods.CIGARS]=8+random.nextInt(10);
        goodsTable[Goods.CLOTH]=7+random.nextInt(10);
        goodsTable[Goods.COATS]=7+random.nextInt(10);
        goodsTable[Goods.TRADE_GOODS]=7+random.nextInt(10);
        goodsTable[Goods.TOOLS]=10;
        goodsTable[Goods.MUSKETS]=5+random.nextInt(10);

        while (iter.hasNext()) {
        
            Map.Position p = (Map.Position)iter.next();
            Tile tile = map.getTile(p);
        
            int forested=0;
        
            if (tile.isForested()) forested=1;
            
            switch (tile.getType()) {
                case Tile.PLAINS:     goodsTable[Goods.FOOD]-=4;
                                      goodsTable[Goods.COTTON]-=2;
                                      goodsTable[Goods.CLOTH]-=2;
                                      goodsTable[Goods.ORE]-=1;
                                      goodsTable[Goods.MUSKETS]-=1;
                                      goodsTable[Goods.LUMBER]-=forested*3;
                                      break;
                case Tile.DESERT:     goodsTable[Goods.FOOD]--;
                                      goodsTable[Goods.COTTON]--;
                                      goodsTable[Goods.CLOTH]--;
                                      goodsTable[Goods.ORE]-=2;
                                      goodsTable[Goods.MUSKETS]-=1;
                                      goodsTable[Goods.LUMBER]-=forested*1;
                                      break;
                case Tile.TUNDRA:     goodsTable[Goods.FOOD]-=2;
                                      goodsTable[Goods.ORE]-=2;
                                      goodsTable[Goods.MUSKETS]-=1;
                                      goodsTable[Goods.LUMBER]-=forested*2;
                                      break;
                case Tile.PRAIRIE:    goodsTable[Goods.FOOD]-=2;
                                      goodsTable[Goods.COTTON]-=3;
                                      goodsTable[Goods.CLOTH]-=3;
                                      goodsTable[Goods.LUMBER]-=forested*2;
                                      break;
                case Tile.GRASSLANDS: goodsTable[Goods.FOOD]-=2;
                                      goodsTable[Goods.TOBACCO]-=3;
                                      goodsTable[Goods.CIGARS]-=3;
                                      goodsTable[Goods.LUMBER]-=forested*3;
                                      break;
                case Tile.SAVANNAH:   goodsTable[Goods.FOOD]-=3;
                                      goodsTable[Goods.SUGAR]-=3;
                                      goodsTable[Goods.RUM]-=3;
                                      goodsTable[Goods.LUMBER]-=forested*2;
                                      break;
                case Tile.MARSH:      goodsTable[Goods.FOOD]-=2;
                                      goodsTable[Goods.TOBACCO]-=2;
                                      goodsTable[Goods.CIGARS]-=2;
                                      goodsTable[Goods.ORE]-=2;
                                      goodsTable[Goods.MUSKETS]-=1;
                                      goodsTable[Goods.LUMBER]-=forested*2;
                                      break;
                case Tile.SWAMP:      goodsTable[Goods.FOOD]-=2;
                                      goodsTable[Goods.SUGAR]-=2;
                                      goodsTable[Goods.RUM]-=2;
                                      goodsTable[Goods.ORE]-=2;
                                      goodsTable[Goods.MUSKETS]-=1;
                                      goodsTable[Goods.LUMBER]-=forested*2;
            }
            /*System.out.print("tile type: "+ tile.getType());
            System.out.println(" forested: "+forested);
            System.out.println("FD  |SG  |TB  |CT  |FU  |LU  |OR  |SI  |HO  |  RU|CI  |CL  |CA  |TG  |TO  |MU");
            for (int i=0;i<=15;i++) {
                if (goodsTable[i]>9||goodsTable[i]<0) System.out.print(goodsTable[i]+"  |");
                if (goodsTable[i]<10&&goodsTable[i]>-1) System.out.print(goodsTable[i]+"   |");
            }*/
        }       
        
        int good,good1,good2;
        good=good1=good2=0;
        int curval;

        for (int i=0;i<=15;i++) {
        
                curval=goodsTable[i];
                
                // found a good that is the greatest so far
                if (curval>=goodsTable[good]) {
                        good2=good1;
                        good1=good;
                        good=i;
                }       

                // found a good that is > good1 but <good
                if (curval>=goodsTable[good1] && curval<goodsTable[good]) {
                        good2=good1;
                        good1=i;
                }

                // found a good that is > good2 but <good1
                if (curval>=goodsTable[good2] && curval<goodsTable[good1]) {
                        good2=i;
                }
                
        }

        //logger.info("the indians choose: "+good+" and "+good1+" and "+good2);
        //logger.info("===============");
        
        // constructs the return array
        int rv[] = new int[3];
        rv[0]=good;
        rv[1]=good1;
        rv[2]=good2;

        return rv;
        
    }

    /**
    * Generates a skill that could be taught from a settlement on the given Tile.
    * TODO: This method should be properly implemented. The surrounding terrain
    *       should be taken into account and it should be partially randomized.
    * @param tile The tile where the settlement will be located.
    * @return A skill that can be taught to Europeans.
    */
    private int generateSkillForLocation(Map map, Tile tile) {
        int rand = random.nextInt(2);

        Iterator iter = map.getAdjacentIterator(tile.getPosition());
        while (iter.hasNext()) {
            Map.Position p = (Map.Position)iter.next();
            Tile t = map.getTile(p);

            // has bonus but no forest
            if (!t.isForested() && t.hasBonus() && t.getAddition()<=Tile.ADD_RIVER_MAJOR) {
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
                        return IndianSettlement.MASTER_TOBACCO_PLANTER;
                    case Tile.DESERT:
                        return IndianSettlement.SEASONED_SCOUT;
                    case Tile.TUNDRA:
                        return (rand==0 ? IndianSettlement.EXPERT_SILVER_MINER : IndianSettlement.EXPERT_ORE_MINER);
                    case Tile.ARCTIC:
                    case Tile.OCEAN:
                        return IndianSettlement.EXPERT_FISHERMAN;
                    default:
                        throw new IllegalArgumentException("Invalid tile provided: Tile type is invalid");
                }
            }
            // has bonus and forest
            else if (t.isForested() && t.hasBonus() && t.getAddition()<=Tile.ADD_RIVER_MAJOR) {
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
            }
            // is hills
            else if (t.hasBonus() && t.getAddition() == Tile.ADD_HILLS) {
                return IndianSettlement.EXPERT_ORE_MINER;
            }
            // has mountains
            else if (t.hasBonus() && t.getAddition() == Tile.ADD_MOUNTAINS) {
                if(t.hasBonus())
                    return IndianSettlement.EXPERT_SILVER_MINER;
                else
                    return (rand==0 ? IndianSettlement.EXPERT_ORE_MINER : IndianSettlement.EXPERT_SILVER_MINER);
            }
        }

        // has no bonuses so use base tile
        switch (tile.getType()) {
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
                return IndianSettlement.MASTER_TOBACCO_PLANTER;
            case Tile.DESERT:
                return IndianSettlement.SEASONED_SCOUT;
            case Tile.TUNDRA:
                return (rand==0 ? IndianSettlement.EXPERT_SILVER_MINER : IndianSettlement.EXPERT_ORE_MINER);
            case Tile.ARCTIC:
            case Tile.OCEAN:
                return IndianSettlement.EXPERT_FISHERMAN;
            default:
                throw new IllegalArgumentException("Invalid tile provided: Tile type is invalid");
        }
    }



    /**
     * Check to see if it is possible to build an Indian settlement at a
     * given map position. A city (Incas and Aztecs) needs a free radius
     * of two tiles, a village or camp needs one tile in every direction.
     * There must be at least three productive tiles in the area including
     * the settlement tile.
     * @param position Candidate position
     * @param radius necessary radius
     * @return True if position suitable for settlement
     * @throws FreeColException if thrown by a called method
     */
    private boolean isIndianSettlementCandidate(Position position, int radius, Map map)
                        throws FreeColException {
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
     * @return Position selected
     */
    private Position getRandomStartingPos(Map map) {
        int x = 0;
        int y = 0;
        while (!map.getTile(x, y).isLand()) {
            x = random.nextInt(width - 20) + 10;
            y = random.nextInt(height - 20) + 10;
        }
        return new Map.Position(x, y);
    }


    /**
     * Create two ships, one with a colonist, for each player, and
     * select suitable starting positions.
     * @param players List of players
     * @throws FreeColException if thrown by a called method
     */
    protected void createEuropeanUnits(Map map, int width, int height, Vector players) throws FreeColException {
        int[] shipYPos = new int[4];
        for (int i = 0; i < 4; i++)
            shipYPos[i] = 0;
        for (int i = 0; i < players.size(); i++) {
            ServerPlayer player = (ServerPlayer)players.elementAt(i);
            if (player.getNation() >= Player.INCA) break; // Stop once you get into the Indian players -sjm
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

            int unitType;
            if (player.getNation() == ServerPlayer.DUTCH) {
                unitType = Unit.MERCHANTMAN;
            } else {
                unitType = Unit.CARAVEL;
            }

            Unit unit1 = new Unit(game, startTile, player, unitType, Unit.ACTIVE);
            Unit unit2 = new Unit(game, unit1, player, Unit.HARDY_PIONEER, Unit.SENTRY);
            Unit unit3 = new Unit(game, unit1, player, Unit.FREE_COLONIST, Unit.SENTRY);
            unit3.setArmed(true, true);
            unit3.setMovesLeft(unit3.getInitialMovesLeft());

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

                    break;
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
        for (int i = 0; i < 4 && usedYPositions[i] != 0; i++) {
            if (Math.abs(usedYPositions[i] - proposedY) < 8) {
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



    protected boolean isValid(int x, int y) {
        return x>=0 && x < width && y >= 0 && y < height;
    }

    protected boolean isValid(Position p) {
        return isValid(p.getX(), p.getY());
    }



    /**
    * Class for making a <code>Map</code> based upon a land map.
    */
    protected class TerrainGenerator {

        private static final int DISTANCE_TO_LAND_FROM_HIGH_SEAS = 4;
        private static final int MAX_DISTANCE_TO_EDGE = 12;

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
              case 0: return Tile.PLAINS;
              case 1: default: return Tile.PRAIRIE;
              case 2: return Tile.DESERT;
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
        */
        public Map createMap() {
            Vector columns = new Vector(width);
            tileType = new Random();
            for (int i = 0; i < width; i++) {
                Vector v = new Vector(height);
                for (int j = 0; j < height; j++) {
                    Tile t;

                    if (landMap[i][j]) {
                        t = new Tile(game, getRandomTileType(((Math.min(j, height - j) * 200) / height)), i, j);

                        if ((t.getType() != Tile.ARCTIC) && (tileType.nextInt(3) > 1)) {
                            t.setForested(true);
                        } else if ((t.getType() != Tile.ARCTIC) && (t.getType() != Tile.TUNDRA)) {
                            int k = tileType.nextInt(8);
                            if (k >= 6) {
                                t.setAddition(Tile.ADD_MOUNTAINS);
                            } else if (k >= 4) {
                                t.setAddition(Tile.ADD_HILLS);
                            }
                        }

                        // Terrain bonus:
                        if (tileType.nextInt(10) == 0 && (t.getType() != Tile.ARCTIC && (t.getType() != Tile.TUNDRA || t.isForested())
                                && t.getType() != Tile.HIGH_SEAS && t.getType() != Tile.DESERT && t.getType() != Tile.MARSH
                                && t.getType() != Tile.SWAMP || t.getAddition() == Tile.ADD_MOUNTAINS || t.getAddition() == Tile.ADD_HILLS)) {
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
        */
        protected void createHighSeas(Map map) {
            for (int y = 0; y < height; y++) {
                for (int x=0; x<MAX_DISTANCE_TO_EDGE && !map.isLandWithinDistance(x, y, DISTANCE_TO_LAND_FROM_HIGH_SEAS); x++) {
                        map.getTile(x, y).setType(Tile.HIGH_SEAS);
                }

                for (int x=1; x<=MAX_DISTANCE_TO_EDGE && !map.isLandWithinDistance(width-x, y, DISTANCE_TO_LAND_FROM_HIGH_SEAS); x++) {
                        map.getTile(width-x, y).setType(Tile.HIGH_SEAS);
                }
            }
        }


    }


    /**
    * Class for creating a land map.
    */
    protected class LandGenerator {


        private static final int PREFERRED_DISTANCE_TO_EDGE = 4;
        private static final int C = 2;

        private boolean[][] map;
        private boolean[][] visited;

        private int numberOfLandTiles = 0;


        /**
        * Creates a new <code>LandGenerator</code>.
        * @see #createLandMap
        */
        LandGenerator(int width, int height) {
            map = new boolean[width][height];
            visited = new boolean[width][height];
        }



        /**
        * Creates a new land map. That is; an array
        * where <i>true</i> means land and <i>false</i>
        * means ocean.
        */
        boolean[][] createLandMap() {
            int x;
            int y;

            int minLandMass = 25;
            int minimumNumberOfTiles = (map.length * map[0].length * minLandMass - 2 * PREFERRED_DISTANCE_TO_EDGE) / 100;
            while (numberOfLandTiles < minimumNumberOfTiles) {
                do {
                    x=((int) (Math.random() * (map.length-PREFERRED_DISTANCE_TO_EDGE*2))) + PREFERRED_DISTANCE_TO_EDGE;
                    y=((int) (Math.random() * (map[0].length-PREFERRED_DISTANCE_TO_EDGE*4))) + PREFERRED_DISTANCE_TO_EDGE * 2;
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
            int u = Math.max(PREFERRED_DISTANCE_TO_EDGE-Math.min(x, map.length-x),
                             2*PREFERRED_DISTANCE_TO_EDGE-Math.min(y, map[0].length-y))
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
