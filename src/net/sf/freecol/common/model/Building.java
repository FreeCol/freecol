package net.sf.freecol.common.model;


import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



/**
* Represents a building in a colony. Each <code>Building</code> has a type and a level.
* The levels are {@link #NOT_BUILT}, {@link #HOUSE}, {@link #SHOP} and {@link #FACTORY}.
* The {@link #getName name} of a <code>Building</code> depends on both the type
* and the level:
*
* <br><br>Type {@link #STOCKADE}
* <br>Level {@link #NOT_BUILT}: <i>null</i>
* <br>Level {@link #HOUSE}: "Stockade"
* <br>Level {@link #SHOP}: "Fort"
* <br>Level {@link #FACTORY}: "Fortress"
*
*/
public final class Building extends FreeColGameObject implements WorkLocation {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /** The maximum level of units in a building ({@value}): */
    private static final int MAX_LEVEL = 3;

    /** The type of a building. */
    public static final int TOWN_HALL = 0,
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
    public static final int NUMBER_OF_TYPES = 16;

    /** The level of a building */
    public static final int NOT_BUILT = 0,
                            HOUSE = 1,
                            SHOP = 2,
                            FACTORY = 3;

    //private static final String[][] buildingNames = {{"Town hall", null, null}, {"Carpenter's house", "Lumber mill", null}, {"Blacksmith's house", "Blacksmith's shop", "Iron works"}, {"Tobacconist's house", "Tobacconist's shop", "Cigar factory"}, {"Weaver's house", "Weaver's shop", "Textile mill"}, {"Distiller's house", "Rum distillery", "Rum factory"}, {"Fur trader's house", "Fur trading post", "Fur factory"}, {"Stockade", "Fort", "Fortress"}, {"Armory", "Magazine", "Arsenal"}, {"Docks", "Drydock", "Shipyard"}, {"Schoolhouse", "College", "University"}, {"Warehouse", "Warehouse expansion", null}, {"Stables", null, null}, {"Church", "Cathedral", null}, {"Printing press", "Newspaper", null}, {"Custom house", null, null}};
    private static final String[][] buildingNames = {
        {"Town hall", null, null},
        {"Carpenter's house", "Lumber mill", null},
        {"Blacksmith's house", "Blacksmith's shop", "Iron works"},
        {"Tobacconist's house", "Tobacconist's shop", "Cigar factory"},
        {"Weaver's house", "Weaver's shop", "Textile mill"},
        {"Distiller's house", "Rum distillery", "Rum factory"},
        {"Fur trader's house", "Fur trading post", "Fur factory"},
        {"Schoolhouse", "College", "University"},
        {"Armory", "Magazine", "Arsenal"},
        {"Church", "Cathedral", null},
        {"Stockade", "Fort", "Fortress"},
        {"Warehouse", "Warehouse expansion", null},
        {"Stables", null, null},
        {"Docks", "Drydock", "Shipyard"},
        {"Printing press", "Newspaper", null},
        {"Custom house", null, null}
    };

    // Sets the maximum number of units in one building (will become a non-constant later):
    private static final int MAX_UNITS = 3;

