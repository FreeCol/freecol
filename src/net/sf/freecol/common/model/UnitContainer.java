
package net.sf.freecol.common.model;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;


/**
* Contains units and can be used by a {@link Location} to make certain
* tasks easier.
*/
public class UnitContainer extends FreeColGameObject {
    private static Logger logger = Logger.getLogger(Location.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The list of units stored in this <code>UnitContainer</code>. */
    private ArrayList units = new ArrayList();
    
    /** The owner of this <code>UnitContainer</code>. */
    private Location parent;




    /**
    * Creates an empty <code>UnitContainer</code>.
    *
    * @param game The <code>Game</code> in which this <code>UnitContainer</code> belong.
    * @param location The <code>Location</code> this <code>UnitContainer</code> will be containg units for.
    */
    public UnitContainer(Game game, Location parent) {
        super(game);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
    }


    /**
    * Initiates a new <code>UnitContainer</code> from an <code>Element</code>.
    *
    * @param game The <code>Game</code> in which this <code>UnitContainer</code> belong.
    * @param element The <code>Element</code> (in a DOM-parsed XML-tree) that describes
    *                this object.
    */
    public UnitContainer(Game game, Location parent, Element element) {
        super(game, element);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXMLElement(element);
    }





    /**
    * Adds a <code>Unit</code> to this containter.
    * @param u The Unit to add to this container.
    */
    public void addUnit(Unit unit) {
        if (!units.contains(unit)) {
            units.add(unit);
        }
    }


    /**
    * Removes a Unit from this containter.
    *
    * @param u The Unit to remove from this container.
    * @return the unit that has been removed from this container.
    */
    public Unit removeUnit(Unit u) {
        int index = units.indexOf(u);

        if (index != -1) {
            return (Unit) units.remove(index);
        } else {
            return null;
        }
    }


    /**
    * Checks if the specified <code>Unit</code> is in this container.
    *
    * @param The <code>Unit</code> to test the presence of.
    * @return The result.
    */
    public boolean contains(Unit u) {
        return units.contains(u);
    }


    /**
    * Returns the amount of Units in this container.
    * @return The amount of Units in this container.
    */
    public int getUnitCount() {
        try {
            return units.size();
        } catch(NullPointerException e) {
            return 0;
        }
    }


    /**
    * Returns the total amount of Units at this Location.
    * This also includes units in a carrier
    *
    * @return The total amount of Units at this Location.
    */
    public int getTotalUnitCount() {
        int amount = 0;

        Iterator unitIterator = getUnitIterator();

        while (unitIterator.hasNext()) {
            amount += ((Unit) unitIterator.next()).getUnitCount();
        }

        return amount + getUnitCount();
    }

    /**
    * Gets a clone of the <code>units</code> array.
    *
    * @return The clone.
    */
    public ArrayList getUnitsClone() {
        return (ArrayList)units.clone();
    }

    /**
    * Gets an <code>Iterator</code> of every <code>Unit</code> in this
    * <code>UnitContainer</code>.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator() {
        return units.iterator();
    }


    /**
    * Gets the first <code>Unit</code> in this <code>UnitContainer</code>.
    * @return The <code>Unit</code>.
    */
    public Unit getFirstUnit() {
        if (units.size() == 0) {
            return null;
        } else {
            return (Unit) units.get(0);
        }
    }


    /**
    * Gets the last <code>Unit</code> in this <code>UnitContainer</code>.
    * @return The <code>Unit</code>.
    */
    public Unit getLastUnit() {
        if (units.size() == 0) {
            return null;
        } else {
            return (Unit) units.get(units.size() - 1);
        }
    }


    /**
    * Removes all references to this object.
    */
    public void dispose() {
        disposeAllUnits();
        super.dispose();
    }


    /**
    * Disposes all units in this <code>UnitContainer</code>.
    */
    public void disposeAllUnits() {
        for (int i=units.size()-1; i>=0; i--) {
            ((Unit) units.get(i)).dispose();
        }
    }


    /**
    * Prepares this <code>UnitContainer</code> for a new turn.
    */
    public void newTurn() {

    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "UnitContainer".
    */
    public Element toXMLElement(Player player, Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", getID());

        Iterator unitIterator = getUnitIterator();

        while (unitIterator.hasNext()) {
            element.appendChild(((Unit) unitIterator.next()).toXMLElement(player, document));
        }

        return element;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param element The DOM-element ("Document Object Model") made to represent this "UnitContainer".
    */
    public void readFromXMLElement(Element element) {
        setID(element.getAttribute("ID"));

        //NodeList unitNodeList = element.getElementsByTagName(Unit.getXMLElementTagName());
        NodeList unitNodeList = element.getChildNodes();

        units.clear();

        for (int i=0; i<unitNodeList.getLength(); i++) {
            Element unitElement = (Element) unitNodeList.item(i);

            // Check if the unit is already here -> only update:
            Unit u = (Unit) getGame().getFreeColGameObject(unitElement.getAttribute("ID"));

            if (u != null) {
                u.readFromXMLElement(unitElement);
                if (!units.contains(u)) {
                    units.add(u);
                }
                //u.setLocation(parent);
            } else {
                u = new Unit(getGame(), unitElement);
                units.add(u);
                //u.setLocation(parent);
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "unitContainer".
    */
    public static String getXMLElementTagName() {
        return "unitContainer";
    }
}
