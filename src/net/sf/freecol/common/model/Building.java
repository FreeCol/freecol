
package net.sf.freecol.common.model;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Element;

/**
 * Represents a building in a colony. 
 * 
 * <br><br>
 * 
 * Each <code>Building</code> has a type and a level.
 * The levels are {@link #NOT_BUILT}, {@link #HOUSE}, 
 * {@link #SHOP} and {@link #FACTORY}. The {@link #getName name} 
 * of a <code>Building</code> depends on both the type and 
 * the level:
 *
 * <br><br>Type {@link #STOCKADE}
 * <br>Level {@link #NOT_BUILT}: <i>null</i>
 * <br>Level {@link #HOUSE}: "Stockade"
 * <br>Level {@link #SHOP}: "Fort"
 * <br>Level {@link #FACTORY}: "Fortress"
 *
 */
public final class Building extends FreeColGameObject implements WorkLocation, Ownable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** 
     * The maximum level.
     */
    public static final int MAX_LEVEL = 3;

    /** The type of a building. */
    public static final int NONE = -1,
                            TOWN_HALL = 0,
                            CARPENTER = 1,
                            BLACKSMITH = 2,
                            TOBACCONIST = 3,
                            WEAVER = 4,
                            DISTILLER = 5,
                            FUR_TRADER = 6,
                            SCHOOLHOUSE = 7, //10
                            ARMORY = 8,
                            CHURCH = 9, //13
                            STOCKADE = 10, //7
                            WAREHOUSE = 11,
                            STABLES = 12,
                            DOCK = 13, //9
                            PRINTING_PRESS = 14,
                            CUSTOM_HOUSE = 15;

    /** The maximum number of building types. */
    public static final int NUMBER_OF_TYPES = FreeCol.specification.numberOfBuildingTypes();

    /** The level of a building. */
    public static final int NOT_BUILT = 0,
                            HOUSE = 1,
                            SHOP = 2,
                            FACTORY = 3;

    /** 
     * Sets the maximum number of units in one building. 
     * This will become a non-constant later so always use
     * the {@link #getMaxUnits()}.
     */
    private static final int MAX_UNITS = 3;

    /** The colony containing this building. */
    private Colony colony;

    /** The type of this building. */
    private int type;

    /** 
     * The level this building has. This should be on of:
     * <ul>
     *   <li>{@link #NOT_BUILT}</li>
     *   <li>{@link #HOUSE}</li>
     *   <li>{@link #SHOP}</li>
     *   <li>{@link #FACTORY}</li>
     * </ul>
     */
    private int level;

    /** 
     * List of the units which have this <code>Building</code>
     * as it's {@link Unit#getLocation() location}.
     */
    private List units = new ArrayList();

    private BuildingType buildingType;

    
    /**
     * Creates a new <code>Building</code>.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param colony The colony in which this building is located.
     * @param type The type of building.
     * @param level The level of the building: {@link #NOT_BUILT}, 
     *      {@link #HOUSE}, {@link #SHOP} or {@link #FACTORY}.
     */
    public Building(Game game, Colony colony, int type, int level) {
        super(game);

        this.colony = colony;
        this.type = type;
        this.level = level;

        buildingType = FreeCol.specification.buildingType( type );
    }


    /**
     * Initiates a new <code>Building</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Building(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);

        buildingType = FreeCol.specification.buildingType( type );
    }
    
    /**
     * Initiates a new <code>Building</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public Building(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);

        buildingType = FreeCol.specification.buildingType( type );
    }
    
    /**
     * Initiates a new <code>Building</code> 
     * with the given ID. The object should later be
     * initialized by calling either
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
     * @return The <code>Player</code> controlling this
     *         {@link Ownable}.
     */
    public Player getOwner() {
        return colony.getOwner();
    }
        
    /**
     * Gets the <code>Tile</code> where this <code>Building</code> 
     * is located.
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
     * @param level The new level of the building. This should be
     *       one of {@link #NOT_BUILT}, {@link #HOUSE},
     *       {@link #SHOP} and {@link #FACTORY}.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Gets the name of a building.
     *
     * @return The name of the <code>Building</code> or
     *         <i>null</i> if the building has not been built.
     */
    public String getName() {
        return isBuilt()  &&  (level - 1) < buildingType.numberOfLevels()
            ? buildingType.level(level - 1).name
            : null;
    }

    /**
     * Gets the name of the improved building of the same type.
     * An improved building is a building of a higher level.
     *
     * @return The name of the improved building or <code>null</code>
     *         if the improvement does not exist.
     */
    public String getNextName() {
        return level < buildingType.numberOfLevels() 
                ? buildingType.level(level).name 
                : null;
    }

    /**
     * Gets the number of hammers required for the improved 
     * building of the same type.
     *
     * @return The number of hammers required for the improved 
     *      building of the same type, or <code>-1</code> if 
     *      the building does not exist.
     */
    public int getNextHammers() {
        if (!canBuildNext()) {
            return -1;
        }

        return level < buildingType.numberOfLevels() 
                ? buildingType.level(level).hammersRequired 
                : -1;
    }

    /**
     * Gets the number of tools required for the improved 
     * building of the same type.
     *
     * @return The number of tools required for the improved 
     *      building of the same type, or <code>-1</code> if 
     *      the building does not exist.
     */
    public int getNextTools() {
        if (!canBuildNext()) {
            return -1;
        }

        return level < buildingType.numberOfLevels() 
                ? buildingType.level(level).toolsRequired 
                : -1;
    }

    /**
     * Gets the colony population required for the improved 
     * building of the same type.
     *
     * @return The colony population required for the improved 
     *      building of the same type, or <code>-1</code> if 
     *      the building does not exist.
     */
    public int getNextPop() {
        return level < buildingType.numberOfLevels() 
                ? buildingType.level(level).populationRequired 
                : -1;
    }

    /**
    * Checks if this building can have a higher level.
    * 
    * @return If this <code>Building</code> can have a
    *       higher level, that {@link FoundingFather Adam Smith}
    *       is present for manufactoring factory level buildings
    *       and that the <code>Colony</code> containing this
    *       <code>Building</code> has a sufficiently high
    *       population.
    */
    public boolean canBuildNext() {
        if (level >= MAX_LEVEL) {
            return false;
        }
        if (getType() == CUSTOM_HOUSE) {
            if (getColony().getOwner().hasFather(FoundingFather.PETER_STUYVESANT)) {
                return true;
            }

            return false;
        }
        if (level+1 >= FACTORY && !getColony().getOwner().hasFather(FoundingFather.ADAM_SMITH)
                && (type == BLACKSMITH || type == TOBACCONIST || type == WEAVER
                || type == DISTILLER || type == FUR_TRADER || type == ARMORY)) {
            return false;
        }
        
        // if there are no more improvements available for this building type..
        if ( buildingType.numberOfLevels() == level ) {
            return false;
        }
        if (getType() == DOCK && getColony().isLandLocked()) {
            return false;
        }
        
        if (buildingType.level(level).populationRequired > colony.getUnitCount()) {
            return false;
        }
        
        return true;
    }

    /**
     * Checks if the building has been built.
     * @return The result.
     */
    public boolean isBuilt() {

        return 0 < level;
    }

    /**
     * Gets a pointer to the colony containing this building.
     * @return The <code>Colony</code>.
     */
    public Colony getColony() {
        return colony;
    }

    /**
     * Gets the type of this building.
     * @return The type.
     */
    public int getType() {
        return type;
    }

    /**
     * Checks if this building is of a given type.
     *
     * @param type The type.
     * @return <i>true</i> if the building is of the given type and <i>false</i> otherwise.
     */
    public boolean isType(int type) {
        return getType() == type;
    }

    /**
     * Gets the maximum number of units allowed in this <code>Building</code>.
     * @return The number.
     */
    public int getMaxUnits() {
        if (type == STOCKADE || type == DOCK || type == WAREHOUSE ||
                type == STABLES || type == PRINTING_PRESS || type == CUSTOM_HOUSE) {
            return 0;
        } else if (type == SCHOOLHOUSE) {
            return getLevel();
        } else {
            return MAX_UNITS;
        }
    }

    /**
     * Gets the amount of units at this <code>WorkLocation</code>.
     * @return The amount of units at this {@link WorkLocation}.
     */
    public int getUnitCount() {
        return units.size();
    }

    /**
     * Checks if the specified <code>Locatable</code> may be added to 
     * this <code>WorkLocation</code>.
     *
     * @param locatable the <code>Locatable</code>.
     * @return <i>true</i> if the <i>Unit</i> may be added and 
     *      <i>false</i> otherwise.
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

        if (getType() == SCHOOLHOUSE && (getLevel() < Unit.getSkillLevel(((Unit) locatable).getType())
                || ((Unit) locatable).getType() == Unit.INDIAN_CONVERT
                || ((Unit) locatable).getType() == Unit.FREE_COLONIST
                || ((Unit) locatable).getType() == Unit.INDENTURED_SERVANT
                || ((Unit) locatable).getType() == Unit.PETTY_CRIMINAL)) {
            return false;
        }

        return true;
    }

    /**
     * Adds the specified <code>Locatable</code> to this 
     * <code>WorkLocation</code>.
     * 
     * @param locatable The <code>Locatable</code> that shall 
     *       be added to this <code>WorkLocation</code>.
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
        switch (getType()) {
            case TOWN_HALL:     return Unit.ELDER_STATESMAN;
            case CARPENTER:     return Unit.MASTER_CARPENTER;
            case BLACKSMITH:    return Unit.MASTER_BLACKSMITH;
            case TOBACCONIST:   return Unit.MASTER_TOBACCONIST;
            case WEAVER:        return Unit.MASTER_WEAVER;
            case DISTILLER:     return Unit.MASTER_DISTILLER;
            case FUR_TRADER:    return Unit.MASTER_FUR_TRADER;
            case ARMORY:        return Unit.MASTER_GUNSMITH;
            case CHURCH:        return Unit.FIREBRAND_PREACHER;
            default:            return -1;
        }
    }

    /**
     * Removes the specified <code>Locatable</code> from 
     * this <code>WorkLocation</code>.
     * 
     * @param locatable The <code>Locatable</code> that shall 
     *       be removed from this <code>WorkLocation</code>.
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
     * @param locatable The <code>Locatable</code> to test the
     *        presence of.
     * @return <ul>
     *           <li><code>>true</code>if the specified 
     *                  <code>Locatable</code> is in this 
     *                  <code>Building</code> and</li>
     *           <li><code>false</code> otherwise.</li>
     *         </ul>
     */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            int index = units.indexOf(locatable);
            return (index != -1) ? true:false;
        }

        return false;
    }

    /**
     * Gets the first unit in this building.
     * @return The <code>Unit</code>.
     */
    public Unit getFirstUnit() {
        if (units.size() > 0) {
            return (Unit) units.get(0);
        }

        return null;
    }

    /**
     * Gets the last unit in this building.
     * @return The <code>Unit</code>.
     */
    public Unit getLastUnit() {
        if (units.size() > 0) {
            return (Unit) units.get(units.size()-1);
        }

        return null;
    }

    /**
     * Gets an <code>Iterator</code> of every <code>Unit</code> 
     * directly located on this <code>Building</code>.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator getUnitIterator() {
        return units.iterator();
    }

    /**
     * Gets the best unit to train for the given unit type.
     *
     * @param unitType The unit type to train for.
     * @return The <code>Unit</code>.
     */
    private Unit getUnitToTrain(int teacherType) {
	Unit bestUnit = null;
	int bestScore = 0;
 
	Iterator i = colony.getUnitIterator();
	while (i.hasNext()) {
	    Unit unit = (Unit) i.next();
 
	    switch(unit.getType()) {
	    case Unit.FREE_COLONIST:
		if (unit.getTrainingType() == teacherType &&
		    (bestUnit == null || unit.getTurnsOfTraining() > bestUnit.getTurnsOfTraining())) {
		    // this unit is already training for the job
		    bestUnit = unit;
		    bestScore = 5;
		} else if (unit.getTurnsOfTraining() == 0 && bestScore < 4) {
		    // this unit has not started training
		    bestUnit = unit;
		    bestScore = 4;
		} else if (bestScore < 1 &&
			   (bestUnit == null || unit.getTurnsOfTraining() < bestUnit.getTurnsOfTraining())) {
		    // this unit has started training for another job,
		    // but has spent less time training than the previous
		    // best choice
		    bestUnit = unit;
		    bestScore = 1;
		}
		break;
	    case Unit.INDENTURED_SERVANT:
		if (bestScore < 3) {
		    bestUnit = unit;
		    bestScore = 3;
		}
		break;
	    case Unit.PETTY_CRIMINAL:
		if (bestScore < 2) {
		    bestUnit = unit;
		    bestScore = 2;
		}
		break;
	    default:
		// ignore any other type of unit
	    }
 
	}
	return bestUnit;
    }


    /**
     * Gets this <code>Location</code>'s <code>GoodsContainer</code>.
     * @return <code>null</code>.
     */
    public GoodsContainer getGoodsContainer() {
        return null;
    }
    
    /**
     * Gets a teacher for the given unit type.
     * 
     * @param unitType The type of unit to find a teacher for.
     * @return The teacher or <code>null</code> if no teacher
     *      can be found for the given unit type.
     */
    private Unit getTeacher(int unitType) {
        Iterator i = colony.getUnitIterator();
        while (i.hasNext()) {
            Unit unit = (Unit) i.next();
            
            if (unit.getType() == unitType) {
                return unit;
            }
        }

        return null;
    }

    /**
     * Prepares this <code>Building</code> for a new turn.
     */
    public void newTurn() {
        if ((level == NOT_BUILT) && (type != CHURCH)) return; // Don't do anything if the building does not exist.

        if (type == SCHOOLHOUSE) {
            Iterator i = getUnitIterator();
            while (i.hasNext()) {
                Unit teacher = (Unit) i.next();
                Unit student = getUnitToTrain(teacher.getType());

                if (student != null) {
                    if (student.getTrainingType() != teacher.getType() && student.getTrainingType() != Unit.FREE_COLONIST) {
                        student.setTrainingType(teacher.getType());
                        student.setTurnsOfTraining(0);
                    }

                    student.setTurnsOfTraining(student.getTurnsOfTraining() + ((colony.getSoL() == 100) ? 2 : 1));

                    if (student.getTurnsOfTraining() >= student.getNeededTurnsOfTraining()) {
                        String oldName = student.getName();

                        if (student.getType() == Unit.INDENTURED_SERVANT) {
                            student.setType(Unit.FREE_COLONIST);
                        } else if (student.getType() == Unit.PETTY_CRIMINAL) {
                            student.setType(Unit.INDENTURED_SERVANT);
                        } else {
                            student.setType(student.getTrainingType());
                        }

                        student.setTrainingType(-1);
                        student.setTurnsOfTraining(0);
                        addModelMessage(this, "model.unit.unitImproved",
                                        new String[][] {{"%oldName%", oldName},
                                                        {"%newName%", student.getName()},
                                                        {"%nation%", getOwner().getName()}},
                                        ModelMessage.UNIT_IMPROVED, student);
                    }
                } else {
                    addModelMessage(this, "model.building.noStudent",
                                    new String[][] {{"%teacher%", teacher.getName()},
                                                    {"%colony%", colony.getName()}},
                                    ModelMessage.WARNING, teacher);
                }
            }
        } else if (getGoodsOutputType() != -1) {
            int goodsInput = getGoodsInput();
            int goodsOutput = getProduction();
            int goodsInputType = getGoodsInputType();
            int goodsOutputType = getGoodsOutputType();

            if (getGoodsInput() == 0 && getMaximumGoodsInput() > 0) {
                addModelMessage(this, "model.building.notEnoughInput", 
                                new String[][] {{"%inputGoods%", Goods.getName(goodsInputType)},
                                                {"%building%", getName()},
                                                {"%colony%", colony.getName()}},
                                ModelMessage.MISSING_GOODS, new Goods(goodsInputType));
            }

            if (goodsOutput <= 0) return;

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
    }

    /**
     * Returns the type of goods this <code>Building</code>
     * produces.
     * 
     * @return The type of goods this <code>Building</code>
     *      produces or <code>-1</code> if there is no
     *      goods production by this <code>Building</code>.
     */
    public int getGoodsOutputType() {
        switch(type) {
            case BLACKSMITH:    return Goods.TOOLS;
            case TOBACCONIST:   return Goods.CIGARS;
            case WEAVER:        return Goods.CLOTH;
            case DISTILLER:     return Goods.RUM;
            case FUR_TRADER:    return Goods.COATS;
            case ARMORY:        return Goods.MUSKETS;
            case CHURCH:        return Goods.CROSSES;
            case TOWN_HALL:     return Goods.BELLS;
            case CARPENTER:     return Goods.HAMMERS;
            default:            return -1;
        }
    }

    /**
     * Returns the type of goods this building needs for
     * input. 
     * 
     * @return The type of goods this <code>Building</code>
     *      requires as input in order to produce it's
     *      {@link #getGoodsOutputType output}.
     */
    public int getGoodsInputType() {
        switch(type) {
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
     * Returns the amount of goods needed to have
     * a full production.
     *
     * @return The maximum level of goods needed in
     *      order to have the maximum possible
     *      production with the current configuration
     *      of workers and improvements. This is actually
     *      the {@link #getGoodsInput input} being
     *      used this turn, provided that the amount of goods in
     *      the <code>Colony</code> is either larger or
     *      the same as the value returned by this method.
     * @see #getGoodsInput
     * @see #getProduction
     */
    public int getMaximumGoodsInput() {
        int goodsInput = getMaximumProduction();
        if (level > SHOP) {
            goodsInput = (goodsInput * 2) / 3; // Factories don't need the extra 3 units.
        }

        return goodsInput;
    }

    /**
     * Returns the amount of goods beeing used to
     * get the current {@link #getProduction production}.
     *
     * @return The actual amount of goods that is being used
     *      to support the current production.
     * @see #getMaximumGoodsInput
     * @see #getProduction
     */
    public int getGoodsInput() {
        if ((getGoodsInputType() > -1) && (colony.getGoodsCount(getGoodsInputType()) < getMaximumGoodsInput()))  { // Not enough goods to do this?
            return colony.getGoodsCount(getGoodsInputType());
        }

        return getMaximumGoodsInput();
    }

    /**
     * Returns the actual production of this building.
     *
     * @return The amount of goods being produced by this
     *      <code>Building</code> the current turn.
     *      The type of goods being produced is given by
     *      {@link #getGoodsOutputType}.
     * @see #getProductionNextTurn
     * @see #getMaximumProduction
     */
    public int getProduction() {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int goodsOutput = getMaximumProduction();

        if ((getGoodsInputType() > -1) && (colony.getGoodsCount(getGoodsInputType()) < goodsOutput))  { // Not enough goods to do this?
            int goodsInput = colony.getGoodsCount(getGoodsInputType());
            if (level < FACTORY) {
                goodsOutput = goodsInput;
            } else {
                goodsOutput = (goodsInput * 3) / 2;
            }
        }

        return goodsOutput;
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
            if (goodsInput < goodsOutput) {                
                if (level < FACTORY) {
                    goodsOutput = goodsInput;
                } else {
                    goodsOutput = (goodsInput * 3) / 2;
                }
            }
        }

        return goodsOutput;
    }

    /**
     * Returns the production of the given type of goods.
     * 
     * @param goodsType The type of goods to get the
     *      production for.
     * @return the production og the given goods this turn.
     *      This method will return the same as
     *      {@link #getProduction} if the given type of
     *      goods is the same as {@link #getGoodsOutputType}
     *      and <code>0</code> otherwise.
     */
    public int getProductionOf(int goodsType) {
        if (goodsType == getGoodsOutputType()) {
            return getProduction();
        }

        return 0;
    }

    /**
     * Returns the maximum production of this building.
     * 
     * @return The production of this building, with the
     *      current amount of workers, when there is
     *      enough "input goods".
     */
    public int getMaximumProduction() {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int goodsOutput = 0;
        int goodsOutputType = getGoodsOutputType();
        Player player = colony.getOwner();

        if (getType() == CHURCH || getType() == TOWN_HALL) {
            goodsOutput = 1;
        }

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            int productivity = ((Unit) unitIterator.next()).getProducedAmount(goodsOutputType);
            if (productivity > 0) {
                productivity += colony.getProductionBonus();
                if (productivity < 1) productivity = 1;
            }
            goodsOutput += productivity;
        }

        goodsOutput *= (type == CHURCH) ? level + 1 : level;

        if (goodsOutputType == Goods.BELLS) {
            goodsOutput += goodsOutput * colony.getBuilding(Building.PRINTING_PRESS).getLevel();

            if (player.hasFather(FoundingFather.THOMAS_JEFFERSON) ||
                player.hasFather(FoundingFather.THOMAS_PAINE)) {
                goodsOutput = (goodsOutput * (100 + player.getBellsBonus()))/100;
            }
        }
        
        if (goodsOutputType == Goods.CROSSES && player.hasFather(FoundingFather.WILLIAM_PENN)) {
            goodsOutput += goodsOutput/2;
        }

        return goodsOutput;
    }

    /**
     * Disposes this building. All units that currently has
     * this <code>Building as it's location will be disposed</code>.
     */
    public void dispose() {
        for (int i=0; i<units.size(); i++) {
            ((Unit) units.get(i)).dispose();
        }
        
        super.dispose();
    }    

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        
        // Add attributes:       
        out.writeAttribute("ID", getID());
        out.writeAttribute("colony", colony.getID());
        out.writeAttribute("type", Integer.toString(type));
        out.writeAttribute("level", Integer.toString(level));

        // Add child elements:
        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            ((FreeColGameObject) unitIterator.next()).toXML(out, player, showAll, toSavedGame);
        }

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
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
    * @return the tag name.
    */
    public static String getXMLElementTagName() {
        return "building";
    }
}
