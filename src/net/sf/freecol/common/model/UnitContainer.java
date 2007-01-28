
package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


/**
* Contains units and can be used by a {@link Location} to make certain
* tasks easier.
*/
public class UnitContainer extends FreeColGameObject {
    private static Logger logger = Logger.getLogger(Location.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
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
    * @param parent The <code>Location</code> this <code>UnitContainer</code> will be containg units for.
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
     * @param game The <code>Game</code> in which this 
     *       <code>UnitContainer</code> belong.
     * @param parent The parent panel.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public UnitContainer(Game game, Location parent, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXML(in);
    }

    /**
     * Initiates a new <code>UnitContainer</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this 
     *       <code>UnitContainer</code> belong.
     * @param parent The parent panel.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public UnitContainer(Game game, Location parent, Element e) {
        super(game, e);

        if (parent == null) {
            throw new NullPointerException();
        }

        this.parent = parent;
        readFromXMLElement(e);
    }





    /**
    * Adds a <code>Unit</code> to this containter.
    * @param unit The Unit to add to this container.
    */
    public void addUnit(Unit unit) {
        if (!units.contains(unit)) {
            units.add(unit);
        }
    }
    
    /**
    * Adds a <code>Unit</code> to this containter at <code>index</code>.
    * @param index the position in the List
    * @param unit The Unit to add to this container.
     */
    public void addUnit(int index,Unit unit) {
        if (!units.contains(unit)) {
        	units.add(index,unit);
        }
    }
    
    /**
    * Removes a Unit from this containter.
    *
    * @param u The Unit to remove from this container.
    * @return true if the unit has been removed from this container.
    */
    public boolean removeUnit(Unit u) {
            return units.remove(u);
    }


    /**
    * Checks if the specified <code>Unit</code> is in this container.
    *
    * @param u The <code>Unit</code> to test the presence of.
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
        return getUnitsClone().iterator();
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
        Iterator it = ((List) units.clone()).iterator();
        while (it.hasNext()) {
            ((Unit) it.next()).dispose();
        }
    }


    /**
    * Prepares this <code>UnitContainer</code> for a new turn.
    */
    public void newTurn() {

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

        out.writeAttribute("ID", getID());

        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit u = (Unit) unitIterator.next();
            u.toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));

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
    * @return "unitContainer".
    */
    public static String getXMLElementTagName() {
        return "unitContainer";
    }
}
