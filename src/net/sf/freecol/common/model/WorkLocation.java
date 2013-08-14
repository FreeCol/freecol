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

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * The <code>WorkLocation</code> is a place in a {@link Colony} where
 * <code>Units</code> can work.  The unit capacity of a WorkLocation
 * is likely to be limited.  ColonyTiles can only hold a single
 * worker, and Buildings can hold no more than three workers, for
 * example.  WorkLocations do not store any Goods.  They take any
 * Goods they consume from the Colony, and put all Goods they produce
 * there, too.  Although the WorkLocation implements {@link Ownable},
 * its owner can not be changed directly, as it is always owned by the
 * owner of the Colony.
 */
public abstract class WorkLocation extends UnitLocation implements Ownable {

    public static final List<AbstractGoods> EMPTY_LIST = Collections.emptyList();

    /** The colony that contains this work location. */
    protected Colony colony;

    /** The production type of this WorkLocation. */
    private ProductionType productionType;


    /**
     * Constructor for ServerWorkLocation.
     *
     * @param game The enclosing <code>Game</code>.
     */
    protected WorkLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new <code>WorkLocation</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(FreeColXMLReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public WorkLocation(Game game, String id) {
        super(game, id);
    }


    /**
     * Gets the owning settlement for this work location.
     *
     * @return The owning settlement for this work location.
     */
    public Settlement getOwningSettlement() {
        return colony;
    }

    /**
     * Get the production type.
     *
     * @return The <code>ProductionType</code> for this work location.
     */
    public final ProductionType getProductionType() {
        return productionType;
    }

    /**
     * Set the prodution type.
     *
     * @param newProductionType The new <code>ProductionType</code> value.
     */
    public final void setProductionType(final ProductionType newProductionType) {
        if (newProductionType != productionType) {
            productionType = newProductionType;
            colony.invalidateCache();
        }
    }

    /**
     * Get the <code>AbstractGoods</code> consumed by this work location.
     *
     * @return A list of <code>AbstractGoods</code> consumed.
     */
    public List<AbstractGoods> getInputs() {
        if (productionType == null || productionType.getInputs() == null) {
            return EMPTY_LIST;
        }
        return productionType.getInputs();
    }

    /**
     * Get the <code>AbstractGoods</code> produced by this work location.
     *
     * @return A list of <code>AbstractGoods</code> produced.
     */
    public List<AbstractGoods> getOutputs() {
        if (productionType == null || productionType.getOutputs() == null) {
            return EMPTY_LIST;
        }
        return productionType.getOutputs();
    }

    /**
     * Does this work location produce a given type of goods?
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return True if this <code>WorkLocation</code> produces the
     *     given <code>GoodsType</code>.
     */
    public boolean produces(GoodsType goodsType) {
        return getBaseProduction(goodsType) > 0;
    }

    /**
     * Gets the base production of the given goods type without
     * applying any modifiers.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The base production.
     */
    public int getBaseProduction(GoodsType goodsType) {
        if (productionType != null) {
            for (AbstractGoods output : productionType.getOutputs()) {
                if (output.getType() == goodsType) {
                    return output.getAmount();
                }
            }
        }
        return 0;
    }

    /**
     * Does this work location have any inputs.
     *
     * @return True if there are any inputs.
     */
    public boolean hasInputs() {
        return !(productionType == null
                 || productionType.getInputs() == null
                 || productionType.getInputs().isEmpty());
    }

    /**
     * Does this work location have any outputs.
     *
     * @return True if there are any outputs.
     */
    public boolean hasOutputs() {
        return !(productionType == null
                 || productionType.getOutputs() == null
                 || productionType.getOutputs().isEmpty());
    }

    /**
     * Checks if this work location can actually be worked.
     *
     * @return True if the work location can be worked.
     */
    public boolean canBeWorked() {
        return getNoWorkReason() == NoAddReason.NONE;
    }

    /**
     * Does this work location have teaching capability?
     *
     * @return True if this is a teaching location.
     * @see Ability#TEACH
     */
    public boolean canTeach() {
        return hasAbility(Ability.TEACH);
    }

    /**
     * Gets the ProductionInfo for this WorkLocation from the Colony's
     * cache.
     *
     * @return The work location <code>ProductionInfo</code>.
     */
    public ProductionInfo getProductionInfo() {
        return getColony().getProductionInfo(this);
    }

    /**
     * Gets the production at this work location.
     *
     * @return The work location production.
     */
    public List<AbstractGoods> getProduction() {
        ProductionInfo info = getProductionInfo();
        if (info == null) return Collections.emptyList();
        return info.getProduction();
    }

    /**
     * Gets the total production of a specified goods type at this
     * work location.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The amount of production.
     */
    public int getTotalProductionOf(GoodsType goodsType) {
        if (goodsType == null) {
            throw new IllegalArgumentException("Null GoodsType.");
        }
        for (AbstractGoods ag : getProduction()) {
            if (ag.getType() == goodsType) return ag.getAmount();
        }
        return 0;
    }

    /**
     * Gets the maximum production of this work location for a given
     * goods type, assuming the current workers and input goods.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The maximum production of the goods at this work location.
     */
    public int getMaximumProductionOf(GoodsType goodsType) {
        ProductionInfo info = getProductionInfo();
        if (info == null) return 0;
        List<AbstractGoods> production = info.getMaximumProduction();
        if (production != null) {
            for (AbstractGoods ag : production) {
                if (ag.getType() == goodsType) return ag.getAmount();
            }
        }
        return getTotalProductionOf(goodsType);
    }

    /**
     * Update production type on the basis of the current building
     * type (which might change due to an upgrade) and the work type
     * of units present.
     */
    public void updateProductionType() {
        Unit unit = getFirstUnit();
        if (unit == null) {
            List<ProductionType> production = getProductionTypes();
            setProductionType((production.isEmpty()) ? null
                : production.get(0));
        } else {
            setProductionType(getBestProductionType(unit.getWorkType()));
        }
    }

    /**
     * Gets the unit type that is the expert for this <code>WorkLocation</code>
     * using its first output for which an expert type can be found.
     *
     * @return The expert <code>UnitType</code>.
     */
    public UnitType getExpertUnitType() {
        final Specification spec = getSpecification();
        for (AbstractGoods goods : getOutputs()) {
            UnitType expert = spec.getExpertForProducing(goods.getType());
            if (expert != null) return expert;
        }
        return null;
    }

    /**
     * Get the potential production of a given goods type using the
     * default unit.  This is useful for planning.
     *
     * @param goodsType The <code>GoodsType</code> to check.
     * @return The potential production.
     */
    public int getGenericPotential(GoodsType goodsType) {
        return getPotentialProduction(goodsType,
            getSpecification().getDefaultUnitType());
    }


    // Interface Location
    // Inherits:
    //   FreeColObject.getId
    //   UnitLocation.getLocationName
    //   UnitLocation.getLocationNameFor
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   final UnitLocation.getUnitIterator
    //   UnitLocation.getGoodsContainer

    /**
     * {@inheritDoc}
     */
    @Override
    public final Tile getTile() {
        return colony.getTile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(final Locatable locatable) {
        NoAddReason reason = getNoAddReason(locatable);
        if (reason != NoAddReason.NONE) {
            throw new IllegalStateException("Can not add " + locatable
                + " to " + toString() + " because " + reason);
        }
        Unit unit = (Unit)locatable;
        if (contains(unit)) return true;
        if (!super.add(unit)) return false;

        unit.setState(Unit.UnitState.IN_COLONY);
        unit.setMovesLeft(0);

        // Choose a sensible work type only if none already specified
        // or it is not useful at this location.
        ProductionType best = null;
        if (unit.getWorkType() != null) {
            if ((best = getBestProductionType(unit.getWorkType())) != null) {
                setProductionType(best);
            }
        }
        if (best == null) {
            if ((best = getBestProductionType(unit)) != null) {
                setProductionType(best);
                // TODO: unit work type needs to be a production
                // type rather than a goods type
                unit.changeWorkType(best.getOutputs().get(0).getType());
            }
        }

        getColony().invalidateCache();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }
        Unit unit = (Unit)locatable;
        if (!contains(unit)) return true;
        if (!super.remove(unit)) return false;

        unit.setState(Unit.UnitState.ACTIVE);
        unit.setMovesLeft(0);
        getColony().invalidateCache();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Settlement getSettlement() {
        return colony;
    }


    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.getUnitCapacity
    //   UnitLocation.canBuildEquipment

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        return (locatable instanceof Unit && ((Unit) locatable).isPerson())
            ? super.getNoAddReason(locatable)
            : NoAddReason.WRONG_TYPE;
    }


    // Abstract and overrideable routines to be implemented by
    // WorkLocation subclasses.

    /**
     * Checks if this work location is available to the colony to be worked.
     *
     * @return The reason why/not the work location can be worked.
     */
    public abstract NoAddReason getNoWorkReason();

    /**
     * Can this work location can produce goods without workers?
     *
     * @return True if this work location can produce goods without workers.
     */
    public abstract boolean canAutoProduce();

    /**
     * Gets the production of a unit of the given type of goods.
     *
     * @param unit The unit to do the work.
     * @param goodsType The type of goods to get the production of.
     * @return The production of the given type of goods.
     */
    public abstract int getProductionOf(Unit unit, GoodsType goodsType);

    /**
     * Gets the potential production of a given goods type from
     * optionally using a unit of a given type in this work location.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType An optional <code>UnitType</code> to produce the goods.
     * @return The amount of goods potentially produced.
     */
    public abstract int getPotentialProduction(GoodsType goodsType,
                                               UnitType unitType);

    /**
     * Gets the production modifiers for the given type of goods and unit.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param unitType The optional <code>unitType</code> to produce them.
     * @return A list of the applicable modifiers.
     */
    public abstract List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                          UnitType unitType);

