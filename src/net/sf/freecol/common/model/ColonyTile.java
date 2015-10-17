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
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Player.NoClaimReason;


/**
 * Represents a work location on a tile. Each ColonyTile except the
 * colony center tile provides a work place for a single unit and
 * produces a single type of goods. The colony center tile generally
 * produces two different of goods, one food type and one new world
 * raw material.
 */
public class ColonyTile extends WorkLocation {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyTile.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The maximum number of units a ColonyTile can hold. */
    public static final int UNIT_CAPACITY = 1;

    /**
     * The tile to work.  This is accessed through getWorkTile().
     * Beware!  Do not confuse this with getTile(), which returns
     * the colony center tile (because every work location belongs to
     * the enclosing colony).
     */
    protected Tile workTile;


    /**
     * Constructor for ServerColonyTile.
     *
     * @param game The enclosing <code>Game</code>.
     * @param colony The <code>Colony</code> this object belongs to.
     * @param workTile The tile in which this <code>ColonyTile</code>
     *                 represents a <code>WorkLocation</code> for.
     */
    protected ColonyTile(Game game, Colony colony, Tile workTile) {
        super(game);
        
        this.colony = colony;
        this.workTile = workTile;
        updateProductionType();
    }

    /**
     * Create a new <code>ColonyTile</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public ColonyTile(Game game, String id) {
        super(game, id);
    }


    /**
     * Is this the tile where the <code>Colony</code> is located?
     *
     * @return True if this is the colony center tile.
     */
    public boolean isColonyCenterTile() {
        return getWorkTile() == getTile();
    }

    /**
     * Gets the work tile, that is the actual tile being worked.
     *
     * @return The <code>Tile</code> in which this
     *     <code>ColonyTile</code> represents a
     *     <code>WorkLocation</code> for.
     */
    public Tile getWorkTile() {
        return workTile;
    }

    /**
     * Sets the work tile.  Needed to fix copied colonies.  Do not use
     * otherwise!
     *
     * @param workTile The new work <code>Tile</code>.
     */
    public void setWorkTile(Tile workTile) {
        this.workTile = workTile;
    }

    /**
     * Gets a unit who is occupying the tile.
     *
     * @return A <code>Unit</code> who is occupying the work tile, if any.
     * @see #isOccupied()
     */
    public Unit getOccupyingUnit() {
        return workTile.getOccupyingUnit();
    }

    /**
     * Is there a fortified enemy unit on the work tile?
     * Production can not occur on occupied tiles.
     *
     * @return True if an fortified enemy unit is in the tile.
     */
    public boolean isOccupied() {
        return workTile.isOccupied();
    }

    /**
     * Gets the basic production information for the colony tile,
     * ignoring any colony limits (which for now, should be
     * irrelevant).
     *
     * In the original game, the following special rules apply to
     * colony center tiles:
     * - All tile improvements contribute to the production of food
     * - Only natural tile improvements, such as rivers, contribute
     *   to the production of other types of goods.
     * - Artificial tile improvements, such as plowing, are ignored.
     *
     * @return The raw production of this colony tile.
     * @see ProductionCache#update
     */
    public ProductionInfo getBasicProductionInfo() {
        final Colony colony = getColony();
        ProductionInfo pi = new ProductionInfo();
        if (isColonyCenterTile()) {
            for (AbstractGoods output : getOutputs()) {
                boolean onlyNaturalImprovements = getSpecification()
                    .getBoolean(GameOptions.ONLY_NATURAL_IMPROVEMENTS)
                    && !output.getType().isFoodType();
                int potential = output.getAmount();
                if (workTile.getTileItemContainer() != null) {
                    potential = workTile.getTileItemContainer()
                        .getTotalBonusPotential(output.getType(), null, potential,
                                                onlyNaturalImprovements);
                }
                potential += Math.max(0, colony.getProductionBonus());
                AbstractGoods production
                    = new AbstractGoods(output.getType(), potential);
                pi.addProduction(production);
            }
        } else {
            for (AbstractGoods output : getOutputs()) {
                final GoodsType goodsType = output.getType();
                int amount = getUnitList().stream()
                    .mapToInt(u -> getUnitProduction(u, goodsType)).sum();
                if (amount > 0) {
                    pi.addProduction(new AbstractGoods(goodsType, amount));
                }
            }
        }
        return pi;
    }

