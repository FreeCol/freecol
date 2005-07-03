
package net.sf.freecol.common.model;

import java.util.Iterator;
import java.util.logging.Logger;

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

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
    * This array represents the types of the units that can be recruited in
    * Europe. They correspond to the slots that can be seen in the gui and
    * that can be used to communicate with the server/client. The array holds
    * exactly 3 elements and element 0 corresponds to recruit slot 1.
    */
    private int[] recruitables = {-1, -1, -1};

    private int artilleryPrice;

    /**
    * Contains the units on this location.
    */
    private UnitContainer unitContainer;

    private Player owner;


    /**
    * Creates a new <code>Europe</code>.
    * @param game The <code>Game</code> in which this object belong.
    */
    public Europe(Game game, Player owner) {
        super(game);
        this.owner = owner;

        unitContainer = new UnitContainer(game, this);

        setRecruitable(1, owner.generateRecruitable());
        setRecruitable(2, owner.generateRecruitable());
        setRecruitable(3, owner.generateRecruitable());

        artilleryPrice = 500;
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
        else {
            logger.warning("setRecruitable: invalid slot(" + slot + ") or type(" + type + ") given.");
        }
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

        if (unit.getOwner().getRecruitPrice() > unit.getOwner().getGold()) {
            throw new IllegalStateException();
        }

        unit.getOwner().modifyGold(-unit.getOwner().getRecruitPrice());
        unit.setLocation(this);
        unit.getOwner().updateCrossesRequired();
        unit.getOwner().setCrosses(0);

        setRecruitable(slot, newRecruitable);
    }


    /**
    * Causes a unit to emigrate from Europe.
    *
    * @param slot The slot the emigrated unit(type) came from. This is needed
    *             for setting a new recruitable to this slot.
    * @param unit The recruited unit.
    * @param newRecruitable The recruitable that will fill the now empty slot.
    * @exception NullPointerException If <code>unit == null</code>.
    * @exception IllegalStateException If there is not enough crosses to
    *             emigrate the <code>Unit</code>.
    */
    public void emigrate(int slot, Unit unit, int newRecruitable) {
        if (unit == null) {
            throw new NullPointerException();
        }

        if (!unit.getOwner().checkEmigrate()) {
            throw new IllegalStateException("Not enough crosses to emigrate unit: " + unit.getOwner().getCrosses() + "/" + unit.getOwner().getCrossesRequired());
        }

        unit.setLocation(this);
        // TODO: shouldn't we subtract a certain amount of crosses instead of just removing all
        //       crosses? I'm not sure how this was done in the original.
        unit.getOwner().setCrosses(0);

        if (!unit.getOwner().hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            addModelMessage(this, "model.europe.emigrate", new String[][] {{"%unit%", unit.getName()}});
        }
        // In case William Brewster is in the congress we don't need to show a message to the
        // user because he has already been busy picking a unit.

        setRecruitable(slot, newRecruitable);
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
    * Checks if the specified <code>Locatable</code> is at this <code>Location</code>.
    *
    * @param locatable The <code>Locatable</code> to test the presence of.
    * @return The result.
    */
    public boolean contains(Locatable locatable) {
        if (locatable instanceof Unit) {
            return unitContainer.contains((Unit) locatable);
        } else {
            return false;
        }
    }


    public GoodsContainer getGoodsContainer() {
        return null;
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

        if (unit.getType() == Unit.ARTILLERY) {
            artilleryPrice += 100;
        }
    }


    /**
    * Gets the current price for an artillery.
    */
    public int getArtilleryPrice() {
        return artilleryPrice;
    }


    /**
    * Gets the <code>Player</code> using this <code>Europe</code>.
    */
    public Player getOwner() {
        return owner;
    }


    /**
    * Prepares this object for a new turn.
    */
    public void newTurn() {
        // Repair any damaged ships:
        Iterator unitIterator = getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();
            if (unit.isNaval() && unit.isUnderRepair()) {
                unit.setHitpoints(unit.getHitpoints()+1);
            }
        }
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Europe".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element europeElement = document.createElement(getXMLElementTagName());

        europeElement.setAttribute("ID", getID());
        europeElement.setAttribute("recruit0", Integer.toString(recruitables[0]));
        europeElement.setAttribute("recruit1", Integer.toString(recruitables[1]));
        europeElement.setAttribute("recruit2", Integer.toString(recruitables[2]));
        europeElement.setAttribute("artilleryPrice", Integer.toString(artilleryPrice));
        europeElement.setAttribute("owner", owner.getID());

        europeElement.appendChild(unitContainer.toXMLElement(player, document, showAll, toSavedGame));

        return europeElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param europeElement The DOM-element ("Document Object Model") made to represent this "Europe".
    */
    public void readFromXMLElement(Element europeElement) {
        setID(europeElement.getAttribute("ID"));

        recruitables[0] = Integer.parseInt(europeElement.getAttribute("recruit0"));
        recruitables[1] = Integer.parseInt(europeElement.getAttribute("recruit1"));
        recruitables[2] = Integer.parseInt(europeElement.getAttribute("recruit2"));
        artilleryPrice = Integer.parseInt(europeElement.getAttribute("artilleryPrice"));
        owner = (Player) getGame().getFreeColGameObject(europeElement.getAttribute("owner"));

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
