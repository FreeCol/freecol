
package net.sf.freecol.common.model;

import java.util.Vector;
import java.util.Iterator;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Element;
import org.w3c.dom.Document;


/**
* Represents Europe in the game. Each <code>Player</code> has it's own
* <code>Europe</code>. 
*
* <br><br>
*
* Europe is the place where you can {@link #recruit}
* and {@link #train} new units. You may also sell/buy goods.
*/
public final class Europe extends FreeColGameObject implements Location {
    private static final Logger logger = Logger.getLogger(Europe.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * This array represents the types of the units that can be recruited in
    * Europe. They correspond to the slots that can be seen in the gui and
    * that can be used to communicate with the server/client. The array holds
    * exactly 3 elements and element 0 corresponds to recruit slot 1.
    */
    private int[] recruitables = {-1, -1, -1};

    /**
    * The price that needs to be paid when recruiting units (changes all the time!).
    */
    private int recruitPrice = -1;

    /**
    * Contains the units on this location.
    */
    private UnitContainer unitContainer;




    /**
    * Creates a new <code>Europe</code>.
    * @param game The <code>Game</code> in which this object belong.
    */
    public Europe(Game game) {
        super(game);

        unitContainer = new UnitContainer(game, this);

        setRecruitPrice(250); // Base price is 250 with no crosses. -sjm

        setRecruitable(1, Unit.generateRecruitable());
        setRecruitable(2, Unit.generateRecruitable());
        setRecruitable(3, Unit.generateRecruitable());
    }


    /**
    * Initializes this object from an XML-representation of this object.
    *
    * @param game The <code>Game</code> in which this object belong.
    * @param element The DOM-element ("Document Object Model") made to represent this "Europe".
    */
    public Europe(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }





    /**
    * Gets the type of the recruitable in Europe at the given slot.
    *
    * @param slot The slot of the recruitable whose type needs to be returned. Should
    *             be 1, 2 or 3.
    * @return The type of the recruitable in Europe at the given slot.
    * @exception IllegalArgumentException if the given <code>slot</code> does not exist.
    */
    public int getRecruitable(int slot) {
        if ((slot > 0) && (slot < 4)) {
            return recruitables[slot - 1];
        } else {
            throw new IllegalArgumentException("Wrong recruitement slot: " + slot);
        }
    }


    /**
    * Sets the type of the recruitable in Europe at the given slot to the given type.
    *
    * @param slot The slot of the recruitable whose type needs to be set. Should
    *             be 1, 2 or 3.
    * @param type The new type for the unit at the given slot in Europe. Should be a
    *             valid unit type.
    */
    public void setRecruitable(int slot, int type) {
        if ((slot > 0) && (slot < 4) && (type >= 0) && (type < Unit.UNIT_COUNT)) {
            recruitables[slot - 1] = type;
        }
    }


    /**
    * Sets the recruitment price (for recruiting units in Europe).
    * @param price The new recruitment price.
    */
    public void setRecruitPrice(int price) {
        recruitPrice = price;
    }


    /**
    * Returns the price that needs to be paid in order to recruit units in Europe.
    * @return The price that needs to be paid in order to recruit units in Europe.
    */
    public int getRecruitPrice() {
        return recruitPrice;
    }


    /**
    * Recruits a unit from Europe.
    *
    * @param slot The slot the recruited unit(type) came from. This is needed
    *             for setting a new recruitable to this slot.
    * @param unit The recruited unit.
    * @param newRecruitable The recruitable that will fill the now empty slot.
    * @exception NullPointerException if <code>unit == null</code>.
    * @exception IllegalStateException if the player recruiting the unit cannot afford the price.
    */
    public void recruit(int slot, Unit unit, int newRecruitable) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (getRecruitPrice() > unit.getOwner().getGold()) {
            throw new IllegalStateException();
        }

        unit.getOwner().modifyGold(-getRecruitPrice());
        unit.setLocation(this);
        
        //setRecruitable(slot, Unit.generateRecruitable());
        setRecruitable(slot, newRecruitable);
        //Note that the player object causes this to change -sjm
        //setRecruitPrice(getRecruitPrice() * 2);
    }


    /**
    * Causes a unit to emigrate from Europe.
    *
    * @param slot The slot the emigrated unit(type) came from. This is needed
    *             for setting a new recruitable to this slot.
    * @param unit The recruited unit.
    * @param newRecruitable The recruitable that will fill the now empty slot.
    * @exception NullPointerException if <code>unit == null</code>.
    */
    public void emigrate(int slot, Unit unit, int newRecruitable) {
        if (unit == null) {
            throw new NullPointerException();
        }

        unit.setLocation(this);
        
        //setRecruitable(slot, Unit.generateRecruitable());
        setRecruitable(slot, newRecruitable);
        //Note that the player object causes this to change -sjm
        //setRecruitPrice(getRecruitPrice() * 2);
    }


