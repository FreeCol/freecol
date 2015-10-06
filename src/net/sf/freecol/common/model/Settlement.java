/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


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
    private final Set<Tile> ownedTiles = new HashSet<>();

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
    public void setType(final SettlementType newType) {
        this.type = newType;
    }

    /**
     * Is this settlement landlocked?
     *
     * @return True if no adjacent tiles are water.
     */
    public boolean isLandLocked() {
        return tile.isLandLocked();
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
    public Set<Tile> getOwnedTiles() {
        return new HashSet<>(ownedTiles);
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
        return (int)applyModifiers((float)getType().getVisibleRadius(),
            getGame().getTurn(), Modifier.LINE_OF_SIGHT_BONUS);
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
            tiles = new ArrayList<>();
            tiles.add(tile);
        }

        tile.setSettlement(this);//-vis(owner),-til
        for (Tile t : tiles) {
            t.changeOwnership(owner, this);//-vis(owner,this),-til
        }
        if (!tile.hasRoad()) {
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
        for (Tile tile : getOwnedTiles()) {
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
     * Does not fix up the units!  That is handled in the server.
     *
     * -vis: Changes visibility.
     * -til: Changes tile appearance.
     *
     * @param newOwner The <code>Player</code> that shall own this
     *            <code>Settlement</code>.
     * @see #getOwner
     */
    public void changeOwner(Player newOwner) {
        final Player oldOwner = this.owner;
        if (newOwner.isIndian() != oldOwner.isIndian()) {
            throw new IllegalArgumentException("Can not transfer settlements between native and European players.");
        }
        setOwner(newOwner);//-til,-vis

        getGame().checkOwners(this, oldOwner);

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
        return any(getTile().getSurroundingTiles(1, 1),
            t -> !t.isLand() && t.isHighSeasConnected());
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
        return Math.max(0, getUnitList().stream()
                .mapToInt(u -> u.getType().getConsumptionOf(goodsType)).sum());
    }

    /**
     * Returns the number of goods of all given types used by the
     * settlement each turn.
     *
     * @param goodsTypes <code>GoodsType</code> values
     * @return an <code>int</code> value
     */
    public int getConsumptionOf(List<GoodsType> goodsTypes) {
        return (goodsTypes == null) ? 0
            : goodsTypes.stream()
                .mapToInt(gt -> getConsumptionOf(gt)).sum();
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
     * Determines if this settlement can build the given type of
     * equipment.  Unlike priceGoods, this takes goods "reserved" for
     * other purposes into account (e.g. breeding).
     *
     * @param goods A list of <code>AbstractGoods</code>
     * @return True if the settlement can provide the equipment.
     */
    public boolean canProvideGoods(List<AbstractGoods> goods) {
        return all(goods, ag -> {
                int available = getGoodsCount(ag.getType());
                int breedingNumber = ag.getType().getBreedingNumber();
                if (breedingNumber != GoodsType.INFINITY) {
                    available -= breedingNumber;
                }
                return available >= ag.getAmount();
            });
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

    /**
     * Check if colony has the ability to bombard an enemy ship
     * adjacent to it.  Only sea-side colonies can bombard.  Does it
     * have the buildings that give such abilities?
     *
     * @return True if bombarding is allowed.
     */
    public boolean canBombardEnemyShip() {
        return (isLandLocked()) ? false
            : hasAbility(Ability.BOMBARD_SHIPS);
    }

    /**
     * Can this settlement provide the goods to improve a given unit's
     * role?
     *
     * @param unit The <code>Unit</code> to check.
     * @return The <code>Role</code> that this settlement could provide.
     */
    public Role canImproveUnitMilitaryRole(Unit unit) {
        final Specification spec = getSpecification();
        final Role role = unit.getRole();

        // Get the military roles that are superior to the current role
        List<Role> military = spec.getMilitaryRoles();
        int index = military.indexOf(role);
        if (index >= 0) military = military.subList(0, index);

        // To succeed, there must exist an available role for the unit
        // where the extra equipment for the role is present.
        return find(unit.getAvailableRoles(military),
            r -> canProvideGoods(unit.getGoodsDifference(r, 1)));
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColGameObject getLinkTarget(Player player) {
        return (player == getOwner()) ? this : getTile();
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
    public void disposeResources() {
        if (owner != null) {
            owner.removeSettlement(this);
            // It is not safe to setOwner(null).  When a settlement is
            // destroyed there is a race between this code and some
            // display routines that still need to know who owned the
            // dead settlement.
        }
        super.disposeResources();
    }


    // Interface Nameable

    /**
     * Get the name of this <code>Settlement</code>.
     *
     * @return The settlement name.
     */
    @Override
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
    @Override
    public void setName(String newName) {
        this.name = newName;
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     *
     * -vis: Changes visibility.
     * -til: Changes tile appearance.
     */
    @Override
    public void setOwner(Player player) {
        this.owner = player;
    }


    // Interface Location (from GoodsLocation via UnitLocation)
    // Inherits
    //   FreeColObject.getId
    //   UnitLocation.getLocationFor
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
    public StringTemplate getLocationLabel() {
        return StringTemplate.name(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Settlement getSettlement() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getRank() {
        return Location.getRank(getTile());
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
    public int priceGoods(List<AbstractGoods> goods) {
        return (any(goods,
                ag -> getGoodsCount(ag.getType()) < ag.getAmount())) ? -1
            : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equipForRole(Unit unit, Role role, int roleCount) {
        if (!unit.roleIsAvailable(role)) return false;

        // Get the change in goods
        List<AbstractGoods> required = unit.getGoodsDifference(role, roleCount);

        // Check if the required goods are available
        if (priceGoods(required) < 0) return false;

        // Make the change
        for (AbstractGoods ag : required) {
            addGoods(ag.getType(), -ag.getAmount());
        }

        unit.changeRole(role, roleCount);
        return true;
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
    public abstract double getDefenceRatio();

    /**
     * Is this settlement insufficiently defended?
     *
     * @return True if this settlement needs more defence.
     */
    public abstract boolean isBadlyDefended();
        
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

    /**
     * Has this settlement contacted a given player?
     *
     * Allow player == null as this is true in the map editor where
     * the user player is moot.
     *
     * @param player The other <code>Player</code> to check.
     * @return True if the settlement has contacted the player.
     */
    public abstract boolean hasContacted(Player player);

    /**
     * Gets a label indicating the alarm level at this settlement with
     * respect to another player.
     *
     * @param player The other <code>Player</code>.
     * @return A <code>StringTemplate</code> describing the alarm.
     */
    public abstract StringTemplate getAlarmLevelLabel(Player player);

    

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

        // Delegate writing of name to subclass, as it is not
        // available for uncontacted native settlements.

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
                if (ability.isIndependent()) ability.toXML(xw);
            }

            final Turn turn = getGame().getTurn();
            for (Modifier modifier : getSortedModifiers()) {
                if (modifier.hasIncrement()
                    && modifier.isOutOfDate(turn)) continue;
                if (modifier.isIndependent()) modifier.toXML(xw);
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

        String newType = xr.getAttribute(SETTLEMENT_TYPE_TAG, (String)null);
        type = owner.getNationType().getSettlementType(newType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        featureContainer.clear();

        super.readChildren(xr);

        // Add back the type-derived features.
        addFeatures(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (Ability.getXMLElementTagName().equals(tag)) {
            Ability ability = new Ability(xr, spec);
            if (ability.isIndependent()) addAbility(ability);

        } else if (Modifier.getXMLElementTagName().equals(tag)) {
            Modifier modifier = new Modifier(xr, spec);
            if (modifier.isIndependent()) addModifier(modifier);

        } else {
            super.readChild(xr);
        }
    }
}