    private static final int requiredTable[][][] = {
        {{  0,  0,  1},{ -1, -1, -1},{ -1, -1, -1},{ -1, -1, -1}}, // TOWNHALL
        {{  0,  0,  1},{ 52,  0,  3},{ -1, -1, -1},{ -1, -1, -1}}, // CARPENTER
        {{  0,  0,  1},{ 64, 20,  1},{240,100,  8},{ -1, -1, -1}}, // BLACKSMITH
        {{  0,  0,  1},{ 62, 20,  1},{160,100,  8},{ -1, -1, -1}}, // TOBACCONIST
        {{  0,  0,  1},{ 64, 20,  1},{160,100,  8},{ -1, -1, -1}}, // WEAVER
        {{  0,  0,  1},{ 64, 20,  1},{160,100,  8},{ -1, -1, -1}}, // DISTILLER
        {{  0,  0,  1},{ 56, 20,  1},{160,100,  6},{ -1, -1, -1}}, // FUR_TRADER
        {{ 64,  0,  4},{160, 50,  8},{200,100, 10},{ -1, -1, -1}}, // SCHOOLHOUSE
        {{ 52,  0,  1},{120, 50,  8},{240,100,  8},{ -1, -1, -1}}, // ARMORY
        {{ 64,  0,  3},{176,100,  8},{ -1, -1, -1},{ -1, -1, -1}}, // CHURCH
        {{ 64,  0,  3},{120,100,  3},{320,200,  8},{ -1, -1, -1}}, // STOCKADE
        {{ 80,  0,  1},{ 80, 20,  1},{ -1, -1, -1},{ -1, -1, -1}}, // WAREHOUSE
        {{ 64,  0,  1},{ -1, -1, -1},{ -1, -1, -1},{ -1, -1, -1}}, // STABLES
        {{ 52,  0,  1},{ 80, 50,  4},{240,100,  8},{ -1, -1, -1}}, // DOCK
        {{ 52, 20,  1},{120, 50,  4},{ -1, -1, -1},{ -1, -1, -1}}, // PRINTING_PRESS
        {{160, 50,  1},{ -1, -1, -1},{ -1, -1, -1},{ -1, -1, -1}}  // CUSTOM_HOUSE
    };

    // The colony containing this building.
    private Colony colony;

    // The type of this building.
    private int type;

    // Using the constants NOT_BUILT, HOUSE, SHOP and FACTORY:
    private int level;

    private ArrayList units = new ArrayList();




    /**
    * Creates a new <code>Building</code>.
    *
    * @param colony The colony in which this building is located.
    * @param type The type of building.
    * @param level The level of the building: {@link #NOT_BUILT}, {@link #HOUSE}, {@link #SHOP} or {@link #FACTORY}.
    */
    public Building(Game game, Colony colony, int type, int level) {
        super(game);

        this.colony = colony;
        this.type = type;
        this.level = level;
    }


    /**
    * Initiates a new <code>Building</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public Building(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }





    /**
    * Gets the <code>Tile</code> where this <code>Building</code> is located.
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

    /* Sets the level of the building.
    * @param level The new level of the building.
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
        if (isBuilt()) {
            return buildingNames[type][level - 1];
        } else {
            return null;
        }
    }


    /**
    * Gets the name of the improved building of the same type.
    * An improved building is a building of a higher level.
    *
    * @return The name of the improved building or <code>null</code>
    *         if the improvement does not exist.
    */
    public String getNextName() {
        if (getType() == CUSTOM_HOUSE) {
            return null;
        }

        if (level+1 >= FACTORY && !getColony().getOwner().hasFather(FoundingFather.ADAM_SMITH)) {
            return null;
        }


        if (level < MAX_LEVEL) {
            return buildingNames[type][level];
        } else {
            return null;
        }
    }

    /**
    * Gets the number of hammers required for the improved building of the same type.
    *
    * @return The number of hammers required for the improved building of the same type,
    *         or -1 if the building does not exist.
    */
    public int getNextHammers() {
        if (getType() == CUSTOM_HOUSE) {
            return -1;
        }

        if (level+1 >= FACTORY && !getColony().getOwner().hasFather(FoundingFather.ADAM_SMITH)) {
            return -1;
        }

        if (level < MAX_LEVEL) {
            return requiredTable[type][level][0];
        } else {
            return -1;
        }
    }

    /**
    * Gets the number of tools required for the improved building of the same type.
    *
    * @return The number of tools required for the improved building of the same type,
    *         or -1 if the building does not exist.
    */
    public int getNextTools() {
        if (getType() == CUSTOM_HOUSE) {
            return -1;
        }

        if (level+1 >= FACTORY && !getColony().getOwner().hasFather(FoundingFather.ADAM_SMITH)) {
            return -1;
        }

        if (level < MAX_LEVEL) {
            return requiredTable[type][level][1];
        } else {
            return -1;
        }
    }


    /**
    * Gets the colony population required for the improved building of the same type.
    *
    * @return The colony population required for the improved building of the same type,
    *         or -1 if the building does not exist.
    */
    public int getNextPop() {
        if (getType() == CUSTOM_HOUSE) {
            return -1;
        }

        if (level+1 >= FACTORY && !getColony().getOwner().hasFather(FoundingFather.ADAM_SMITH)) {
            return -1;
        }

        if (level < MAX_LEVEL) {
            return requiredTable[type][level][2];
        } else {
            return -1;
        }
    }

