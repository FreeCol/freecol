/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Element;

/**
 * Represents a building in a colony.
 */
public final class Building extends FreeColGameObject implements Abilities, WorkLocation, Ownable, Named {
    


    
    // TODO: Remove when it isn't used in AI code
    public static final int MAX_LEVEL = 3;

    /** The type of a building. */
    public static final int NONE = -1;

    /** The colony containing this building. */
    private Colony colony;

    /**
     * List of the units which have this <code>Building</code> as it's
     * {@link Unit#getLocation() location}.
     */
    private ArrayList<Unit> units = new ArrayList<Unit>();

    private BuildingType buildingType;
    

    /**
     * Creates a new <code>Building</code>.
     * 
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The colony in which this building is located.
     * @param type The type of building.
     */
    public Building(Game game, Colony colony, BuildingType type) {
        super(game);

        this.colony = colony;
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
    public void setOwner(Player p) {
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

    /**
     * Gets the name of a building.
     * 
     * @return The name of the <code>Building</code>
     */
    public String getName() {
        return buildingType.getName();
    }

    public int getLevel() {
        return buildingType.getLevel();
    }

    /**
     * Returns the name of this location.
     * 
     * @return The name of this location.
     */
    public String getLocationName() {
        return Messages.message("inLocation", "%location%", getName());
    }

    /**
     * Gets the name of the improved building of the same type. An improved
     * building is a building of a higher level.
     * 
     * @return The name of the improved building or <code>null</code> if the
     *         improvement does not exist.
     */
    public String getNextName() {
        BuildingType next = buildingType.getUpgradesTo();
        return next != null ? next.getName() : null;
    }

    /**
     * Gets the number of hammers required for the improved building of the same
     * type.
     * 
     * @return The number of hammers required for the improved building of the
     *         same type, or <code>-1</code> if the building does not exist.
     */
    public int getNextHammers() {
        if (!canBuildNext()) {
            return -1;
        }
        if (buildingType.getUpgradesTo() != null) {
            return buildingType.getUpgradesTo().getHammersRequired();
        } else {
            return -1;
        }
    }

    /**
     * Gets the number of tools required for the improved building of the same
     * type.
     * 
     * @return The number of tools required for the improved building of the
     *         same type, or <code>-1</code> if the building does not exist.
     */
    public int getNextTools() {
        if (!canBuildNext()) {
            return -1;
        }

        if (buildingType.getUpgradesTo() != null) {
            return buildingType.getUpgradesTo().getToolsRequired();
        } else {
            return -1;
        }
    }

    /**
     * Gets the colony population required for the improved building of the same
     * type.
     * 
     * @return The colony population required for the improved building of the
     *         same type, or <code>-1</code> if the building does not exist.
     */
    public int getNextPop() {
        if (!canBuildNext()) {
            return -1;
        }

        if (buildingType.getUpgradesTo() != null) {
            return buildingType.getUpgradesTo().getPopulationRequired();
        } else {
            return -1;
        }
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
        return canBuild(buildingType.getUpgradesTo());
    }
    

    /**
     * Checks if the given building type can be built.
     * 
     * @return If this <code>Building</code> can have a higher level, that
     *         {@link FoundingFather Adam Smith} is present for manufactoring
     *         factory level buildings and that the <code>Colony</code>
     *         containing this <code>Building</code> has a sufficiently high
     *         population.
     */
    public boolean canBuild(BuildingType next) {
        if (next == null) {
            return false;
        }
        /* TODO: check required abilities for next
        if () {
            return false;
        }
         */

        if (colony.getUnitCount() < next.getPopulationRequired()) {
            return false;
        }

        return true;
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
        return buildingType.getHammersRequired() > 0;
    }
    
    /**
     * Reduces this building to previous level (is set to UpgradesFrom
     * attribute in BuildingType) or is destroyed if it's the first level
     */
    public void damage() {
        if (canBeDamaged()) {
            setType(buildingType.getUpgradesFrom());
        }
    }
    
    /**
     * Upgrades this building to next level (is set to UpgradesTo
     * attribute in BuildingType)
     */
    public void upgrade() {
    	
    	if (!canBuildNext())
    		throw new IllegalStateException("Cannot upgrade this building.");
    	
        setType(buildingType.getUpgradesTo());
        
    }
    
    private void setType(BuildingType newBuildingType) {
        // remove features from current type
        if (buildingType.getFeatures() != null) {
            Map<String, Feature> oldFeatures = buildingType.getFeatures();
            for (Entry<String, Feature> entry : oldFeatures.entrySet()) {
                colony.removeFeature(entry.getKey(), entry.getValue());
            }
        }
        colony.setDefenseBonus(colony.getDefenseBonus() - buildingType.getDefenseBonus());

        if (newBuildingType != null) {
            buildingType = newBuildingType;
            
            // add new features and abilities from new type
            Map<String, Feature> newFeatures = buildingType.getFeatures();
            for (Feature feature : newFeatures.values()) {
                colony.setFeature(feature);
            }
            colony.setDefenseBonus(colony.getDefenseBonus() + buildingType.getDefenseBonus());
            colony.putAbilities(buildingType.getAbilities());
            
            // Colonists which can't work here must be put outside
            for (Unit unit : units) {
                if (!canAdd(unit.getUnitType())) {
                    unit.setLocation(getTile());
                }
            }
        }
        
        // Colonists exceding units limit must be put outside
        while (units.size() > getMaxUnits()) {
            getLastUnit().setLocation(getTile());
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
     * Checks if the specified <code>Locatable</code> may be added to this
     * <code>WorkLocation</code>.
     * 
     * @param locatable the <code>Locatable</code>.
     * @return <i>true</i> if the <i>Unit</i> may be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAdd(Locatable locatable) {
        if (locatable.getLocation() == this) {
            return true;
        }
        
        if (getUnitCount() >= getMaxUnits()) {
            return false;
        }

        if (!(locatable instanceof Unit)) {
            return false;
        }
        return canAdd(((Unit) locatable).getUnitType());
    }

    /**
     * Checks if the specified <code>UnitType</code> may be added to this
     * <code>WorkLocation</code>.
     * 
     * @param unitType the <code>UnitTYpe</code>.
     * @return <i>true</i> if the <i>UnitType</i> may be added and <i>false</i>
     *         otherwise.
     */
    public boolean canAdd(UnitType unitType) {
        return buildingType.canAdd(unitType);
    }


    /**
     * Adds the specified <code>Locatable</code> to this
     * <code>WorkLocation</code>.
     * 
     * @param locatable The <code>Locatable</code> that shall be added to this
     *            <code>WorkLocation</code>.
     */
    public void add(Locatable locatable) {
        if (!canAdd(locatable)) {
            throw new IllegalStateException();
        }

        Unit unit = (Unit) locatable;

        if (unit.isArmed()) {
            unit.setArmed(false);
        }

        if (unit.isMounted()) {
            unit.setMounted(false);
        }

        if (unit.isMissionary()) {
            unit.setMissionary(false);
        }

        if (unit.getNumberOfTools() > 0) {
            unit.setNumberOfTools(0);
        }

        Unit student = unit.getStudent();
        if (hasAbility("model.ability.teach")) {
            if (student == null) {
                student = findStudent(unit);
                if (student != null) {
                    unit.setStudent(student);
                    student.setTeacher(unit);
                }
            }
        } else if (student != null) {
            student.setTeacher(null);
            unit.setStudent(null);
        }

        units.add(unit);
        getColony().updatePopulation();
    }

    /**
     * Returns true if the Building has the ability identified by
     * <code>id</code.
     *
     * @param id a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public boolean hasAbility(String id) {
        return buildingType.hasAbility(id);
    }

    /**
     * Sets the ability identified by <code>id</code.
     *
     * @param id a <code>String</code> value
     * @param newValue a <code>boolean</code> value
     */
    public void setAbility(String id, boolean newValue) {
    }

    /**
     * Returns the unit type being an expert in this <code>Building</code>.
     * 
     * @return The {@link Unit#getType unit type}.
     * @see Unit#getExpertWorkType
     * @see ColonyTile#getExpertForProducing
     */
    public UnitType getExpertUnitType() {
        return FreeCol.getSpecification().getExpertForProducing(getGoodsOutputType());
    }

    /**
     * Removes the specified <code>Locatable</code> from this
     * <code>WorkLocation</code>.
     * 
     * @param locatable The <code>Locatable</code> that shall be removed from
     *            this <code>WorkLocation</code>.
     */
    public void remove(Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException();
        }

        int index = units.indexOf(locatable);

        if (index != -1) {
            ((Unit) locatable).setMovesLeft(0);
            units.remove(index);
            getColony().updatePopulation();
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
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return units.contains(locatable);
        }

        return false;
    }

    /**
     * Gets the first unit in this building.
     * 
     * @return The <code>Unit</code>.
     */
    public Unit getFirstUnit() {
        if (units.size() > 0) {
            return units.get(0);
        }

        return null;
    }

    /**
     * Gets the last unit in this building.
     * 
     * @return The <code>Unit</code>.
     */
    public Unit getLastUnit() {
        if (units.size() > 0) {
            return units.get(units.size() - 1);
        }

        return null;
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

    @SuppressWarnings("unchecked")
    public List<Unit> getUnitList() {
        return (List<Unit>) units.clone();
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
     * Prepares this <code>Building</code> for a new turn.
     */
    public void newTurn() {
        if (hasAbility("model.ability.teach")) {
            trainStudents();
        }
        if (hasAbility("model.ability.repairShips")) {
            repairShips();
        }
        if (getGoodsOutputType() != null) {
            produceGoods();
        }
    }

    // Repair any damaged ships:
    private void repairShips() {
        for (Unit unit : getTile().getUnitList()) {
            if (unit.isNaval() && unit.isUnderRepair()) {
                unit.setHitpoints(unit.getHitpoints() + 1);
                if (!unit.isUnderRepair()) {
                    addModelMessage(this, "model.unit.shipRepaired",
                            new String[][] {
                                { "%unit%", unit.getName() },
                                { "%repairLocation%", getLocationName() } },
                                ModelMessage.DEFAULT, this);
                }
            }
        }
    }

    private void produceGoods() {
        int goodsInput = getGoodsInput();
        int goodsOutput = getProduction();
        GoodsType goodsInputType = getGoodsInputType();
        GoodsType goodsOutputType = getGoodsOutputType();

        if (goodsInput == 0 && getMaximumGoodsInput() > 0) {
            addModelMessage(getColony(), "model.building.notEnoughInput", new String[][] {
                    { "%inputGoods%", goodsInputType.getName() }, { "%building%", getName() },
                    { "%colony%", colony.getName() } }, ModelMessage.MISSING_GOODS,
                goodsInputType);
        }

        if (goodsOutput <= 0)
            return;

        // Actually produce the goods:
        if (goodsInputType != null) {
            colony.removeGoods(goodsInputType, goodsInput);
        }
        colony.addGoods(goodsOutputType, goodsOutput);

        if (getUnitCount() > 0) {
            int experience = goodsOutput / getUnitCount();
            for (Unit unit : getUnitList()) {
                unit.modifyExperience(experience);
            }
        }
    }

    private Unit findStudent(Unit teacher) {
        Unit student = null;
        int skill = Integer.MIN_VALUE;
        for (Unit potentialStudent : getColony().getUnitList()) {
            if (potentialStudent.canBeStudent(teacher) &&
                potentialStudent.getTeacher() == null) {
                // prefer students with higher skill levels
                if (potentialStudent.getSkillLevel() > skill) {
                    student = potentialStudent;
                    skill = student.getSkillLevel();
                }
            }
        }
        return student;
    }


    private void trainStudents() {
        Iterator<Unit> teachers = getUnitIterator();
        while (teachers.hasNext()) {
            Unit teacher = teachers.next();
            if (teacher.getStudent() == null) {
                Unit student = findStudent(teacher);
                if (student == null) {
                    addModelMessage(getColony(), "model.building.noStudent",
                                    new String[][] {
                                        { "%teacher%", teacher.getName() },
                                        { "%colony%", colony.getName() } },
                                    ModelMessage.WARNING, teacher);
                    continue;
                } else {
                    teacher.setStudent(student);
                    student.setTeacher(teacher);
                }                
            }
            int training = teacher.getTurnsOfTraining() + 1;
            if (training < teacher.getNeededTurnsOfTraining()) {
                teacher.setTurnsOfTraining(training);
            } else {
                teacher.setTurnsOfTraining(0);
                teacher.getStudent().train();
                teacher.getStudent().setTeacher(null);
                teacher.setStudent(null);
            }
        }
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
     * Returns the amount of goods needed to have a full production if
     * <code>Unit</code> was added.
     */
    public int getMaximumGoodsInputAdding(Unit unit) {
        if (getGoodsInputType() == null) {
            return 0;
        }
        return getProductivityAdding(unit);
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
        if (hasAbility("model.ability.autoProduction")) {
            return getGoodsInputAuto();
        } else {
            return getMaximumGoodsInputAdding(null);
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
        if (hasAbility("model.ability.autoProduction")) {
            return getGoodsInputAuto();
        } else {
            return calculateGoodsInput(getMaximumGoodsInput(), 0);
        }
    }
    
    private int getGoodsInputAuto() {
        if (getGoodsInputType() == null) {
            return 0;
        }
        GoodsType inputType = getGoodsInputType();
        int surplus = colony.getProductionOf(inputType);
        if (inputType.isFoodType()) {
            surplus -= colony.getFoodConsumption();
        }
        return surplus / 2; // store the half of surplus
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
        if (hasAbility("model.ability.autoProduction")) {
            return getGoodsInputAutoNextTurn();
        } else {
            return calculateGoodsInput(getMaximumGoodsInput(),
                    colony.getProductionNextTurn(getGoodsInputType()));
        }
    }

    private int calculateGoodsInput(int goodsInput, int addToWarehouse) {
        if (getGoodsInputType() != null) {
            int available = colony.getGoodsCount(getGoodsInputType()) + addToWarehouse;
            if (available < goodsInput) {
                // Not enough goods to do this?
                goodsInput = available;
            }
        }
        return goodsInput;
    }
    
    private int getGoodsInputAutoNextTurn() {
        if (getGoodsInputType() == null) {
            return 0;
        }
        GoodsType inputType = getGoodsInputType();
        int surplus = colony.getProductionNextTurn(inputType);
        if (inputType.isFoodType()) {
            surplus -= colony.getFoodConsumption();
        }
        return surplus / 2; // store the half of surplus
    }

    /**
     * Calculates the output of this building from the input if
     * <code>Unit</code> was added.
     *
     * @param new_unit The Unit that was added, to check for Expertise.
     *        <code>null</code> if not applicable
     * @param goodsInput Number of input goods,
     *        including those contributed by the new_unit, if applicable.
     */
    public int calculateOutputAdding(int goodsInput, Unit new_unit) {
        int goodsOutput = getProductionFromProductivity(goodsInput);
        if (hasAbility("model.ability.expertsUseConnections") &&
                getGameOptions().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
            int minimumProduction = 0;
            Iterator<Unit> i = getUnitIterator();
            while (i.hasNext()) {
                Unit unit = (Unit)i.next();
                if (unit.getUnitType() == getExpertUnitType()) {
                    minimumProduction += 4;
                }
            }
            if (new_unit != null && canAdd(new_unit) && new_unit.getUnitType() == getExpertUnitType()) {
                minimumProduction += 4;
            }
            if (goodsOutput < minimumProduction) {
                goodsOutput = minimumProduction;
            }
        }
        return goodsOutput;
    }

    /**
     * Calculates and returns the output of this building from the input.
     *
     * @param goodsInput The quantity of consumed goods
     * @return The production of this building from the input.
     * @see #getProduction
     * @see #getProductionNextTurn
     */
    public int calculateOutput(int goodsInput) {
        return calculateOutputAdding(goodsInput, null);
    }

    /**
     * Returns the actual production of this building if
     * <Code>Unit</code> was added.
     *
     * @param unit The Unit that was added
     * @return The amount of goods being produced by this <code>Building</code>
     */
    public int getProductionAdding(Unit unit) {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int goodsOutput = getMaximumProductionAdding(unit);

        if (getGoodsInputType() != null) {
            int available = colony.getGoodsCount(getGoodsInputType());
            if (available < getMaximumGoodsInputAdding(unit)) {
                goodsOutput = calculateOutputAdding(available, unit);
            }
        }

        return goodsOutput;
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
        if (hasAbility("model.ability.autoProduction")) {
            return getAutoProduction();
        } else {
            return getProductionAdding(null);
        }
    }
    
    /**
     * Returns the actual production of a buliding with 0 workplaces
     */
    private int getAutoProduction() {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int goodsOutput = getMaximumAutoProduction();

        if (getGoodsInputType() != null) {
            int available = getGoodsInput();
            if (available < goodsOutput) {
                goodsOutput = available;
            }
        }

        return getProductionFromProductivity(goodsOutput);
    }

    /**
     * Returns the actual production of this building for next turn.
     * 
     * @return The production of this building the next turn.
     * @see #getProduction
     */
    public int getProductionNextTurn() {
        if (hasAbility("model.ability.autoProduction")) {
            return getAutoProductionNextTurn();
        } else {
            return getProductionNextTurnAdding(null);
        }
    }

    /**
     * Returns the actual production of this building for next turn.
     * 
     * @param unit The Unit that was added
     * @return The production of this building the next turn.
     * @see #getProduction
     */
    public int getProductionNextTurnAdding(Unit unit) {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int goodsOutput = getMaximumProductionAdding(unit);

        if (getGoodsInputType() != null) {
            int available = colony.getGoodsCount(getGoodsInputType()) + 
                    colony.getProductionNextTurn(getGoodsInputType());
            if (available < getMaximumGoodsInputAdding(unit)) {
                goodsOutput = calculateOutputAdding(available, unit);
            }
        }

        return goodsOutput;
    }
    
    /**
     * Returns the actual production of a buliding with 0 workplaces for next turn
     */
    private int getAutoProductionNextTurn() {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int goodsOutput = getMaximumAutoProduction();

        if (getGoodsInputType() != null) {
            int available = getGoodsInputNextTurn();
            if (available < goodsOutput) {
                goodsOutput = available;
            }
        }

        return getProductionFromProductivity(goodsOutput);
    }

    /**
     * Returns the additional production of new <code>Unit</code> at this building for next turn.
     * 
     * @return The production of this building the next turn.
     * @see #getProduction
     */
    public int getAdditionalProductionNextTurn(Unit addUnit) {
        return getProductionNextTurnAdding(addUnit) - getProductionNextTurn();
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
     * Returns the maximum productivity of worker/s working 
     * in this building after adding the given unit
     *
     * @param addUnit A unit to guess productivity adding it
     * @return The maximum returns from workers in this building,
     *         assuming enough "input goods".
     */
    public int getProductivityAdding(Unit addUnit) {
        int productivity = getProductivity();
        if (addUnit != null && canAdd(addUnit)) {
            productivity += getProductivity(addUnit);
        }
        return productivity;
    }
    
    /**
     * Returns the maximum productivity of worker/s currently working
     * in this building.
     *
     * It doesn't add production bonus of colony
     *
     * @return The maximum returns from workers in this building,
     *         assuming enough "input goods".
     */
    public int getProductivity() {
        if (getGoodsOutputType() == null) {
            return 0;
        }

        int productivity = 0;
        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            productivity += getProductivity(unitIterator.next());
        }
        return productivity;
    }

    /**
     * Returns the maximum productivity of a unit working in this building.
     *
     * @return The maximum returns from this unit if in this <code>Building</code>,
     *         assuming enough "input goods".
     */
    public int getProductivity(Unit prodUnit) {
        if (getGoodsOutputType() == null) {
            return 0;
        }
        if (prodUnit == null) {
            return 0;
        }

        int base = buildingType.getBasicProduction();
        int productivity = prodUnit.getProductionOf(getGoodsOutputType(), base);
        if (productivity > 0) {
            productivity += colony.getProductionBonus();
            if (productivity < 1)
                productivity = 1;
        }
        return productivity;
    }

    /**
     * Returns the maximum production of this building.
     * 
     * @return The production of this building, with the current amount of
     *         workers, when there is enough "input goods".
     */
    public int getMaximumProduction() {
        if (hasAbility("model.ability.autoProduction")) {
            return getMaximumAutoProduction();
        } else {
            return getMaximumProductionAdding(null);
        }
    }
    
    /**
     * Returns the maximum production of a buliding with 0 workplaces
     */
    private int getMaximumAutoProduction() {
        int available = colony.getGoodsCount(getGoodsOutputType());
        if (available < 2) {
            return 0;
        }
        return Math.max(1, available / 10);
    }

    /**
     * Returns the maximum production with a given unit to be added to this building.
     *
     * @return Maximum production potential from all workers
     *         with additional worker if valid.
     */
    public int getMaximumProductionAdding(Unit addUnit) {
        int maximumProductivity = getProductivityAdding(addUnit);
        return getProductionFromProductivity(maximumProductivity);
    }

    /**
     * Returns the maximum production of a given unit to be added to this building.
     *
     * @return If unit can be added, will return the maximum production potential
     *         if it cannot be added, will return <code>0</code>.
     */
    public int getAdditionalProduction(Unit addUnit) {
        return getProductionAdding(addUnit) - getProduction();
    }

    /**
     * Returns the Production from this building applying the production bonus
     * of the colony to the given productivity of the worker(s).
     *
     * @param productivity From {@link #getProductivity}
     * @return Production based on Productivity
     */
    public int getProductionFromProductivity(int productivity) {
        GoodsType goodsOutputType = getGoodsOutputType();
        if (goodsOutputType == null) {
            return 0;
        }
        Player player = colony.getOwner();

        float goodsOutput = productivity;
        Modifier modifier = colony.getModifier(goodsOutputType.getId());
        if (modifier != null) {
            goodsOutput = modifier.applyTo(productivity);
        }

        // TODO: Be sure this bonus is returned with colony.getModifier and remove
        if (goodsOutputType == Goods.CROSSES &&
            player.hasFather(FreeCol.getSpecification().getFoundingFather("model.foundingFather.williamPenn"))) {
            goodsOutput += goodsOutput / 2;
        }

        return (int) goodsOutput;
    }

    /**
     * Disposes this building. All units that currently has this
     * <code>Building as it's location will be disposed</code>.
     */
    @Override
    public void dispose() {
        for (int i = 0; i < units.size(); i++) {
            units.get(i).dispose();
        }

        super.dispose();
    }

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
        out.writeAttribute("buildingType", Integer.toString(buildingType.getIndex()));

        // Add child elements:
        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            ((FreeColGameObject) unitIterator.next()).toXML(out, player, showAll, toSavedGame);
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

        colony = (Colony) getGame().getFreeColGameObject(in.getAttributeValue(null, "colony"));
        if (colony == null) {
            colony = new Colony(getGame(), in.getAttributeValue(null, "colony"));
        }
        int buildingTypeIndex = Integer.parseInt(in.getAttributeValue(null, "buildingType"));
        buildingType = FreeCol.getSpecification().getBuildingType(buildingTypeIndex);

        units.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            Unit unit = (Unit) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
            if (unit != null) {
                unit.readFromXML(in);
                if (!units.contains(unit)) {
                    units.add(unit);
                }
            } else {
                unit = new Unit(getGame(), in);
                units.add(unit);
            }
        }
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
