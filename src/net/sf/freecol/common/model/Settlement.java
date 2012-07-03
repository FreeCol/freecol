/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.UnitTypeChange.ChangeType;


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

    /** Contains the abilities and modifiers of this Settlement. */
    private final FeatureContainer featureContainer = new FeatureContainer();

    /** The tiles this settlement owns. */
    private List<Tile> ownedTiles = new ArrayList<Tile>();

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

        setType(owner.getNationType().getSettlementType(false));
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
        if (type != null) removeFeatures(type);
        this.type = newType;
        if (newType != null) addFeatures(newType);
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
     * Get this settlement's feature container.
     *
     * @return The <code>FeatureContainer</code>.
     */
    @Override
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }


    /**
     * Gets this colony's line of sight.
     * @return The line of sight offered by this
     *       <code>Colony</code>.
     * @see Player#canSee(Tile)
     */
    public int getLineOfSight() {
        return (int)applyModifier((float)getType().getVisibleRadius(),
                                  "model.modifier.lineOfSightBonus");
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
     *
     * @return A list of tiles.
     */
    public List<Tile> getOwnedTiles() {
        return new ArrayList<Tile>(ownedTiles);
    }

    /**
     * Adds a tile to this settlement.
     *
     * @param tile The <code>Tile</code> to add.
     */
    public void addTile(Tile tile) {
        ownedTiles.add(tile);
    }

    /**
     * Removes a tile from this settlement.
     *
     * @param tile The <code>Tile</code> to remove.
     */
    public void removeTile(Tile tile) {
        ownedTiles.remove(tile);
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
     * Gets whether this settlement is connected to the high seas.
     * This is more than merely non-landlocked, because the settlement
     * could be on an inland lake.
     *
     * @return True if the settlement is connected to the high seas.
     */
    public boolean isConnectedPort() {
        for (Tile t : getTile().getSurroundingTiles(1)) {
            if (!t.isLand() && t.isHighSeasConnected()) return true;
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
     * @param equipmentType The <code>EquipmentType</code> to build.
     * @return True if the equipment can be built.
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
     * Determines if this settlement can build the given type of equipment.
     * Unlike canBuildEquipment, this takes goods "reserved"
     * for other purposes into account (e.g. breeding).
     *
     * @param equipmentType an <code>EquipmentType</code> value
     * @return True if the settlement can provide the equipment.
     * @see Settlement#canBuildEquipment(EquipmentType equipmentType)
     */
    public boolean canProvideEquipment(EquipmentType equipmentType) {
        for (AbstractGoods goods : equipmentType.getGoodsRequired()) {
            int available = getGoodsCount(goods.getType());

            int breedingNumber = goods.getType().getBreedingNumber();
            if (breedingNumber != GoodsType.INFINITY) {
                available -= breedingNumber;
            }

            if (available < goods.getAmount()) return false;
        }
        return true;
    }

    /**
     * Return true if this Settlement could provide at least one item of
     * all the given EquipmentTypes.  This is designed specifically to
     * mesh with getRoleEquipment().
     *
     * @param equipment A list of <code>EquipmentType</code>s to build.
     * @return True if the settlement can provide all the equipment.
     */
    public boolean canProvideEquipment(List<EquipmentType> equipment) {
        for (EquipmentType e : equipment) {
            if (!canProvideEquipment(e)) return false;
        }
        return true;
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
        // TODO: Not owner, it is subject to PlayerExploredTile handling.
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

        // @compat 0.9.x
        String typeStr = in.getAttributeValue(null, "settlementType");
        SettlementType settlementType;
        if (typeStr == null) {
            String capital = in.getAttributeValue(null, "isCapital");
            settlementType = owner.getNationType()
                .getSettlementType("true".equals(capital));
        // end compatibility code
        } else {
            settlementType = owner.getNationType().getSettlementType(typeStr);
        }
        setType(settlementType);
    }
}
