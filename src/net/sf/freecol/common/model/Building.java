package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Element;

/**
 * Represents a building in a colony.
 * 
 * <br>
 * <br>
 * 
 * Each <code>Building</code> has a type and a level. The levels are
 * {@link #NOT_BUILT}, {@link #HOUSE}, {@link #SHOP} and {@link #FACTORY}.
 * The {@link #getName name} of a <code>Building</code> depends on both the
 * type and the level:
 * 
 * <br>
 * <br>
 * Type {@link #STOCKADE} <br>
 * Level {@link #NOT_BUILT}: <i>null</i> <br>
 * Level {@link #HOUSE}: "Stockade" <br>
 * Level {@link #SHOP}: "Fort" <br>
 * Level {@link #FACTORY}: "Fortress"
 * 
 */
public final class Building extends FreeColGameObject implements WorkLocation, Ownable, Nameable {
    
    public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /**
     * The maximum level.
     */
    public static final int MAX_LEVEL = 3;

    /** The type of a building. */
    public static final int NONE = -1, TOWN_HALL = 0, CARPENTER = 1, BLACKSMITH = 2, TOBACCONIST = 3, WEAVER = 4,
            DISTILLER = 5, FUR_TRADER = 6, SCHOOLHOUSE = 7, // 10
            ARMORY = 8, CHURCH = 9, // 13
            STOCKADE = 10, // 7
            WAREHOUSE = 11, STABLES = 12, DOCK = 13, // 9
            PRINTING_PRESS = 14, CUSTOM_HOUSE = 15;

    /** The maximum number of building types. */
    public static final int NUMBER_OF_TYPES = FreeCol.getSpecification().numberOfBuildingTypes();

    /** The level of a building. */
    public static final int NOT_BUILT = 0, HOUSE = 1, SHOP = 2, FACTORY = 3;

    /**
     * Sets the maximum number of units in one building. This will become a
     * non-constant later so always use the {@link #getMaxUnits()}.
     */
    private static final int MAX_UNITS = 3;

    /** The colony containing this building. */
    private Colony colony;

    /** The type of this building. */
    private int type;

