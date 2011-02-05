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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


import org.w3c.dom.Element;

/**
 * Represents a building in a colony.
 */
public class Building extends FreeColGameObject
    implements WorkLocation, Ownable, Named, Comparable<Building>, Consumer {

    private static Logger logger = Logger.getLogger(Building.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The colony containing this building. */
    protected Colony colony;

    /** The type of building. */
    protected BuildingType buildingType;

    /**
     * List of the units which have this <code>Building</code> as it's
     * {@link Unit#getLocation() location}.
     */
    private final List<Unit> units = new ArrayList<Unit>();


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
     * @param colony The colony in which this building is located.
     * @param type The type of building.
     */
    protected Building(Game game) {
        super(game);
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
     * Initiates a new <code>Building</code> from an XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize this object.
     */
    public Building(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Building</code> with the given ID. The object
     * should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Building(Game game, String id) {
        super(game, id);
    }

    /**
     * Gets the owner of this <code>Ownable</code>.
     *
     * @return The <code>Player</code> controlling this {@link Ownable}.
     */
    public Player getOwner() {
        return colony.getOwner();
    }

    /**
     * Sets the owner of this <code>Ownable</code>.
     *
     * @param p The <code>Player</code> that should take ownership of this
     *            {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by this method.
     */
    public void setOwner(final Player p) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the <code>Tile</code> where this <code>Building</code> is
     * located.
     *
     * @return The <code>Tile</code>.
     */
    public Tile getTile() {
        return colony.getTile();
    }

    public String getNameKey() {
        return buildingType.getNameKey();
    }

    /**
     * Returns the level of this building.
     *
     * @return an <code>int</code> value
     */
    public int getLevel() {
        return buildingType.getLevel();
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    public StringTemplate getLocationName() {
        return StringTemplate.template("inLocation")
            .add("%location%", getNameKey());
    }

    /**
     * Returns the name of this location for a particular player.
     *
     * @param player The <code>Player</code> to prepare the name for.
     * @return The name of this location.
     */
    public StringTemplate getLocationNameFor(Player player) {
        return getLocationName();
    }

    /**
     * Gets the name of the improved building of the same type. An improved
     * building is a building of a higher level.
     *
     * @return The name of the improved building or <code>null</code> if the
     *         improvement does not exist.
     */
    public String getNextNameKey() {
        final BuildingType next = buildingType.getUpgradesTo();
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
        return getColony().canBuild(buildingType.getUpgradesTo());
    }


    /**
     * Gets a pointer to the settlement containing this building.
     *
     * @return This colony.
     */
    public Settlement getSettlement() {
        return colony;
    }

    /**
     * Gets a pointer to the colony containing this building.
     *
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Gets the type of this building.
     *
     * @return The type.
     */
    public BuildingType getType() {
        return buildingType;
    }

    /**
     * Returns whether this building can be damaged
     *
     * @return <code>true</code> if can be damaged
     * @see #damage
     */
    public boolean canBeDamaged() {
        return !buildingType.isAutomaticBuild()
            && !colony.isAutomaticBuild(buildingType);
    }

    /**
     * Reduces this building to previous level (is set to UpgradesFrom
     * attribute in BuildingType) or is destroyed if it's the first level
     *
     * @return True if the building was damaged.
     */
    public boolean damage() {
        if (canBeDamaged()) {
            setType(buildingType.getUpgradesFrom());
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
            setType(buildingType.getUpgradesTo());
            return true;
        }
        return false;
    }

    private void setType(final BuildingType newBuildingType) {
        // remove features from current type
        colony.getFeatureContainer().remove(buildingType.getFeatureContainer());

        if (newBuildingType != null) {
            buildingType = newBuildingType;

            // add new features and abilities from new type
            colony.getFeatureContainer().add(buildingType.getFeatureContainer());

            // Colonists which can't work here must be put outside
            for (Unit unit : units) {
                if (!canAdd(unit.getType())) {
                    unit.putOutsideColony();
                }
            }
        }

        // Colonists exceding units limit must be put outside
        while (units.size() > getMaxUnits()) {
            getLastUnit().putOutsideColony();
        }
    }

    /**
     * Gets the maximum number of units allowed in this <code>Building</code>.
     *
     * @return The number.
     */
    public int getMaxUnits() {
        return buildingType.getWorkPlaces();
    }

    /**
     * Gets the amount of units at this <code>WorkLocation</code>.
     *
     * @return The amount of units at this {@link WorkLocation}.
     */
    public int getUnitCount() {
        return units.size();
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
     * Checks if the specified <code>Locatable</code> may be added to this
     * <code>WorkLocation</code>.
     *
     * @param locatable the <code>Locatable</code>.
     * @return <i>true</i> if the <i>Unit</i> may be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAdd(final Locatable locatable) {
        if (locatable.getLocation() == this) {
            return true;
        }

        if (getUnitCount() >= getMaxUnits()) {
            return false;
        }

        if (!(locatable instanceof Unit)) {
            return false;
        }
        return canAdd(((Unit) locatable).getType());
    }

    /**
     * Checks if the specified <code>UnitType</code> may be added to this
     * <code>WorkLocation</code>.
     *
     * @param unitType the <code>UnitTYpe</code>.
     * @return <i>true</i> if the <i>UnitType</i> may be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAdd(final UnitType unitType) {
        return buildingType.canAdd(unitType);
    }


    /**
     * Adds the specified locatable to this building.
     *
     * @param locatable The <code>Locatable</code> to add.
     */
    public void add(final Locatable locatable) {
        if (!canAdd(locatable)) {
            throw new IllegalStateException("Can not add " + locatable
                                            + " to " + toString());
        }
        if (units.contains(locatable)) return;

        final Unit unit = (Unit) locatable;
        units.add(unit);
        unit.setState(Unit.UnitState.IN_COLONY);

        if (buildingType.hasAbility("model.ability.teach")) {
            Unit student = unit.getStudent();
            if (student == null && (student = findStudent(unit)) != null) {
                unit.setStudent(student);
                student.setTeacher(unit);
            }
        }
    }

    /**
     * Removes the specified locatable from this building.
     *
     * @param locatable The <code>Locatable</code> to remove.
     */
    public void remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Can only remove units from building.");
        }

        final Unit unit = (Unit) locatable;
        if (units.remove(unit)) {
            unit.setMovesLeft(0);
            unit.setState(Unit.UnitState.ACTIVE);

            if (buildingType.hasAbility("model.ability.teach")) {
                Unit student = unit.getStudent();
                if (student != null) {
                    student.setTeacher(null);
                    unit.setStudent(null);
                }
            }
        }
    }

    /**
     * Checks if this <code>Building</code> contains the specified
     * <code>Locatable</code>.
     *
     * @param locatable The <code>Locatable</code> to test the presence of.
     * @return
     *            <ul>
     *            <li><code>>true</code>if the specified
     *            <code>Locatable</code> is in this <code>Building</code>
     *            and</li>
     *            <li><code>false</code> otherwise.</li>
     *            </ul>
     */
    public boolean contains(final Locatable locatable) {
        return units.contains(locatable);
    }

    /**
     * Gets the first unit in this building.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getFirstUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(0);
        }
    }

    /**
     * Gets the last unit in this building.
     *
     * @return The <code>Unit</code>.
     */
    public Unit getLastUnit() {
        if (units.isEmpty()) {
            return null;
        } else {
            return units.get(units.size() - 1);
        }
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> directly
     * located on this <code>Building</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Unit> getUnitIterator() {
        return units.iterator();
    }

    public List<Unit> getUnitList() {
        return new ArrayList<Unit>(units);
    }

    /**
     * Gets this <code>Location</code>'s <code>GoodsContainer</code>.
     *
     * @return <code>null</code>.
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }

    /**
     * Find a student for the specified teacher.
     * Do not search if ALLOW_STUDENT_SELECTION is true--- its the player's
     * job then.
     *
     * @param teacher The teacher <code>Unit</code> that needs a student.
     * @return A potential student, or null of none found.
     */
    public Unit findStudent(final Unit teacher) {
        if (getSpecification().getBoolean(GameOptions.ALLOW_STUDENT_SELECTION)){
            return null; // Do not automatically assign students.
        }
        Unit student = null;
        GoodsType expertProduction = teacher.getType().getExpertProduction();
        int skillLevel = INFINITY;
        for (Unit potentialStudent : getColony().getUnitList()) {
            /**
             * Always pick the student with the least skill first.
             * Break ties by favouring the one working in the teacher's trade,
             * otherwise first applicant wins.
             */
            if (potentialStudent.getTeacher() == null
                && potentialStudent.canBeStudent(teacher)) {
                if (student == null
                    || potentialStudent.getSkillLevel() < skillLevel
                    || (potentialStudent.getSkillLevel() == skillLevel
                        && potentialStudent.getWorkType() == expertProduction)){
                    student = potentialStudent;
                    skillLevel = student.getSkillLevel();
                }
            }
        }
        return student;
    }

    /**
     * Returns the type of goods this <code>Building</code> produces.
     *
     * @return The type of goods this <code>Building</code> produces or
     *         <code>-1</code> if there is no goods production by this
     *         <code>Building</code>.
     */
    public GoodsType getGoodsOutputType() {
        return getType().getProducedGoodsType();
    }

    /**
     * Returns the type of goods this building needs for input.
     *
     * @return The type of goods this <code>Building</code> requires as input
     *         in order to produce it's {@link #getGoodsOutputType output}.
     */
    public GoodsType getGoodsInputType() {
        return getType().getConsumedGoodsType();
    }

    /**
     * Returns the amount of goods needed to have a full production.
     *
     * @return The maximum level of goods needed in order to have the maximum
     *         possible production with the current configuration of workers and
     *         improvements. This is actually the {@link #getGoodsInput input}
     *         being used this turn, provided that the amount of goods in the
     *         <code>Colony</code> is either larger or the same as the value
     *         returned by this method.
     * @see #getGoodsInput
     * @see #getProduction
     */
    public int getMaximumGoodsInput() {
        if (getGoodsInputType() == null) {
            return 0;
        } else if (canAutoProduce()) {
            return getMaximumAutoProduction();
        } else {
            return getProductivity();
        }
    }

    protected int getStoredInput() {
        if (getGoodsInputType() == null) {
            return 0;
        } else {
            return colony.getGoodsCount(getGoodsInputType());
        }
    }

    /**
     * Returns the amount of goods being used to get the current
     * {@link #getProduction production}.
     *
     * @return The actual amount of goods that is being used to support the
     *         current production.
     * @see #getMaximumGoodsInput
     * @see #getProduction
     */
    public int getGoodsInput() {
        GoodsType inputType = getGoodsInputType();
        if (inputType == null) {
            return 0;
        } else if (canAutoProduce()) {
            return getMaximumAutoProduction();
        } else {
            return Math.min(getMaximumGoodsInput(), getStoredInput());
        }
    }

    /**
     * Returns the amount of goods being used to get the current
     * {@link #getProduction production} at the next turn.
     *
     * @return The actual amount of goods that will be used to support the
     *         production at the next turn.
     * @see #getMaximumGoodsInput
     * @see #getProduction
     */
    public int getGoodsInputNextTurn() {
        int available = colony.getProductionNextTurn(getGoodsInputType());
        if (!canAutoProduce()) {
            available += getStoredInput();
        }
        return getGoodsInputNextTurn(available);
    }

    /**
     * Returns the amount of goods being used to get the current
     * {@link #getProduction production} at the next turn.
     *
     * @param available the amount of input goods available
     * @return The actual amount of goods that will be used to support the
     *         production at the next turn.
     * @see #getMaximumGoodsInput
     * @see #getProduction
     */
    public int getGoodsInputNextTurn(int available) {
        GoodsType inputType = getGoodsInputType();
        if (inputType == null) {
            return 0;
        } else if (canAutoProduce()) {
            return getGoodsInputAuto(available);
        } else {
            return Math.min(getMaximumGoodsInput(), available);
        }
    }

    private int getGoodsInputAuto(int available) {
        if (getGoodsInputType() == null) {
            return 0;
        } else {
            int outputGoods = colony.getGoodsCount(getGoodsOutputType());
            if (outputGoods < getGoodsOutputType().getBreedingNumber()) {
                // not enough animals to breed
                return 0;
            } else if (outputGoods >= colony.getWarehouseCapacity()) {
                // warehouse is already full
                return 0;
            } else {
                return available;
            }
        }
    }

    /**
     * Returns the actual production of this building given the number
     * of input goods available, and optionally adding any number of
     * <Code>Unit</code>s.
     *
     * @param availableGoodsInput The amount of input goods available
     * @param additionalUnits The Units to be added
     * @return The amount of goods being produced by this <code>Building</code>
     */
    protected int getProductionAdding(int availableGoodsInput, Unit... additionalUnits) {
        if (getGoodsOutputType() == null) {
            return 0;
        } else {
            int maximumGoodsInput = getProductivity(additionalUnits);
            if (getGoodsInputType() != null) {
                // only consider alternatives if we really need input
                if (availableGoodsInput < maximumGoodsInput) {
                    maximumGoodsInput = availableGoodsInput;
                }
                if (buildingType.hasAbility("model.ability.expertsUseConnections") &&
                    getSpecification().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
                    int minimumGoodsInput = 0;
                    for (Unit unit: units) {
                        if (unit.getType() == getExpertUnitType()) {
                            minimumGoodsInput += 4;
                        }
                    }
                    for (Unit unit : additionalUnits) {
                        if (canAdd(unit) && unit.getType() == getExpertUnitType()) {
                            minimumGoodsInput += 4;
                        }
                    }
                    if (maximumGoodsInput < minimumGoodsInput) {
                        maximumGoodsInput = minimumGoodsInput;
                    }
                }
            }
            // output is the same as input, plus production bonuses
            return applyModifiers(maximumGoodsInput);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ProductionInfo getProductionInfo(List<AbstractGoods> input) {
        ProductionInfo result = new ProductionInfo();
        for (AbstractGoods goods : input) {
            if (goods.getType() == getGoodsInputType()) {
                int amount = canAutoProduce()
                    ? getAutoProduction(goods.getAmount())
                    : getProductionAdding(goods.getAmount());
                result.addProduction(new AbstractGoods(getGoodsOutputType(), amount));
                result.addConsumption(new AbstractGoods(getGoodsInputType(), getGoodsInput()));
                result.addMaximumProduction(new AbstractGoods(getGoodsOutputType(), getMaximumProduction()));
                break;
            }
        }
        return result;
    }


    /**
     * Returns the actual production of this building.
     *
     * @return The amount of goods being produced by this <code>Building</code>
     *         the current turn. The type of goods being produced is given by
     *         {@link #getGoodsOutputType}.
     * @see #getProductionNextTurn
     * @see #getMaximumProduction
     */
    public int getProduction() {
        if (canAutoProduce()) {
            return getAutoProduction(getGoodsInput());
        } else {
            return getProductionAdding(getStoredInput());
        }
    }

    /**
     * Returns the actual production of this building for next turn.
     *
     * @return The production of this building the next turn.
     * @see #getProduction
     */
    public int getProductionNextTurn() {
        if (canAutoProduce()) {
            int rawInput = colony.getSurplusFoodProduction(getGoodsInputType());
            if (rawInput > 0) {
                int input = getGoodsInputAuto(rawInput);
                int output = getAutoProduction(input);
                System.out.println("Raw input: " + rawInput + ", input: " + input
                                   + " --> " + output);
                return output;
            } else {
                return 0;
            }
        } else if (getGoodsInputType() == null) {
            return getProductionAdding(0);
        } else {
            return getProductionAdding(getStoredInput() +
                                       colony.getProductionNextTurn(getGoodsInputType()));
        }
    }

    /**
     * Returns true if this building can produce goods without workers.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canAutoProduce() {
        return !buildingType.getModifierSet("model.modifier.autoProduction").isEmpty();
    }

    /**
     * Returns the production of a building with no workplaces with
     * given input. Unlike other buildings, buildings with no
     * workplaces stop producing as soon as the warehouse capacity has
     * been reached. In the original game, the only building of this
     * type is the pasture/stable.
     *
     * @param availableInput an <code>int</code> value
     * @return an <code>int</code> value
     */
    protected int getAutoProduction(int availableInput) {
        if (getGoodsOutputType() == null ||
            colony.getGoodsCount(getGoodsOutputType()) >= colony.getWarehouseCapacity()) {
            return 0;
        }

        int goodsOutput = getMaximumAutoProduction();

        // Limit production to available raw materials
        if (getGoodsInputType() != null && availableInput < goodsOutput) {
            goodsOutput = availableInput;
        }

        // apply modifiers, if any
        goodsOutput = applyModifiers(goodsOutput);

        // auto-produced goods should not overflow
        int availSpace = colony.getWarehouseCapacity() - colony.getGoodsCount(getGoodsOutputType());
        if (goodsOutput > availSpace) {
            goodsOutput = availSpace;
        }
        return goodsOutput;
    }

    /**
     * Returns the additional production of new <code>Unit</code> at this building for next turn.
     *
     * @return The production of this building the next turn.
     * @see #getProduction
     */
    public int getAdditionalProductionNextTurn(Unit addUnit) {
        return getProductionAdding(getStoredInput() +
                                   colony.getProductionNextTurn(getGoodsInputType()), addUnit) -
            getProductionNextTurn();
    }

    /**
     * Returns the production of the given type of goods.
     *
     * @param goodsType The type of goods to get the production for.
     * @return the production og the given goods this turn. This method will
     *         return the same as {@link #getProduction} if the given type of
     *         goods is the same as {@link #getGoodsOutputType} and
     *         <code>0</code> otherwise.
     */
    public int getProductionOf(GoodsType goodsType) {
        if (goodsType == getGoodsOutputType()) {
            return getProduction();
        }

        return 0;
    }

    /**
     * Returns the maximum productivity of worker/s currently working
     * in this building.
     *
     * @param additionalUnits units to add before calculating result
     * @return The maximum returns from workers in this building,
     *         assuming enough "input goods".
     */
    private int getProductivity(Unit... additionalUnits) {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int productivity = 0;
        for (Unit unit : units) {
            productivity += getUnitProductivity(unit);
        }
        for (Unit unit : additionalUnits) {
            if (canAdd(unit)) {
                productivity += getUnitProductivity(unit);
            }
        }
        return productivity;
    }

    /**
     * Returns the maximum productivity of a unit working in this building.
     *
     * @return The maximum returns from this unit if in this <code>Building</code>,
     *         assuming enough "input goods".
     */
    public int getUnitProductivity(Unit prodUnit) {
        if (getGoodsOutputType() == null || prodUnit == null) {
            return 0;
        }

        int productivity = buildingType.getBasicProduction();
        if (productivity > 0) {
            productivity += colony.getProductionBonus();
            return (int) prodUnit.getType().getFeatureContainer()
                .applyModifier(Math.max(1, productivity),
                               getGoodsOutputType().getId());
        } else {
            return 0;
        }
    }

    /**
     * Returns the maximum production of this building.
     *
     * @return The production of this building, with the current amount of
     *         workers, when there is enough "input goods".
     */
    public int getMaximumProduction() {
        if (canAutoProduce()) {
            return getMaximumAutoProduction();
        } else {
            return applyModifiers(getProductivity());
        }
    }

    /**
     * Returns the maximum production of a building that breeds
     * animals.
     *
     * TODO: make this more generic
     */
    private int getMaximumAutoProduction() {
        int available = colony.getGoodsCount(getGoodsOutputType());
        System.out.println(available + " " + getGoodsOutputType() + " available");
        if (available < getGoodsOutputType().getBreedingNumber()) {
            // we need at least two horses/animals to breed
            System.out.println("Too few animals to breed");
            return 0;
        }

        int result = (int) getType().getFeatureContainer()
            .applyModifier(available, "model.modifier.autoProduction");
        System.out.println("Maximum autoproduction is " + Math.max(1, result));
        return Math.max(1, result);
    }

    /**
     * Returns the maximum production of a given unit to be added to this building.
     *
     * @return If unit can be added, will return the maximum production potential
     *         if it cannot be added, will return <code>0</code>.
     */
    public int getAdditionalProduction(Unit addUnit) {
        return getProductionAdding(getStoredInput(), addUnit) - getProduction();
    }

    /**
     * Returns the Production from this building applying the production bonus
     * of the colony to the given productivity of the worker(s).
     *
     * @param productivity From {@link #getProductivity}
     * @return Production based on Productivity
     */
    // TODO: this really should not be public
    public int applyModifiers(int productivity) {
        GoodsType goodsOutputType = getGoodsOutputType();
        if (goodsOutputType == null) {
            return 0;
        }
        /*
        return Math.round(colony.getFeatureContainer().applyModifier(productivity,
                                                                     goodsOutputType.getId(),
                                                                     buildingType, getGame().getTurn()));
        */
        List<Modifier> modifiers =
            new ArrayList<Modifier>(colony.getFeatureContainer().
                                    getModifierSet(goodsOutputType.getId(), buildingType, getGame().getTurn()));
        Collections.sort(modifiers);
        return (int) FeatureContainer.applyModifiers(productivity, getGame().getTurn(), modifiers);

    }

    public int compareTo(Building other) {
        return getType().compareTo(other.getType());
    }


    /**
     * Dispose of this building.
     *
     * @return A list of disposed objects.
     */
    @Override
    public List<FreeColGameObject> disposeList() {
        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        while (units.size() > 0) {
            objects.addAll(units.remove(0).disposeList());
        }
        objects.addAll(super.disposeList());
        return objects;
    }

    /**
     * Disposes this building. All units that currently has this
     * <code>Building as it's location will be disposed</code>.
     */
    @Override
    public void dispose() {
        disposeList();
    }


    // Interface Consumer

    /**
     * Returns the number of units of the given GoodsType this
     * Building consumes per turn.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param available an <code>int</code> value
     * @return units consumed
     */
    public int getConsumedAmount(GoodsType goodsType, int available) {
        if (consumes(goodsType)) {
            return getGoodsInputNextTurn(available);
        } else {
            return 0;
        }
    }

    /**
     * Returns true if this Consumer consumes the given GoodsType.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean consumes(GoodsType goodsType) {
        return goodsType == getGoodsInputType()
            || !(buildingType.getFeatureContainer()
                 .getModifierSet("model.modifier.storeSurplus", goodsType)
                 .isEmpty());
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
            result.add(new AbstractGoods(inputType, getConsumedAmount(inputType, Integer.MAX_VALUE)));
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
        return buildingType.getPriority();
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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute("ID", getId());
        out.writeAttribute("colony", colony.getId());
        out.writeAttribute("buildingType", buildingType.getId());

        // Add child elements:
        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            unitIterator.next().toXML(out, player, showAll, toSavedGame);
        }

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        colony = getFreeColGameObject(in, "colony", Colony.class);
        buildingType = getSpecification().getBuildingType(in.getAttributeValue(null, "buildingType"));

        units.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            Unit unit = updateFreeColGameObject(in, Unit.class);
            if (!units.contains(unit)) units.add(unit);
        }
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
    protected void readFromXMLPartialImpl(XMLStreamReader in)
        throws XMLStreamException {
        readFromXMLPartialByClass(in, getClass());
    }

    /**
     * String converter for debugging.
     *
     * @return The name of the building.
     */
    public String toString() {
        return getType().getId() + " [" + colony.getName() + "]";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "building";
    }
}
