package net.sf.freecol.common.model;


import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Goods;

import net.sf.freecol.common.FreeColException;

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
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
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
                            STOCKADE = 7,
                            ARMORY = 8,
                            DOCK = 9,
                            SCHOOLHOUSE = 10,
                            WAREHOUSE = 11,
                            STABLES = 12,
                            CHURCH = 13,
                            PRINTING_PRESS = 14,
                            CUSTOM_HOUSE = 15;

    /** The maximum number of building types. */
    public static final int NUMBER_OF_TYPES = 16;

    /** The level of a building */
    public static final int NOT_BUILT = 0,
                            HOUSE = 1,
                            SHOP = 2,
                            FACTORY = 3;

    private static final String[][] buildingNames = {{"Town hall", null, null}, {"Carpenter's house", "Lumber mill", null}, {"Blacksmith's house", "Blacksmith's shop", "Iron works"}, {"Tobacconist's house", "Tobacconist's shop", "Cigar factory"}, {"Weaver's house", "Weaver's shop", "Textile mill"}, {"Distiller's house", "Rum distillery", "Rum factory"}, {"Fur trader's house", "Fur trading post", "Fur factory"}, {"Stockade", "Fort", "Fortress"}, {"Armory", "Magazine", "Arsenal"}, {"Docks", "Drydock", "Shipyard"}, {"Schoolhouse", "College", "University"}, {"Warehouse", "Warehouse expansion", null}, {"Stables", null, null}, {"Church", "Cathedral", null}, {"Printing press", "Newspaper", null}, {"Custom house", null, null}};

    // Sets the maximum number of units in one building (will become a non-constant later):
    private static final int MAX_UNITS = 3;

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
        if (level < MAX_LEVEL) {
            return buildingNames[type][level];
        } else {
            return null;
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
        return MAX_UNITS;
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

        units.add((Unit) locatable);
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
    * Prepares this <code>Building</code> for a new turn.
    */
    public void newTurn() {
    
        int goodsInput = 0;
        int goodsOutput = 0;
        int goodsInputType = -1;
        int goodsOutputType = -1;
	
	Goods thepackage;
        
        if (level == NOT_BUILT) return; // Don't do anything if the building does not exist.

        // Figure out what's produced here and what it requires to do so.
        switch(type) {
            case BLACKSMITH:
                goodsInputType = Goods.ORE;
                goodsOutputType = Goods.TOOLS;
                break;
            case TOBACCONIST:
                goodsInputType = Goods.TOBACCO;
                goodsOutputType = Goods.CIGARS;
                break;
            case WEAVER:
                goodsInputType = Goods.COTTON;
                goodsOutputType = Goods.CLOTH;
                break;
            case DISTILLER:
                goodsInputType = Goods.SUGAR;
                goodsOutputType = Goods.RUM;
                break;
            case FUR_TRADER:
                goodsInputType = Goods.FURS;
                goodsOutputType = Goods.COATS;
                break;
            case ARMORY:
                goodsInputType = Goods.TOOLS;
                goodsOutputType = Goods.MUSKETS;
                break;
           default:
                break;
        }
        if (goodsOutputType < 0) return;

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext())
        {
            goodsOutput += ((Unit) unitIterator.next()).getProducedAmount(goodsOutputType);
        }
        goodsInput = goodsOutput;
        goodsOutput *= level;
        goodsInput *= ((level > SHOP) ? SHOP : level); // Factories don't need the extra 3 units.
        if (colony.getGoodsCount(goodsInputType) < goodsInput) // Not enough goods to do this?
        {
            goodsInput = colony.getGoodsCount(goodsInputType);
            if (level < FACTORY) {
                goodsOutput = goodsInput;
            } else {
                goodsOutput = (goodsInput * 3) / 2;
            }
        }
        if (goodsOutput <= 0) return;
        
        // Actually produce the goods.
        colony.removeAmountAndTypeOfGoods(goodsInputType, goodsInput);
        thepackage = new Goods(getGame(), null, goodsOutputType, goodsOutput);
	thepackage.setLocation(colony);
        //colony.add(thepackage);
        getGame().mustRestartNewTurn = true;
    }


    /**
    * Makes a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Building".
    */
    public Element toXMLElement(Player player, Document document) {
        Element buildingElement = document.createElement(getXMLElementTagName());

        buildingElement.setAttribute("ID", getID());
        buildingElement.setAttribute("colony", colony.getID());
        buildingElement.setAttribute("type", Integer.toString(type));
        buildingElement.setAttribute("level", Integer.toString(level));

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            buildingElement.appendChild(((FreeColGameObject) unitIterator.next()).toXMLElement(player, document));
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

        NodeList unitNodeList = buildingElement.getChildNodes();
        for (int i=0; i<unitNodeList.getLength(); i++) {
            Element unitElement = (Element) unitNodeList.item(i);

            Unit unit = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));
            if (unit != null) {
                unit.readFromXMLElement(unitElement);
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
