package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Map.CircleIterator;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.server.generator.River;

import org.w3c.dom.Element;

/**
 * Represents a single tile on the <code>Map</code>.
 * 
 * @see Map
 */
public final class Tile extends FreeColGameObject implements Location, Nameable {
    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    // The type of a Tile can be one of the following.
    public static final int UNEXPLORED = 0, PLAINS = 1, GRASSLANDS = 2, PRAIRIE = 3, SAVANNAH = 4, MARSH = 5,
            SWAMP = 6, DESERT = 7, TUNDRA = 8, ARCTIC = 9, OCEAN = 10, HIGH_SEAS = 11;

    // An addition onto the tile can be one of the following:
    public static final int ADD_NONE = 0, ADD_RIVER_MINOR = 1, ADD_RIVER_MAJOR = 2, ADD_HILLS = 3, ADD_MOUNTAINS = 4;

    // Indians' claims on the tile may be one of the following:
    public static final int CLAIM_NONE = 0, CLAIM_VISITED = 1, CLAIM_CLAIMED = 2;

    // Please someone tell me they want to put this data into a separate file...
    // -sjm
    // Twelve tile types, sixteen goods types, and unforested/forested.
    public static final int[][][] potentialtable = {
        //  Food      Sugar    Tobacco   Cotton     Furs      Wood      Ore      Silver
        { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } }, // Unexp
        { { 5, 3 }, { 0, 0 }, { 0, 0 }, { 2, 1 }, { 0, 3 }, { 0, 6 }, { 1, 0 }, { 0, 0 } }, // Plains
        { { 3, 2 }, { 0, 0 }, { 3, 1 }, { 0, 0 }, { 0, 2 }, { 0, 6 }, { 0, 0 }, { 0, 0 } }, // Grasslands
        { { 3, 2 }, { 0, 0 }, { 0, 0 }, { 3, 1 }, { 0, 2 }, { 0, 4 }, { 0, 0 }, { 0, 0 } }, // Prairie
        { { 4, 3 }, { 3, 1 }, { 0, 0 }, { 0, 0 }, { 0, 2 }, { 0, 4 }, { 0, 0 }, { 0, 0 } }, // Savannah
        { { 3, 2 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 2 }, { 0, 4 }, { 2, 1 }, { 0, 0 } }, // Marsh
        { { 3, 2 }, { 2, 1 }, { 2, 1 }, { 0, 0 }, { 0, 1 }, { 0, 4 }, { 2, 1 }, { 0, 0 } }, // Swamp
        { { 1, 2 }, { 0, 0 }, { 0, 0 }, { 1, 1 }, { 0, 2 }, { 0, 2 }, { 2, 1 }, { 0, 0 } }, // Desert
        { { 3, 2 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 3 }, { 0, 4 }, { 2, 1 }, { 0, 0 } }, // Tundra
        { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } }, // Arctic
        { { 2, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } }, // Ocean
        { { 2, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 } }, // High seas
        { { 2, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 4, 0 }, { 0, 0 } }, // Hills
        { { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 0, 0 }, { 3, 0 }, { 1, 0 } } // Mountains
    };

    private boolean road, plowed, forested, bonus, lostCityRumour;

    private int type;

    /**
     * The type of river on this tile.
     */
    private int river = 0;

    private int additionType;

    private int x, y;

    private Position position;

    private int indianClaim;

    /** The nation that consider this tile to be their land. */
    private int nationOwner = Player.NO_NATION;

    /**
     * A pointer to the settlement located on this tile or 'null' if there is no
     * settlement on this tile.
     */
    private Settlement settlement;

    private UnitContainer unitContainer;

    /** The number of adjacent land tiles, if this is an ocean tile */
    private int landCount = Integer.MIN_VALUE;

    /** The fish bonus of this tile, if it is an ocean tile */
    private int fishBonus = Integer.MIN_VALUE;

    /**
     * Indicates which colony or Indian settlement that owns this tile ('null'
     * indicates no owner). A colony owns the tile it is located on, and every
     * tile with a worker on it. Note that while units and settlements are owned
     * by a player, a tile is owned by a settlement.
     */
    private Settlement owner;

    /**
     * Stores each player's image of this tile. Only initialized when needed.
     */
    private PlayerExploredTile[] playerExploredTiles = null;


    /**
     * Creates a new object with the type <code>UNEXPLORED</code>.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> belongs to.
     * @param locX The x-position of this tile on the map.
     * @param locY The y-position of this tile on the map.
     */
    public Tile(Game game, int locX, int locY) {
        this(game, UNEXPLORED, locX, locY);

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }
    }

    /**
     * A constructor to use.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> belongs to.
     * @param type The type.
     * @param locX The x-position of this tile on the map.
     * @param locY The y-position of this tile on the map.
     */
    public Tile(Game game, int type, int locX, int locY) {
        super(game);

        unitContainer = new UnitContainer(game, this);
        this.type = type;
        this.additionType = ADD_NONE;
        this.indianClaim = CLAIM_NONE;

        road = false;
        plowed = false;
        forested = false;
        bonus = false;
        lostCityRumour = false;

        x = locX;
        y = locY;
        position = new Position(x, y);

        owner = null;
        settlement = null;

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> should be
     *            created in.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Tile(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }

        readFromXML(in);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> should be
     *            created in.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Tile(Game game, Element e) {
        super(game, e);

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Tile</code> with the given ID. The object should
     * later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     * 
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Tile(Game game, String id) {
        super(game, id);

        if (getGame().getViewOwner() == null) {
            playerExploredTiles = new PlayerExploredTile[Player.NUMBER_OF_NATIONS];
        }
    }

    /**
     * Gets the name of this tile. The value of the name depends on the
     * {@link #getType type}, {@link #isForested forested} and
     * {@link #getAddition addition} of the tile.
     * 
     * @return The name as a <code>String</code>.
     */
    public String getName() {
        return getName(getType(), isForested(), getAddition());
    }
    
    /**
     * Gets the name of the given tile type.
     * 
     * @return The name as a <code>String</code>.
     */
    public static String getName(int tileType, boolean forested, int addition) {
        if (addition == ADD_MOUNTAINS) {
            return Messages.message("mountains");
        } else if (addition == ADD_HILLS) {
            return Messages.message("hills");
        } else if (0 < tileType && tileType < FreeCol.specification.numberOfTileTypes()) {

            TileType t = FreeCol.specification.tileType(tileType);
            return forested ? Messages.message(t.whenForested.name) : Messages.message(t.name);
        }
        return Messages.message("unexplored");
    }

    /**
     * Set the <code>Name</code> value.
     * 
     * @param newName The new Name value.
     */
    public void setName(String newName) {
        // this.name = newName;
    }

    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public String getLocationName() {
        if (settlement == null) {
            Settlement nearSettlement = null;
            int radius = 8; // more than 8 tiles away is no longer "near"
            CircleIterator mapIterator = getMap().getCircleIterator(getPosition(), true, radius);
            while (mapIterator.hasNext()) {
                nearSettlement = getMap().getTile(mapIterator.nextPosition()).getSettlement();
                if (nearSettlement != null && nearSettlement instanceof Colony) {
                    String name = ((Colony) nearSettlement).getName();
                    return getName() + " ("
                            + Messages.message("nearLocation", new String[][] { { "%location%", name } }) + ")";
                }
            }
            return getName();
        } else {
            return settlement.getLocationName();
        }
    }

    /**
     * Gets the distance in tiles between this <code>Tile</code> and the
     * specified one.
     * 
     * @param tile The <code>Tile</code> to check the distance to.
     * @return Distance
     */
    public int getDistanceTo(Tile tile) {
        return getGame().getMap().getDistance(getPosition(), tile.getPosition());
    }

    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Gets the total value of all treasure trains on this <code>Tile</code>.
     * 
     * @return The total value of all treasure trains on this <code>Tile</code>
     *         or <code>0</code> if there are no treasure trains at all.
     */
    public int getUnitTreasureAmount() {
        int amount = 0;

        Iterator<Unit> ui = getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (u.getType() == Unit.TREASURE_TRAIN) {
                amount += u.getTreasureAmount();
            }
        }

        return amount;
    }

    /**
     * Returns the treasure train carrying the largest treasure located on this
     * <code>Tile</code>.
     * 
     * @return The best treasure train or <code>null</code> if no treasure
     *         train is located on this <code>Tile</code>.
     */
    public Unit getBestTreasureTrain() {
        Unit bestTreasureTrain = null;

        Iterator<Unit> ui = getUnitIterator();
        while (ui.hasNext()) {
            Unit u = ui.next();
            if (u.getType() == Unit.TREASURE_TRAIN
                    && (bestTreasureTrain == null || bestTreasureTrain.getTreasureAmount() < u.getTreasureAmount())) {
                bestTreasureTrain = u;
            }
        }

        return bestTreasureTrain;
    }

    /**
     * Calculates the value of a future colony at this tile.
     * 
     * @return The value of a future colony located on this tile. This value is
     *         used by the AI when deciding where to build a new colony.
     */
    public int getColonyValue() {
        if (!isLand()) {
            return 0;
        } else if (potential(Goods.FOOD) < 2) {
            return 0;
        } else if (getSettlement() != null) {
            return 0;
        } else {
            int value = potential(Goods.FOOD) * 3;

            boolean nearbyTileHasForest = false;
            boolean nearbyTileIsOcean = false;

            for (Tile tile : getGame().getMap().getSurroundingTiles(this, 1)) {
                if (tile.getColony() != null) {
                    // can't build next to colony
                    return 0;
                } else if (tile.getSettlement() != null) {
                    // can build next to an indian settlement
                    value -= 10;
                } else {
                    if (tile.isLand()) {
                        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
                            value += tile.potential(i);
                        }
                        if (tile.isForested()) {
                            nearbyTileHasForest = true;
                        }
                    } else {
                        nearbyTileIsOcean = true;
                        value += tile.potential(Goods.FOOD);
                    }
                    if (tile.hasBonus()) {
                        value += 20;
                    }

                    if (tile.getNationOwner() != Player.NO_NATION
                            && tile.getNationOwner() != getGame().getCurrentPlayer().getNation()) {
                        // tile is already owned by someone (and not by us!)
                        if (Player.isEuropean(tile.getNationOwner())) {
                            value -= 20;
                        } else {
                            value -= 5;
                        }
                    }
                }
            }

            if (hasBonus()) {
                value -= 10;
            }

            if (isForested()) {
                value -= 5;
            }

            if (!nearbyTileHasForest) {
                value -= 30;
            }
            if (!nearbyTileIsOcean) {
                // TODO: Uncomment when wagon train code has been written:
                // value -= 20;
                value = 0;
            }

            return Math.max(0, value);
        }
    }

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Tile</code>.
     * 
     * @param attacker The target that would be attacking this tile.
     * @return The <code>Unit</code> that has been choosen to defend this
     *         tile.
     */
    public Unit getDefendingUnit(Unit attacker) {
        Iterator<Unit> unitIterator = getUnitIterator();
        Unit defender = null;
        while (unitIterator.hasNext()) {
            Unit nextUnit = unitIterator.next();

            if (this.isLand() != nextUnit.isNaval()
                    && (defender == null || nextUnit.getDefensePower(attacker) > defender.getDefensePower(attacker))) {
                defender = nextUnit;
            }
        }

        if (getSettlement() != null) {
            if (defender == null || defender.isColonist() && !defender.isArmed() && !defender.isMounted()) {
                return settlement.getDefendingUnit(attacker);
            }

            return defender;
        }

        return defender;
    }

    /**
     * Returns the cost of moving onto this tile from a given <code>Tile</code>.
     * 
     * <br>
     * <br>
     * 
     * This method does not take special unit behavior into account. Use
     * {@link Unit#getMoveCost} whenever it is possible.
     * 
     * @param fromTile The <code>Tile</code> the moving {@link Unit} comes
     *            from.
     * @return The cost of moving the unit.
     * @see Unit#getMoveCost
     */
    public int getMoveCost(Tile fromTile) {

        if (hasRoad() && fromTile.hasRoad()) {
            return 1;
        } else if (hasRiver() && fromTile.hasRiver()) {
            return 1;
        } else if (getSettlement() != null) {
            return 3;
        } else if (getAddition() == ADD_MOUNTAINS) {
            return 9;
        } else if (getAddition() == ADD_HILLS) {
            return 6;
        }

        TileType t = FreeCol.specification.tileType(type);
        return forested ? t.whenForested.basicMoveCost : t.basicMoveCost;
    }

    /**
     * Disposes all units on this <code>Tile</code>.
     */
    public void disposeAllUnits() {
        unitContainer.disposeAllUnits();
        updatePlayerExploredTiles();
    }

    /**
     * Gets the first <code>Unit</code> on this tile.
     * 
     * @return The first <code>Unit</code> on this tile.
     */
    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }

    /**
     * Gets the last <code>Unit</code> on this tile.
     * 
     * @return The last <code>Unit</code> on this tile.
     */
    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }

    /**
     * Returns the total amount of Units at this Location. This also includes
     * units in a carrier
     * 
     * @return The total amount of Units at this Location.
     */
    public int getTotalUnitCount() {
        return unitContainer.getTotalUnitCount();
    }

    /**
     * Checks if this <code>Tile</code> contains the specified
     * <code>Locatable</code>.
     * 
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><i>true</i> if the specified <code>Locatable</code> is
     *            on this <code>Tile</code> and
     *            <li><i>false</i> otherwise.
     *            </ul>
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        }

        logger.warning("Tile.contains(" + locatable + ") Not implemented yet!");

        return false;
    }

    /**
     * Gets the <code>Map</code> in which this <code>Tile</code> belongs.
     * 
     * @return The <code>Map</code>.
     */
    public Map getMap() {
        return getGame().getMap();
    }

    /**
     * Check if the tile has been explored.
     * 
     * @return true iff tile is known.
     */
    public boolean isExplored() {
        return getType() != UNEXPLORED;
    }

    /**
     * Returns 'true' if this Tile is a land Tile, 'false' otherwise.
     * 
     * @return 'true' if this Tile is a land Tile, 'false' otherwise.
     */
    public boolean isLand() {
        return (getType() != OCEAN) && (getType() != HIGH_SEAS);
    }

    /**
     * Returns 'true' if this Tile has a road.
     * 
     * @return 'true' if this Tile has a road.
     */
    public boolean hasRoad() {
        return road || (getSettlement() != null);
    }

    public boolean hasRiver() {

        return ADD_RIVER_MINOR == additionType || ADD_RIVER_MAJOR == additionType;
    }

    /**
     * Returns 'true' if this Tile has been plowed.
     * 
     * @return 'true' if this Tile has been plowed.
     */
    public boolean isPlowed() {
        return plowed;
    }

    /**
     * Returns 'true' if this Tile is forested.
     * 
     * @return 'true' if this Tile is forested.
     */
    public boolean isForested() {
        return forested;
    }

    /**
     * Returns 'true' if this Tile has a bonus (an extra resource) on it.
     * 
     * @return 'true' if this Tile has a bonus (an extra resource) on it.
     */
    public boolean hasBonus() {
        return bonus;
    }

    /**
     * Returns the type of this Tile. Returns UNKNOWN if the type of this Tile
     * is unknown.
     * 
     * @return The type of this Tile.
     */
    public int getType() {
        return type;
    }

    /**
     * The nation that consider this tile to be their property.
     * 
     * @return The nation or {@link Player#NO_NATION} is there is no nation
     *         owning this tile.
     */
    public int getNationOwner() {
        return nationOwner;
    }

    /**
     * Sets the nation that should consider this tile to be their property.
     * 
     * @param nationOwner The nation or {@link Player#NO_NATION} is there is no
     *            nation owning this tile.
     * @see #getNationOwner
     */
    public void setNationOwner(int nationOwner) {
        this.nationOwner = nationOwner;
        updatePlayerExploredTiles();
    }

    /**
     * Makes the given player take the ownership of this <code>Tile</code>.
     * The tension level is modified accordingly.
     * 
     * @param player The <code>Player</code>.
     */
    public void takeOwnership(Player player) {
        if (getNationOwner() != Player.NO_NATION && getNationOwner() != player.getNation()
                && !player.hasFather(FoundingFather.PETER_MINUIT)) {
            Player otherPlayer = getGame().getPlayer(getNationOwner());
            if (otherPlayer != null) {
                if (!otherPlayer.isEuropean()) {
                    otherPlayer.modifyTension(player, Tension.TENSION_ADD_LAND_TAKEN);
                }
            } else {
                logger.warning("Could not find player with nation: " + getNationOwner());
            }
        }
        setNationOwner(player.getNation());
        updatePlayerExploredTiles();
    }

    /**
     * Returns the addition on this Tile.
     * 
     * @return The addition on this Tile.
     */
    public int getAddition() {
        return additionType;
    }

    /**
     * Sets the addition on this Tile.
     * 
     * @param addition The addition on this Tile.
     */
    public void setAddition(int addition) {
        if (addition == ADD_HILLS || addition == ADD_MOUNTAINS) {
            setForested(false);
            river = 0;
        }
        
        if (!isLand() && addition > ADD_RIVER_MAJOR) {
            logger.warning("Setting addition to Ocean.");
            type = PLAINS;
        }
        
        additionType = addition;
        updatePlayerExploredTiles();
    }

    /**
     * Returns the type of river on this tile.
     * 
     * @return an <code>int</code> value
     */
    public int getRiver() {
        return this.river;
    }

    /**
     * Adds a river to this tile.
     * 
     * @param addition an <code>int</code> value
     * @param river an <code>int</code> value
     */
    public void addRiver(int addition, int river) {
        setAddition(addition);
        this.river = river;
    }

    public void updateRiver(int direction, int addition) {
        this.river = River.updateRiver(river, direction, addition);
    }

    public void addRiver(int addition) {
        if (addition == ADD_RIVER_MINOR ||
            addition == ADD_RIVER_MAJOR) {
            if (addition == getAddition()) {
                return;
            } else {
                setAddition(addition);
            }
        } else {
            return;
        }
        int[] base = {0, 1, 0, 3, 0, 9, 0, 27};
        int[] directions = {Map.NE, Map.SE, Map.SW, Map.NW};
        river = 0;
        for (int i = 0; i < directions.length; i++) {
            int branch = 0;
            Tile t = getMap().getNeighbourOrNull(directions[i], this);
            if (t.getAddition() == ADD_RIVER_MINOR) {
                branch = 1;
            } else if (t.getAddition() == ADD_RIVER_MAJOR) {
                branch = 2;
            } else {
                continue;
            }
            river += branch * base[directions[i]];
            int otherRiver = t.getRiver();
            int otherDirection = getMap().getOppositeDirection(directions[i]);
            int newRiver = River.updateRiver(otherRiver, otherDirection, addition);
            t.addRiver(branch, newRiver);
        }
    }


    /**
     * Return the number of land tiles adjacent to this one.
     * 
     * @return an <code>int</code> value
     */
    public int getLandCount() {
        if (landCount < 0) {
            landCount = 0;
            Iterator<Position> tileIterator = getMap().getAdjacentIterator(getPosition());
            while (tileIterator.hasNext()) {
                if (getMap().getTile(tileIterator.next()).isLand()) {
                    landCount++;
                }
            }
        }
        return landCount;
    }

    /**
     * Return the fish bonus of this tile. The fish bonus is zero if
     * this is a land tile. Otherwise it depends on the number of
     * adjacent land tiles and the rivers on these tiles (if any).
     * 
     * @return an <code>int</code> value
     */
    public int getFishBonus() {
        if (fishBonus < 0) {
            fishBonus = 0;
            if (!isLand()) {
                Iterator<Position> tileIterator = getMap().getAdjacentIterator(getPosition());
                while (tileIterator.hasNext()) {
                    Tile t = getMap().getTile(tileIterator.next());
                    if (t.isLand()) {
                        fishBonus++;
                    }
                    if (t.getAddition() == ADD_RIVER_MAJOR) {
                        fishBonus += 2;
                    } else if (t.getAddition() == ADD_RIVER_MINOR) {
                        fishBonus += 1;
                    }
                }
            }
        }
        return fishBonus;
    }

    /**
     * Returns the claim on this Tile.
     * 
     * @return The claim on this Tile.
     */
    public int getClaim() {
        return indianClaim;
    }

    /**
     * Sets the claim on this Tile.
     * 
     * @param claim The claim on this Tile.
     */
    public void setClaim(int claim) {
        indianClaim = claim;
        updatePlayerExploredTiles();
    }

    /**
     * Puts a <code>Settlement</code> on this <code>Tile</code>. A
     * <code>Tile</code> can only have one <code>Settlement</code> located
     * on it. The <code>Settlement</code> will also become the owner of this
     * <code>Tile</code>.
     * 
     * @param s The <code>Settlement</code> that shall be located on this
     *            <code>Tile</code>.
     * @see #getSettlement
     */
    public void setSettlement(Settlement s) {
        settlement = s;
        owner = s;
        setLostCityRumour(false);
        updatePlayerExploredTiles();
    }

    /**
     * Gets the <code>Settlement</code> located on this <code>Tile</code>.
     * 
     * @return The <code>Settlement</code> that is located on this
     *         <code>Tile</code> or <i>null</i> if no <code>Settlement</code>
     *         apply.
     * @see #setSettlement
     */
    public Settlement getSettlement() {
        return settlement;
    }

    /**
     * Gets the <code>Colony</code> located on this <code>Tile</code>. Only
     * a convenience method for {@link #getSettlement}.
     * 
     * @return The <code>Colony</code> that is located on this
     *         <code>Tile</code> or <i>null</i> if no <code>Colony</code>
     *         apply.
     * @see #getSettlement
     */
    public Colony getColony() {

        if (settlement != null && settlement instanceof Colony) {
            return ((Colony) settlement);
        }

        return null;
    }

    /**
     * Sets the owner of this tile. A <code>Settlement</code> become an owner
     * of a <code>Tile</code> when having workers placed on it.
     * 
     * @param owner The Settlement that owns this tile.
     * @see #getOwner
     */
    public void setOwner(Settlement owner) {
        this.owner = owner;
        updatePlayerExploredTiles();
    }

    /**
     * Gets the owner of this tile.
     * 
     * @return The Settlement that owns this tile.
     * @see #setOwner
     */
    public Settlement getOwner() {
        return owner;
    }

    /**
     * Sets whether the tile is forested or not.
     * 
     * @param value New value for forested.
     */
    public void setForested(boolean value) {
        forested = value;
        if (additionType == ADD_HILLS
                || additionType == ADD_MOUNTAINS) {
            additionType = ADD_NONE;
        }
        
        if (!isLand() && value) {
            logger.warning("Setting forested to Ocean.");
            type = PLAINS;
        }
        
        // bonus = false;
        updatePlayerExploredTiles();
    }

    /**
     * Sets whether the tile is plowed or not.
     * 
     * @param value New value.
     */
    public void setPlowed(boolean value) {
        plowed = value;
        updatePlayerExploredTiles();
    }

    /**
     * Sets whether the tile has a road or not.
     * 
     * @param value New value.
     */
    public void setRoad(boolean value) {
        road = value;
        updatePlayerExploredTiles();
    }

    /**
     * Sets whether the tile has a bonus or not.
     * 
     * @param value New value for bonus
     */
    public void setBonus(boolean value) {
        bonus = value;
        updatePlayerExploredTiles();
    }

    /**
     * Sets the type for this Tile.
     * 
     * @param t The new type for this Tile.
     */
    public void setType(int t) {
        if (t < UNEXPLORED || t > HIGH_SEAS)
            throw new IllegalStateException("Tile type must be valid");
        type = t;
        bonus = false;

        if (!isLand()) {
            settlement = null;
            road = false;
            plowed = false;
            forested = false;
            additionType = ADD_NONE;
        }
        
        if (type == ARCTIC) {
            forested = false;
        }
        
        updatePlayerExploredTiles();
    }

    /**
     * Returns the x-coordinate of this Tile.
     * 
     * @return The x-coordinate of this Tile.
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the y-coordinate of this Tile.
     * 
     * @return The y-coordinate of this Tile.
     */
    public int getY() {
        return y;
    }

    /**
     * Gets the <code>Position</code> of this <code>Tile</code>.
     * 
     * @return The <code>Position</code> of this <code>Tile</code>.
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Returns true if there is a lost city rumour on this tile.
     * 
     * @return True or false.
     */
    public boolean hasLostCityRumour() {
        return lostCityRumour;
    }

    /**
     * Sets the lost city rumour for this tile.
     * 
     * @param rumour If <code>true</code> then this <code>Tile</code> will
     *            have a lost city rumour. The type of rumour will be determined
     *            by the server.
     */
    public void setLostCityRumour(boolean rumour) {
        lostCityRumour = rumour;
        
        if (!isLand() && rumour) {
            logger.warning("Setting lost city rumour to Ocean.");
            type = PLAINS;
        }
        
        updatePlayerExploredTiles();
    }

    /**
     * Check if the tile type is suitable for a <code>Settlement</code>,
     * either by a <code>Colony</code> or an <code>IndianSettlement</code>.
     * What are unsuitable tile types are Arctic or water tiles.
     * 
     * @return true if tile suitable for settlement
     */
    public boolean isSettleable() {
        int type = getType();

        if (type == ARCTIC || type == OCEAN || type == HIGH_SEAS || getAddition() == ADD_MOUNTAINS) {
            return false;
        }

        return true;
    }

    /**
     * Check to see if this tile can be used to construct a new
     * <code>Colony</code>. If there is a colony here or in a tile next to
     * this one, it is unsuitable for colonization.
     * 
     * @return true if tile is suitable for colonization, false otherwise
     */
    public boolean isColonizeable() {
        if (!isSettleable()) {
            return false;
        }

        if (settlement != null) {
            return false;
        }

        for (int direction = Map.N; direction <= Map.NW; direction++) {
            Tile otherTile = getMap().getNeighbourOrNull(direction, this);
            if (otherTile != null) {
                Settlement set = otherTile.getSettlement();
                if ((set != null) && (set.getOwner().isEuropean())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets a <code>Unit</code> that can become active. This is preferably a
     * <code>Unit</code> not currently preforming any work.
     * 
     * @return A <code>Unit</code> with <code>movesLeft > 0</code> or
     *         <i>null</i> if no such <code>Unit</code> is located on this
     *         <code>Tile</code>.
     */
    public Unit getMovableUnit() {
        if (getFirstUnit() != null) {
            Iterator<Unit> unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit u = unitIterator.next();

                Iterator<Unit> childUnitIterator = u.getUnitIterator();
                while (childUnitIterator.hasNext()) {
                    Unit childUnit = childUnitIterator.next();

                    if ((childUnit.getMovesLeft() > 0) && (childUnit.getState() == Unit.ACTIVE)) {
                        return childUnit;
                    }
                }

                if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE)) {
                    return u;
                }
            }
        } else {
            return null;
        }

        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = unitIterator.next();

            Iterator<Unit> childUnitIterator = u.getUnitIterator();
            while (childUnitIterator.hasNext()) {
                Unit childUnit = childUnitIterator.next();

                if ((childUnit.getMovesLeft() > 0)) {
                    return childUnit;
                }
            }

            if (u.getMovesLeft() > 0) {
                return u;
            }
        }

        return null;
    }

    /**
     * Gets the <code>Tile</code> where this <code>Location</code> is
     * located or null if no <code>Tile</code> applies.
     * 
     * @return This <code>Tile</code>.
     */
    public Tile getTile() {
        return this;
    }

    /**
     * Adds a <code>Locatable</code> to this Location.
     * 
     * @param locatable The <code>Locatable</code> to add to this Location.
     */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.addUnit((Unit) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a tile.");
        }
        updatePlayerExploredTiles();
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     * 
     * @param locatable The <code>Locatable</code> to remove from this
     *            Location.
     */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.removeUnit((Unit) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a tile.");
        }
        updatePlayerExploredTiles();
    }

    /**
     * Returns the amount of units at this <code>Location</code>.
     * 
     * @return The amount of units at this <code>Location</code>.
     */
    public int getUnitCount() {
        /*
         * if (settlement != null) { return settlement.getUnitCount() +
         * unitContainer.getUnitCount(); }
         */
        return unitContainer.getUnitCount();
    }

    /**
     * Gets a
     * <code>List/code> of every <code>Unit</code> directly located on this
     * <code>Tile</code>. This does not include <code>Unit</code>s located in a
     * <code>Settlement</code> or on another <code>Unit</code> on this <code>Tile</code>.
     *
     * @return The <code>List</code>.
     */
    public List<Unit> getUnitList() {
        return unitContainer.getUnitsClone();
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Tile</code>. This does not include
     * <code>Unit</code>s located in a <code>Settlement</code> or on
     * another <code>Unit</code> on this <code>Tile</code>.
     * 
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return getUnitList().iterator();
    }

    /**
     * Gets a clone of the UnitContainer's <code>units</code> array.
     * 
     * @return The clone.
     */
    public ArrayList<Unit> getUnitsClone() {
        return unitContainer.getUnitsClone();
    }

    /**
     * Checks wether or not the specified locatable may be added to this
     * <code>Location</code>.
     * 
     * @param locatable The <code>Locatable</code> to test the addabillity of.
     * @return <i>true</i>.
     */
    public boolean canAdd(Locatable locatable) {
        return true;
    }

    /**
     * The potential of this tile to produce a certain type of goods.
     * 
     * @param goods The type of goods to check the potential for.
     * @return The normal potential of this tile to produce that amount of
     *         goods.
     */
    public int potential(int goods) {
        return getTileTypePotential(getType(), goods, getAddition(), getFishBonus(), 
                                    hasBonus(), isForested(), isPlowed(), hasRoad());
    }

    /**
     * Gets the maximum potential for producing the given type of goods. The
     * maximum potential is the potential of a tile after the tile has been
     * plowed/built road on.
     * 
     * @param goodsType The type of goods.
     * @return The maximum potential.
     */
    public int getMaximumPotential(int goodsType) {
        if (potentialtable[getType()][goodsType][0] < 
            potentialtable[getType()][goodsType][1]) {
            return getTileTypePotential(getType(), goodsType, getAddition(), 0,
                                        hasBonus(), isForested(), false, true);
        } else {
            return getTileTypePotential(getType(), goodsType, getAddition(), getFishBonus(), 
                                        hasBonus(), false, true, true);
        }
    }

    /**
     * Checks wether this <code>Tile</code> can be plowed or not. This method
     * will return <code>false</code> if the tile has already been plowed.
     * 
     * @return The result.
     * @see Unit#canPlow()
     */
    public boolean canBePlowed() {
        return (!isPlowed() && isLand() && getAddition() != Tile.ADD_HILLS && 
                getAddition() != Tile.ADD_MOUNTAINS && getType() != Tile.ARCTIC);
    }

    /**
     * Checks wether this <code>Tile</code> can have a road or not. This
     * method will return <code>false</code> if a road has already been built.
     * 
     * @return The result.
     */
    public boolean canGetRoad() {
        return isLand() && !hasRoad();
    }

    /**
     * Return the type of bonus a certain tile type can have. TODO:
     * put all this in TileType.
     *
     * @param type The tile type.
     * @param addition The addition.
     * @param forested Whether the tile is forested.
     * @return the type of bonus.
     */
    public static int getBonusType(int type, int addition, boolean forested) {
        if (addition == Tile.ADD_MOUNTAINS) {
            return Goods.SILVER;
        } else if (addition == Tile.ADD_HILLS) {
            return Goods.ORE;
        } else if (forested) {
            if (type == Tile.GRASSLANDS || type == Tile.SAVANNAH) {
                return Goods.LUMBER;
            } else {
                return Goods.FURS;
            }
        } else {
            switch(type) {
                case Tile.UNEXPLORED:
                    return -1;
                case Tile.PLAINS:
                    return Goods.FOOD;
                case Tile.GRASSLANDS:
                    return Goods.TOBACCO;
                case Tile.PRAIRIE:
                    return Goods.COTTON;
                case Tile.SAVANNAH:
                    return Goods.SUGAR;
                case Tile.MARSH:
                    return Goods.ORE;
                case Tile.SWAMP:
                    return Goods.SILVER;
                case Tile.DESERT:
                    return Goods.FOOD;
                case Tile.TUNDRA:
                    return Goods.ORE;                    
                case Tile.ARCTIC:
                    return -1;
                case Tile.OCEAN:
                    return Goods.FOOD;
                case Tile.HIGH_SEAS:
                    return -1;
                default:
                    // Should never happen
                    throw new IllegalArgumentException("Unknown tile type " + type + " for getBonusType!");
            }
        }
    }

    /**
     * The potential of a given type of tile to produce a certain type of goods.
     * 
     * @param tileType The type of tile
     * @param goods The type of goods to check the potential for.
     * @param additionType The type of addition (mountains, hills etc).
     * @param bonus Should be <code>true</code> to indicate that a bonus is
     *            present.
     * @param forested <code>true</code> to indicate a forest.
     * @param plowed <code>true</code> to indicate that it is plowed.
     * @param road <code>true</code> to indicate a road
     * @return The amount of goods.
     */
    public static int getTileTypePotential(int tileType, int goods, int additionType, int fishBonus, 
                                           boolean bonus, boolean forested, boolean plowed, boolean road) {

        GoodsType goodsType = FreeCol.specification.goodsType(goods);

        if (!goodsType.isFarmed()) {
            return 0;
        }


        int basepotential = 0;
        switch (additionType) {
        case ADD_HILLS:
            basepotential = potentialtable[12][goods][0];
            break;
        case ADD_MOUNTAINS:
            basepotential = potentialtable[13][goods][0];
            break;
        default:
            if (tileType == OCEAN && goods == Goods.FOOD) {
                basepotential = potentialtable[tileType][goods][0] + fishBonus;
            } else {
                basepotential = potentialtable[tileType][goods][(forested ? 1 : 0)];
            }
            break;
        }

        if (basepotential > 0) {
            if (goodsType.isImprovedByPlowing() && plowed) {
                basepotential++;
            } 
            if (goodsType.isImprovedByRoad() && road) {
                basepotential++;
            }
            if (goodsType.isImprovedByRiver()) {
                if (additionType == ADD_RIVER_MAJOR) {
                    basepotential += 2;
                } else if (additionType == ADD_RIVER_MINOR) {
                    basepotential += 1;
                }
            }
        }

        if (bonus && goods == getBonusType(tileType, additionType, forested)) {
            switch (goods) {
            case Goods.LUMBER:
            case Goods.FURS:
            case Goods.TOBACCO:
            case Goods.COTTON:
                basepotential += 6;
                break;
            case Goods.FOOD:
                basepotential += 4;
                break;
            case Goods.SUGAR:
                basepotential += 7;
                break;
            case Goods.SILVER:
            case Goods.ORE:
                basepotential += 2;
                break;
            }
        }

        return basepotential;
    }

    /**
     * The type of secondary good this tile produces best (used for Town Commons
     * squares).
     * 
     * @return The type of secondary good best produced by this tile.
     */
    public int secondaryGoods() {
        if (isForested())
            return Goods.FURS;
        if (getAddition() >= ADD_HILLS)
            return Goods.ORE;
        switch (getType()) {
        case PLAINS:
        case PRAIRIE:
        case DESERT:
            return Goods.COTTON;
        case SWAMP:
        case GRASSLANDS:
            return Goods.TOBACCO;
        case SAVANNAH:
            return Goods.SUGAR;
        case MARSH:
            return Goods.FURS;
        case TUNDRA:
        case ARCTIC:
        default:
            return Goods.ORE;
        }
    }

    /**
     * The defense/ambush bonus of this tile.
     * <p>
     * Note that the defense bonus is relative to the unit base strength,
     * not to the cumulative strength.
     * 
     * @return The defense modifier (in percent) of this tile.
     */
    public int defenseBonus() {

        if (additionType == ADD_HILLS) {
            return 100;
        } else if (additionType == ADD_MOUNTAINS) {
            return 150;
        }

        TileType t = FreeCol.specification.tileType(type);
        return forested ? t.whenForested.defenceBonus : t.defenceBonus;
    }


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        if (toSavedGame && !showAll) {
            logger.warning("toSavedGame is true, but showAll is false");
        }

        PlayerExploredTile pet = null;
        if (!(showAll)) {
            // We're sending the Tile from the server to the client and showAll
            // is false.
            if (player != null) {
                if (playerExploredTiles[player.getNation()] != null) {
                    pet = playerExploredTiles[player.getNation()];
                }
            } else {
                logger.warning("player == null");
            }
        }

        out.writeAttribute("ID", getID());
        out.writeAttribute("x", Integer.toString(x));
        out.writeAttribute("y", Integer.toString(y));
        out.writeAttribute("type", Integer.toString(type));

        if (river != 0) {
            out.writeAttribute("river", Integer.toString(river));
        }

        if (additionType != ADD_NONE) {
            out.writeAttribute("addition", Integer.toString(additionType));
        }

        final String[] names = new String[] { "road", "plowed", "forested", "bonus", "lostCityRumour" };
        boolean[] values;
        if (pet == null) {
            values = new boolean[] { road, plowed, forested, bonus, lostCityRumour };
        } else {
            values = new boolean[] { pet.hasRoad(), pet.isPlowed(), pet.isForested(), pet.hasBonus(),
                    pet.hasLostCityRumour() };
        }
        for (int i = 0; i < names.length; i++) {
            if (values[i]) {
                out.writeAttribute(names[i], Boolean.toString(values[i]));
            }
        }

        if (nationOwner != Player.NO_NATION) {
            if (getGame().isClientTrusted() || showAll || player.canSee(this)) {
                out.writeAttribute("nationOwner", Integer.toString(nationOwner));
            } else if (pet != null) {
                out.writeAttribute("nationOwner", Integer.toString(pet.getNationOwner()));
            }
        }

        if ((getGame().isClientTrusted() || showAll || player.canSee(this)) && (owner != null)) {
            out.writeAttribute("owner", owner.getID());
        }

        // if ((settlement != null) && (showAll || player.canSee(this))) {
        if (settlement != null) {
            if (pet == null || getGame().isClientTrusted() || showAll || settlement.getOwner() == player) {
                settlement.toXML(out, player, showAll, toSavedGame);
            } else {
                if (getColony() != null) {
                    if (!player.canSee(getTile())) {
                        if (pet.getColonyUnitCount() != 0) {
                            out.writeStartElement(Colony.getXMLElementTagName());
                            out.writeAttribute("ID", getColony().getID());
                            out.writeAttribute("name", getColony().getName());
                            out.writeAttribute("owner", getColony().getOwner().getID());
                            out.writeAttribute("tile", getID());
                            out.writeAttribute("unitCount", Integer.toString(pet.getColonyUnitCount()));

                            Building b = getColony().getBuilding(Building.STOCKADE);
                            out.writeStartElement(Building.getXMLElementTagName());
                            out.writeAttribute("ID", b.getID());
                            out.writeAttribute("level", Integer.toString(pet.getColonyStockadeLevel()));
                            out.writeAttribute("colony", getColony().getID());
                            out.writeAttribute("type", Integer.toString(Building.STOCKADE));
                            out.writeEndElement();

                            GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), getColony());
                            emptyGoodsContainer.setFakeID(getColony().getGoodsContainer().getID());
                            emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);

                            out.writeEndElement();
                        } // Else: Colony not discovered.
                    } else {
                        settlement.toXML(out, player, showAll, toSavedGame);
                    }
                } else if (getSettlement() instanceof IndianSettlement) {
                    final IndianSettlement is = (IndianSettlement) getSettlement();

                    out.writeStartElement(IndianSettlement.getXMLElementTagName());
                    out.writeAttribute("ID", getSettlement().getID());
                    out.writeAttribute("tile", getID());
                    out.writeAttribute("owner", getSettlement().getOwner().getID());
                    out.writeAttribute("tribe", Integer.toString(is.getTribe()));
                    out.writeAttribute("kind", Integer.toString(is.getKind()));
                    out.writeAttribute("isCapital", Boolean.toString(is.isCapital()));
                    out.writeAttribute("learnableSkill", Integer.toString(pet.getSkill()));
                    out.writeAttribute("highlyWantedGoods", Integer.toString(pet.getHighlyWantedGoods()));
                    out.writeAttribute("wantedGoods1", Integer.toString(pet.getWantedGoods1()));
                    out.writeAttribute("wantedGoods2", Integer.toString(pet.getWantedGoods2()));

                    int[] tensionArray = new int[Player.NUMBER_OF_NATIONS];
                    for (int i = 0; i < tensionArray.length; i++) {
                        tensionArray[i] = is.getAlarm(i).getValue();
                    }
                    toArrayElement("alarm", tensionArray, out);

                    if (pet.getMissionary() != null) {
                        out.writeStartElement("missionary");
                        pet.getMissionary().toXML(out, player, false, false);
                        out.writeEndElement();
                    }

                    UnitContainer emptyUnitContainer = new UnitContainer(getGame(), getSettlement());
                    emptyUnitContainer.setFakeID(is.getUnitContainer().getID());
                    emptyUnitContainer.toXML(out, player, showAll, toSavedGame);

                    GoodsContainer emptyGoodsContainer = new GoodsContainer(getGame(), is);
                    emptyGoodsContainer.setFakeID(is.getGoodsContainer().getID());
                    emptyGoodsContainer.toXML(out, player, showAll, toSavedGame);

                    out.writeEndElement();
                } else {
                    logger.warning("Unknown type of settlement: " + getSettlement());
                }
            }
        }

        // Check if the player can see the tile: Do not show enemy units on a
        // tile out-of-sight.
        if (getGame().isClientTrusted() || showAll
                || (player.canSee(this) && (settlement == null || settlement.getOwner() == player))
                || !getGameOptions().getBoolean(GameOptions.UNIT_HIDING) && player.canSee(this)) {
            unitContainer.toXML(out, player, showAll, toSavedGame);
        } else {
            UnitContainer emptyUnitContainer = new UnitContainer(getGame(), this);
            emptyUnitContainer.setFakeID(unitContainer.getID());
            emptyUnitContainer.toXML(out, player, showAll, toSavedGame);
        }

        if (toSavedGame) {
            for (int i = 0; i < playerExploredTiles.length; i++) {
                if (playerExploredTiles[i] != null && playerExploredTiles[i].isExplored()) {
                    playerExploredTiles[i].toXML(out, player, showAll, toSavedGame);
                }
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));

        x = Integer.parseInt(in.getAttributeValue(null, "x"));
        y = Integer.parseInt(in.getAttributeValue(null, "y"));
        position = new Position(x, y);
        type = Integer.parseInt(in.getAttributeValue(null, "type"));

        final String riverStr = in.getAttributeValue(null, "river");
        if (riverStr != null) {
            river = Integer.parseInt(riverStr);
        } else {
            river = 0;
        }

        final String additionStr = in.getAttributeValue(null, "addition");
        if (additionStr != null) {
            additionType = Integer.parseInt(additionStr);
        } else {
            additionType = ADD_NONE;
        }

        final String roadStr = in.getAttributeValue(null, "road");
        if (roadStr != null) {
            road = Boolean.valueOf(roadStr).booleanValue();
        } else {
            road = false;
        }

        final String plowedStr = in.getAttributeValue(null, "plowed");
        if (plowedStr != null) {
            plowed = Boolean.valueOf(plowedStr).booleanValue();
        } else {
            plowed = false;
        }

        final String forestedStr = in.getAttributeValue(null, "forested");
        if (forestedStr != null) {
            forested = Boolean.valueOf(forestedStr).booleanValue();
        } else {
            forested = false;
        }

        final String bonusStr = in.getAttributeValue(null, "bonus");
        if (bonusStr != null) {
            bonus = Boolean.valueOf(bonusStr).booleanValue();
        } else {
            bonus = false;
        }

        final String lostCityRumourStr = in.getAttributeValue(null, "lostCityRumour");
        if (lostCityRumourStr != null) {
            lostCityRumour = Boolean.valueOf(lostCityRumourStr).booleanValue();
        } else {
            lostCityRumour = false;
        }

        final String nationOwnerStr = in.getAttributeValue(null, "nationOwner");
        if (nationOwnerStr != null) {
            nationOwner = Integer.parseInt(nationOwnerStr);
        } else {
            nationOwner = Player.NO_NATION;
        }

        final String ownerStr = in.getAttributeValue(null, "owner");
        if (ownerStr != null) {
            owner = (Settlement) getGame().getFreeColGameObject(ownerStr);
            if (owner == null) {
                if (ownerStr.startsWith(IndianSettlement.getXMLElementTagName())) {
                    owner = new IndianSettlement(getGame(), ownerStr);
                } else if (ownerStr.startsWith(Colony.getXMLElementTagName())) {
                    owner = new Colony(getGame(), ownerStr);
                } else {
                    logger.warning("Unknown type of Settlement.");
                }
            }
        } else {
            owner = null;
        }

        boolean settlementSent = false;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Colony.getXMLElementTagName())) {
                settlement = (Settlement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (settlement != null) {
                    settlement.readFromXML(in);
                } else {
                    settlement = new Colony(getGame(), in);
                }
                settlementSent = true;
            } else if (in.getLocalName().equals(IndianSettlement.getXMLElementTagName())) {
                settlement = (Settlement) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (settlement != null) {
                    settlement.readFromXML(in);
                } else {
                    settlement = new IndianSettlement(getGame(), in);
                }
                settlementSent = true;
            } else if (in.getLocalName().equals(UnitContainer.getXMLElementTagName())) {
                unitContainer = (UnitContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (unitContainer != null) {
                    unitContainer.readFromXML(in);
                } else {
                    unitContainer = new UnitContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals("playerExploredTile")) {
                // Only from a savegame:
                if (playerExploredTiles[Integer.parseInt(in.getAttributeValue(null, "nation"))] == null) {
                    PlayerExploredTile pet = new PlayerExploredTile(in);
                    playerExploredTiles[pet.getNation()] = pet;
                } else {
                    playerExploredTiles[Integer.parseInt(in.getAttributeValue(null, "nation"))].readFromXML(in);
                }
            }
        }
        if (!settlementSent && settlement != null) {
            settlement.dispose();
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return "tile".
     */
    public static String getXMLElementTagName() {
        return "tile";
    }

    /**
     * Gets the <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    private PlayerExploredTile getPlayerExploredTile(Player player) {
        if (playerExploredTiles == null) {
            return null;
        }
        return playerExploredTiles[player.getNation()];
    }

    /**
     * Creates a <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    private void createPlayerExploredTile(Player player) {
        playerExploredTiles[player.getNation()] = new PlayerExploredTile(player.getNation());
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     */
    public void updatePlayerExploredTile(Player player) {
        updatePlayerExploredTile(player.getNation());
    }

    /**
     * Updates the <code>PlayerExploredTile</code> for each player. This
     * update will only be performed if the player
     * {@link Player#canSee(Tile) can see} this <code>Tile</code>.
     */
    public void updatePlayerExploredTiles() {
        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        Iterator<Player> it = getGame().getPlayerIterator();
        while (it.hasNext()) {
            Player p = it.next();
            if (playerExploredTiles[p.getNation()] == null && !p.isEuropean()) {
                continue;
            }
            if (p.canSee(this)) {
                updatePlayerExploredTile(p);
            }
        }
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     * 
     * @param nation The {@link Player#getNation nation} identifying the
     *            <code>Player</code>.
     */
    public void updatePlayerExploredTile(int nation) {
        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        if (playerExploredTiles[nation] == null && !Player.isEuropean(nation)) {
            return;
        }
        if (playerExploredTiles[nation] == null) {
            logger.warning("'playerExploredTiles' for " + Player.getNationAsString(nation) + " is 'null'.");
            throw new IllegalStateException("'playerExploredTiles' for " + Player.getNationAsString(nation)
                    + " is 'null'. " + getGame().getPlayer(nation).canSee(this) + ", "
                    + isExploredBy(getGame().getPlayer(nation)) + " ::: " + getPosition());
        }

        playerExploredTiles[nation].setRoad(road);
        playerExploredTiles[nation].setPlowed(plowed);
        playerExploredTiles[nation].setForested(forested);
        playerExploredTiles[nation].setBonus(bonus);
        playerExploredTiles[nation].setLostCityRumour(lostCityRumour);
        playerExploredTiles[nation].setNationOwner(nationOwner);

        if (getColony() != null) {
            playerExploredTiles[nation].setColonyUnitCount(getSettlement().getUnitCount());
            playerExploredTiles[nation].setColonyStockadeLevel(getColony().getBuilding(Building.STOCKADE).getLevel());
        } else if (getSettlement() != null) {
            playerExploredTiles[nation].setMissionary(((IndianSettlement) getSettlement()).getMissionary());

            /*
             * These attributes should not be updated by this method: skill,
             * highlyWantedGoods, wantedGoods1 and wantedGoods2
             */
        } else {
            playerExploredTiles[nation].setColonyUnitCount(0);
        }
    }

    /**
     * Checks if this <code>Tile</code> has been explored by the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @return <code>true</code> if this <code>Tile</code> has been explored
     *         by the given <code>Player</code> and <code>false</code>
     *         otherwise.
     */
    public boolean isExploredBy(Player player) {
        if (player.isIndian()) {
            return true;
        }
        if (playerExploredTiles[player.getNation()] == null || !isExplored()) {
            return false;
        }

        return getPlayerExploredTile(player).isExplored();
    }

    /**
     * Sets this <code>Tile</code> to be explored by the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @param explored <code>true</code> if this <code>Tile</code> should be
     *            explored by the given <code>Player</code> and
     *            <code>false</code> otherwise.
     */
    public void setExploredBy(Player player, boolean explored) {
        if (player.isIndian()) {
            return;
        }
        if (playerExploredTiles[player.getNation()] == null) {
            createPlayerExploredTile(player);
        }
        getPlayerExploredTile(player).setExplored(explored);
        updatePlayerExploredTile(player);
    }

    /**
     * Updates the skill available from the <code>IndianSettlement</code>
     * located on this <code>Tile</code>.
     * <p>
     * 
     * @param player The <code>Player</code> which should get the updated
     *            information.
     * @exception NullPointerException If there is no settlement on this
     *                <code>Tile</code>.
     * @exception ClassCastException If the <code>Settlement</code> on this
     *                <code>Tile</code> is not an
     *                <code>IndianSettlement</code>.
     * @see IndianSettlement
     */
    public void updateIndianSettlementSkill(Player player) {
        IndianSettlement is = (IndianSettlement) getSettlement();
        getPlayerExploredTile(player).setSkill(is.getLearnableSkill());
    }

    /**
     * Updates the information about the <code>IndianSettlement</code> located
     * on this <code>Tile</code>.
     * <p>
     * 
     * @param player The <code>Player</code> which should get the updated
     *            information.
     * @exception NullPointerException If there is no settlement on this
     *                <code>Tile</code>.
     * @exception ClassCastException If the <code>Settlement</code> on this
     *                <code>Tile</code> is not an
     *                <code>IndianSettlement</code>.
     * @see IndianSettlement
     */
    public void updateIndianSettlementInformation(Player player) {
        if (player.isIndian()) {
            return;
        }
        PlayerExploredTile playerExploredTile = getPlayerExploredTile(player);
        IndianSettlement is = (IndianSettlement) getSettlement();
        playerExploredTile.setSkill(is.getLearnableSkill());
        playerExploredTile.setHighlyWantedGoods(is.getHighlyWantedGoods());
        playerExploredTile.setWantedGoods1(is.getWantedGoods1());
        playerExploredTile.setWantedGoods2(is.getWantedGoods2());
    }


    private final Tile theTile = this;


    /**
     * This class contains the data visible to a specific player.
     * 
     * <br>
     * <br>
     * 
     * Sometimes a tile contains information that should not be given to a
     * player. For instance; a settlement that was built after the player last
     * viewed the tile.
     * 
     * <br>
     * <br>
     * 
     * The <code>toXMLElement</code> of {@link Tile} uses information from
     * this class to hide information that is not available.
     */
    public class PlayerExploredTile {

        private int nation;

        private boolean explored = false;

        // Tile data:
        private boolean road, plowed, forested, bonus;

        private int nationOwner;

        // Colony data:
        private int colonyUnitCount = 0, colonyStockadeLevel;

        // IndianSettlement data:
        private int skill = IndianSettlement.UNKNOWN, highlyWantedGoods = IndianSettlement.UNKNOWN,
                wantedGoods1 = IndianSettlement.UNKNOWN, wantedGoods2 = IndianSettlement.UNKNOWN;

        private Unit missionary = null;

        // private Settlement settlement;

        private boolean lostCityRumour;


        /**
         * Creates a new <code>PlayerExploredTile</code>.
         * 
         * @param nation The nation.
         */
        public PlayerExploredTile(int nation) {
            this.nation = nation;
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * 
         * @param in The XML stream to read the data from.
         * @throws XMLStreamException if an error occured during parsing.
         */
        public PlayerExploredTile(XMLStreamReader in) throws XMLStreamException {
            readFromXML(in);
        }

        public void setColonyUnitCount(int colonyUnitCount) {
            this.colonyUnitCount = colonyUnitCount;
        }

        public int getColonyUnitCount() {
            return colonyUnitCount;
        }

        public void setColonyStockadeLevel(int colonyStockadeLevel) {
            this.colonyStockadeLevel = colonyStockadeLevel;
        }

        public int getColonyStockadeLevel() {
            return colonyStockadeLevel;
        }

        public void setRoad(boolean road) {
            this.road = road;
        }

        public boolean hasRoad() {
            return road;
        }

        public void setPlowed(boolean plowed) {
            this.plowed = plowed;
        }

        public boolean isPlowed() {
            return plowed;
        }

        public void setForested(boolean forested) {
            this.forested = forested;
        }

        public boolean isForested() {
            return forested;
        }

        public void setBonus(boolean bonus) {
            this.bonus = bonus;
        }

        public boolean hasBonus() {
            return bonus;
        }

        public void setLostCityRumour(boolean lostCityRumour) {
            this.lostCityRumour = lostCityRumour;
        }

        public boolean hasLostCityRumour() {
            return lostCityRumour;
        }

        public void setExplored(boolean explored) {
            this.explored = explored;
        }

        public void setSkill(int newSkill) {
            this.skill = newSkill;
        }

        public int getSkill() {
            return skill;
        }

        public void setNationOwner(int nationOwner) {
            this.nationOwner = nationOwner;
        }

        public int getNationOwner() {
            return nationOwner;
        }

        public void setHighlyWantedGoods(int highlyWantedGoods) {
            this.highlyWantedGoods = highlyWantedGoods;
        }

        public int getHighlyWantedGoods() {
            return highlyWantedGoods;
        }

        public void setWantedGoods1(int wantedGoods1) {
            this.wantedGoods1 = wantedGoods1;
        }

        public int getWantedGoods1() {
            return wantedGoods1;
        }

        public void setWantedGoods2(int wantedGoods2) {
            this.wantedGoods2 = wantedGoods2;
        }

        public int getWantedGoods2() {
            return wantedGoods2;
        }

        public void setMissionary(Unit missionary) {
            this.missionary = missionary;
        }

        public Unit getMissionary() {
            return missionary;
        }

        /**
         * Checks if this <code>Tile</code> has been explored.
         * 
         * @return <i>true</i> if the tile has been explored.
         */
        public boolean isExplored() {
            return explored;
        }

        /**
         * Gets the nation owning this object.
         * 
         * @return The nation of this <code>PlayerExploredTile</code>.
         */
        public int getNation() {
            return nation;
        }

        /**
         * This method writes an XML-representation of this object to the given
         * stream.
         * 
         * <br>
         * <br>
         * 
         * Only attributes visible to the given <code>Player</code> will be
         * added to that representation if <code>showAll</code> is set to
         * <code>false</code>.
         * 
         * @param out The target stream.
         * @param player The <code>Player</code> this XML-representation
         *            should be made for, or <code>null</code> if
         *            <code>showAll == true</code>.
         * @param showAll Only attributes visible to <code>player</code> will
         *            be added to the representation if <code>showAll</code>
         *            is set to <i>false</i>.
         * @param toSavedGame If <code>true</code> then information that is
         *            only needed when saving a game is added.
         * @throws XMLStreamException if there are any problems writing to the
         *             stream.
         */
        public void toXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
                throws XMLStreamException {
            // Start element:
            out.writeStartElement("playerExploredTile");

            out.writeAttribute("nation", Integer.toString(nation));

            if (!explored) {
                out.writeAttribute("explored", Boolean.toString(explored));
            }
            if (theTile.hasRoad() != road) {
                out.writeAttribute("road", Boolean.toString(road));
            }
            if (theTile.isPlowed() != plowed) {
                out.writeAttribute("plowed", Boolean.toString(plowed));
            }
            if (theTile.isForested() != forested) {
                out.writeAttribute("forested", Boolean.toString(forested));
            }
            if (theTile.hasBonus() != bonus) {
                out.writeAttribute("bonus", Boolean.toString(bonus));
            }
            if (theTile.hasLostCityRumour()) {
                out.writeAttribute("lostCityRumour", Boolean.toString(lostCityRumour));
            }
            if (theTile.getNationOwner() != nationOwner) {
                out.writeAttribute("nationOwner", Integer.toString(nationOwner));
            }
            if (colonyUnitCount != 0) {
                out.writeAttribute("colonyUnitCount", Integer.toString(colonyUnitCount));
                out.writeAttribute("colonyStockadeLevel", Integer.toString(colonyStockadeLevel));
            }
            if (skill != IndianSettlement.UNKNOWN) {
                out.writeAttribute("learnableSkill", Integer.toString(skill));
            }
            if (highlyWantedGoods != IndianSettlement.UNKNOWN) {
                out.writeAttribute("highlyWantedGoods", Integer.toString(highlyWantedGoods));
                out.writeAttribute("wantedGoods1", Integer.toString(wantedGoods1));
                out.writeAttribute("wantedGoods2", Integer.toString(wantedGoods2));
            }
            if (missionary != null) {
                out.writeStartElement("missionary");
                missionary.toXML(out, player, false, false);
                out.writeEndElement();
            }
            out.writeEndElement();
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * 
         * @param in The input stream with the XML.
         * @throws XMLStreamException if an error occured during parsing.
         */
        public void readFromXML(XMLStreamReader in) throws XMLStreamException {
            nation = Integer.parseInt(in.getAttributeValue(null, "nation"));

            final String exploredStr = in.getAttributeValue(null, "explored");
            if (exploredStr != null) {
                explored = Boolean.valueOf(exploredStr).booleanValue();
            } else {
                explored = true;
            }

            final String roadStr = in.getAttributeValue(null, "road");
            if (roadStr != null) {
                road = Boolean.valueOf(roadStr).booleanValue();
            } else {
                road = theTile.hasRoad();
            }

            final String plowedStr = in.getAttributeValue(null, "plowed");
            if (plowedStr != null) {
                plowed = Boolean.valueOf(plowedStr).booleanValue();
            } else {
                plowed = theTile.isPlowed();
            }

            final String forestedStr = in.getAttributeValue(null, "forested");
            if (forestedStr != null) {
                forested = Boolean.valueOf(forestedStr).booleanValue();
            } else {
                forested = theTile.isForested();
            }

            final String bonusStr = in.getAttributeValue(null, "bonus");
            if (bonusStr != null) {
                bonus = Boolean.valueOf(bonusStr).booleanValue();
            } else {
                bonus = theTile.hasBonus();
            }

            final String lostCityRumourStr = in.getAttributeValue(null, "lostCityRumour");
            if (lostCityRumourStr != null) {
                lostCityRumour = Boolean.valueOf(lostCityRumourStr).booleanValue();
            } else {
                lostCityRumour = theTile.hasLostCityRumour();
            }

            final String nationOwnerStr = in.getAttributeValue(null, "nationOwner");
            if (nationOwnerStr != null) {
                nationOwner = Integer.parseInt(nationOwnerStr);
            } else {
                nationOwner = theTile.getNationOwner();
            }

            final String colonyUnitCountStr = in.getAttributeValue(null, "colonyUnitCount");
            if (colonyUnitCountStr != null) {
                colonyUnitCount = Integer.parseInt(colonyUnitCountStr);
                colonyStockadeLevel = Integer.parseInt(in.getAttributeValue(null, "colonyStockadeLevel"));
            } else {
                colonyUnitCount = 0;
            }

            final String learnableSkillStr = in.getAttributeValue(null, "learnableSkill");
            if (learnableSkillStr != null) {
                skill = Integer.parseInt(learnableSkillStr);
            } else {
                skill = IndianSettlement.UNKNOWN;
            }

            final String highlyWantedGoodsStr = in.getAttributeValue(null, "highlyWantedGoods");
            if (highlyWantedGoodsStr != null) {
                highlyWantedGoods = Integer.parseInt(highlyWantedGoodsStr);
                wantedGoods1 = Integer.parseInt(in.getAttributeValue(null, "wantedGoods1"));
                wantedGoods2 = Integer.parseInt(in.getAttributeValue(null, "wantedGoods2"));
            } else {
                highlyWantedGoods = IndianSettlement.UNKNOWN;
                wantedGoods1 = IndianSettlement.UNKNOWN;
                wantedGoods2 = IndianSettlement.UNKNOWN;
            }

            in.nextTag(); // <missionary> | </playerExploredTile>
            if (in.getLocalName().equals("missionary")) {
                in.nextTag();
                missionary = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (missionary == null) {
                    missionary = new Unit(getGame(), in);
                } else {
                    missionary.readFromXML(in);
                }
                in.nextTag(); // </missionary>
                in.nextTag(); // </playerExploredTile>
            } else {
                missionary = null;
            }
        }

        /**
         * Returns the tag name of the root element representing this object.
         * 
         * @return "playerExploredTile".
         */
        /*
         * public static String getXMLElementTagName() { return
         * "playerExploredTile"; }
         */
    }

    /**
     * Returns the number of turns it takes for a non-expert pioneer to either
     * PLOW or BUILD_ROAD on the current tile.
     * 
     * This function was extracted by playing the original game and writing down
     * how many turns it took for a regular pioneer to finish.
     * 
     * @param workType either Unit.PLOW or Unit.BUILD_ROAD. This function
     *            expects a valid workType and does not check it for validity.
     * 
     * @return The number of turns it should take a non-expert pioneer to finish
     *         the work.
     */
    public int getWorkAmount(int workType) {

        if (getTile().getAddition() == Tile.ADD_HILLS) {
            return 4;
        }

        if (getTile().getAddition() == Tile.ADD_MOUNTAINS) {
            return 7;
        }

        int workAmount;
        switch (getType()) {
        case Tile.SAVANNAH:
            workAmount = isForested() ? 8 : 5;
            break;
        case Tile.DESERT:
        case Tile.PLAINS:
        case Tile.PRAIRIE:
        case Tile.GRASSLANDS:
            workAmount = isForested() ? 6 : 5;
            break;
        case Tile.MARSH:
            workAmount = isForested() ? 8 : 7;
            break;
        case Tile.SWAMP:
            workAmount = 9;
            break;
        case Tile.ARCTIC:
        case Tile.TUNDRA:
            workAmount = 6;
            break;
        default:
            throw new IllegalArgumentException("Unknown Tile Type: " + getType());
        }

        if (workType == Unit.BUILD_ROAD) {
            return workAmount - 2;
        } else {
            return workAmount;
        }
    }
}
