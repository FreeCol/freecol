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

import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


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

    /**
     * The colony that contains this work location.
     */
    private Colony colony;

    /**
     * The production type of this WorkLocation.
     */
    private ProductionType productionType;


    /**
     * Constructor for ServerWorkLocation.
     */
    protected WorkLocation() {
        // empty constructor
    }

    /**
     * Constructor for ServerWorkLocation.
     *
     * @param game The <code>Game</code> this object belongs to.
     */
    protected WorkLocation(Game game) {
        super(game);
    }

    /**
     * Initiates a new <code>WorkLocation</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public WorkLocation(Game game, XMLStreamReader in)
        throws XMLStreamException {
        super(game, in);
    }

    /**
     * Initiates a new <code>WorkLocation</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public WorkLocation(Game game, String id) {
        super(game, id);
    }


    /**
     * Set the <code>Colony</code> value.
     *
     * @param newColony The new Colony value.
     */
    public final void setColony(final Colony newColony) {
        this.colony = newColony;
    }

    /**
     * Get the <code>ProductionType</code> value.
     *
     * @return a <code>ProductionType</code> value
     */
    public final ProductionType getProductionType() {
        return productionType;
    }

    /**
     * Set the <code>ProductionType</code> value.
     *
     * @param newProductionType The new ProductionType value.
     */
    public final void setProductionType(final ProductionType newProductionType) {
        if (newProductionType != productionType) {
            productionType = newProductionType;
            colony.invalidateCache();
        }
    }

    /**
     * Returns a list of <code>AbstractGoods</code> consumed by this
     * WorkLocation.
     *
     * @return a list of AbstractGoods
     */
    public List<AbstractGoods> getInputs() {
        if (productionType == null || productionType.getInputs() == null) {
            return EMPTY_LIST;
        } else {
            return productionType.getInputs();
        }
    }

    /**
     * Returns a list of <code>AbstractGoods</code> produced by this
     * WorkLocation.
     *
     * @return a list of AbstractGoods
     */
    public List<AbstractGoods> getOutputs() {
        if (productionType == null || productionType.getOutputs() == null) {
            return EMPTY_LIST;
        } else {
            return productionType.getOutputs();
        }
    }

    /**
     * Returns whether this WorkLocation produces the given GoodsType.
     *
     * @param goodsType the GoodsType
     * @return  whether this WorkLocation produces the given GoodsType
     */
    public boolean produces(GoodsType goodsType) {
        return getBaseProduction(goodsType) > 0;
    }


    /**
     * Returns the base production of the given goods type without
     * applying any modifiers.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return an <code>int</code> value
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

    public boolean hasInputs() {
        return !(productionType == null
                 || productionType.getInputs() == null
                 || productionType.getInputs().isEmpty());
    }

    public boolean hasOutputs() {
        return !(productionType == null
                 || productionType.getOutputs() == null
                 || productionType.getOutputs().isEmpty());
    }

    /**
     * Gets the owning settlement for this work location.
     *
     * Usually the same as getColony() but overridden by ColonyTile
     * to handle unclaimed tiles.
     *
     * @return The owning settlement for this work location.
     */
    public Settlement getOwningSettlement() {
        return colony;
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
     * @see Ability#CAN_TEACH
     */
    public boolean canTeach() {
        return hasAbility(Ability.CAN_TEACH);
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
    public final Tile getTile() {
        return colony.getTile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }

        Unit unit = (Unit) locatable;
        if (super.add(unit)) {
            if (this.canTeach()) {
                Unit student = unit.getStudent();
                if (student == null
                    && (student = getColony().findStudent(unit)) != null) {
                    unit.setStudent(student);
                    student.setTeacher(unit);
                }
                unit.setWorkType(null);
            } else {
                Unit teacher = unit.getTeacher();
                if (teacher == null
                    && (teacher = getColony().findTeacher(unit)) != null) {
                    unit.setTeacher(teacher);
                    teacher.setStudent(unit);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }

        Unit unit = (Unit) locatable;
        if (super.remove(unit)) {
            if (this.canTeach()) {
                Unit student = unit.getStudent();
                if (student != null) {
                    student.setTeacher(null);
                    unit.setStudent(null);
                }
                unit.setTurnsOfTraining(0);
            }
            // Do not clear teacher like in add(), do that at the
            // colony level so that students can be moved from one
            // work location to another without disrupting teaching.
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public final Settlement getSettlement() {
        return colony;
    }

    /**
     * {@inheritDoc}
     */
    public final Colony getColony() {
        return colony;
    }


    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.getUnitCapacity

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
    public abstract ProductionType getBestProductionType(Unit unit);

    /**
     * Returns the best production type for the production of the
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
     * Sets the owner of this <code>Ownable</code>. Do not call this
     * method, ever. Since the owner of this WorkLocation is the owner
     * of the Colony, you must set the owner of the Colony instead.
     *
     * @param p The <code>Player</code> that should take ownership
     *      of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *      this method.
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
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, COLONY_TAG, colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        colony = findFreeColGameObject(in, COLONY_TAG,
                                       Colony.class, (Colony)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        if (ProductionType.getXMLElementTagName().equals(in.getLocalName())) {
            productionType = new ProductionType(getSpecification());
            productionType.readFromXML(in);

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll,
                                 boolean toSavedGame) throws XMLStreamException {
        super.writeChildren(out, player, showAll, toSavedGame);

        if (productionType != null) productionType.toXML(out);
    }
}