    /**
    * Checks if the building has been built.
    * @return The result.
    */
    public boolean isBuilt() {
        if (level > 0) {
            return true;
        } else {
            return false;
        }
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
    * Checks if the specified <code>Locatable</code> may be added to this <code>WorkLocation</code>.
    *
    * @param locatable the <code>Locatable</code>.
    * @return <i>true</i> if the <i>Unit</i> may be added and <i>false</i> otherwise.
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
    * Adds the specified <code>Locatable</code> to this <code>WorkLocation</code>.
    * @param locatable The <code>Locatable</code> that shall be added to this <code>WorkLocation</code>.
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
    * Removes the specified <code>Locatable</code> from this <code>WorkLocation</code>.
    *
    * @param locatable The <code>Locatable</code> that shall be removed from this <code>WorkLocation</code>.
    * @return <code>true</code> if the code>Locatable</code> was removed and <code>false</code> otherwise.
    */
    public void remove(Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException();
        }

        int index = units.indexOf((Unit) locatable);

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
    *           <li><i>true</i>  if the specified <code>Locatable</code>
    *                            is in this <code>Building</code> and
    *           <li><i>false</i> otherwise.
    *         </ul>
    */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            int index = units.indexOf((Unit) locatable);
            return (index != -1) ? true:false;
        } else {
            return false;
        }
    }


    /**
    * Gets the first unit in this building.
    * @return The <code>Unit</code>.
    */
    public Unit getFirstUnit() {
        if (units.size() > 0) {
            return (Unit) units.get(0);
        } else {
            return null;
        }
    }


    /**
    * Gets the last unit in this building.
    * @return The <code>Unit</code>.
    */
    public Unit getLastUnit() {
        if (units.size() > 0) {
            return (Unit) units.get(units.size()-1);
        } else {
            return null;
        }
    }


    /**
    * Gets an <code>Iterator</code> of every <code>Unit</code> directly located on this
    * <code>Building</code>.
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
    private Unit getUnitToTrain(int unitType) {
        Unit bestUnit = null;
        int bestScore = 0;

        Iterator i = colony.getUnitIterator();
        while (i.hasNext()) {
            Unit unit = (Unit) i.next();

            if (unit.getTrainingType() != -1 && unit.getNeededTurnsOfTraining() <= unit.getTurnsOfTraining()) {
                continue;
            }

            if (unit.getType() == Unit.FREE_COLONIST && unit.getTrainingType() == unitType) {
                if (bestUnit == null || unit.getTurnsOfTraining() > bestUnit.getTurnsOfTraining()) {
                    bestUnit = unit;
                    bestScore = 5;
                }
            } else if (unit.getType() == Unit.FREE_COLONIST && unit.getTurnsOfTraining() == 0) {
                if (bestScore < 4) {
                    bestUnit = unit;
                    bestScore = 4;
                }
            } else if (unit.getType() == Unit.INDENTURED_SERVANT) {
                if (bestScore < 3) {
                    bestUnit = unit;
                    bestScore = 3;
                }
            } else if (unit.getType() == Unit.PETTY_CRIMINAL) {
                if (bestScore < 2) {
                    bestUnit = unit;
                    bestScore = 2;
                }
            } else if (unit.getType() == Unit.FREE_COLONIST && getTeacher(unitType) == null) {
                if (bestScore < 1) {
                    bestUnit = unit;
                    bestScore = 1;
                }
            }
        }

        return bestUnit;
    }

    public GoodsContainer getGoodsContainer() {
        return null;
    }

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
                        addModelMessage(this, "model.unit.unitImproved", new String[][] {{"%oldName%", oldName}, {"%newName%", student.getName()}});
                    }
                }
            }
        } else if (getGoodsOutputType() != -1) {
            int goodsInput = getGoodsInput();
            int goodsOutput = getProduction();
            int goodsInputType = getGoodsInputType();
            int goodsOutputType = getGoodsOutputType();

            if (getGoodsInput() == 0 && getMaximumGoodsInput() > 0) {
                addModelMessage(this, "model.building.notEnoughInput", new String[][] {{"%building%", getName()}, {"%colony%", colony.getName()}});
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
    * Returns the type of goods this building produces or '-1'
    * if no type applies.
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
    * Returns the type of goods this building needs or '-1'
    * if no type applies.
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
    * @see #getGoodsInput
    * @see #getProduction
    */
    public int getMaximumGoodsInput() {
        int goodsInput = getMaximumProduction();
        goodsInput *= ((level > SHOP) ? SHOP : level); // Factories don't need the extra 3 units.

        return goodsInput;
    }


    /**
    * Returns the amount of goods beeing used to
    * get the current {@link #getProduction production}.
    *
    * @see #getMaximumGoodsInput
    * @see #getProduction
    */
    public int getGoodsInput() {
        if ((getGoodsInputType() > -1) && (colony.getGoodsCount(getGoodsInputType()) < getMaximumGoodsInput()))  { // Not enough goods to do this?
            return colony.getGoodsCount(getGoodsInputType());
        } else {
            return getMaximumGoodsInput();
        }
    }


    /**
    * Returns the actual production of this building.
    *
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
    * @see #getProduction
    */
    public int getProductionNextTurn() {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int goodsOutput = getMaximumProduction();

        if ((getGoodsInputType() > -1) && (colony.getGoodsCount(getGoodsInputType()) + colony.getProductionOf(getGoodsInputType()) < goodsOutput))  { // Not enough goods to do this?
            int goodsInput = colony.getGoodsCount(getGoodsInputType()) + colony.getProductionOf(getGoodsInputType());
            if (level < FACTORY) {
                goodsOutput = goodsInput;
            } else {
                goodsOutput = (goodsInput * 3) / 2;
            }
        }

        return goodsOutput;
    }


    /**
    * Returns the production of the given type of goods.
    */
    public int getProductionOf(int goodsType) {
        if (goodsType == getGoodsOutputType()) {
            return getProduction();
        } else {
            return 0;
        }
    }


    /**
    * Returns the maximum production of this building.
    * That is; the production of this building when there is
    * enough "input goods".
    */
    public int getMaximumProduction() {
        if (getGoodsOutputType() == -1) {
            return 0;
        }

        int goodsOutput = 0;
        int goodsOutputType = getGoodsOutputType();

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
            
            if (getColony().getOwner().hasFather(FoundingFather.THOMAS_JEFFERSON)) {
                goodsOutput += goodsOutput/2;
            }
        }
        
        if (goodsOutputType == Goods.CROSSES && getColony().getOwner().hasFather(FoundingFather.WILLIAM_PENN)) {
            goodsOutput += goodsOutput/2;
        }

        return goodsOutput;
    }


    public void dispose() {
        for (int i=0; i<units.size(); i++) {
            ((Unit) units.get(i)).dispose();
        }
        
        super.dispose();
    }
    

    /**
    * Makes a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Building".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element buildingElement = document.createElement(getXMLElementTagName());

        buildingElement.setAttribute("ID", getID());
        buildingElement.setAttribute("colony", colony.getID());
        buildingElement.setAttribute("type", Integer.toString(type));
        buildingElement.setAttribute("level", Integer.toString(level));

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            buildingElement.appendChild(((FreeColGameObject) unitIterator.next()).toXMLElement(player, document, showAll, toSavedGame));
        }

        return buildingElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param buildingElement The DOM-element ("Document Object Model") made to represent this "Building".
    */
    public void readFromXMLElement(Element buildingElement) {
        setID(buildingElement.getAttribute("ID"));

        colony = (Colony) getGame().getFreeColGameObject(buildingElement.getAttribute("colony"));
        type = Integer.parseInt(buildingElement.getAttribute("type"));
        level = Integer.parseInt(buildingElement.getAttribute("level"));

        units.clear();

        NodeList unitNodeList = buildingElement.getChildNodes();
        for (int i=0; i<unitNodeList.getLength(); i++) {
            Element unitElement = (Element) unitNodeList.item(i);

            Unit unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit != null) {
                unit.readFromXMLElement(unitElement);
                if (!units.contains(unit)) {
                    units.add(unit);
                }
            } else {
                unit = new Unit(getGame(), unitElement);
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
