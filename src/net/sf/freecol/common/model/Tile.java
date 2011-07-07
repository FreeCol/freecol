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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Unit.UnitState;

import org.w3c.dom.Element;


/**
 * Represents a single tile on the <code>Map</code>.
 *
 * @see Map
 */
public final class Tile extends FreeColGameObject
    implements Location, Named, Ownable {

    private static final Logger logger = Logger.getLogger(Tile.class.getName());

    private static final String UNITS_TAG_NAME = "units";

    // This must be distinct from ColonyTile/Building.UNIT_CHANGE or
    // the colony panel can get confused.
    public static final String UNIT_CHANGE = "TILE_UNIT_CHANGE";

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
     * Does this tile have an explicit moveToEurope state.  If null,
     * just use the defaults (usually not, unless water and on map edge),
     * otherwise use the explicit value provided here.
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
                PlayerExploredTile pet = getPlayerExploredTile(player);
                return (pet != null) ? getType().getNameKey() : "unexplored";
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
            for (Tile tile: getSurroundingTiles(radius)) {
                nearSettlement = tile.getSettlement();
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
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to prepare the location name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return (settlement == null) ? getLocationName()
            : settlement.getLocationNameFor(player);
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
        return getPosition().getDistance(tile.getPosition());
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
     * Is the alternate unit a better defender than the current choice.
     * Prefer if there is no current defender, or if the alternate unit
     * provides greater defensive power and does not replace a defensive
     * unit defender with a non-defensive unit.
     *
     * @param defender The current defender <code>Unit</code>.
     * @param defenderPower Its defence power.
     * @param other An alternate <code>Unit</code>.
     * @param otherPower Its defence power.
     * @return True if the other unit should be preferred.
     */
    private static boolean betterDefender(Unit defender, float defenderPower,
                                          Unit other, float otherPower) {
        return defender == null
            || (otherPower > defenderPower
                && !(defender.isDefensiveUnit() && !other.isDefensiveUnit()));
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
        CombatModel cm = getGame().getCombatModel();
        Unit defender = null;
        float defenderPower = -1.0f;
        float power;

        // Check the units on the tile...
        for (Unit u : units) {
            if (isLand() != u.isNaval()) {
                // On land, ships are normally docked in port and
                // cannot defend.  Except if beached (see below).
                // On ocean tiles, land units behave as ship cargo and
                // cannot defend
                power = cm.getDefencePower(attacker, u);
                if (Tile.betterDefender(defender, defenderPower, u, power)) {
                    defender = u;
                    defenderPower = power;
                }
            }
        }

        // ...then a settlement defender if any...
        if ((defender == null || !defender.isDefensiveUnit())
            && getSettlement() != null) {
            Unit u = null;
            try {
                // HACK: The AI is prone to removing all units in a
                // settlement which causes Colony.getDefendingUnit()
                // to throw.
                u = settlement.getDefendingUnit(attacker);
            } catch (IllegalStateException e) {
                logger.warning("Empty settlement: " + settlement.getName());
            }
            // This routine can be called on the client for the pre-combat
            // popup where enemy settlement defenders are not visible,
            // thus u == null is valid.
            if (u != null) {
                power = cm.getDefencePower(attacker, u);
                if (Tile.betterDefender(defender, defenderPower, u, power)) {
                    defender = u;
                    defenderPower = power;
                }
            }
        }

        // ...finally, if we have failed to find a valid defender
        // for a land tile, allow a beached naval unit to defend (and
        // lose) as a last resort.
        if (defender == null && isLand()) defender = getFirstUnit();

        return defender;
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
     * Get the move-to-Europe state of the tile.
     *
     * @return The move-to-Europe state of the tile.
     */
    public Boolean getMoveToEurope() {
        return moveToEurope;
    }

    /**
     * Set the move-to-Europe state of the tile.
     *
     * @param moveToEurope The new move-to-Europe state for the tile.
     */
    public void setMoveToEurope(Boolean moveToEurope) {
        this.moveToEurope = moveToEurope;
    }

    /**
     * Can a unit move to Europe from this tile?
     *
     * @return True if a unit can move to Europe from this tile.
     */
    public boolean canMoveToEurope() {
        return (getMoveToEurope() != null) ? getMoveToEurope()
            : (type == null) ? false
            : (type.hasAbility("model.ability.moveToEurope")) ? true
            // TODO: remove this when we are confident all the maps have
            // appropriate moveToEurope overrides.
            : isAdjacentToMapEdge() && type.isWater();
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
     *         <code>Tile</code> or <i>null</i> if none found.
     * @see #getSettlement
     */
    public Colony getColony() {
        return (settlement != null && settlement instanceof Colony)
            ? (Colony) settlement
            : null;
    }

    /**
     * Gets the <code>IndianSettlement</code> located on this
     * <code>Tile</code>. Only a convenience method for {@link
     * #getSettlement} that makes sure that the settlement is a native
     * settlement.
     *
     * @return The <code>IndianSettlement</code> that is located on this
     *         <code>Tile</code> or <i>null</i> if none found.
     * @see #getSettlement
     */
    public IndianSettlement getIndianSettlement() {
        return (settlement != null && settlement instanceof IndianSettlement)
            ? (IndianSettlement) settlement
            : null;
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
     * Change the tile ownership.  Also change the owning settlement
     * as the two are commonly related.
     *
     * @param player The <code>Player</code> to own the tile.
     * @param settlement The <code>Settlement</code> to own the tile.
     */
    public void changeOwnership(Player player, Settlement settlement) {
        Player old = getOwner();
        setOwner(player);
        setOwningSettlement(settlement);
        updatePlayerExploredTiles(old);
    }

    /**
     * Is this tile under active use?
     *
     * @return True if a colony is using this tile.
     */
    public boolean isInUse() {
        return getOwningSettlement() instanceof Colony
            && ((Colony) getOwningSettlement()).isTileInUse(this);
    }

    /**
     * Adds a tile item to this tile.
     *
     * @param item The <code>TileItem</code> to add.
     */
    private void addTileItem(TileItem item) {
        if (tileItemContainer == null) {
            tileItemContainer = new TileItemContainer(getGame(), this);
        }
        tileItemContainer.addTileItem(item);
        updatePlayerExploredTiles();
    }

    /**
     * Gets the lost city rumour on this <code>Tile</code> if any.
     *
     * @return The <code>LostCityRumour</code> on this tile, or null if none.
     */
    public LostCityRumour getLostCityRumour() {
        return (tileItemContainer == null) ? null
            : tileItemContainer.getLostCityRumour();
    }

    /**
     * Adds a lost city rumour to this tile.
     *
     * @param rumour The <code>LostCityRumour</code> to add.
     */
    public void addLostCityRumour(LostCityRumour rumour) {
        addTileItem(rumour);
    }

    /**
     * Removes the lost city rumour from this <code>Tile</code> if there
     * is one.
     */
    public void removeLostCityRumour() {
        if (tileItemContainer != null) {
            tileItemContainer.removeAll(LostCityRumour.class);
            updatePlayerExploredTiles();
        }
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
     * Returns the neighbouring Tile of the given Tile in the given direction.
     *
     * @param direction
     *            The direction in which the neighbour tile is located.
     * @return The neighbouring Tile of the given Tile in the given direction.
     */
    public Tile getNeighbourOrNull(Direction direction) {
        Position position = getPosition();
        if (getMap().isValid(position)) {
            Position neighbourPosition = position.getAdjacent(direction);
            return getMap().getTile(neighbourPosition);
        } else {
            return null;
        }
    }

    /**
     * Determine whether this tile has adjacent tiles that are unexplored.
     *
     * @return true if at least one neighbouring tiles is unexplored, otherwise false
     */
    public boolean hasUnexploredAdjacent() {
        for (Tile t: getSurroundingTiles(1)) {
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
            Tile otherTile = getNeighbourOrNull(direction);
            if (otherTile != null && otherTile.isLand()!=this.isLand()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a <code>Resource</code> to this <code>Tile</code>.
     *
     * @param resource The <code>Resource</code> to add.
     */
    public void addResource(Resource resource) {
        if (resource == null) return;
        addTileItem(resource);
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

    // TODO: this is used only to update false Tiles, a practice that
    // should be killed with fire.
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
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
            }
        } else if (locatable instanceof TileItem) {
            addTileItem((TileItem) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a tile.");
        }
    }

    /**
     * Removes a <code>Locatable</code> from this Location.
     *
     * @param locatable The <code>Locatable</code> to remove from this
     *            Location.
     */
    public void remove(Locatable locatable) {
        Player old = getOwner();
        if (locatable instanceof Unit) {
            units.remove(locatable);
        } else if (locatable instanceof TileItem) {
            tileItemContainer.addTileItem((TileItem) locatable);
            updatePlayerExploredTiles(old);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a tile.");
        }
    }

    /**
     * Removes the unit from the tile.
     *
     * @param unit The unit to be removed
     */
    public void removeUnitNoUpdate(Unit unit) {
        units.remove(unit);
    }

    /**
     * Adds the unit to the tile.
     *
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
        return new ArrayList<Unit>(units);
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
        for (TileImprovementType impType : getSpecification().getTileImprovementTypeList()) {
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
            for (TileImprovementType impType : getSpecification().getTileImprovementTypeList()) {
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
        if (tileItemContainer != null) {
            Resource resource = tileItemContainer.getResource();
            if (resource != null) {
                result.addAll(resource.getType().getProductionModifier(goodsType, unitType));
            }
            if (!result.isEmpty()) {
                result.addAll(tileItemContainer.getProductionBonus(goodsType, unitType));
            }
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
            potential = tiContainer.getTotalBonusPotential(goodsType, unitType, potential, false);
        }
        return potential;
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
            for (GoodsType goodsType : getSpecification().getFarmedGoodsTypeList()) {
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
     * This method is called only when a new turn is beginning. It
     * will reduce the quantity of the bonus <code>Resource</code>
     * that is on the tile, if any and if applicable.
     *
     * @return The resource if it is exhausted by this call (so it can
     *     be used in a message), otherwise null.
     * @see ResourceType
     */
    public Resource expendResource(GoodsType goodsType, UnitType unitType, Settlement settlement) {
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
                tileItemContainer.removeTileItem(resource);
                updatePlayerExploredTiles();
                return resource;
            }
        }
        return null;
    }

    /**
     * Updates the <code>PlayerExploredTile</code> for each player. This
     * update will only be performed if the player
     * {@link Player#canSee(Tile) can see} this <code>Tile</code>.
     */
    public void updatePlayerExploredTiles() {
        updatePlayerExploredTiles(null);
    }

    /**
     * Updates the <code>PlayerExploredTile</code> for each player. This
     * update will only be performed if the player
     * {@link Player#canSee(Tile) can see} this <code>Tile</code>.
     *
     * @param oldPlayer The optional <code>Player</code> that formerly
     *     had visibility of this tile and should see the change.
     */
    public void updatePlayerExploredTiles(Player oldPlayer) {
        if (playerExploredTiles == null || getGame().getViewOwner() != null) {
            return;
        }
        for (Player player : getGame().getLiveEuropeanPlayers()) {
            if (player == oldPlayer || player.canSee(this)) {
                updatePlayerExploredTile(player, false);
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
        return (playerExploredTiles == null) ? null
            : playerExploredTiles.get(player);
    }

    /**
     * Updates the information about this <code>Tile</code> for the given
     * <code>Player</code>.
     *
     * @param player The <code>Player</code>.
     * @param full If true, also update any hidden information specific to a
     *    settlement present on the tile.
     */
    public void updatePlayerExploredTile(Player player, boolean full) {
        if (playerExploredTiles == null || getGame().getViewOwner() != null
            || !player.isEuropean()) {
            return;
        }
        PlayerExploredTile pet = playerExploredTiles.get(player);
        if (pet == null) {
            pet = new PlayerExploredTile(getGame(), player, this);
            playerExploredTiles.put(player, pet);
        }
        pet.update(full);
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
        if (!player.isEuropean()) return true;
        if (!isExplored()) return false;
        return getPlayerExploredTile(player) != null;
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
        if (!player.isEuropean()) return;
        if (explored) {
            updatePlayerExploredTile(player, false);
        } else {
            if (playerExploredTiles != null) {
                playerExploredTiles.remove(player);
            }
        }
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
        return (tile == null) ? false : this.getDistanceTo(tile) == 1;
    }

    /**
     * Gets the position adjacent Tile to a given Tile, in a given
     * direction.
     *
     * @param direction The direction (N, NE, E, etc.)
     * @return Adjacent tile
     */
     public Tile getAdjacentTile(Direction direction) {
         int x = getX() + ((getY() & 1) != 0 ?
                               direction.getOddDX() : direction.getEvenDX());
         int y = getY() + ((getY() & 1) != 0 ?
                               direction.getOddDY() : direction.getEvenDY());
         return getMap().getTile(x, y);
     }

     /**
      * Returns all the tiles surrounding this tile within the
      * given range. This tile is not included.
      *
      * @param range
      *            How far away do we need to go starting from this.
      * @return The tiles surrounding this tile.
      */
     public Iterable<Tile> getSurroundingTiles(final int range) {
         return new Iterable<Tile>() {
             public Iterator<Tile> iterator() {
                 final Iterator<Position> m = (range == 1)
                     ? getMap().getAdjacentIterator(getPosition())
                     : getMap().getCircleIterator(getPosition(), true, range);

                 return new Iterator<Tile>() {
                     public boolean hasNext() {
                         return m.hasNext();
                     }

                     public Tile next() {
                         return getMap().getTile(m.next());
                     }

                     public void remove() {
                         m.remove();
                     }
                 };
             }
         };
     }


     /**
      * Returns all the tiles surrounding this tile within the
      * given inclusive upper and lower bounds.
      * getSurroundingTiles(r) is equivalent to getSurroundingTiles(1, r),
      * thus this tile is included if rangeMin is zero.
      *
      * @param rangeMin The inclusive minimum distance from this tile.
      * @param rangeMax The inclusive maximum distance from this tile.
      * @return A list of the tiles surrounding this tile.
      */
     public List<Tile> getSurroundingTiles(int rangeMin, int rangeMax) {
         List<Tile> result = new ArrayList<Tile>();
         if (rangeMin > rangeMax || rangeMin < 0) return result;

         if (rangeMin == 0) result.add(this);

         if (rangeMax > 0) {
             for (Tile t : getSurroundingTiles(rangeMax)) {
                 // add all tiles up to rangeMax
                 result.add(t);
             }
         }
         if (rangeMin > 1) {
             for (Tile t : getSurroundingTiles(rangeMin - 1)) {
                 // remove the tiles closer than rangeMin
                 result.remove(t);
             }
         }
         return result;
     }

    /**
     * Checks if the given <code>Tile</code> is adjacent to the
     * east or west edge of the map.
     *
     * @return <code>true</code> if the given tile is at the edge of the map.
     */
    public boolean isAdjacentToVerticalMapEdge() {
        if ((getNeighbourOrNull(Direction.E) == null)
            || (getNeighbourOrNull(Direction.W) == null)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the given <code>Tile</code> is adjacent to the edge of the
     * map.
     *
     * @return <code>true</code> if the given tile is at the edge of the map.
     */
    public boolean isAdjacentToMapEdge() {
        for (Direction direction : Direction.values()) {
            if (getNeighbourOrNull(direction) == null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Finds the nearest settlement to this tile.
     *
     * @param owner If non-null, the settlement should be owned by this player.
     * @param radius The maximum radius of the search.
     * @return The nearest settlement, or null if none.
     */
    public Settlement getNearestSettlement(Player owner, int radius) {
        if (radius <= 0) radius = INFINITY;
        Map map = getGame().getMap();
        Iterator<Position> iter = map.getCircleIterator(getPosition(), true,
                                                        radius);
        while (iter.hasNext()) {
            Tile t = map.getTile(iter.next());
            if (t == this) continue;
            Settlement settlement = t.getSettlement();
            if (settlement != null
                && (owner == null || settlement.getOwner() == owner)) {
                return settlement;
            }
        }
        return null;
    }

    /**
     * Finds a safe tile to put a unit on, near to this one.
     * Useful on return from Europe.
     *
     * @param player The owner of the unit to place (may be null).
     * @param random An optional pseudo-random number source.
     * @return A vacant tile near this one.
     */
    public Tile getSafeTile(Player player, Random random) {
        if ((getFirstUnit() == null || getFirstUnit().getOwner() == player)
            && (getSettlement() == null || getSettlement().getOwner() == player)) {
            return this;
        }

        for (int r = 1; true; r++) {
            List<Tile> tiles = getSurroundingTiles(r, r);
            if (random != null) Collections.shuffle(tiles, random);
            for (Tile t : tiles) {
                if ((t.getFirstUnit() == null || t.getFirstUnit().getOwner() == player)
                    && (t.getSettlement() == null || t.getSettlement().getOwner() == player)) {
                    return t;
                }
            }
        }
    }


    /**
     * Write a minimal version of the tile.  Useful if the player
     * has not explored the tile.
     *
     * @param out The target stream.
     */
    public void toXMLMinimal(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("x", Integer.toString(x));
        out.writeAttribute("y", Integer.toString(y));
        out.writeAttribute("style", Integer.toString(style));
        if (moveToEurope != null) {
            out.writeAttribute("moveToEurope", Boolean.toString(moveToEurope));
        }
        out.writeEndElement();
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
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        if (!showAll) {
            if (toSavedGame) {
                logger.warning("toSavedGame is true, but showAll is false");
            }
            if (player == null) {
                logger.warning("player is null, but showAll is false");
            }
        }
        PlayerExploredTile pet = (showAll || toSavedGame) ? null
            : getPlayerExploredTile(player);

        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("x", Integer.toString(x));
        out.writeAttribute("y", Integer.toString(y));
        out.writeAttribute("style", Integer.toString(style));

        writeAttribute(out, "type", getType());
        writeAttribute(out, "region", getRegion());
        if (moveToEurope != null) {
            out.writeAttribute("moveToEurope", Boolean.toString(moveToEurope));
        }

        if (connected && !type.isConnected()) {
            out.writeAttribute("connected", Boolean.toString(true));
        }

        if (showAll || toSavedGame || player.canSee(this)) {
            if (owner != null) {
                out.writeAttribute("owner", owner.getId());
            }
            if (owningSettlement != null) {
                out.writeAttribute("owningSettlement",
                    owningSettlement.getId());
            }
        } else if (pet != null) {
            if (pet.getOwner() != null) {
                out.writeAttribute("owner", pet.getOwner().getId());
            }
            if (pet.getOwningSettlement() != null) {
                out.writeAttribute("owningSettlement",
                    pet.getOwningSettlement().getId());
            }
        }
        // End of attributes

        if (showAll || toSavedGame || player.canSee(this)) {
            if (settlement != null) {
                settlement.toXML(out, player, showAll, toSavedGame);
            }
            // Show enemy units if there is no enemy settlement.
            if ((showAll || toSavedGame || settlement == null
                    || settlement.getOwner() == player)
                && !units.isEmpty()) {
                out.writeStartElement(UNITS_TAG_NAME);
                for (Unit unit : units) {
                    unit.toXML(out, player, showAll, toSavedGame);
                }
                out.writeEndElement();
            }
        } else if (pet != null) {
            // Only display the settlement if we know it owns the tile
            // and we have a useful level of information about it.
            // This is a compromise, but something more precise is too
            // complex for the present.
            if (settlement != null
                && settlement == pet.getOwningSettlement()
                && settlement.getOwner() == pet.getOwner()
                && !(settlement instanceof Colony
                    && pet.getColonyUnitCount() <= 0)) {
                settlement.toXML(out, player, showAll, toSavedGame);
            }
        }
        if (tileItemContainer != null) {
            tileItemContainer.toXML(out, player, showAll, toSavedGame);
        }

        // Save the pets.
        if (toSavedGame && playerExploredTiles != null) {
            for (Entry<Player, PlayerExploredTile> entry
                     : playerExploredTiles.entrySet()) {
                entry.getValue().toXML(out, entry.getKey(),
                    showAll, toSavedGame);
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        Settlement oldSettlement = settlement;
        Player oldSettlementOwner = (settlement == null) ? null
            : settlement.getOwner();

        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        x = Integer.parseInt(in.getAttributeValue(null, "x"));
        y = Integer.parseInt(in.getAttributeValue(null, "y"));
        style = getAttribute(in, "style", 0);

        String typeString = in.getAttributeValue(null, "type");
        if (typeString != null) {
            type = getSpecification().getTileType(typeString);
        }

        // compatibility mode
        boolean needsRumour = getAttribute(in, LostCityRumour.getXMLElementTagName(), false);

        connected = getAttribute(in, "connected", false);
        owner = getFreeColGameObject(in, "owner", Player.class, null);
        region = getFreeColGameObject(in, "region", Region.class, null);
        moveToEurope = (in.getAttributeValue(null, "moveToEurope") == null)
            ? null
            : getAttribute(in, "moveToEurope", false);

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
            } else if (in.getLocalName().equals(PlayerExploredTile.getXMLElementTagName())) {
                // Only from a savegame:
                Player player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
                PlayerExploredTile pet = getPlayerExploredTile(player);
                if (pet == null) {
                    pet = new PlayerExploredTile(getGame(), in);
                    playerExploredTiles.put(player, pet);
                } else {
                    pet.readFromXML(in);
                }
            } else {
                logger.warning("Unknown tag: " + in.getLocalName()
                    + " [" + in.getAttributeValue(null, ID_ATTRIBUTE) + "] "
                    + " loading tile with ID " + getId());
                in.nextTag();
            }
        }

        // Player settlement list is not passed in player updates
        // so do it here.  TODO: something better.
        Player settlementOwner = (settlement == null) ? null
            : settlement.getOwner();
        if (settlement == null && oldSettlement != null) {
            // Settlement disappeared
            oldSettlement.setOwner(null);
            oldSettlementOwner.removeSettlement(oldSettlement);
        } else if (settlement != null && oldSettlement == null) {
            // Settlement appeared
            settlementOwner.addSettlement(settlement);
            owner = settlementOwner;
        } else if (settlementOwner != oldSettlementOwner) {
            // Settlement changed owner
            oldSettlement.setOwner(null);
            oldSettlementOwner.removeSettlement(oldSettlement);
            settlement.setOwner(settlementOwner);
            settlementOwner.addSettlement(settlement);
            owner = settlementOwner;
        }

        if (getColony() != null && getColony().isTileInUse(this)) {
            getColony().invalidateCache();
        }
    }

    /**
     * Fixes visible pets where there is a settlement present but the
     * tile is not owned correctly as ownership was not implemented in
     * 0.9.x.
     * Need to do this after reading the game so that canSee() is valid.
     * TODO: remove when 0.9.x is not supported.
     */
    public void fixup09x() {
        if (playerExploredTiles == null) return;
        for (Entry<Player, PlayerExploredTile> e
                 : playerExploredTiles.entrySet()) {
            Player p = e.getKey();
            PlayerExploredTile pet = e.getValue();
            if (settlement != null) {
                if (pet.getOwner() == null
                    || pet.getOwningSettlement() == null) {
                    if (p.canSee(this)) {
                        // Correct with an ordinary update
                        pet.update(false);
                    } else if (settlement instanceof Colony) {
                        if (pet.getColonyUnitCount() > 0) {
                            // Have seen the colony, update the ownership
                            // and the stockade level but not the unit count
                            // as that is the one that was seen.
                            pet.setOwner(settlement.getOwner());
                            pet.setOwningSettlement(settlement);
                            pet.setColonyStockadeKey(((Colony) settlement)
                                .getStockadeKey());
                        }
                    } else if (settlement instanceof IndianSettlement) {
                        // Unclear what has been seen, update just the ownership
                        pet.setOwner(settlement.getOwner());
                        pet.setOwningSettlement(settlement);
                    }
                }
            } else {
                if (pet.getOwningSettlement() != null
                    && pet.getOwner() == null) {
                    pet.setOwner(pet.getOwningSettlement().getOwner());
                }
            }
        }
    }

    /**
     * Returns a String representation of this Tile.
     *
     * @return A String representation of this Tile.
     */
    @Override
    public String toString() {
        return "Tile(" + x + "," + y +"):"
            + ((type == null) ? "unknown" : type.getId());
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