    /**
     * Returns the production types available for this WorkLocation.
     *
     * @return available production types
     */
    public abstract List<ProductionType> getProductionTypes();

    /**
     * Gets the best production type for a unit to perform at this
     * work location.
     *
     * @param unit The <code>Unit</code> to check.
     * @return The best production type.
     */
    public ProductionType getBestProductionType(Unit unit) {
        ProductionType best = null;
        int amount = 0;
        for (ProductionType productionType : getProductionTypes()) {
            if (productionType.getOutputs() != null) {
                for (AbstractGoods output : productionType.getOutputs()) {
                    int newAmount = getPotentialProduction(output.getType(),
                                                           unit.getType());
                    if (newAmount > amount) {
                        amount = newAmount;
                        best = productionType;
                    }
                }
            }
        }
        return best;
    }

    /**
     * Gets the best production type for the production of the
     * given goods type.  This method is likely to be removed in the
     * future.
     *
     * @param goodsType goods type
     * @return production type
     */
    public ProductionType getBestProductionType(GoodsType goodsType) {
        ProductionType best = null;
        int amount = 0;
        for (ProductionType productionType : getProductionTypes()) {
            if (productionType.getOutputs() != null) {
                for (AbstractGoods output : productionType.getOutputs()) {
                    if (output.getType() == goodsType) {
                        int newAmount = output.getAmount();
                        if (newAmount > amount) {
                            amount = newAmount;
                            best = productionType;
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * Gets a template describing whether this work location can/needs-to
     * be claimed.  To be overridden by classes where this is meaningful.
     *
     * This is a default null implementation.
     *
     * @return A suitable template.
     */
    public StringTemplate getClaimTemplate() {
        return StringTemplate.name("");
    }


    // Interface Ownable

    /**
     * Gets the owner of this <code>Ownable</code>.
     *
     * @return The <code>Player</code> controlling this
     *         {@link Ownable}.
     */
    public Player getOwner() {
        return colony.getOwner();
    }

    /**
     * Sets the owner of this <code>Ownable</code>.  Do not call this
     * method, ever.  The owner of this WorkLocation is the owner
     * of the Colony, you must set the owner of the Colony instead.
     *
     * @param p The <code>Player</code> that should take ownership
     *     of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *     this method.
     */
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }


    // Serialization

    private static final String COLONY_TAG = "colony";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(COLONY_TAG, colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (productionType != null) productionType.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        colony = xr.findFreeColGameObject(getGame(), COLONY_TAG,
                                          Colony.class, (Colony)null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {

        super.readChildren(xr);

        updateProductionType();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (ProductionType.getXMLElementTagName().equals(tag)) {
            productionType = new ProductionType(xr, spec);

        } else {
            super.readChild(xr);
        }
    }
}