    /**
     * Would a given tile improvement be beneficial to this colony tile?
     *
     * @param ti The <code>TileImprovementType</code> to assess.
     * @return A measure of improvement (negative is a bad idea).
     */
    public int improvedBy(TileImprovementType ti) {
        final Tile tile = getWorkTile();
        final Colony colony = getColony();
        if (tile == null  // Colony has not claimed the tile
            || tile.getOwningSettlement() != colony // Not our tile
            || tile.hasTileImprovement(ti)) // Pointless work
            return 0;

        final TileType oldType = tile.getType();
        if (!ti.isTileTypeAllowed(oldType)) return 0; // Impossible
        
        final ProductionType productionType = getProductionType();
        if (productionType == null) return 0; // Not using the tile

        final Resource resource = tile.getResource();
        final TileType newType = ti.getChange(oldType);

        // Unattended production is the hard case.
        if (productionType.getUnattended()) {
            if (newType == null) {
                // Tile type stays the same, return the sum of any food bonues.
                return getSpecification().getFoodGoodsTypeList().stream()
                    .mapToInt(gt -> ti.getBonus(gt)).sum();
            }

            // Tile type change.
            final List<AbstractGoods> newProd
                = newType.getPossibleProduction(true);
            int food = newProd.stream()
                .filter(ag -> ag.getType().isFoodType())
                .mapToInt(AbstractGoods::getAmount).sum();
            // Get the current food production.  Otherwise for goods
            // that are being passively produced and consumed, check
            // if production remains in surplus following a negative change.
            for (AbstractGoods ag : getProduction()) {
                final GoodsType goodsType = ag.getType();
                if (goodsType.isFoodType()) {
                    food -= ag.getAmount();
                } else if (colony.isConsuming(goodsType)) {
                    int change = -ag.getAmount()
                        + AbstractGoods.getCount(goodsType, newProd);
                    if (change < 0
                        && change + colony.getNetProductionOf(goodsType) < 0) {
                        // The change drives the net production (more?)
                        // negative.  Do not do this.
                        return change;
                    }
                }
            }
            return food;
        }

        // Units are present, see what the change would do to their work.
        final GoodsType work = getCurrentWorkType();
        final UnitType unitType = getFirstUnit().getType();
        return (work == null) // No work, improvement does nothing
            ? 0
            : (newType == null) // No tile change, but return the new bonus
            ? ti.getBonus(work)
            : (resource == null) // The tile change impact on the work
            ? newType.getPotentialProduction(work, unitType)
                - oldType.getPotentialProduction(work, unitType)
            // The production impact with the new resource in place
            : newType.getPotentialProduction(work, unitType)
                - resource.applyBonus(work, unitType,
                    oldType.getPotentialProduction(work, unitType));
    }
        
    /**
     * Evaluate this work location for a given player.
     *
     * @param player The <code>Player</code> to evaluate for.
     * @return A value for the player.
     */
    @Override
    public int evaluateFor(Player player) {
        return super.evaluateFor(player)
            + getProductionInfo().getProduction().stream()
                .mapToInt(ag -> ag.evaluateFor(player)).sum();
    }