    /**
    * Returns <i>null</i>.
    * @return <i>null</i>.
    */
    public Tile getTile() {
        return null;
    }


    /**
    * Adds a <code>Locatable</code> to this Location.
    * @param locatable The code>Locatable</code> to add to this Location.
    */
    public void add(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.addUnit((Unit) locatable);
        } else {
            logger.warning("Tried to add an unrecognized 'Locatable' to a europe.");
        }
    }


    /**
    * Removes a code>Locatable</code> from this Location.
    * @param locatable The <code>Locatable</code> to remove from this Location.
    */
    public void remove(Locatable locatable) {
        if (locatable instanceof Unit) {
            unitContainer.removeUnit((Unit) locatable);
        } else {
            logger.warning("Tried to remove an unrecognized 'Locatable' from a europe.");
        }
    }


    /**
    * Checks if the specified <code>Unit</code> is at this <code>Location</code>.
    *
    * @param The <code>Unit</code> to test the presence of.
    * @return The result.
    */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        } else {
            return false;
        }
    }


    /**
    * Checks wether or not the specified locatable may be added to this
    * <code>Location</code>.
    *
    * @param locatable The <code>Locatable</code> to test the addabillity of.
    * @return <i>true</i>.
    */
    public boolean canAdd(Locatable locatable) {
        return true;
    }


    /**
    * Gets the amount of Units at this Location.
    * @return The amount of Units at this Location.
    */
    public int getUnitCount() {
        return unitContainer.getUnitCount();
    }


    /**
    * Gets an <code>Iterator</code> of every <code>Unit</code> directly located in this
    * <code>Europe</code>. This does not include <code>Unit</code>s on ships.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getUnitIterator() {
        return unitContainer.getUnitIterator();
    }


    /**
    * Gets the first <code>Unit</code> in this <code>Europe</code>.
    * @return The first <code>Unit</code> in this <code>Europe</code>.
    */
    public Unit getFirstUnit() {
        return unitContainer.getFirstUnit();
    }


    /**
    * Gets the last <code>Unit</code> in this <code>Europe</code>.
    * @return The last <code>Unit</code> in this <code>Europe</code>.
    */
    public Unit getLastUnit() {
        return unitContainer.getLastUnit();
    }
    

    /**
    * Trains a unit in Europe.
    *
    * @param unit The trained unit.
    * @exception NullPointerException if <code>unit == null</code>.
    * @exception IllegalStateException if the player recruiting the unit cannot afford the price.
    */
    public void train(Unit unit) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (unit.getPrice() > unit.getOwner().getGold()) {
            throw new IllegalStateException();
        }

        unit.getOwner().modifyGold(-unit.getPrice());
        unit.setLocation(this);
    }

    
    /**
    * Prepares this object for a new turn.
    */
    public void newTurn() {

    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Europe".
    */
    public Element toXMLElement(Player player, Document document) {
        Element europeElement = document.createElement(getXMLElementTagName());

        europeElement.setAttribute("ID", getID());
        europeElement.setAttribute("recruitPrice", Integer.toString(recruitPrice));
        europeElement.setAttribute("recruit0", Integer.toString(recruitables[0]));
        europeElement.setAttribute("recruit1", Integer.toString(recruitables[1]));
        europeElement.setAttribute("recruit2", Integer.toString(recruitables[2]));

        europeElement.appendChild(unitContainer.toXMLElement(player, document));

        return europeElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param europeElement The DOM-element ("Document Object Model") made to represent this "Europe".
    */
    public void readFromXMLElement(Element europeElement) {
        setID(europeElement.getAttribute("ID"));

        recruitPrice = Integer.parseInt(europeElement.getAttribute("recruitPrice"));
        recruitables[0] = Integer.parseInt(europeElement.getAttribute("recruit0"));
        recruitables[1] = Integer.parseInt(europeElement.getAttribute("recruit1"));
        recruitables[2] = Integer.parseInt(europeElement.getAttribute("recruit2"));

        Element unitContainerElement = getChildElement(europeElement, UnitContainer.getXMLElementTagName());
        if (unitContainer != null) {
            unitContainer.readFromXMLElement(unitContainerElement);
        } else {
            unitContainer = new UnitContainer(getGame(), this, unitContainerElement);
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "europe".
    */
    public static String getXMLElementTagName() {
        return "europe";
    }

}
