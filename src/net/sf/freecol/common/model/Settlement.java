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

    /** The type of settlement. */
    private SettlementType type = null;

    /** The tiles this settlement owns. */
    private final List<Tile> ownedTiles = new ArrayList<Tile>();

    /** Contains the abilities and modifiers of this Settlement. */
    private final FeatureContainer featureContainer = new FeatureContainer();


    /**
     * Deliberately empty constructor for ServerColony.
     */
    protected Settlement() {}

    /**
     * Create a new <code>Settlement</code>.
     *
     * @param game The enclosing <code>Game</code>.
     * @param owner The owning <code>Player</code>.
     * @param name The settlement name.
     * @param tile The containing <code>Tile</code>.
     */
    public Settlement(Game game, Player owner, String name, Tile tile) {
        super(game);

        this.owner = owner;
        this.name = name;
        this.tile = tile;
        changeType(owner.getNationType().getSettlementType(false));
    }

    /**
     * Initiates a new <code>Settlement</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Settlement(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the type of this settlement.
     *
     * @return The settlement type.
     */
    public final SettlementType getType() {
        return type;
    }

    /**
     * Set the settlement type.
     *
     * @param newType The new <code>SettlementType</code>.
     */
    private final void setType(final SettlementType newType) {
        this.type = newType;
    }

    /**
     * Change the settlement type, setting the consequent features.
     *
     * @param newType The new <code>SettlementType</code>.
     */
    private final void changeType(final SettlementType newType) {
        if (type != null) removeFeatures(type);
        setType(newType);
        if (newType != null) addFeatures(newType);
    }

    /**
     * Get the name of this <code>Settlement</code>.
     *
     * @return The settlement name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this <code>Settlement</code>.
     *
     * @param newName The new name.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Is this a national capital?
     *
     * @return True if this is a national capital.
     */
    public boolean isCapital() {
        return getType().isCapital();
    }

    /**
     * Sets the capital value.
     *
     * @param capital The new capital value.
     */
    public void setCapital(boolean capital) {
        if (isCapital() != capital) {
            changeType(owner.getNationType().getSettlementType(capital));
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
     * Gets the radius of what the <code>Settlement</code> considers
     * as it's own land.
     *
     * @return Settlement radius
     */
    public int getRadius() {
        return getType().getClaimableRadius();
    }

    /**
     * Gets this settlement's line of sight value.
     *
     * @return The line of sight value.
     * @see Player#canSee(Tile)
     */
    public int getLineOfSight() {
        return (int)applyModifier((float)getType().getVisibleRadius(),
                                  "model.modifier.lineOfSightBonus");
    }

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
     * Put a prepared settlement onto the map.
     *
     * @param maximal If true, also claim all the tiles possible.
     */
    public void placeSettlement(boolean maximal) {
        List<Tile> tiles;
        if (maximal) {
            tiles = owner.getClaimableTiles(tile, getRadius());
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

        getGame().notifyOwnerChanged(this, oldOwner, newOwner);
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
     * Gets the minimum high seas count of the adjacent high-seas-connected
     * tiles.  This is a measure of how close this settlement is to Europe.
     *
     * @return A high seas count, INFINITY if not connected.
     */
    public int getHighSeasCount() {
        int best = INFINITY;
        for (Tile t : getTile().getSurroundingTiles(1)) {
            if (t.isLand() || t.getHighSeasCount() < 0) continue;
            if (best > t.getHighSeasCount()) {
                best = t.getHighSeasCount();
            }
        }
        return best;
    }
        
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
        for (AbstractGoods ag : equipmentType.getRequiredGoods()) {
            if (getGoodsCount(ag.getType()) < ag.getAmount()) return false;
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
        for (AbstractGoods ag : equipmentType.getRequiredGoods()) {
            int available = getGoodsCount(ag.getType());

            int breedingNumber = ag.getType().getBreedingNumber();
            if (breedingNumber != GoodsType.INFINITY) {
                available -= breedingNumber;
            }
            if (available < ag.getAmount()) return false;
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
     * Gets the storage capacity of this settlement.
     *
     * @return The storage capacity of this settlement.
     * @see #getGoodsCapacity
     */
    public int getWarehouseCapacity() {
        return getGoodsCapacity();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
            owner.removeSettlement(this);
            owner.invalidateCanSeeTiles();
            // It is not safe to setOwner(null).  When a settlement is
            // destroyed there is a race between this code and some
            // display routines that still need to know who owned the
            // dead settlement.
        }

        objects.addAll(super.disposeList());
        return objects;
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    public String getNameKey() {
        return getName();
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    public void setOwner(Player player) {
        this.owner = player;
    }


    // Interface Location (from GoodsLocation via UnitLocation)
    // Inherits
    //   FreeColObject.getId
    //   UnitLocation.getLocationNameFor
    //   GoodsLocation.add
    //   GoodsLocation.remove
    //   GoodsLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   UnitLocation.getUnitList
    //   UnitLocation.getColony

    /**
     * {@inheritDoc}
     */
    @Override
    public final Tile getTile() {
        return tile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationName() {
        return StringTemplate.name(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Settlement getSettlement() {
        return this;
    }

    // Interface UnitLocation
    // Inherits
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.getUnitCapacity

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        if (locatable instanceof Unit) {
            // Tighter ownership test now possible.
            if (((Unit)locatable).getOwner() != getOwner()) {
                return NoAddReason.OWNED_BY_ENEMY;
            }
        } else if (locatable instanceof Goods) {
            // Goods can always be added to settlements.  Any
            // excess Goods will be removed during end-of-turn
            // processing.
            return NoAddReason.NONE;
        }
        return super.getNoAddReason(locatable);
    }
  
    // Interface GoodsLocation
    // Inherits
    //   GoodsLocation.addGoods
    //   GoodsLocation.removeGoods

    // No need to implement abstract getGoodsCapacity here, yet.


    // Settlement routines to be implemented by subclasses.

    /**
     * Gets an image key for this settlement.
     *
     * @return An image key
     */
    abstract public String getImageKey();

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Settlement</code>.
     *
     * @param attacker The <code>Unit</code> that is attacking this
     *     <code>Settlement</code>.
     * @return The <code>Unit</code> that has been chosen to defend
     *     this <code>Settlement</code>.
     */
    abstract public Unit getDefendingUnit(Unit attacker);

    /**
     * Get the ratio between defence at this settlement, and the
     * general settlement size.
     *
     * @return A ratio of defence power to settlement size.
     */
    abstract public float getDefenceRatio();

    /**
     * Gets the range of gold plunderable when this settlement is captured.
     *
     * @param attacker The <code>Unit</code> that takes the settlement.
     * @return A <code>RandomRange</code> encapsulating the range of plunder
     *     available.
     */
    abstract public RandomRange getPlunderRange(Unit attacker);

    /**
     * Gets the current Sons of Liberty in this settlement.
     *
     * @return The current SoL.
     */
    abstract public int getSoL();

    /**
     * Get the amount of gold necessary to maintain all of the
     * settlement's buildings.
     *
     * @return The gold required for upkeep.
     */
    abstract public int getUpkeep();

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
    abstract public boolean propagateAlarm(Player player, int addToAlarm);

    /**
     * Gets the total production of the given type of goods in this settlement.
     *
     * @param goodsType The type of goods to get the production for.
     * @return The total production of the given type of goods.
     */
    abstract public int getTotalProductionOf(GoodsType goodsType);


    // Serialization

    private static final String NAME_TAG = "name";
    private static final String OWNER_TAG = "owner";
    private static final String SETTLEMENT_TYPE_TAG = "settlementType";
    private static final String TILE_TAG = "tile";
    // @compat 0.9.x
    private static final String IS_CAPITAL_TAG = "isCapital";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, NAME_TAG, getName());

        // TODO: Not owner, it is subject to PlayerExploredTile handling?
        writeAttribute(out, OWNER_TAG, owner);

        writeAttribute(out, TILE_TAG, tile);

        writeAttribute(out, SETTLEMENT_TYPE_TAG, getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        name = getAttribute(in, NAME_TAG, (String)null);

        owner = makeFreeColGameObject(in, OWNER_TAG, Player.class);

        tile = makeFreeColGameObject(in, TILE_TAG, Tile.class);

        String str = getAttribute(in, SETTLEMENT_TYPE_TAG, (String)null);
        SettlementType settlementType;
        // @compat 0.9.x
        if (str == null) {
            boolean capital = getAttribute(in, IS_CAPITAL_TAG, false);
            settlementType = owner.getNationType().getSettlementType(capital);
        // end @compat
        } else {
            settlementType = owner.getNationType().getSettlementType(str);
        }
        changeType(settlementType);
    }
}