    // Interface Location
    // Inheriting
    //   FreeColObject.getId
    //   WorkLocation.getTile (Beware this returns the colony center tile!),
    //   UnitLocation.getLocationLabelFor
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   WorkLocation.remove
    //   UnitLocation.getUnitCount
    //   final UnitLocation.getUnitIterator
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer
    //   final WorkLocation getSettlement
    //   final WorkLocation getColony
    //   final int getRank

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return (workTile == null) ? null
            : workTile.getColonyTileLocationLabel(getColony());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return getColony();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        return getColony().getName()
            + "-" + getWorkTile().getType().getSuffix()
            + "-" + getTile().getDirection(getWorkTile());
    }


    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.equipForRole

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        NoAddReason reason = super.getNoAddReason(locatable);
        return (reason != NoAddReason.NONE) ? reason : getNoWorkReason();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCapacity() {
        return (isColonyCenterTile()) ? 0 : UNIT_CAPACITY;
    }


    // Interface WorkLocation

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLabel() {
        return (workTile == null) ? null : workTile.getLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        return isCurrent() || getOwner().canClaimForSettlement(getWorkTile());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCurrent() {
        return getWorkTile().getOwningSettlement() == getColony();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoWorkReason() {
        Tile tile = getWorkTile();
        NoClaimReason claim;

        return (isColonyCenterTile())
            ? NoAddReason.COLONY_CENTER
            : (!getColony().hasAbility(Ability.PRODUCE_IN_WATER)
                && !tile.isLand())
            ? NoAddReason.MISSING_ABILITY
            : (tile.getOwningSettlement() == getColony())
            ? NoAddReason.NONE
            : ((claim = getOwner().canClaimForSettlementReason(tile))
                == NoClaimReason.NONE)
            ? NoAddReason.CLAIM_REQUIRED
            : (claim == NoClaimReason.TERRAIN
                || claim == NoClaimReason.RUMOUR
                || claim == NoClaimReason.WATER)
            ? NoAddReason.MISSING_ABILITY
            : (claim == NoClaimReason.SETTLEMENT)
            ? ((getOwner().owns(tile.getSettlement()))
                ? NoAddReason.ANOTHER_COLONY
                : NoAddReason.OWNED_BY_ENEMY)
            : (claim == NoClaimReason.OCCUPIED)
            ? NoAddReason.OCCUPIED_BY_ENEMY
            : (claim == NoClaimReason.WORKED)
            ? NoAddReason.ANOTHER_COLONY
            : (claim == NoClaimReason.EUROPEANS)
            ? NoAddReason.OWNED_BY_ENEMY
            : (claim == NoClaimReason.NATIVES)
            ? NoAddReason.CLAIM_REQUIRED
            : NoAddReason.WRONG_TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAutoProduce() {
        return isColonyCenterTile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        final Tile workTile = getWorkTile();
        return workTile != null && workTile.canProduce(goodsType, unitType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseProduction(ProductionType productionType,
                                 GoodsType goodsType, UnitType unitType) {
        Tile tile = getWorkTile();
        return (tile == null) ? 0
            : tile.getBaseProduction(productionType, goodsType, unitType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        if (!canProduce(goodsType, unitType)) {
            return Collections.<Modifier>emptyList();
        }

        final Tile workTile = getWorkTile();
        final TileType type = workTile.getType();
        final String id = goodsType.getId();
        final Colony colony = getColony();
        final Player owner = colony.getOwner();
        final Turn turn = getGame().getTurn();

        List<Modifier> mods = new ArrayList<>();
        if (unitType == null) {
            if (isColonyCenterTile()) { // Unattended only possible in center
                mods.addAll(workTile.getProductionModifiers(goodsType, null));
                mods.addAll(colony.getProductionModifiers(goodsType));
                mods.addAll(colony.getModifiers(id, null, turn));
                if (owner != null) {
                    mods.addAll(owner.getModifiers(id, type, turn));
                }
            }

        } else {
            mods.addAll(workTile.getProductionModifiers(goodsType, unitType));
            mods.addAll(colony.getProductionModifiers(goodsType));
            mods.addAll(unitType.getModifiers(id, type, turn));
            if (owner != null) {
                mods.addAll(owner.getModifiers(id, unitType, turn));
            }
        }
        return mods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
        return (workTile == null || workTile.getType() == null
            || unattended != isColonyCenterTile())
            ? Collections.<ProductionType>emptyList()
            : workTile.getType().getAvailableProductionTypes(unattended);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getClaimTemplate() {
        return (isColonyCenterTile()) ? super.getClaimTemplate()
            : (StringTemplate.template("model.colonyTile.claim")
                .addNamed("%direction%", getTile().getDirection(workTile)));
    }


    // Serialization

    private static final String WORK_TILE_TAG = "workTile";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(WORK_TILE_TAG, workTile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        workTile = xr.makeFreeColGameObject(getGame(), WORK_TILE_TAG,
                                            Tile.class, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" ").append(getWorkTile())
            .append("/").append(getColony().getName())
            .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "colonyTile".
     */
    public static String getXMLElementTagName() {
        return "colonyTile";
    }
}
