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
     * Initiates a new <code>Building</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)}.
     *
     * @param game The <code>Game</code> in which this object belong.
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
     * Changes the type of the Building. The type of a building may
     * change when it is upgraded or damaged.
     *
     * @param newBuildingType
     * @see #upgrade
     * @see #damage
     */
    private void setType(final BuildingType newBuildingType) {
        // remove features from current type
        Colony colony = getColony();
        colony.removeFeatures(buildingType);

        if (newBuildingType != null) {
            buildingType = newBuildingType;

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
     * Is an ability present in this Building? The method actually
     * returns whether the type of the building has the required
     * ability, since Buildings have no abilities independent of their
     * type.
     *
     * @param id The id of the ability to test.
     * @param type A <code>FreeColGameObjectType</code> (ignored)
     * @param turn An <code>Turn</code> (ignored)
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
     * @param id The id of the modifier to test.
     * @param fcgot An optional <code>FreeColGameObjectType</code> the
     *     modifier applies to.
     * @param turn An optional applicable <code>Turn</code>.
     * @return A set of modifiers.
     */
    @Override
    public Set<Modifier> getModifierSet(String id, FreeColGameObjectType fcgot,
                                        Turn turn) {
        return getType().getModifierSet(id);
    }

    /**
     * {@inheritDoc}
     */
    public String getNameKey() {
        return getType().getNameKey();
    }

    /**
     * Returns the level of this building.
     *
     * @return an <code>int</code> value
     */
    public int getLevel() {
        return getType().getLevel();
    }

    /**
     * Gets the name of the improved building of the same type. An improved
     * building is a building of a higher level.
     *
     * @return The name of the improved building or <code>null</code> if the
     *         improvement does not exist.
     */
    public String getNextNameKey() {
        final BuildingType next = getType().getUpgradesTo();
        return next == null ? null : next.getNameKey();
    }

    /**
     * Checks if this building can have a higher level.
     *
     * @return If this <code>Building</code> can have a higher level, that
     *         {@link FoundingFather Adam Smith} is present for manufactoring
     *         factory level buildings and that the <code>Colony</code>
     *         containing this <code>Building</code> has a sufficiently high
     *         population.
     */
    public boolean canBuildNext() {
        return getColony().canBuild(getType().getUpgradesTo());
    }

    /**
     * Returns whether this building can be damaged
     *
     * @return <code>true</code> if can be damaged
     * @see #damage
     */
    public boolean canBeDamaged() {
        return !getType().isAutomaticBuild()
            && !getColony().isAutomaticBuild(getType());
    }

    /**
     * Reduces this building to previous level (is set to UpgradesFrom
     * attribute in BuildingType) or is destroyed if it's the first level
     *
     * @return True if the building was damaged.
     */
    public boolean damage() {
        if (canBeDamaged()) {
            setType(getType().getUpgradesFrom());
            getColony().invalidateCache();
            return true;
        }
        return false;
    }

    /**
     * Upgrades this building to next level (is set to UpgradesTo
     * attribute in BuildingType)
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
     * Returns the unit type being an expert in this <code>Building</code>.
     *
     * @return The UnitType.
     */
    public UnitType getExpertUnitType() {
        return getSpecification().getExpertForProducing(getGoodsOutputType());
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
     * Returns the type of goods this <code>Building</code> produces,
     * or <code>null</code> if the Building does not produce any
     * goods.
     *
     * @return The type of goods this <code>Building</code> produces
     */
    public GoodsType getGoodsOutputType() {
        return getType().getProducedGoodsType();
    }

    /**
     * Returns the type of goods this building needs for input, or
     * <code>null</code> if the Building does not consume any goods.
     *
     * @return The type of goods this <code>Building</code> requires as input
     *         in order to produce it's {@link #getGoodsOutputType output}.
     */
    public GoodsType getGoodsInputType() {
        return getType().getConsumedGoodsType();
    }

    /**
     * Gets the maximum productivity of a unit working in this work location,
     * considering *only* the contribution of the unit, exclusive of
     * that of the work location.
     *
     * Used below, only public for the test suite.
     * 
     * @param unit The <code>Unit</code> to check.
     * @return The maximum return from this unit.
     */
    public int getUnitConsumption(Unit unit) {
        if (getGoodsOutputType() == null || unit == null) return 0;

        int productivity = getType().getBasicProduction();
        if (productivity > 0) {
            productivity += getColony().getProductionBonus();
            productivity = (int)unit.getType()
                .applyModifier(Math.max(1, productivity),
                    getGoodsOutputType().getId());
        }
        return Math.max(0, productivity);
    }

    /**
     * Gets the production information for this building taking account
     * of the available input and output goods.
     *
     * @param output The output goods already available in the colony,
     *     necessary in order to avoid excess production.
     * @param input The input goods available.
     * @return The production information.
     * @see ProductionCache#update
     */
    public ProductionInfo getAdjustedProductionInfo(AbstractGoods output,
                                                    List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        GoodsType outputType = getGoodsOutputType();
        GoodsType inputType = getGoodsInputType();
        if (outputType != null && outputType != output.getType()) {
            throw new IllegalArgumentException("Wrong output type: " + output.getType()
                                               + " should have been: " + outputType);
        }
        int capacity = getColony().getWarehouseCapacity();
        if (getType().hasAbility(Ability.AVOID_EXCESS_PRODUCTION)
            && output.getAmount() >= capacity) {
            // warehouse is already full: produce nothing
            return result;
        }

        int availableInput = 0;
        if (inputType != null) {
            boolean found = false;
            for (AbstractGoods goods : input) {
                if (goods.getType() == inputType) {
                    availableInput = goods.getAmount();
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("No input goods of type "
                                                   + inputType + " available.");
            }
        }

        if (outputType != null) {
            int maximumInput = 0;
            if (inputType != null && canAutoProduce()) {
                int available = getColony().getGoodsCount(outputType);
                if (available >= outputType.getBreedingNumber()) {
                    // we need at least these many horses/animals to breed
                    int divisor = (int)getType().applyModifier(0f,
                        "model.modifier.breedingDivisor");
                    int factor = (int)getType().applyModifier(0f,
                        "model.modifier.breedingFactor");
                    maximumInput = ((available - 1) / divisor + 1) * factor;
                }
            } else {
                for (Unit u : getUnitList()) {
                    maximumInput += getUnitConsumption(u);
                }
                maximumInput = Math.max(0, maximumInput);
            }
            Turn turn = getGame().getTurn();
            List<Modifier> productionModifiers = getProductionModifiers(getGoodsOutputType(), null);
            int maxProd = (int)FeatureContainer.applyModifiers(maximumInput,
                turn, productionModifiers);
            int actualInput = (inputType == null)
                ? maximumInput
                : Math.min(maximumInput, availableInput);
            // experts in factory level buildings may produce a
            // certain amount of goods even when no input is available
            if (availableInput < maximumInput
                && getType().hasAbility(Ability.EXPERTS_USE_CONNECTIONS)
                && getSpecification().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
                int minimumGoodsInput = 0;
                for (Unit unit: getUnitList()) {
                    if (unit.getType() == getExpertUnitType()) {
                        // TODO: put magic number in specification
                        minimumGoodsInput += 4;
                    }
                }
                if (minimumGoodsInput > availableInput) {
                    actualInput = minimumGoodsInput;
                }
            }
            // output is the same as input, plus production bonuses
            int prod = (int)FeatureContainer.applyModifiers(actualInput, turn,
                                                            productionModifiers);
            if (prod > 0) {
                if (getType().hasAbility(Ability.AVOID_EXCESS_PRODUCTION)) {
                    int total = output.getAmount() + prod;
                    while (total > capacity) {
                        if (actualInput <= 0) {
                            // produce nothing
                            return result;
                        } else {
                            actualInput--;
                        }
                        prod = (int)FeatureContainer.applyModifiers(actualInput,
                            turn, productionModifiers);
                        total = output.getAmount() + prod;
                        // in this case, maximum production does not
                        // exceed actual production
                        maximumInput = actualInput;
                        maxProd = prod;
                    }
                }
                prod = Math.max(0, prod);
                maxProd = Math.max(0, maxProd);
                result.addProduction(new AbstractGoods(outputType, prod));
                if (maxProd > prod) {
                    result.addMaximumProduction(new AbstractGoods(outputType, maxProd));
                }
                if (inputType != null) {
                    result.addConsumption(new AbstractGoods(inputType, actualInput));
                    if (maximumInput > actualInput) {
                        result.addMaximumConsumption(new AbstractGoods(inputType, maximumInput));
                    }
                }
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
            unit.setWorkType(getGoodsOutputType());

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
        int result = (getGoodsOutputType() == null
            || getGoodsOutputType() != goodsType) ? 0
            : getPotentialProduction(goodsType, unit.getType());
        return Math.max(0, result);
    }

    /**
     * {@inheritDoc}
     */
    public int getPotentialProduction(GoodsType goodsType, UnitType unitType) {
        int production = 0;
        if (getGoodsOutputType() == goodsType
            && getType().getBasicProduction() > 0) {
            production = (int)FeatureContainer.applyModifiers(0f,
                getGame().getTurn(), 
                getProductionModifiers(goodsType, unitType));
        }
        return Math.max(0, production);
    }

    /**
     * {@inheritDoc}
     */
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        List<Modifier> mods = new ArrayList<Modifier>();
        if (goodsType != null && goodsType == getGoodsOutputType()) {
            final BuildingType type = getType();
            final String id = goodsType.getId();
            final Turn turn = getGame().getTurn();
            final Player owner = getOwner();
            // This is fragile.  The colony contains all the buildings,
            // including this one, so as long as buildings produce
            // distinct goods, this works.
            mods.addAll(getColony().getModifierSet(id, type, turn));
            if (unitType != null) {
                mods.add(getColony().getProductionModifier(goodsType));
                mods.add(type.getProductionModifier());
                mods.addAll(unitType.getModifierSet(id, type, turn));
                // If a unit is present add unspecific owner bonuses
                // (which includes things like the Building national
                // advantage).
                if (owner != null) {
                    mods.addAll(owner.getModifierSet(id, null, turn));
                }
            } else {
                // If a unit is not present add only the owner bonuses
                // specific to the building (such as the Paine bells bonus).
                if (owner != null) {
                    mods.addAll(owner.getModifierSet(id, type, turn));
                }
            }
        }
        return mods;
    }

    /**
     * {@inheritDoc}
     */
    public GoodsType getBestWorkType(Unit unit) {
        return getGoodsOutputType();
    }

    // Omitted getClaimTemplate, buildings do not need to be claimed.


    // Interface Consumer

    /**
     * Returns true if this Consumer consumes the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean consumes(GoodsType goodsType) {
        return goodsType == getGoodsInputType();
    }

    /**
     * Returns a list of GoodsTypes this Consumer consumes.
     *
     * @return a <code>List</code> value
     */
    public List<AbstractGoods> getConsumedGoods() {
        List<AbstractGoods> result = new ArrayList<AbstractGoods>();
        GoodsType inputType = getGoodsInputType();
        if (inputType != null) {
            result.add(new AbstractGoods(inputType, 0));
        }
        return result;
    }

    /**
     * The priority of this Consumer. The higher the priority, the
     * earlier will the Consumer be allowed to consume the goods it
     * requires.
     *
     * @return an <code>int</code> value
     */
    public int getPriority() {
        return getType().getPriority();
    }

    // Serialization

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
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start
        out.writeStartElement(getXMLElementTagName());

        // Attributes
        super.writeAttributes(out);
        out.writeAttribute("buildingType", buildingType.getId());

        // Children
        super.writeChildren(out, player, showAll, toSavedGame);

        // End
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        buildingType = getSpecification().getBuildingType(in.getAttributeValue(null, "buildingType"));
    }

    /**
     * Partial writer, so that "remove" messages can be brief.
     *
     * @param out The target stream.
     * @param fields The fields to write.
     * @throws XMLStreamException If there are problems writing the stream.
     */
    @Override
    protected void toXMLPartialImpl(XMLStreamWriter out, String[] fields)
        throws XMLStreamException {
        toXMLPartialByClass(out, getClass(), fields);
    }

    /**
     * Partial reader, so that "remove" messages can be brief.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    @Override
    public void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * String converter for debugging.
     *
     * @return The name of the building.
     */
    @Override
    public String toString() {
        return Utils.lastPart(getType().getId(), ".")
            + "/" + getColony().getName();
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