    /**
     * The level this building has. This should be on of:
     * <ul>
     * <li>{@link #NOT_BUILT}</li>
     * <li>{@link #HOUSE}</li>
     * <li>{@link #SHOP}</li>
     * <li>{@link #FACTORY}</li>
     * </ul>
     */
    private int level;

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
     * @param level The level of the building: {@link #NOT_BUILT},
     *            {@link #HOUSE}, {@link #SHOP} or {@link #FACTORY}.
     */
    public Building(Game game, Colony colony, int type, int level) {
        super(game);

        this.colony = colony;
        this.type = type;
        this.level = level;

        buildingType = FreeCol.getSpecification().buildingType(type);
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

        buildingType = FreeCol.getSpecification().buildingType(type);
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

        buildingType = FreeCol.getSpecification().buildingType(type);
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
     * Gets the level of the building. One of {@link #NOT_BUILT},
     * {@link #HOUSE}, {@link #SHOP} and {@link #FACTORY}.
     * 
     * @return The current level.
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets the level of the building.
     * 
     * @param level The new level of the building. This should be one of
     *            {@link #NOT_BUILT}, {@link #HOUSE}, {@link #SHOP} and
     *            {@link #FACTORY}.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Gets the name of a building.
     * 
     * @return The name of the <code>Building</code> or <i>null</i> if the
     *         building has not been built.
     */
    public String getName() {
        if (isBuilt()) {
            return getName(type, level);
        } else {
            return null;
        }
    }

    /**
     * Gets the name of a building.
     *
     * @param someType the type of building
     * @param someLevel the level of the building
     * @return a <code>String</code> value
     */
    public static String getName(int someType, int someLevel) {
        String id = FreeCol.getSpecification().buildingType(someType).level(someLevel - 1).name;
        return Messages.message(id);
    }

    /**
     * Set the <code>Name</code> value.
     * 
     * @param newName The new Name value.
     */
    public void setName(String newName) {
        // this.name = newName;
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
        return level < buildingType.numberOfLevels() ?
               Messages.message(buildingType.level(level).name) : null;
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

        return level < buildingType.numberOfLevels() ? buildingType.level(level).hammersRequired : -1;
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

        return level < buildingType.numberOfLevels() ? buildingType.level(level).toolsRequired : -1;
    }

    /**
     * Gets the colony population required for the improved building of the same
     * type.
     * 
     * @return The colony population required for the improved building of the
     *         same type, or <code>-1</code> if the building does not exist.
     */
    public int getNextPop() {
        return level < buildingType.numberOfLevels() ? buildingType.level(level).populationRequired : -1;
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
        if (level >= MAX_LEVEL) {
            return false;
        }
        if (level + 1 >= FACTORY
                && !getColony().getOwner().hasFather(FoundingFather.ADAM_SMITH)
                && (type == BLACKSMITH || type == TOBACCONIST || type == WEAVER || type == DISTILLER
                        || type == FUR_TRADER || type == ARMORY)) {
            return false;
        }

        // if there are no more improvements available for this building type..
        if (buildingType.numberOfLevels() < level + 1) {
            return false;
        }

        if (getType() == DOCK && getColony().isLandLocked()) {
            return false;
        }

        if (buildingType.level(level).populationRequired > colony.getUnitCount()) {
            return false;
        }

        if (getType() == CUSTOM_HOUSE && !getColony().getOwner().hasFather(FoundingFather.PETER_STUYVESANT)) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the building has been built.
     * 
     * @return The result.
     */
    public boolean isBuilt() {

        return 0 < level;
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
    public int getType() {
        return type;
    }

    /**
     * Gets the maximum number of units allowed in this <code>Building</code>.
     * 
     * @return The number.
     */
    public int getMaxUnits() {
        if (type == STOCKADE || type == DOCK || type == WAREHOUSE || type == STABLES || type == PRINTING_PRESS
                || type == CUSTOM_HOUSE) {
            return 0;
        } else if (type == SCHOOLHOUSE) {
            return getLevel();
        } else {
            return MAX_UNITS;
        }
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
        if (getUnitCount() >= getMaxUnits()) {
            return false;
        }

        if (!(locatable instanceof Unit)) {
            return false;
        }

        if (!((Unit) locatable).isColonist() && ((Unit) locatable).getType() != Unit.INDIAN_CONVERT) {
            return false;
        }

        if (getType() == SCHOOLHOUSE) {
            return canAddAsTeacher((Unit) locatable);
        }

        return true;
    }


    /**
     * Returns true if this building is a schoolhouse and the unit is
     * a skilled unit with a skill level not exceeding the level of
     * the schoolhouse. The number of units already in the schoolhouse
     * is not taken into account. @see #canAdd
     * 
     * @param unit The unit to add as a teacher.
     * @return <code>true</code> if this unit could be added.
    */
    public boolean canAddAsTeacher(Unit unit) {
        return canAddAsTeacher(unit.getType());
    }

    public boolean canAddAsTeacher(int unitType) {
        if (getType() == SCHOOLHOUSE) {
            if (Unit.getSkillLevel(unitType) <= 0) {
                return false;
            } else {
                return (getLevel() >= Unit.getSkillLevel(unitType));
            }
        } else {
            return false;
        }
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
        if (getType() == SCHOOLHOUSE) {
            if (student == null) {
                student = findStudent();
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
     * Returns the unit type being an expert in this <code>Building</code>.
     * 
     * @return The {@link Unit#getType unit type}.
     * @see Unit#getExpertWorkType
     * @see ColonyTile#getExpertForProducing
     */
    public int getExpertUnitType() {
        return getExpertUnitType(getType());
    }

    /**
     * Returns the unit type being an expert in this <code>Building</code>.
     * 
     * @param type The type of building.
     * @return The {@link Unit#getType unit type}.
     * @see Unit#getExpertWorkType
     * @see ColonyTile#getExpertForProducing
     */
    public static int getExpertUnitType(int type) {
        switch (type) {
        case TOWN_HALL:
            return Unit.ELDER_STATESMAN;
        case CARPENTER:
            return Unit.MASTER_CARPENTER;
        case BLACKSMITH:
            return Unit.MASTER_BLACKSMITH;
        case TOBACCONIST:
            return Unit.MASTER_TOBACCONIST;
        case WEAVER:
            return Unit.MASTER_WEAVER;
        case DISTILLER:
            return Unit.MASTER_DISTILLER;
        case FUR_TRADER:
            return Unit.MASTER_FUR_TRADER;
        case ARMORY:
            return Unit.MASTER_GUNSMITH;
        case CHURCH:
            return Unit.FIREBRAND_PREACHER;
        default:
            return -1;
        }
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
            int index = units.indexOf(locatable);
            return (index != -1) ? true : false;
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
        if ((level == NOT_BUILT) && (type != CHURCH)) {
            // Don't do anything if the building does not exist.
            return; 
        } else if (type == SCHOOLHOUSE) {
            trainStudents();
        } else if (getGoodsOutputType() != -1) {
            produceGoods();
        }
    }

    private void produceGoods() {
        int goodsInput = getGoodsInput();
        int goodsOutput = getProduction();
        int goodsInputType = getGoodsInputType();
        int goodsOutputType = getGoodsOutputType();

        if (goodsInput == 0 && getMaximumGoodsInput() > 0) {
            addModelMessage(getColony(), "model.building.notEnoughInput", new String[][] {
                { "%inputGoods%", Goods.getName(goodsInputType) }, { "%building%", getName() },
                { "%colony%", colony.getName() } }, ModelMessage.MISSING_GOODS, new Goods(goodsInputType));
        }

        if (goodsOutput <= 0)
            return;

        // Actually produce the goods:
        if (goodsOutputType == Goods.CROSSES) {
            colony.getOwner().incrementCrosses(goodsOutput);
        } else if (goodsOutputType == Goods.BELLS) {
            colony.getOwner().incrementBells(goodsOutput);
            colony.addBells(goodsOutput);
        } else {
            colony.removeGoods(goodsInputType, goodsInput);

            if (goodsOutputType == Goods.HAMMERS) {
                colony.addHammers(goodsOutput);
                return;
            }

            colony.addGoods(goodsOutputType, goodsOutput);
        }
    }

    private Unit findStudent() {
        Unit student = null;
        int skill = Integer.MIN_VALUE;
        for (Unit potentialStudent : getColony().getUnitList()) {
            if (potentialStudent.canBeStudent() &&
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
                Unit student = findStudent();
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
                teacher.getStudent().train(teacher);
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
    public int getGoodsOutputType() {
        return getGoodsOutputType(getType());
    }

    /**
     * Returns the type of goods this <code>Building</code> produces.
     * 
     * @param type The type of building.
     * @return The type of goods this <code>Building</code> produces or
     *         <code>-1</code> if there is no goods production by this
     *         <code>Building</code>.
     */
    public static int getGoodsOutputType(int type) {
        switch (type) {
        case BLACKSMITH:
            return Goods.TOOLS;
        case TOBACCONIST:
            return Goods.CIGARS;
        case WEAVER:
            return Goods.CLOTH;
        case DISTILLER:
            return Goods.RUM;
        case FUR_TRADER:
            return Goods.COATS;
        case ARMORY:
            return Goods.MUSKETS;
        case CHURCH:
            return Goods.CROSSES;
        case TOWN_HALL:
            return Goods.BELLS;
        case CARPENTER:
            return Goods.HAMMERS;
        default:
            return -1;
        }
    }

    /**
     * Returns the type of goods this building needs for input.
     * 
     * @return The type of goods this <code>Building</code> requires as input
     *         in order to produce it's {@link #getGoodsOutputType output}.
     */
    public int getGoodsInputType() {
        return getGoodsInputType(getType());
    }

    /**
     * Returns the type of goods this building needs for input.
     * 
     * @param type The type of building.
     * @return The type of goods this <code>Building</code> requires as input
     *         in order to produce it's {@link #getGoodsOutputType output}.
     */
    public static int getGoodsInputType(int type) {
        switch (type) {
        case BLACKSMITH:
            return Goods.ORE;
        case TOBACCONIST:
            return Goods.TOBACCO;
        case WEAVER:
            return Goods.COTTON;
        case DISTILLER:
            return Goods.SUGAR;
        case FUR_TRADER:
            return Goods.FURS;
        case ARMORY:
            return Goods.TOOLS;
        case CARPENTER:
            return Goods.LUMBER;
        default:
            return -1;
        }
    }

    /**
     * Returns the amount of goods needed to have a full production if
     * <code>Unit</code> was added.
     */
    public int getMaximumGoodsInputAdding(Unit unit){
        int goodsInput = getMaximumProductionAdding(unit);
        if (level > SHOP) {
            goodsInput = (goodsInput * 2) / 3; // Factories don't need the
                                                // extra 3 units.
        }

        return goodsInput;
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
        return getMaximumGoodsInputAdding(null);
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
        int goodsInput = getMaximumGoodsInput();
        if (getGoodsInputType() > -1 && colony.getGoodsCount(getGoodsInputType()) < goodsInput) {
            goodsInput = colony.getGoodsCount(getGoodsInputType());
        }
        return goodsInput;
    }

    /**
     * Returns the amount of goods being used to get a desired
     * {@link #getProduction production}.
     * 
     * @return The actual amount of goods that is being used to support the
     *         desired production.
     * @see #getMaximumGoodsInput
     * @see #getProduction
     */
    public int getGoodsInput(int goodsOutput) {
        int goodsInput = goodsOutput;
        if (level > SHOP) {
            goodsInput = (goodsInput * 2) / 3; // Factories don't need the
                                                // extra 3 units.
        }
        if (getGoodsInputType() > -1 && colony.getGoodsCount(getGoodsInputType()) < goodsInput) {
            goodsInput = colony.getGoodsCount(getGoodsInputType());
        }
        return goodsInput;
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
        int goodsInput = getMaximumGoodsInput();
        if (getGoodsInputType() > -1) {
            int available = colony.getGoodsCount(getGoodsInputType()) +
                colony.getProductionOf(getGoodsInputType());
            if (available < goodsInput) {
                // Not enough goods to do this?
                goodsInput = available;
            }
        }
        return goodsInput;
    }

    /**
     * Returns the amount of goods being used to get the desired
     * {@link #getProduction production} at the next turn.
     * 
     * @return The actual amount of goods that will be used to support the
     *         desired production at the next turn.
     * @see #getMaximumGoodsInput
     * @see #getProduction
     */
    public int getGoodsInputNextTurn(int goodsOutput) {
        int goodsInput = goodsOutput;
        if (level > SHOP) {
            goodsInput = (goodsInput * 2) / 3; // Factories don't need the
                                                // extra 3 units.
        }
        if (getGoodsInputType() > -1) {
            int available = colony.getGoodsCount(getGoodsInputType()) +
                colony.getProductionOf(getGoodsInputType());
            if (available < goodsInput) {
                // Not enough goods to do this?
                goodsInput = available;
            }
        }
        return goodsInput;
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
        int goodsOutput = 0;
        if (level < FACTORY) {
            goodsOutput = goodsInput;
        } else {
            goodsOutput = (goodsInput * 3) / 2;
            if (getGameOptions().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
                int minimumProduction = 0;
                Iterator<Unit> i = getUnitIterator();
                while (i.hasNext()) {
                    Unit unit = (Unit)i.next();
                    if (unit.getType() == getExpertUnitType()) {
                        minimumProduction += 4;
                    }
                }
                if (new_unit != null && canAdd(new_unit) && new_unit.getType() == getExpertUnitType()) {
                    minimumProduction += 4;
                }
                if (goodsOutput < minimumProduction) {
                    goodsOutput = minimumProduction;
                }
            }
        }
        return goodsOutput;
    }

     /**
      * Calculates and returns the output of this building from the input.
      *
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
     */
    public int getProductionAdding(Unit unit) {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int goodsOutput = getMaximumProductionAdding(unit);

        if (getGoodsInputType() > -1) {
            int goodsInput = colony.getGoodsCount(getGoodsInputType());
            if (goodsInput < getMaximumGoodsInputAdding(unit)) {
                goodsOutput = calculateOutputAdding(goodsInput, unit);
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
        return getProductionAdding(null);
    }

    /**
     * Returns the actual production of this building for next turn.
     * 
     * @return The production of this building the next turn.
     * @see #getProduction
     */
    public int getProductionNextTurn() {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int goodsOutput = getMaximumProduction();

        if (getGoodsInputType() > -1) {
            int goodsInput = colony.getGoodsCount(getGoodsInputType()) + colony.getProductionOf(getGoodsInputType());
            if (goodsInput < getMaximumGoodsInput()) {
                goodsOutput = calculateOutput(goodsInput);
            }
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
        if (getGoodsOutputType() == -1) {
            return 0;
        }
        if (addUnit == null || !canAdd(addUnit)) {
            return 0;
        }

        int addGoodsOutput = getAdditionalProduction(addUnit);

        if (getGoodsInputType() > -1) {
            // addGoodsInput = total available stock + new production - current requirements
            //               = remaining input goods to support additional output
            int addGoodsInput = colony.getGoodsCount(getGoodsInputType())
                                + colony.getProductionOf(getGoodsInputType())
                                - getGoodsInput();
            // Additional input goods need = Additional goods output (with modifier for FACTORY)
            int neededGoodsInput = addGoodsOutput;
            if (level > SHOP) {
                neededGoodsInput = (neededGoodsInput * 2) / 3;
            }
            if (addGoodsInput < neededGoodsInput) {
                // Not enough additional input goods to support desired output
                if (level < FACTORY) {
                    addGoodsOutput = addGoodsInput;
                } else {
                    addGoodsOutput = (addGoodsInput * 3) / 2;
                    if (getGameOptions().getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)
                        && addUnit.getType() == getExpertUnitType()
                        && addGoodsOutput < 4) {
                        addGoodsOutput = 4;
                    }
                }
            }
        }

        return addGoodsOutput;
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
    public int getProductionOf(int goodsType) {
        if (goodsType == getGoodsOutputType()) {
            return getProduction();
        }

        return 0;
    }

    /**
     * Returns the maximum productivity of worker/s
     * currently working in this building.
     *
     * @return The maximum returns from workers in this building,
     *         assuming enough "input goods".
     */
    public int getProductivity() {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int totProductivity = 0;
        Iterator<Unit> unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            int productivity = unitIterator.next().getProducedAmount(getGoodsOutputType());
            if (productivity > 0) {
                productivity += colony.getProductionBonus();
                if (productivity < 1)
                    productivity = 1;
            }
            totProductivity += productivity;
        }
        return totProductivity;
    }

    /**
     * Returns the maximum productivity of a unit working in this building.
     *
     * @return The maximum returns from this unit if in this <code>Building</code>,
     *         assuming enough "input goods".
     */
    public int getProductivity(Unit prodUnit) {
        if (getGoodsOutputType() == -1) {
            return 0;
        }
        if (prodUnit == null) {
            return 0;
        }

        int productivity = prodUnit.getProducedAmount(getGoodsOutputType());
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
        return getProductionFromProductivity(getProductivity());
    }

    /**
     * Returns the maximum production with a given unit to be added to this building.
     *
     * @return Maximum production potential from all workers
     *         with additional worker if valid.
     */
    public int getMaximumProductionAdding(Unit addUnit) {
        if (addUnit == null || !canAdd(addUnit)) {
            return getMaximumProduction();
        }
        return getProductionFromProductivity(getProductivity() +
                                              getProductivity(addUnit));
    }

    /**
     * Returns the maximum production of a given unit to be added to this building.
     *
     * @return If unit can be added, will return the maximum production potential
     *         if it cannot be added, will return <code>0</code>.
     */
    public int getAdditionalProduction(Unit addUnit) {
        if (addUnit == null || !canAdd(addUnit)) {
            return 0;
        }
        return getProductionFromProductivity(getProductivity(addUnit));
    }

    /**
     * Returns the Production from this building given the productivity of the worker(s).
     *
     * @param productivity From {@link #getProductivity}
     * @return Maximum Production based on Productivity
     */
    public int getProductionFromProductivity(int productivity) {
        int goodsOutputType = getGoodsOutputType();
        Player player = colony.getOwner();

        int goodsOutput = productivity;
        if (getType() == CHURCH || getType() == TOWN_HALL) {
            goodsOutput++;
        }
        goodsOutput *= (type == CHURCH) ? level + 1 : level;

        if (goodsOutputType == Goods.BELLS) {
            goodsOutput += goodsOutput * colony.getBuilding(Building.PRINTING_PRESS).getLevel();

            if (player.hasFather(FoundingFather.THOMAS_JEFFERSON) || player.hasFather(FoundingFather.THOMAS_PAINE)) {
                goodsOutput = (goodsOutput * (100 + player.getBellsBonus())) / 100;
            }
        }

        if (goodsOutputType == Goods.CROSSES && player.hasFather(FoundingFather.WILLIAM_PENN)) {
            goodsOutput += goodsOutput / 2;
        }

        return goodsOutput;
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
        out.writeAttribute("ID", getID());
        out.writeAttribute("colony", colony.getID());
        out.writeAttribute("type", Integer.toString(type));
        out.writeAttribute("level", Integer.toString(level));

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
        setID(in.getAttributeValue(null, "ID"));

        colony = (Colony) getGame().getFreeColGameObject(in.getAttributeValue(null, "colony"));
        if (colony == null) {
            colony = new Colony(getGame(), in.getAttributeValue(null, "colony"));
        }
        type = Integer.parseInt(in.getAttributeValue(null, "type"));
        level = Integer.parseInt(in.getAttributeValue(null, "level"));

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
