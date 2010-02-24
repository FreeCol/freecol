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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.Map.CircleIterator;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;

import org.w3c.dom.Element;

/**
 * Represents a single tile on the <code>Map</code>.
 * 
 * @see Map
 */
public final class Tile extends FreeColGameObject implements Location, Named, Ownable {

    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    private static final String UNITS_TAG_NAME = "units";

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    private TileType type;
    
    private int x, y;

    /** The player that consider this tile to be their land. */
    private Player owner;

    /**
     * A pointer to the settlement located on this tile or 'null' if there is no
     * settlement on this tile.
     */
    private Settlement settlement;

    /**
     * Stores all Improvements and Resources (if any)
     */
    private TileItemContainer tileItemContainer;

    /**
     * Stores all Units (if any).
     */
    private List<Unit> units = Collections.emptyList();


    /**
     * Indicates which colony or Indian settlement that owns this tile ('null'
     * indicates no owner). A colony owns the tile it is located on, and every
     * tile with a worker on it. Note that while units and settlements are owned
     * by a player, a tile is owned by a settlement.
     */
    private Settlement owningSettlement;

    /**
     * Stores each player's image of this tile. Only initialized when needed.
     */
    private java.util.Map<Player, PlayerExploredTile> playerExploredTiles;

    /**
     * Describe region here.
     */
    private Region region;

    /**
     * Whether this tile is connected to Europe.
     */
    private boolean connected = false;

    /**
     * Describe moveToEurope here.
     */
    private Boolean moveToEurope;

    /**
     * Describe style here.
     */
    private int style;

