/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.GoodsLocation;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;


/**
 * The super class of all settlements on the map (that is colonies and
 * indian settlements).
 */
public abstract class Settlement extends GoodsLocation
    implements Nameable, Ownable {

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
     * {@link #readFromXML(FreeColXMLReader)} or
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
     * We can not clear the settlement owned tiles container when the
     * settlement is read because this is called when the Tile is
     * read, and tiles can appear before and after the settlement in
     * the map definition.  So we just accumulate and defend against
     * duplicates.
     *
     * @param tile The <code>Tile</code> to add.
     */
    public void addTile(Tile tile) {
        if (!ownedTiles.contains(tile)) ownedTiles.add(tile);
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
     */
    public int getLineOfSight() {
        return (int)applyModifier((float)getType().getVisibleRadius(),
                                  Modifier.LINE_OF_SIGHT_BONUS);
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
     * -vis: Several visibility issues accumulated here.
     * -til: Several tile appearance issues accumulated here.
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

        tile.setSettlement(this);//-vis(owner),-til
        for (Tile t : tiles) {
            t.changeOwnership(owner, this);//-vis(owner,this),-til
        }
        if (this instanceof Colony && !tile.hasRoad()) {
            TileImprovement road = tile.addRoad();
            road.setTurnsToComplete(0);
            road.setVirtual(true);
            road.updateRoadConnections(true);
        }
    }

    /**
     * Remove a settlement from the map.
     *
     * -vis: Visibility reduced when settlement goes away.
     * -til: Several tile appearance issues accumulated here.
     *
     * Several visibility issues accumulated here.
     */
    public void exciseSettlement() {
        Tile settlementTile = getTile();
        List<Tile> lostTiles = getOwnedTiles();
        for (Tile tile : lostTiles) {
            tile.changeOwnership(null, null);//-til
        }
        settlementTile.setSettlement(null);//-vis(owner),-til
        settlementTile.changeOwnership(null, null);//-til
        TileImprovement road = settlementTile.getRoad();
        if (road != null && road.isVirtual()) {
            settlementTile.removeRoad();//-til
        }
    }

    /**
     * Change the owner of this <code>Settlement</code>.
     *
     * -vis: Changes visibility.
     * -til: Changes tile appearance.
     *
     * @param newOwner The <code>Player</code> that shall own this
     *            <code>Settlement</code>.
     * @see #getOwner
     */
    public void changeOwner(Player newOwner) {
        Player oldOwner = this.owner;
        setOwner(newOwner);//-til

        getGame().checkOwners(this, oldOwner);

        ChangeType change = (newOwner.isUndead()) ? ChangeType.UNDEAD
            : ChangeType.CAPTURE;
        List<Unit> units = getUnitList();
        units.addAll(getTile().getUnitList());
        while (!units.isEmpty()) {
            Unit u = units.remove(0);
            units.addAll(u.getUnitList());
            u.setState(Unit.UnitState.ACTIVE);
            UnitType type = u.getTypeChange(change, newOwner);
            if (type != null) u.setType(type);//-vis(newOwner)
            u.changeOwner(newOwner);//-vis(oldOwner,newOwner)
        }

        for (Tile t : getOwnedTiles()) {
            t.changeOwnership(newOwner, this);//-til
        }

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
     * Determines if this settlement can build the given type of equipment.
     * Unlike canBuildEquipment, this takes goods "reserved"
     * for other purposes into account (e.g. breeding).
     *
     * @param goods A list of <code>AbstractGoods</code>
     * @return True if the settlement can provide the equipment.
     * @see Settlement#canBuildEquipment(EquipmentType equipmentType)
     */
    public boolean canProvideEquipment(List<AbstractGoods> goods) {
        for (AbstractGoods ag : goods) {
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
    /*
    public boolean canProvideEquipment(List<EquipmentType> equipment) {
        for (EquipmentType e : equipment) {
            if (!canProvideEquipment(e)) return false;
        }
        return true;
    }
    */

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
     *
     * -vis: Visibility changes when the settlement is removed.
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        if (owner != null) {
            owner.removeSettlement(this);
            // It is not safe to setOwner(null).  When a settlement is
            // destroyed there is a race between this code and some
            // display routines that still need to know who owned the
            // dead settlement.
        }

        objects.addAll(super.disposeList());
        return objects;
    }


    // Interface Nameable

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
     * -til: Changes the tile appearance.
     *
     * @param newName The new name.
     */
    public void setName(String newName) {
        this.name = newName;
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
     *
     * -vis: Changes visibility.
     * -til: Changes tile appearance.
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canBuildEquipment(EquipmentType equipmentType, int amount) {
        for (AbstractGoods ag : equipmentType.getRequiredGoods()) {
            if (getGoodsCount(ag.getType()) < ag.getAmount() * amount) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int canBuildRoleEquipment(Role role) {
        for (AbstractGoods ag : role.getRequiredGoods()) {
            if (getGoodsCount(ag.getType()) < ag.getAmount()) return -1;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equipForRole(Unit unit, Role role) {
        if (!getGame().isInServer()) {
            throw new RuntimeException("Must be in server");
        }
        int price = canBuildRoleEquipment(role);
        if (price < 0 || !owner.checkGold(price)) return false;
        // Process adding equipment first, so as to settle what has to
        // be removed.
        List<EquipmentType> remove = null;
        for (EquipmentType et : getSpecification().getRoleEquipment(role.getId())) {
            for (AbstractGoods ag : et.getRequiredGoods()) {
                removeGoods(ag);
            }
            for (EquipmentType rt : unit.changeEquipment(et, 1)) {
                int a = unit.getEquipmentCount(rt);
                for (AbstractGoods ag : rt.getRequiredGoods()) {
                    addGoods(ag.getType(), ag.getAmount() * a);
                }
                unit.changeEquipment(rt, -a);
            }
        }
        unit.setRole();
        return unit.getRole() == role;
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
    public abstract String getImageKey();

    /**
     * Gets the <code>Unit</code> that is currently defending this
     * <code>Settlement</code>.
     *
     * @param attacker The <code>Unit</code> that is attacking this
     *     <code>Settlement</code>.
     * @return The <code>Unit</code> that has been chosen to defend
     *     this <code>Settlement</code>.
     */
    public abstract Unit getDefendingUnit(Unit attacker);

    /**
     * Get the ratio between defence at this settlement, and the
     * general settlement size.
     *
     * @return A ratio of defence power to settlement size.
     */
    public abstract float getDefenceRatio();

    /**
     * Gets the range of gold plunderable when this settlement is captured.
     *
     * @param attacker The <code>Unit</code> that takes the settlement.
     * @return A <code>RandomRange</code> encapsulating the range of plunder
     *     available.
     */
    public abstract RandomRange getPlunderRange(Unit attacker);

    /**
     * Gets the current Sons of Liberty in this settlement.
     *
     * @return The current SoL.
     */
    public abstract int getSoL();

    /**
     * Get the amount of gold necessary to maintain all of the
     * settlement's buildings.
     *
     * @return The gold required for upkeep.
     */
    public abstract int getUpkeep();

    /**
     * Gets the total production of the given type of goods in this settlement.
     *
     * @param goodsType The type of goods to get the production for.
     * @return The total production of the given type of goods.
     */
    public abstract int getTotalProductionOf(GoodsType goodsType);


    // Serialization

    private static final String NAME_TAG = "name";
    private static final String OWNER_TAG = "owner";
    private static final String SETTLEMENT_TYPE_TAG = "settlementType";
    private static final String TILE_TAG = "tile";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NAME_TAG, getName());

        xw.writeAttribute(OWNER_TAG, owner);

        xw.writeAttribute(TILE_TAG, tile);

        xw.writeAttribute(SETTLEMENT_TYPE_TAG, getType());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (xw.validFor(getOwner())) {

            // Settlement contents only visible to the owner by default.
            super.writeChildren(xw);

            for (Ability ability : getSortedAbilities()) {
                ability.toXML(xw);
            }

            for (Modifier modifier : getSortedModifiers()) {
                modifier.toXML(xw);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Game game = getGame();

        name = xr.getAttribute(NAME_TAG, (String)null);

        Player oldOwner = owner;
        owner = xr.findFreeColGameObject(game, OWNER_TAG,
                                         Player.class, (Player)null, true);
        if (xr.shouldIntern()) game.checkOwners(this, oldOwner);

        tile = xr.findFreeColGameObject(game, TILE_TAG,
                                        Tile.class, (Tile)null, true);

        String type = xr.getAttribute(SETTLEMENT_TYPE_TAG, (String)null);
        changeType(owner.getNationType().getSettlementType(type));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        featureContainer.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (Ability.getXMLElementTagName().equals(tag)) {
            addAbility(new Ability(xr, spec));

        } else if (Modifier.getXMLElementTagName().equals(tag)) {
            addModifier(new Modifier(xr, spec));

        } else {
            super.readChild(xr);
        }
    }
}
