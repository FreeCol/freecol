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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.Utils;


/**
 * Represents a building in a colony.
 */
public class Building extends WorkLocation implements Named, Comparable<Building>, Consumer {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Building.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The type of building. */
    protected BuildingType buildingType;


    /**
     * Constructor for ServerBuilding.
     */
    protected Building() {
        // empty constructor
    }

    /**
     * Constructor for ServerBuilding.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The <code>Colony</code> in which this building is located.
     * @param type The <code>BuildingType</code> of building.
     */
    protected Building(Game game, Colony colony, BuildingType type) {
        super(game);
        setColony(colony);
        this.buildingType = type;
        // set production type to default value
        updateProductionType();
    }

    /**
     * Initiates a new <code>Building</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Building(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Initiates a new <code>Building</code> with the given ID.  The
     * object should later be initialized by calling
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param id The unique identifier for this object.
     */
    public Building(Game game, String id) {
        super(game, id);
    }


    /**
     * Gets the type of this building.
     *
     * @return The building type.
     */
    public BuildingType getType() {
        return buildingType;
    }

    /**
     * Changes the type of the Building.  The type of a building may
     * change when it is upgraded or damaged.
     *
     * @param newBuildingType The new <code>BuildingType</code>.
     * @see #upgrade
     * @see #damage
     */
    private void setType(final BuildingType newBuildingType) {
        // remove features from current type
        Colony colony = getColony();
        colony.removeFeatures(buildingType);

        if (newBuildingType != null) {
            buildingType = newBuildingType;

            // change default production type
            updateProductionType();

            // add new features and abilities from new type
            colony.addFeatures(buildingType);

            // Colonists which can't work here must be put outside
            for (Unit unit : getUnitList()) {
                if (!canAddType(unit.getType())) {
                    unit.setLocation(colony.getTile());
                }
            }
        }

        // Colonists exceding units limit must be put outside
        if (getUnitCount() > getUnitCapacity()) {
            for (Unit unit : getUnitList().subList(getUnitCapacity(),
                                                   getUnitCount())) {
                unit.setLocation(colony.getTile());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getNameKey() {
        return getType().getNameKey();
    }

    /**
     * Gets the level of this building.
     * Delegates to type.
     *
     * @return The building level.
     */
    public int getLevel() {
        return getType().getLevel();
    }

    /**
     * Gets the name of the improved building of the same type.
     * An improved building is a building of a higher level.
     *
     * @return The name of the improved building or <code>null</code> if the
     *     improvement does not exist.
     */
    public String getNextNameKey() {
        final BuildingType next = getType().getUpgradesTo();
        return (next == null) ? null : next.getNameKey();
    }

    /**
     * Does this building have a higher level?
     *
     * @return True if this <code>Building</code> can have a higher level.
     */
    public boolean canBuildNext() {
        return getColony().canBuild(getType().getUpgradesTo());
    }

    /**
     * Can this building can be damaged?
     *
     * @return True if this building can be damaged.
     * @see #damage
     */
    public boolean canBeDamaged() {
        return !getType().isAutomaticBuild()
            && !getColony().isAutomaticBuild(getType());
    }

    /**
     * Downgrade this building.
     *
     * @return True if the building was downgraded.
     */
    public boolean downgrade() {
        if (canBeDamaged()) {
            setType(getType().getUpgradesFrom());
            getColony().invalidateCache();
            return true;
        }
        return false;
    }

    /**
     * Upgrade this building to next level.
     *
     * @return True if the upgrade succeeds.
     */
    public boolean upgrade() {
        if (canBuildNext()) {
            setType(getType().getUpgradesTo());
            getColony().invalidateCache();
            return true;
        }
        return false;
    }

    /**
     * Gets the unit type that is the expert for this <code>Building</code>.
     *
     * @return The expert <code>UnitType</code>.
     */
    public UnitType getExpertUnitType() {
        for (AbstractGoods goods : getOutputs()) {
            UnitType expert = getSpecification().getExpertForProducing(goods.getType());
            if (expert != null) {
                return expert;
            }
        }
        return null;
    }

    /**
     * Can a particular type of unit be added to this building?
     *
     * @param unitType The <code>UnitType</code> to check.
     * @return True if unit type can be added to this building.
     */
    public boolean canAddType(UnitType unitType) {
        return canBeWorked() && getType().canAdd(unitType);
    }

    /**
     * Gets the maximum productivity of a unit working in this work
     * location, considering *only* the contribution of the unit,
     * exclusive of that of the work location.
     *
     * Used below, only public for the test suite.
     *
     * @param unit The <code>Unit</code> to check.
     * @return The maximum return from this unit.
     */
    public int getUnitProduction(Unit unit, GoodsType goodsType) {
        int productivity = 0;
        List<AbstractGoods> outputs = getOutputs();
        if (!(unit == null || outputs == null)) {
            for (AbstractGoods output : outputs) {
                if (output.getType() == goodsType) {
                    productivity = output.getAmount();
                    if (productivity > 0) {
                        final UnitType unitType = unit.getType();
                        final Turn turn = getGame().getTurn();

                        productivity = (int) FeatureContainer
                            .applyModifiers(output.getAmount(), turn,
                                            getProductionModifiers(goodsType, unitType));
                    }
                    return Math.max(0, productivity);
                }
            }
        }
        return 0;
    }

    private int getAvailable(GoodsType type, List<AbstractGoods> available) {
        for (AbstractGoods goods : available) {
            if (goods.getType() == type) {
                return goods.getAmount();
            }
        }
        return 0;
    }

    /**
     * Gets the production information for this building taking account
     * of the available input and output goods.
     *
     * @param inputs The input goods available.
     * @param outputs The output goods already available in the colony,
     *     necessary in order to avoid excess production.
     * @return The production information.
     * @see ProductionCache#update
     */
    public ProductionInfo getAdjustedProductionInfo(List<AbstractGoods> inputs,
                                                    List<AbstractGoods> outputs) {

        ProductionInfo result = new ProductionInfo();
        if (!hasOutputs()) return result;

        // first, calculate the maximum production

        double minimumRatio = Double.MAX_VALUE;
        double maximumRatio = 0;
        if (canAutoProduce()) {
            for (AbstractGoods output : getOutputs()) {
                GoodsType outputType = output.getType();
                int available = getColony().getGoodsCount(outputType);
                if (available >= outputType.getBreedingNumber()) {
                    // we need at least these many horses/animals to breed
                    double newRatio = 0;
                    int divisor = (int) getType()
                        .applyModifier(0f, "model.modifier.breedingDivisor");
                    if (divisor > 0) {
                        int factor = (int) getType()
                            .applyModifier(0f, "model.modifier.breedingFactor");
                        int maximumOutput = ((available - 1) / divisor + 1) * factor;
                        newRatio = maximumOutput / output.getAmount();
                    }
                    minimumRatio = Math.min(minimumRatio, newRatio);
                    maximumRatio = Math.max(maximumRatio, newRatio);
                } else {
                    minimumRatio = 0;
                }
            }
        } else {
            Turn turn = getGame().getTurn();
            for (AbstractGoods output : getOutputs()) {
                float production = 0;
                for (Unit u : getUnitList()) {
                    production += getUnitProduction(u, output.getType());
                }
                List<Modifier> productionModifiers = getProductionModifiers(output.getType(), null);
                production = FeatureContainer
                    .applyModifiers(production, turn, productionModifiers);
                double newRatio = production / output.getAmount();
                minimumRatio = Math.min(minimumRatio, newRatio);
                maximumRatio = Math.max(maximumRatio, newRatio);
            }
        }

        // then, check whether the required inputs are available

        for (AbstractGoods input : getInputs()) {
            int required = (int) (input.getAmount() * minimumRatio);
            int available = getAvailable(input.getType(), inputs);
            // experts in factory level buildings may produce a
            // certain amount of goods even when no input is available
            if (available < required
                && getType().hasAbility(Ability.EXPERTS_USE_CONNECTIONS)
                && getSpecification().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
                int minimumGoodsInput = 0;
                for (Unit unit: getUnitList()) {
                    if (unit.getType() == getExpertUnitType()) {
                        // TODO: put magic number in specification
                        minimumGoodsInput += 4;
                    }
                }
                if (minimumGoodsInput > available) {
                    available = minimumGoodsInput;
                }
            }
            if (available < required) {
                minimumRatio = (minimumRatio * available) / required;
                maximumRatio = Math.max(maximumRatio, minimumRatio);
            }
        }

        // finally, check whether there is space enough to store the
        // goods produced in order to avoid excess production

        if (getType().hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
            int capacity = getColony().getWarehouseCapacity();
            for (AbstractGoods output : getOutputs()) {
                double production = (output.getAmount() * minimumRatio);
                if (production > 0) {
                    int amountPresent = getAvailable(output.getType(), outputs);
                    if (production + amountPresent > capacity) {
                        // don't produce more than the warehouse can hold
                        double newRatio = (capacity - amountPresent) / output.getAmount();
                        minimumRatio = Math.min(minimumRatio, newRatio);
                        // and don't claim that more could be produced
                        maximumRatio = minimumRatio;
                    }
                }
            }
        }

        for (AbstractGoods input : getInputs()) {
            GoodsType type = input.getType();
            // maximize consumption
            int consumption = (int) Math.ceil(input.getAmount() * minimumRatio);
            int maximumConsumption = (int) Math.ceil(input.getAmount() * maximumRatio);
            result.addConsumption(new AbstractGoods(type, consumption));
            if (consumption < maximumConsumption) {
                result.addMaximumConsumption(new AbstractGoods(type, maximumConsumption));
            }
        }
        for (AbstractGoods output : getOutputs()) {
            GoodsType type = output.getType();
            // minimize production, but add a magic little something
            // to counter rounding errors
            int production = (int) Math.floor(output.getAmount() * minimumRatio + 0.0001);
            int maximumProduction = (int) Math.floor(output.getAmount() * maximumRatio);
            result.addProduction(new AbstractGoods(type, production));
            if (production < maximumProduction) {
                result.addMaximumProduction(new AbstractGoods(type, maximumProduction));
            }
        }
        return result;
    }


    // Interface Comparable
    /**
     * {@inheritDoc}
     */
    public int compareTo(Building other) {
        return getType().compareTo(other.getType());
    }


    // Interface Location
    // Inherits:
    //   FreeColObject.getId
    //   WorkLocation.getTile
    //   UnitLocation.getLocationNameFor
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   final UnitLocation.getUnitIterator
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer
    //   final WorkLocation getSettlement
    //   final WorkLocation getColony

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationName() {
        return StringTemplate.template("inLocation")
            .add("%location%", getNameKey());
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
        Unit unit = (Unit) locatable;
        if (contains(unit)) return true;

        if (super.add(unit)) {
            unit.setState(Unit.UnitState.IN_COLONY);
            List<AbstractGoods> outputs = getOutputs();
            if (outputs.size() == 1) {
                unit.setWorkType(outputs.get(0).getType());
            }
            getColony().invalidateCache();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }
        Unit unit = (Unit) locatable;
        if (!contains(unit)) return true;

        if (super.remove(unit)) {
            unit.setState(Unit.UnitState.ACTIVE);
            unit.setMovesLeft(0);

            getColony().invalidateCache();
            return true;
        }
        return false;
    }


    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        if (!(locatable instanceof Unit)) return NoAddReason.WRONG_TYPE;
        NoAddReason reason = getNoWorkReason();
        Unit unit = (Unit) locatable;
        BuildingType type = getType();

        return (reason != NoAddReason.NONE) ? reason
            : !type.canAdd(unit.getType()) ? NoAddReason.MISSING_SKILL
            : super.getNoAddReason(locatable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCapacity() {
        return getType().getWorkPlaces();
    }


    // Interface WorkLocation
    // Inherits:
    //   WorkLocation.getBestProductionType(goodsType): moot for buildings.
    //   WorkLocation.getClaimTemplate: buildings do not need to be claimed.

    /**
     * {@inheritDoc}
     */
    public NoAddReason getNoWorkReason() {
        return NoAddReason.NONE;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canAutoProduce() {
        return getType().hasAbility(Ability.AUTO_PRODUCTION);
    }

    /**
     * {@inheritDoc}
     */
    public int getProductionOf(Unit unit, GoodsType goodsType) {
        if (unit == null) {
            throw new IllegalArgumentException("Null unit.");
        }
        for (AbstractGoods goods : getOutputs()) {
            if (goods.getType() == goodsType) {
                return Math.max(0, getPotentialProduction(goodsType, unit.getType()));
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public int getPotentialProduction(GoodsType goodsType, UnitType unitType) {
        for (AbstractGoods output : getOutputs()) {
            if (output.getType() == goodsType) {
                int amount = (unitType == null) ? 0 : output.getAmount();
                int production = (int) FeatureContainer
                    .applyModifiers(amount, getGame().getTurn(),
                                    getProductionModifiers(goodsType, unitType));
                return Math.max(0, production);
            }
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        List<Modifier> mods = new ArrayList<Modifier>();
        for (AbstractGoods output : getOutputs()) {
            if (output.getType() == goodsType) {
                final BuildingType type = getType();
                final String id = goodsType.getId();
                final Turn turn = getGame().getTurn();
                final Player owner = getOwner();
                if (unitType == null) {
                    // If a unit is not present add only the bonuses
                    // specific to the building (such as the Paine bells bonus).
                    mods.addAll(getColony().getModifierSet(id, type, turn));
                    if (owner != null) {
                        mods.addAll(owner.getModifierSet(id, type, turn));
                    }
                } else {
                    // If a unit is present add unit specific bonuses and
                    // unspecific owner bonuses (which includes things
                    // like the Building national advantage).
                    mods.addAll(getModifierSet(id, unitType, turn));
                    mods.add(getColony().getProductionModifier(goodsType));
                    mods.addAll(unitType.getModifierSet(id, goodsType, turn));
                    if (owner != null) {
                        mods.addAll(owner.getModifierSet(id, unitType, turn));
                    }
                }
                break;
            }
        }
        return mods;
    }

    /**
     * Returns the production types available for this Building.
     *
     * @return available production types
     */
    public List<ProductionType> getProductionTypes() {
        return getType().getProductionTypes();
    }

    /**
     * {@inheritDoc}
     */
    public ProductionType getBestProductionType(Unit unit) {
        // TODO: think of something better
        return getType().getProductionTypes().isEmpty()
            ? null : getType().getProductionTypes().get(0);
    }


    // Interface Consumer

    /**
     * Can this Consumer consume the given GoodsType?
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean consumes(GoodsType goodsType) {
        for (AbstractGoods input : getInputs()) {
            if (input.getType() == goodsType) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public List<AbstractGoods> getConsumedGoods() {
        return getInputs();
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return getType().getPriority();
    }

    /**
     * Is an ability present in this Building?
     *
     * The method actually returns whether the type of the building
     * has the required ability, since Buildings have no abilities
     * independent of their type.
     *
     * @param id The id of the ability to test.
     * @param type A <code>FreeColGameObjectType</code> (ignored).
     * @param turn A <code>Turn</code> (ignored).
     * @return True if the ability is present.
     */
    @Override
    public boolean hasAbility(String id, FreeColGameObjectType type,
                              Turn turn) {
        return getType().hasAbility(id);
    }

    /**
     * Gets the set of modifiers with the given Id from this Building.
     * Delegate to the type.
     *
     * @param id The id of the modifier to retrieve.
     * @param fcgot A <code>FreeColGameObjectType</code> (ignored).
     * @param turn A <code>Turn</code> (ignored).
     * @return A set of modifiers.
     */
    @Override
    public Set<Modifier> getModifierSet(String id, FreeColGameObjectType fcgot,
                                        Turn turn) {
        return getType().getModifierSet(id, fcgot, turn);
    }


    // Serialization

    private static final String BUILDING_TYPE_TAG = "buildingType";

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll,
                             boolean toSavedGame) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        writeAttributes(out);
        super.writeChildren(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, BUILDING_TYPE_TAG, buildingType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields) throws XMLStreamException {
        toXMLPartialByClass(out, Building.class, fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXMLPartialImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXMLPartialByClass(in, Building.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final Specification spec = getSpecification();
        buildingType = spec.getType(in, BUILDING_TYPE_TAG,
                                    BuildingType.class, (BuildingType)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append("/").append(getColony().getName())
            .append("]");
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "building".
     */
    public static String getXMLElementTagName() {
        return "building";
    }
}