    /**
     * A constructor to use.
     * 
     * @param game The <code>Game</code> this <code>Tile</code> belongs to.
     * @param type The type.
     * @param locX The x-position of this tile on the map.
     * @param locY The y-position of this tile on the map.
     */
    public Tile(Game game, TileType type, int locX, int locY) {
        super(game);

        this.type = type;

        x = locX;
        y = locY;

        owningSettlement = null;
        settlement = null;

        if (!isViewShared()) {
            playerExploredTiles = new HashMap<Player, PlayerExploredTile>();
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

        if (!isViewShared()) {
            playerExploredTiles = new HashMap<Player, PlayerExploredTile>();
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

        if (!isViewShared()) {
            playerExploredTiles = new HashMap<Player, PlayerExploredTile>();
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

        if (!isViewShared()) {
            playerExploredTiles = new HashMap<Player, PlayerExploredTile>();
        }
    }

    // ------------------------------------------------------------ static methods

    public boolean isViewShared() {
        return (getGame().getViewOwner() != null);
    }

    /**
     * Get the <code>Region</code> value.
     *
     * @return a <code>Region</code> value
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Set the <code>Region</code> value.
     *
     * @param newRegion The new Region value.
     */
    public void setRegion(final Region newRegion) {
        this.region = newRegion;
    }

    /**
     * Return the discoverable Region of this Tile, or
     * <code>null</code> if there is none.
     *
     * @return a <code>Region</code> value
     */
    public Region getDiscoverableRegion() {
        if (region == null) {
            return null;
        } else {
            return region.getDiscoverableRegion();
        }
    }

    /**
     * Gets the name of this tile, or shows "unexplored" if not explored by player.
     * 
     * @return The name as a <code>String</code>.
     */
    public String getNameKey() {
        if (isViewShared()) {
            if (isExplored()) {
                return getType().getNameKey();
            } else {
                return "unexplored";
            }
        } else {
            Player player = getGame().getCurrentPlayer();
            if (player != null) {
                PlayerExploredTile pet = playerExploredTiles.get(player);
                if (pet != null && pet.isExplored()) {
                    return getType().getNameKey();
                }
                return "unexplored";
            } else {
                logger.warning("player == null");
                return "";
            }
        }
    }

    /**
     * Returns a description of the <code>Tile</code>, with the name of the tile
     * and any improvements on it (road/plow/etc) from <code>TileItemContainer</code>.
     * @return The description label for this tile
     */
    public StringTemplate getLabel() {
        if (tileItemContainer == null) {
            return StringTemplate.key(type.getNameKey());
        } else {
            return StringTemplate.label("/")
                .add(type.getNameKey())
                .addStringTemplate(tileItemContainer.getLabel());
        }
    }
    
    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        if (settlement == null) {
            Settlement nearSettlement = null;
            int radius = 8; // more than 8 tiles away is no longer "near"
            CircleIterator mapIterator = getMap().getCircleIterator(getPosition(), true, radius);
            while (mapIterator.hasNext()) {
                nearSettlement = getMap().getTile(mapIterator.nextPosition()).getSettlement();
                if (nearSettlement != null) {
                    return StringTemplate.template("nameLocation")
                        .add("%name%", type.getNameKey())
                        .addStringTemplate("%location%", StringTemplate.template("nearLocation")
                                           .addName("%location%", nearSettlement.getName()));
                }
            }
            if (region != null && region.getName() != null) {
                return StringTemplate.template("nameLocation")
                    .add("%name%", type.getNameKey())
                    .add("%location%", region.getNameKey());
            } else {
                return StringTemplate.key(type.getNameKey());
            }
        } else {
            return settlement.getLocationName();
        }
    }

    /**
     * Get the <code>Style</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getStyle() {
        return style;
    }

    /**
     * Set the <code>Style</code> value.
     *
     * @param newStyle The new Style value.
     */
    public void setStyle(final int newStyle) {
        this.style = newStyle;
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

    /**
     * Returns null.
     *
     * @return null
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Returns the <code>TileItemContainer</code>.
     *
     * @return a <code>TileItemContainer</code> value
     */
    public TileItemContainer getTileItemContainer() {
        return tileItemContainer;
    }

    /**
     * Sets the <code>TileItemContainer</code>.
     *
     * @param newTileItemContainer a <code>TileItemContainer</code> value
     */
    public void setTileItemContainer(TileItemContainer newTileItemContainer) {
        tileItemContainer = newTileItemContainer;
    }

    /**
     * Returns a List of <code>TileImprovements</code>.
     *
     * @return a List of <code>TileImprovements</code>
     */
    public List<TileImprovement> getTileImprovements() {
        if (tileItemContainer == null) {
            return Collections.emptyList();
        } else {
            return tileItemContainer.getImprovements();
        }
    }

    /**
     * Returns a List of completed <code>TileImprovements</code>.
     *
     * @return a List of <code>TileImprovements</code>
     */
    public List<TileImprovement> getCompletedTileImprovements() {
        if (tileItemContainer == null) {
            return Collections.emptyList();
        } else {
            List<TileImprovement> result = new ArrayList<TileImprovement>();
            for (TileImprovement improvement : tileItemContainer.getImprovements()) {
                if (improvement.getTurnsToComplete() == 0) {
                    result.add(improvement);
                }
            }
            return result;
        }
    }

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Tile</code>.
     * <p>If this tile has a settlement, the units inside the settlement 
     * are also considered as potential defenders.
     * <p>As this method is quite expensive, it should not be used to test
     * for the presence of enemy units.
     * 
     * @param attacker The target that would be attacking this tile.
     * @return The <code>Unit</code> that has been chosen to defend this
     *         tile.
     */
    public Unit getDefendingUnit(Unit attacker) {
        // First, find the strongest defender of this tile, if any
        Unit tileDefender = null;
        float defencePower = -1.0f;

        for (Unit nextUnit : units) {
            if (isLand() != nextUnit.isNaval()) {
                // on land tiles, ships are docked in port and cannot defend
                // on ocean tiles, land units behave as ship cargo and cannot defend
                float tmpPower = getGame().getCombatModel().getDefencePower(attacker,nextUnit);
                if (tmpPower > defencePower || tileDefender == null) {
                    tileDefender = nextUnit;
                    defencePower = tmpPower;
                }
            }
        }

        if ((tileDefender == null || !tileDefender.isDefensiveUnit()) &&
            getSettlement() != null) {
            // Then, find the strongest defender working in a settlement, if any
            Unit settlementDefender = settlement.getDefendingUnit(attacker);
            // return the strongest of these two units
            if (settlementDefender != null && 
                getGame().getCombatModel().getDefencePower(attacker, settlementDefender) > defencePower) {
                return settlementDefender;
            }
        }
        return tileDefender;
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
        // TODO: find more elegant way to deny river movement bonus to
        // ships
        if (!isLand() || tileItemContainer == null) {
            return getType().getBasicMoveCost();
        } else {
            return tileItemContainer.getMoveCost(getType().getBasicMoveCost(), fromTile);
        }
    }

    /**
     * Disposes all units on this <code>Tile</code>.
     */
    public void disposeAllUnits() {
        // Copy the list first, as the Unit will try to remove itself
        // from its location.
        for (Unit unit : new ArrayList<Unit>(units)) {
            unit.dispose();
        }
        updatePlayerExploredTiles();
    }
    
    public void dispose() {
        if (settlement != null) {
            settlement.dispose();
        }
        if (tileItemContainer != null) {
            tileItemContainer.dispose();
        }
        
        super.dispose();
    }

    /**
     * Gets the first <code>Unit</code> on this tile.
     * 
     * @return The first <code>Unit</code> on this tile.
     */
    public Unit getFirstUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(0);
        }
    }

    /**
     * Gets the last <code>Unit</code> on this tile.
     * 
     * @return The last <code>Unit</code> on this tile.
     */
    public Unit getLastUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(units.size() - 1);
        }
    }

    /**
     * Returns the total amount of Units at this Location. This also includes
     * units in a carrier
     * 
     * @return The total amount of Units at this Location.
     */
    public int getTotalUnitCount() {
        int result = 0;
        for (Unit unit : units) {
            result++;
            result += unit.getUnitCount();
        }
        return result;
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
            return units.contains(locatable);
        } else if (locatable instanceof TileItem) {
            return tileItemContainer != null && tileItemContainer.contains((TileItem) locatable);
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
     * Whether this tile is connected to Europe.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isConnected() {
        return (connected || (type != null && type.isConnected()));
    }

    /**
     * Set the <code>Connected</code> value.
     *
     * @param newConnected The new Connected value.
     */
    public void setConnected(final boolean newConnected) {
        this.connected = newConnected;
    }

    /**
     * Get the <code>MoveToEurope</code> value.
     *
     * @return a <code>Boolean</code> value
     */
    public boolean canMoveToEurope() {
        if (moveToEurope != null) {
            return moveToEurope;
        } else if (type == null) {
            return false;
        } else {
            return type.hasAbility("model.ability.moveToEurope");
        }
    }

    /**
     * Set the <code>MoveToEurope</code> value.
     *
     * @param newMoveToEurope The new MoveToEurope value.
     */
    public void setMoveToEurope(final Boolean newMoveToEurope) {
        this.moveToEurope = newMoveToEurope;
    }

    /**
     * Check if the tile has been explored.
     * 
     * @return true if tile is known.
     */
    public boolean isExplored() {
        return type != null;
    }

    /**
     * Returns <code>true</code> if this Tile is a land Tile, 'false' otherwise.
     * 
     * @return <code>true</code> if this Tile is a land Tile, 'false' otherwise.
     */
    public boolean isLand() {
        return type != null && !type.isWater();
    }

    /**
     * Returns <code>true</code> if this Tile is forested.
     * 
     * @return <code>true</code> if this Tile is forested.
     */
    public boolean isForested() {
        return type != null && type.isForested();
    }

    /**
     * Returns <code>true</code> if this Tile has a River.
     * 
     * @return <code>true</code> if this Tile has a River.
     */
    public boolean hasRiver() {
        return tileItemContainer != null && getTileItemContainer().getRiver() != null;
    }

    /**
     * Returns <code>true</code> if this Tile has a resource on it.
     * 
     * @return <code>true</code> if this Tile has a resource on it.
     */
    public boolean hasResource() {
        return tileItemContainer != null && getTileItemContainer().getResource() != null;
    }

    /**
     * Returns <code>true</code> if this Tile has a lostCityRumour on it.
     * 
     * @return <code>true</code> if this Tile has a lostCityRumour on it.
     */
    public boolean hasLostCityRumour() {
        return tileItemContainer != null && getTileItemContainer().getLostCityRumour() != null;
    }

    /**
     * Returns <code>true</code> if this Tile has a road.
     * 
     * @return <code>true</code> if this Tile has a road.
     */
    public boolean hasRoad() {
        return tileItemContainer != null && getTileItemContainer().getRoad() != null;
    }

    /**
     * Returns the road on this tile, if there is one, and
     * <code>null</code> otherwise.
     *
     * @return a <code>TileImprovement</code> value
     */
    public TileImprovement getRoad() {
        if (tileItemContainer == null) {
            return null;
        } else {
            return getTileItemContainer().getRoad();
        }
    }

    /**
     * Returns the type of this Tile. Returns UNKNOWN if the type of this Tile
     * is unknown.
     * 
     * @return The type of this Tile.
     */
    public TileType getType() {
        return type;
    }

    /**
     * The nation that consider this tile to be their property.
     * 
     * @return The player owning this tile.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the nation that should consider this tile to be their property.
     * 
     * @param owner The player, new owner of this tile.
     * @see #getOwner
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    /**
     * Can the ownership of this tile be claimed?
     * Quick test that does not handle the curly case of tile transfer between
     * colonies, or guarantee success (natives may want to be paid), but
     * just that success is possible.
     *
     * @param player The <code>Player</code> that may wish to claim the tile.
     *
     * @return True if the tile ownership can be claimed.
     */
    public boolean claimable(Player player) {
        Player owner = getOwner();
        return owner == null || owner == player || player.getLandPrice(this) >= 0;
    }

    /**
     * Returns the river on this <code>Tile</code> if any
     * @return River <code>TileImprovement</code>
     */
    public TileImprovement getRiver() {
        if (tileItemContainer == null) {
            return null;
        } else {
            return tileItemContainer.getRiver();
        }
    }

    /**
     * Returns the lost city rumour on this <code>Tile</code> if any
     * @return a <code>LostCityRumour</code> value
     */
    public LostCityRumour getLostCityRumour() {
        if (tileItemContainer == null) {
            return null;
        } else {
            return tileItemContainer.getLostCityRumour();
        }
    }

    /**
     * Removes the lost city rumour from this <code>Tile</code> if there
     * is one.
     */
    public void removeLostCityRumour() {
        if (tileItemContainer != null) {
            tileItemContainer.removeAll(LostCityRumour.class);
        }
    }

    /**
     * Returns the style of a river <code>TileImprovement</code> on this <code>Tile</code>.
     * 
     * @return an <code>int</code> value
     */
    public int getRiverStyle() {
        if (tileItemContainer == null) {
            return 0;
        } else {
            TileImprovement river = tileItemContainer.getRiver();
            if (river == null) {
                return 0;
            } else {
                return river.getStyle();
            }
        }
    }

    /**
     * Describe <code>getNeighbourOrNull</code> method here.
     *
     * @param d a <code>Direction</code> value
     * @return a <code>Tile</code> value
     */
    public Tile getNeighbourOrNull(Direction d) {
        return getMap().getNeighbourOrNull(d, this);
    }

    /**
     * Determine whether this tile has adjacent tiles that are unexplored.
     * 
     * @return true if at least one neighbouring tiles is unexplored, otherwise false
     */
    public boolean hasUnexploredAdjacent() {
        Iterator<Position> tileIterator = getMap().getAdjacentIterator(getPosition());
        while (tileIterator.hasNext()) {
            Tile t = getMap().getTile(tileIterator.next());
            if (!t.isExplored()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this tile has at least one adjacent land tile (if water),
     * or at least one adjacent water tile (if land).
     *
     * @return a <code>boolean</code> value
     */
    public boolean isCoast() {
        for (Direction direction : Direction.values()) {
            Tile otherTile = getMap().getNeighbourOrNull(direction, this);
            if (otherTile != null && otherTile.isLand()!=this.isLand()) {
                return true;
            }
        }
        return false;
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
        owningSettlement = s;
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
     * a convenience method for {@link #getSettlement} that makes sure that 
     * the settlement is a colony.
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
    public void setOwningSettlement(Settlement owner) {
        this.owningSettlement = owner;
    }

    /**
     * Gets the owner of this tile.
     * 
     * @return The Settlement that owns this tile.
     * @see #setOwner
     */
    public Settlement getOwningSettlement() {
        return owningSettlement;
    }

    /**
     * Sets the <code>Resource</code> for this <code>Tile</code>
     */
    public void setResource(Resource resource) {
        if (resource == null) {
            return;
        }
        if (tileItemContainer == null) {
            tileItemContainer = new TileItemContainer(getGame(), this);
        }

        tileItemContainer.addTileItem(resource);
        
        updatePlayerExploredTiles();
    }
    
    /**
     * Sets the type for this Tile.
     * 
     * @param t The new TileType for this Tile.
     */
    public void setType(TileType t) {
        if (t == null) {
            throw new IllegalArgumentException("Tile type must not be null");
        }
        type = t;
        if (tileItemContainer != null) {
            tileItemContainer.removeIncompatibleImprovements();
        }
        if (!isLand()) {
            settlement = null;
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
        return new Position(x, y);
    }

    /**
     * Check if the tile type is suitable for a <code>Settlement</code>,
     * either by a <code>Colony</code> or an <code>IndianSettlement</code>.
     * 
     * @return true if tile suitable for settlement
     */
    public boolean isSettleable() {
        return getType().canSettle();
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

        for (Direction direction : Direction.values()) {
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
     * <code>Unit</code> not currently performing any work.
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

                    if ((childUnit.getMovesLeft() > 0) && (childUnit.getState() == UnitState.ACTIVE)) {
                        return childUnit;
                    }
                }

                if ((u.getMovesLeft() > 0) && (u.getState() == UnitState.ACTIVE)) {
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
            if (!units.contains(locatable)) {
                if (units.equals(Collections.emptyList())) {
                    units = new ArrayList<Unit>();
                } 
                units.add((Unit) locatable);
                ((Unit) locatable).setState(Unit.UnitState.ACTIVE);
                firePropertyChange(UNIT_CHANGE, null, locatable);
            }
        } else if (locatable instanceof TileItem) {
            if (tileItemContainer == null) {
                tileItemContainer = new TileItemContainer(getGame(), this);
            }
            tileItemContainer.addTileItem((TileItem) locatable);
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
            boolean removed = units.remove(locatable);
            if (removed) {
                firePropertyChange(UNIT_CHANGE, locatable, null);
            } else {
                logger.warning("Unit with ID " + ((Unit) locatable).getId() +
                               " could not be removed from " + this.toString() + " with ID " +
                               getId());
            }
        } else if (locatable instanceof TileItem) {
            tileItemContainer.addTileItem((TileItem) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a tile.");
        }
        updatePlayerExploredTiles();
    }
    
    /**
     * Removes the unit from the tile. It does not updatePlayerExploredTiles.
     * @param unit The unit to be removed
     */
    public void removeUnitNoUpdate(Unit unit) {
        units.remove(unit);
    }
    /**
     * Adds the unit to the tile. It does not updatePlayerExploredTiles.
     * @param unit The unit to be added
     */
    public void addUnitNoUpdate(Unit unit) {
        if (units.equals(Collections.emptyList())) {
            units = new ArrayList<Unit>();
        }
        units.add(unit);
    }

    /**
     * Returns the amount of units at this <code>Location</code>.
     * 
     * @return The amount of units at this <code>Location</code>.
     */
    public int getUnitCount() {
        return units.size();
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
        return units;
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
        return units.iterator();
    }

    /**
     * Checks whether or not the specified locatable may be added to this
     * <code>Location</code>.
     *
     * @param locatable a <code>Locatable</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canAdd(Locatable locatable) {
        if (locatable instanceof Unit) {
            // TODO: check for land/naval units?
            return true;
        } else if (locatable instanceof TileImprovement) {
            return ((TileImprovement) locatable).getType().isTileTypeAllowed(getType());
        } else {
            return false;
        }
    }

    /**
     * The potential of this tile to produce a certain type of goods.
     * 
     * @param goodsType The type of goods to check the potential for.
     * @param unitType an <code>UnitType</code> value
     * @return The normal potential of this tile to produce that amount of
     *         goods.
     */
    public int potential(GoodsType goodsType, UnitType unitType) {
        return getTileTypePotential(getType(), goodsType, getTileItemContainer(), unitType);
    }

    /**
     * Gets the maximum potential for producing the given type of goods. The
     * maximum potential is the potential of a tile after the tile has been
     * plowed/built road on.
     * 
     * @param goodsType The type of goods.
     * @param unitType an <code>UnitType</code> value
     * @return The maximum potential.
     */
    public int getMaximumPotential(GoodsType goodsType, UnitType unitType) {
        // If we consider maximum potential to the effect of having
        // all possible improvements done, iterate through the
        // improvements and get the bonuses of all related ones.  If
        // there are options to change tiletype using an improvement,
        // consider that too.

        List<TileType> tileTypes = new ArrayList<TileType>();
        tileTypes.add(getType());

        // Add to the list the various possible tile type changes
        for (TileImprovementType impType : FreeCol.getSpecification().getTileImprovementTypeList()) {
            if (impType.getChange(getType()) != null) {
                // There is an option to change TileType
                tileTypes.add(impType.getChange(getType()));
            }
        }

        int maxProduction = 0;

        for (TileType tileType : tileTypes) {
            float potential = tileType.getProductionOf(goodsType, unitType);
            if (tileType == getType() && hasResource()) {
                for (TileItem item : tileItemContainer.getTileItems()) {
                    if (item instanceof Resource) {
                        potential = ((Resource) item).getBonus(goodsType, unitType, (int) potential);
                    }
                }
            }
            for (TileImprovementType impType : FreeCol.getSpecification().getTileImprovementTypeList()) {
                if (impType.isNatural() || !impType.isTileTypeAllowed(tileType)) {
                    continue;
                } else if (impType.getBonus(goodsType) > 0) {
                    potential = impType.getProductionModifier(goodsType).applyTo(potential);
                }
            }
            maxProduction = Math.max((int) potential, maxProduction);
        }
        return maxProduction;
    }

    /**
     * Describe <code>getProductionBonus</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>Modifier</code> value
     */
    public Set<Modifier> getProductionBonus(GoodsType goodsType, UnitType unitType) {
        Set<Modifier> result = new HashSet<Modifier>();
        result.addAll(type.getProductionBonus(goodsType));
        if (!result.isEmpty() && tileItemContainer != null) {
            result.addAll(tileItemContainer.getProductionBonus(goodsType, unitType));
        }
        return result;
    }

    /**
     * Checks whether this <code>Tile</code> can have a road or not. This
     * method will return <code>false</code> if a road has already been built.
     * 
     * @return The result.
     */
    public boolean canGetRoad() {
        return isLand() && (tileItemContainer == null || tileItemContainer.getRoad() == null);
    }

    /**
     * Finds the TileImprovement of a given Type, or null if there is no match.
     */
    public TileImprovement findTileImprovementType(TileImprovementType type) {
        if (tileItemContainer == null) {
            return null;
        } else {
            return tileItemContainer.findTileImprovementType(type);
        }
    }
    
    /**
     * Will check whether this tile has a completed improvement of the given
     * type.
     * 
     * Useful for checking whether the tile for instance has a road or is
     * plowed.
     * 
     * @param type
     *            The type to check for.
     * @return Whether the tile has the improvement and the improvement is
     *         completed.
     */
    public boolean hasImprovement(TileImprovementType type) {
        if (type.changeContainsTarget(getType())) {
            return true;
        } else if (tileItemContainer != null) {
            return tileItemContainer.hasImprovement(type);
        }
        return false;
    }
    
    /**
     * Calculates the potential of a certain <code>GoodsType</code>.
     * 
     * @param tileType
     *            The <code>TileType</code>.
     * @param goodsType
     *            The <code>GoodsType</code> to check the potential for.
     * @param tiContainer
     *            The <code>TileItemContainer</code> with any TileItems to
     *            give bonuses.
     * @param unitType an <code>UnitType</code> value
     *            The Bonus Fish to be considered if valid
     * @return The amount of goods.
     */
    public static int getTileTypePotential(TileType tileType, GoodsType goodsType, 
                                           TileItemContainer tiContainer, UnitType unitType) {
        if (tileType == null || goodsType == null || !goodsType.isFarmed()) {
            return 0;
        }
        // Get tile potential + bonus if any
        int potential = tileType.getProductionOf(goodsType, unitType);
        if (tiContainer != null) {
            potential = tiContainer.getTotalBonusPotential(goodsType, unitType, potential);
        }
        return potential;
    }

    public int getPrimaryProduction() {
        AbstractGoods primaryProduction = type.getPrimaryGoods();
        if (primaryProduction == null) {
            return 0;
        } else {
            int potential = primaryProduction.getAmount();
            if (tileItemContainer != null) {
                potential = tileItemContainer.getTotalBonusPotential(primaryProduction.getType(), null, potential);
            }
            return potential;
        }
    }        

    public int getSecondaryProduction() {
        AbstractGoods secondaryProduction = type.getSecondaryGoods();
        if (secondaryProduction == null) {
            return 0;
        } else {
            int potential = secondaryProduction.getAmount();
            if (tileItemContainer != null) {
                potential = tileItemContainer.getTotalBonusPotential(secondaryProduction.getType(), null, potential);
            }
            return potential;
        }
    }        

    /**
     * Sorts GoodsTypes according to potential based on TileType,
     * TileItemContainer if any.
     *
     * @return The sorted GoodsTypes.
     */
    public List<AbstractGoods> getSortedPotential() {
        return getSortedPotential(null, null);
    }

    /**
     * Sorts GoodsTypes according to potential based on TileType,
     * TileItemContainer if any.
     *
     * @param unit the <code>Unit</code> to work on this Tile
     *
     * @return The sorted GoodsTypes.
     */
    public List<AbstractGoods> getSortedPotential(Unit unit) {
        return getSortedPotential(unit.getType(), unit.getOwner());
    }

    /**
     * Sorts GoodsTypes according to potential based on TileType,
     * TileItemContainer if any.
     *
     * @param unitType the <code>UnitType</code> to work on this Tile
     * @param owner the <code>Player</code> owning the unit
     *
     * @return The sorted GoodsTypes.
     */
    public List<AbstractGoods> getSortedPotential(UnitType unitType, Player owner) {

        List<AbstractGoods> goodsTypeList = new ArrayList<AbstractGoods>();
        if (getType() != null) {
            // It is necessary to consider all farmed goods, since the
            // tile might have a resource that produces goods not
            // produced by the tile type.
            for (GoodsType goodsType : FreeCol.getSpecification().getFarmedGoodsTypeList()) {
                int potential = potential(goodsType, unitType);
                if (potential > 0) {
                    goodsTypeList.add(new AbstractGoods(goodsType, potential));
                }
            }
            if (owner == null || owner.getMarket() == null) {
                Collections.sort(goodsTypeList, new Comparator<AbstractGoods>() {
                        public int compare(AbstractGoods o, AbstractGoods p) {
                            return p.getAmount() - o.getAmount();
                        }
                    });
            } else {
                final Market market = owner.getMarket();
                Collections.sort(goodsTypeList, new Comparator<AbstractGoods>() {
                        public int compare(AbstractGoods o, AbstractGoods p) {
                            return market.getSalePrice(p.getType(), p.getAmount())
                                - market.getSalePrice(o.getType(), o.getAmount());
                        }
                    });
            }
        }
        return goodsTypeList;
    }

    /**
     * This method is called only when a new turn is beginning. It will reduce the quantity of
     * the bonus <code>Resource</code> that is on the tile, if any and if applicable.
     * @see ResourceType
     * @see ColonyTile#newTurn
     */
    public void expendResource(GoodsType goodsType, UnitType unitType, Settlement settlement) {
        if (hasResource() && tileItemContainer.getResource().getQuantity() != -1) {
            Resource resource = tileItemContainer.getResource();
            // Potential of this Tile and Improvements
            // TODO: review
            int potential = getTileTypePotential(getType(), goodsType, tileItemContainer, unitType);
            for (TileItem item : tileItemContainer.getTileItems()) {
                if (item instanceof TileImprovement) {
                    potential += ((TileImprovement) item).getBonus(goodsType);
                }
            }

            if (resource.useQuantity(goodsType, unitType, potential) == 0) {
                Player owner = settlement.getOwner();
                owner.addModelMessage(new ModelMessage(ModelMessage.MessageType.WARNING,
                                                       "model.tile.resourceExhausted",
                                                       settlement)
                                .add("%resource%", resource.getNameKey())
                                .addName("%colony%", settlement.getName()));
                tileItemContainer.removeTileItem(resource);
                updatePlayerExploredTiles();
            }
        }
    }


    /**
     * Gets the <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    public PlayerExploredTile getPlayerExploredTile(Player player) {
        if (playerExploredTiles == null) {
            return null;
        }
        return playerExploredTiles.get(player);
    }

    /**
     * Creates a <code>PlayerExploredTile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     * @see PlayerExploredTile
     */
    private void createPlayerExploredTile(Player player) {
        playerExploredTiles.put(player, new PlayerExploredTile(getGame(), player, this));
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     * 
     * @param player The <code>Player</code>.
     */
    public void updatePlayerExploredTile(Player player) {

        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        PlayerExploredTile pet = playerExploredTiles.get(player);
        if (pet == null) {

            if (player.isEuropean()) {
                String message = "'playerExploredTiles' for " + player.getPlayerType() + 
                                 " player '" + player.getName() + "' is 'null'. " + 
                                 player.canSee(this) + ", " + isExploredBy(player) + " ::: " + getPosition();
                logger.warning(message);
                //throw new IllegalStateException(message);
                pet = new PlayerExploredTile(getGame(), player, this);
                playerExploredTiles.put(player, pet);
            } else {
                return;
            }
        }

        pet.getTileItemInfo(tileItemContainer);

        pet.setConnected(connected);
        pet.setOwner(owner);

        if (getColony() != null) {
            pet.setColonyUnitCount(getSettlement().getUnitCount());
            
            // TODO stockade may now be null, but is 0 the right way to set this?
            // This might as well be a mistake in the spec.
            Building stockade = getColony().getStockade();
            if (stockade != null){
            	pet.setColonyStockadeLevel(stockade.getType().getIndex());
            } else {
            	pet.setColonyStockadeLevel(0);
            }
        } else if (getSettlement() != null) {
            IndianSettlement settlement = (IndianSettlement) getSettlement();
            pet.setMissionary(settlement.getMissionary());
            if (settlement.hasBeenVisited(player)) {
                pet.setVisited();
            }
            /*
             * These attributes should not be updated by this method: skill,
             * highlyWantedGoods, wantedGoods1 and wantedGoods2
             */
        } else {
            pet.setColonyUnitCount(0);
        }
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
        for (Player player : getGame().getPlayers()) {
            if (playerExploredTiles.get(player) != null ||
                (player.isEuropean() && player.canSee(this))) {
                updatePlayerExploredTile(player);
            }
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
        if (playerExploredTiles == null || playerExploredTiles.get(player) == null || !isExplored()) {
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
        if (playerExploredTiles.get(player) == null) {
            createPlayerExploredTile(player);
        }
        getPlayerExploredTile(player).setExplored(explored);
        updatePlayerExploredTile(player);
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
        playerExploredTile.setWantedGoods(is.getWantedGoods());
        playerExploredTile.setVisited();
    }


    /**
     * Returns the number of turns it takes for a non-expert pioneer to perform
     * the given <code>TileImprovementType</code>. It will check if it is valid
     * for this <code>TileType</code>.
     * 
     * @param workType The <code>TileImprovementType</code>
     * 
     * @return The number of turns it should take a non-expert pioneer to finish
     *         the work.
     */
    public int getWorkAmount(TileImprovementType workType) {
        if (workType == null) {
            return -1;
        }
        if (!workType.isTileAllowed(this)) {
            return -1;
        }
        // Return the basic work turns + additional work turns
        return (getType().getBasicWorkTurns() + workType.getAddWorkTurns());
    }

    /**
     * Returns the unit who is occupying the tile
     * @return the unit who is occupying the tile
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        Unit unit = getFirstUnit();
        Player owner = null;
        if (owningSettlement != null) {
            owner = owningSettlement.getOwner();
        }
        if (owner != null && unit != null && unit.getOwner() != owner
            && owner.getStance(unit.getOwner()) != Stance.ALLIANCE) {
            for(Unit enemyUnit : getUnitList()) {
                if (enemyUnit.isOffensiveUnit() && enemyUnit.getState() == UnitState.FORTIFIED) {
                    return enemyUnit;
                }
            }
        }
        return null;
    }

    /**
     * Checks whether there is a fortified enemy unit in the tile.
     * Units can't produce in occupied tiles
     * @return <code>true</code> if an fortified enemy unit is in the tile
     */
    public boolean isOccupied() {
        return getOccupyingUnit() != null;
    }
    
    /**
     * Determines whether this tile is adjacent to the specified tile.
     * 
     * @param tile A potentially adjacent <code>Tile</code>.
     * @return <code>true</code> if the tile is adjacent to this tile
     */
    public boolean isAdjacent(Tile tile) {
    	if (tile == null) {
    		return false;
    	}
    	return (this.getDistanceTo(tile) == 1);
    }
    
    /**
     * Returns a String representation of this Tile.
     * 
     * @return A String representation of this Tile.
     */
    public String toString() {
        return "Tile("+x+","+y+"):"+((type==null)?"unknown":type.getId());
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
                pet = playerExploredTiles.get(player);
            } else {
                logger.warning("player == null");
            }
        }

        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("x", Integer.toString(x));
        out.writeAttribute("y", Integer.toString(y));
        out.writeAttribute("style", Integer.toString(style));

        writeAttribute(out, "type", getType());
        writeAttribute(out, "region", getRegion());

        if (connected && !type.isConnected()) {
            out.writeAttribute("connected", Boolean.toString(true));
        }

        if (owner != null) {
            if (getGame().isClientTrusted() || showAll || player.canSee(this)) {
                out.writeAttribute("owner", owner.getId());
            } else if (pet != null) {
                writeAttribute(out, "owner", pet.getOwner());
            }
        }

        if ((getGame().isClientTrusted() || showAll || player.canSee(this)) && (owningSettlement != null)) {
            out.writeAttribute("owningSettlement", owningSettlement.getId());
        }

        if (settlement != null) {
            settlement.toXML(out, player, showAll, toSavedGame);
        }

        // Check if the player can see the tile:
        // Do not show enemy units or any tileitems on a tile out-of-sight.
        if (getGame().isClientTrusted() || showAll
            || (player.canSee(this) && (settlement == null || settlement.getOwner() == player))) {
            if (!units.isEmpty()) {
                out.writeStartElement(UNITS_TAG_NAME);
                for (Unit unit : units) {
                    unit.toXML(out, player, showAll, toSavedGame);
                }
                out.writeEndElement();
            }
            if (tileItemContainer != null) {
                tileItemContainer.toXML(out, player, showAll, toSavedGame);
            }
        } else {
            if (tileItemContainer != null) {
                TileItemContainer newTileItemContainer = null;
                if (pet != null) {
                    newTileItemContainer = new TileItemContainer(getGame(), this, pet);
                } else {
                    newTileItemContainer = new TileItemContainer(getGame(), this);                
                }
                newTileItemContainer.setFakeID(tileItemContainer.getId());
                newTileItemContainer.toXML(out, player, showAll, toSavedGame);
            }
        }

        if (toSavedGame) {
            for (Entry<Player, PlayerExploredTile> entry : playerExploredTiles.entrySet()) {
                if (entry.getValue().isExplored()) {
                    entry.getValue().toXML(out, entry.getKey(), showAll, toSavedGame);
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
        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        x = Integer.parseInt(in.getAttributeValue(null, "x"));
        y = Integer.parseInt(in.getAttributeValue(null, "y"));
        style = getAttribute(in, "style", 0);

        String typeString = in.getAttributeValue(null, "type");
        if (typeString != null) {
            type = FreeCol.getSpecification().getTileType(typeString);
        }

        // compatibility mode
        boolean needsRumour = getAttribute(in, LostCityRumour.getXMLElementTagName(), false);

        connected = getAttribute(in, "connected", false);
        owner = getFreeColGameObject(in, "owner", Player.class, null);
        region = getFreeColGameObject(in, "region", Region.class, null);

        final String owningSettlementStr = in.getAttributeValue(null, "owningSettlement");
        if (owningSettlementStr != null) {
            owningSettlement = (Settlement) getGame().getFreeColGameObject(owningSettlementStr);
            if (owningSettlement == null) {
                if (owningSettlementStr.startsWith(IndianSettlement.getXMLElementTagName())) {
                    owningSettlement = new IndianSettlement(getGame(), owningSettlementStr);
                } else if (owningSettlementStr.startsWith(Colony.getXMLElementTagName())) {
                    owningSettlement = new Colony(getGame(), owningSettlementStr);
                } else {
                    logger.warning("Unknown type of Settlement.");
                }
            }
        } else {
            owningSettlement = null;
        }

        Settlement oldSettlement = settlement;
        settlement = null;
        units.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(Colony.getXMLElementTagName())) {
                settlement = updateFreeColGameObject(in, Colony.class);
            } else if (in.getLocalName().equals(IndianSettlement.getXMLElementTagName())) {
                settlement = updateFreeColGameObject(in, IndianSettlement.class);
            } else if (in.getLocalName().equals(UNITS_TAG_NAME)) {
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(Unit.getXMLElementTagName())) {
                        if (units.equals(Collections.emptyList())) {
                            units = new ArrayList<Unit>();
                        }
                        units.add(updateFreeColGameObject(in, Unit.class));
                    }
                }
            } else if (in.getLocalName().equals(TileItemContainer.getXMLElementTagName())) {
                tileItemContainer = (TileItemContainer) getGame().getFreeColGameObject(in.getAttributeValue(null, ID_ATTRIBUTE));
                if (tileItemContainer != null) {
                    tileItemContainer.readFromXML(in);
                } else {
                    tileItemContainer = new TileItemContainer(getGame(), this, in);
                }
            } else if (in.getLocalName().equals("playerExploredTile")) {
                // Only from a savegame:
                Player player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
                if (playerExploredTiles.get(player) == null) {
                    PlayerExploredTile pet = new PlayerExploredTile(getGame(), in);
                    playerExploredTiles.put(player, pet);
                } else {
                    playerExploredTiles.get(player).readFromXML(in);
                }
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " [" +
                               in.getAttributeValue(null, "ID") + "] " +
                               " loading tile with ID " +
                               getId());
                in.nextTag();
            }
        }

        // Player settlement list is not passed in player updates
        // so do it here.  TODO: something better.
        if (settlement == null && oldSettlement != null) {
            Player owner = oldSettlement.getOwner();
            oldSettlement.setOwner(null);
            owner.removeSettlement(oldSettlement);
        } else if (settlement != null && oldSettlement == null) {
            Player owner = settlement.getOwner();
            owner.addSettlement(settlement);
        }

        // compatibility mode
        if (needsRumour) {
            add(new LostCityRumour(getGame(), this));
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
}
