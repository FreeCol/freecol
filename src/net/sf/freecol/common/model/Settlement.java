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
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;

import org.w3c.dom.Element;


/**
 * The super class of all settlements on the map (that is colonies and
 * indian settlements).
 */
abstract public class Settlement extends GoodsLocation
    implements Named, Ownable {

    private static final Logger logger = Logger.getLogger(Settlement.class.getName());

    public static final int FOOD_PER_COLONIST = 200;

    /** The <code>Player</code> owning this <code>Settlement</code>. */
    protected Player owner;

    /** The name of the Settlement. */
    private String name;

    /** The <code>Tile</code> where this <code>Settlement</code> is located. */
    protected Tile tile;

    /**
     * Contains the abilities and modifiers of this Colony.
     */
    private FeatureContainer featureContainer;

    /**
     * Describe type here.
     */
    private SettlementType type;


    /**
     * Empty constructor needed for Colony -> ServerColony.
     */
    protected Settlement() {
        // empty constructor
    }

    /**
     * Creates a new <code>Settlement</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param owner The owner of this <code>Settlement</code>.
     * @param name The name for this <code>Settlement</code>.
     * @param tile The location of the <code>Settlement</code>.
     */
    public Settlement(Game game, Player owner, String name, Tile tile) {
        super(game);
        this.owner = owner;
        this.name = name;
        this.tile = tile;

        featureContainer = new FeatureContainer();
        setType(owner.getNationType().getSettlementType(false));

        owner.addSettlement(this);
    }



    /**
     * Initiates a new <code>Settlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Settlement(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
    }

    /**
     * Initiates a new <code>Settlement</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public Settlement(Game game, Element e) {
        super(game, e);
    }

    /**
     * Initiates a new <code>Settlement</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Settlement(Game game, String id) {
        super(game, id);
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>SettlementType</code> value
     */
    public final SettlementType getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final SettlementType newType) {
        if (type != null) {
            featureContainer.remove(type.getFeatureContainer());
        }
        this.type = newType;
        if (newType != null) {
            featureContainer.add(newType.getFeatureContainer());
        }
    }

    public Set<Modifier> getModifierSet(String key) {
        return featureContainer.getModifierSet(key);
    }

    // TODO: remove this again
    public String getNameKey() {
        return getName();
    }

    /**
     * Gets the name of this <code>Settlement</code>.
     *
     * @return The name as a <code>String</code>.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the name of this <code>Settlement</code> for a particular player.
     *
     * @param player A <code>Player</code> to return the name for.
     * @return The name as a <code>String</code>.
     */
    abstract public String getNameFor(Player player);

    /**
     * Sets the name of this <code>Settlement</code>.
     *
     * @param newName The new name.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Gets an image key for this settlement.
     */
    abstract public String getImageKey();

     /**
     * Returns <code>true</code> if this is the Nation's capital.
     *
     * @return <code>true</code> if this is the Nation's capital.
     */
    public boolean isCapital() {
        return getType().isCapital();
    }

     /**
     * Sets the capital value.
     *
     * @param isCapital a <code>boolean</code> value
     */
    public void setCapital(boolean isCapital) {
        if (isCapital() != isCapital) {
            setType(owner.getNationType().getSettlementType(isCapital));
        }
    }


    /**
     * Describe <code>getFeatureContainer</code> method here.
     *
     * @return a <code>FeatureContainer</code> value
     */
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    /**
     * Describe <code>setFeatureContainer</code> method here.
     *
     * @param container a <code>FeatureContainer</code> value
     */
    protected void setFeatureContainer(FeatureContainer container) {
        featureContainer = container;
    }

    /**
     * Gets this colony's line of sight.
     * @return The line of sight offered by this
     *       <code>Colony</code>.
     * @see Player#canSee(Tile)
     */
    public int getLineOfSight() {
        return getType().getVisibleRadius();
    }


    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Settlement</code>.
     *
     * @param attacker The unit be attacking this <code>Settlement</code>.
     * @return The <code>Unit</code> that has been chosen to defend
     * this <code>Settlement</code>.
     */
    abstract public Unit getDefendingUnit(Unit attacker);

    /**
     * Gets the range of gold plunderable when this settlement is captured.
     *
     * @param attacker The <code>Unit</code> that takes the settlement.
     * @return A <code>RandomRange</code> encapsulating the range of plunder
     *     available.
     */
    abstract public RandomRange getPlunderRange(Unit attacker);

    /**
     * Gets an amount of plunder when this settlement is taken.
     *
     * @param attacker The <code>Unit</code> that takes the settlement.
     * @param random A pseudo-random number source.
     * @return An amount of gold plundered.
     */
    public int getPlunder(Unit attacker, Random random) {
        RandomRange range = getPlunderRange(attacker);
        return (range == null) ? 0
            : range.getAmount("Plunder " + getName(), random, false);
    }

    /**
     * Gets the <code>Tile</code> where this <code>Settlement</code> is located.
     * @return The <code>Tile</code> where this <code>Settlement</code> is located.
     */
    public Tile getTile() {
        return tile;
    }

    /**
     * Returns this Settlement.
     *
     * @return this Settlement
     */
    public Settlement getSettlement() {
        return this;
    }

    /**
     * Put a prepared settlement onto the map.
     *
     * @param maximal If true, also claim all the tiles possible.
     */
    public void placeSettlement(boolean maximal) {
        List<Tile> tiles;
        if (maximal) {
            tiles = getGame().getMap()
                .getClaimableTiles(owner, tile, getRadius());
        } else {
            tiles = new ArrayList<Tile>();
            tiles.add(tile);
        }

        tile.setSettlement(this);
        for (Tile t : tiles) {
            t.changeOwnership(owner, this);
        }
        for (Tile t : tile.getSurroundingTiles(getLineOfSight())) {
            owner.setExplored(t);
        }
        owner.invalidateCanSeeTiles();
    }

    /**
     * Gets the owner of this <code>Settlement</code>.
     *
     * @return The owner of this <code>Settlement</code>.
     * @see #setOwner
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this <code>Settlement</code>.
     *
     * @param player The new owner of this <code>Settlement</code>.
     */
    public void setOwner(Player player) {
        owner = player;
    }

    /**
     * Change the owner of this <code>Settlement</code>.
     *
     * @param newOwner The <code>Player</code> that shall own this
     *            <code>Settlement</code>.
     * @see #getOwner
     */
    public void changeOwner(Player newOwner) {
        Player oldOwner = this.owner;
        setOwner(newOwner);

        if (oldOwner.hasSettlement(this)) {
            oldOwner.removeSettlement(this);
        }
        if (!newOwner.hasSettlement(this)) {
            newOwner.addSettlement(this);
        }

        List<Unit> units = getUnitList();
        units.addAll(getTile().getUnitList());
        while (!units.isEmpty()) {
            Unit u = units.remove(0);
            units.addAll(u.getUnitList());
            u.setState(Unit.UnitState.ACTIVE);
            UnitType type = u.getTypeChange((newOwner.isUndead())
                                            ? ChangeType.UNDEAD
                                            : ChangeType.CAPTURE, newOwner);
            if (type != null) u.setType(type);
            u.setOwner(newOwner);
        }

        for (Tile t : getOwnedTiles()) {
            t.changeOwnership(newOwner, this);
        }
        oldOwner.invalidateCanSeeTiles();
        newOwner.invalidateCanSeeTiles();

        if (getGame().getFreeColGameObjectListener() != null) {
            getGame().getFreeColGameObjectListener()
                .ownerChanged(this, oldOwner, newOwner);
        }
    }

    /**
     * Get the tiles this settlement owns.
     * Exploits the fact that the map generator only grows connected tiles.
     *
     * @return A list of tiles.
     */
    public List<Tile> getOwnedTiles() {
        Tile settlementTile = getTile();
        List<Tile> tiles = new ArrayList<Tile>();
        tiles.add(settlementTile);
        for (Tile t : settlementTile.getSurroundingTiles(getRadius())) {
            if (t.getOwningSettlement() == this) tiles.add(t);
        }
        List<Tile> todo = new ArrayList<Tile>(tiles);
        while (!todo.isEmpty()) {
            Tile t = todo.remove(0);
            for (Tile s : t.getSurroundingTiles(1)) {
                if (s.getOwningSettlement() == this && !tiles.contains(s)) {
                    tiles.add(s);
                    todo.add(s);
                }
            }
        }
        return tiles;
    }

    /**
     * Dispose of this settlement.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        if (owner != null
            && getTile() != null
            && getTile().getSettlement() != null) {
            // Defensive tests to handle transition from calling dispose()
            // on both sides to when it is only called on server-side.

            // Get off the map
            Tile settlementTile = getTile();
            List<Tile> lostTiles = getOwnedTiles();
            for (Tile tile : lostTiles) {
                tile.changeOwnership(null, null);
            }
            settlementTile.setSettlement(null);

            // The owner forgets about the settlement.
            Player oldOwner = owner;
            setOwner(null);
            oldOwner.removeSettlement(this);
            oldOwner.invalidateCanSeeTiles();
        }

        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.
     *
     * @return Settlement radius
     */
    public int getRadius() {
        return getType().getClaimableRadius();
    }

    /**
     * Returns whether this settlement is connected by water to Europe.
     *
     * @return <code>true</code> if this <code>Settlement</code> is connected
     *         to Europe.
     */
    public boolean isConnected() {
        for (Tile t : getTile().getSurroundingTiles(1)) {
            if (t.isExplored() && t.getType().isWater()
                && t.isConnected()) return true;
        }
        return false;
    }

    /**
     * Gets the current Sons of Liberty in this settlement.
     */
    public abstract int getSoL();

    /**
     * Propagates a global change in tension down to a settlement.
     * Only apply the change if the settlement is aware of the player
     * causing alarm.
     *
     * @param player The <code>Player</code> towards whom the alarm is felt.
     * @param addToAlarm The amount to add to the current alarm level.
     * @return True if the settlement alarm level changes as a result
     *     of this change.
     */
    public abstract boolean propagateAlarm(Player player, int addToAlarm);

    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType The type of goods to get the production for.
     * @return The production of the given type of goods the current turn by the
     * <code>Settlement</code>
     */
    public abstract int getProductionOf(GoodsType goodsType);

    /**
     * Returns the number of goods of a given type used by the settlement
     * each turn.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
     */
    public int getConsumptionOf(GoodsType goodsType) {
        int result = 0;
        for (Unit unit : getUnitList()) {
            result += unit.getType().getConsumptionOf(goodsType);
        }
        return Math.max(0, result);
    }

    /**
     * Returns the number of goods of all given types used by the
     * settlement each turn.
     *
     * @param goodsTypes <code>GoodsType</code> values
     * @return an <code>int</code> value
     */
    public int getConsumptionOf(List<GoodsType> goodsTypes) {
        int result = 0;
        if (goodsTypes != null) {
            for (GoodsType goodsType : goodsTypes) {
                result += getConsumptionOf(goodsType);
            }
        }
        return result;
    }


    /**
     * Gives the food needed to keep all units alive in this Settlement.
     *
     * @return The amount of food eaten in this colony each this turn.
     */
    public int getFoodConsumption() {
        return getConsumptionOf(getSpecification().getFoodGoodsTypeList());
    }

    /**
     * Return true if this Colony could build at least one item of the
     * given EquipmentType.
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean canBuildEquipment(EquipmentType equipmentType) {
        for (AbstractGoods requiredGoods : equipmentType.getGoodsRequired()) {
            if (getGoodsCount(requiredGoods.getType()) < requiredGoods.getAmount()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        setName(in.getAttributeValue(null, "name"));
        owner = getFreeColGameObject(in, "owner", Player.class);
        tile = getFreeColGameObject(in, "tile", Tile.class);
        featureContainer = new FeatureContainer();

        // TODO: remove 0.9.x compatibility code
        String typeStr = in.getAttributeValue(null, "settlementType");
        SettlementType settlementType;
        if (typeStr == null) {
            // must be old style
            String capital = in.getAttributeValue(null, "isCapital");
            settlementType = owner.getNationType()
                .getSettlementType("true".equals(capital));
        } else {
            settlementType = owner.getNationType().getSettlementType(typeStr);
        }
        // end compatibility code
        setType(settlementType);
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("name", getName());
        out.writeAttribute("owner", owner.getId());
        out.writeAttribute("tile", tile.getId());
        out.writeAttribute("settlementType", getType().getId());
        // Not owner, it is subject to PlayerExploredTile handling.
    }
}
