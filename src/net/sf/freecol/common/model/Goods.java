
package net.sf.freecol.common.model;

import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* Represents a locatable goods of a specified type and amount.
*/
public final class Goods extends FreeColGameObject implements Locatable {
    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(Goods.class.getName());

    public static final int FOOD = 0,
                            SUGAR = 1,
                            TOBACCO = 2,
                            COTTON = 3,
                            FURS = 4,
                            LUMBER = 5,
                            ORE = 6,
                            SILVER = 7,
                            HORSES = 8,
                            RUM = 9,
                            CIGARS = 10,
                            CLOTH = 11,
                            COATS = 12,
                            TRADE_GOODS = 13,
                            TOOLS = 14,
                            MUSKETS = 15,
                            //FISH = 16; //Probably not needed except for graphical purposes -sjm
                            BELLS = 17,
                            CROSSES = 18,
                            HAMMERS = 19;
    
    public static final int NUMBER_OF_TYPES = 16; // Anything past this point can't be stored -sjm


    private int type;
    private int amount;

    private Location        location;


    
    /**
    * Creates a new <code>Goods</code>.
    *
    * @param game The <code>Game</code> in which this object belongs
    * @param location The location of the goods,
    * @param type The type of the goods.
    * @param amount The amount of the goods.
    */
    public Goods(Game game, Location location, int type, int amount) {
        super(game);

        this.type = type;
        this.amount = amount;
    }

    
    /**
    * Initializes this object from an XML-representation of this object.
    *
    * @param The <code>Game</code> in which this <code>Unit</code> belong.
    * @param element The DOM-element ("Document Object Model") made to represent this "Goods".
    */
    public Goods(Game game, Element element) {
        super(game, element);
        readFromXMLElement(element);
    }




    /**
    * Sets the location of the goods.
    * @param location The new location of the goods,
    */
    public void setLocation(Location location) {
        if ((this.location != null)) {
	    this.location.remove(this);
	}
        
        if (location != null) {
            location.add(this);
        }

        this.location = location;
    }


    /**
    * Gets the location of this goods.
    * @return The location.
    */
    public Location getLocation() {
        return location;
    }


    /**
    * Gets the amount of space this <code>Goods</code> take.
    * @return The amount.
    */
    public int getTakeSpace() {
        return (((amount - 1) / 100) + 1);
    } 
    
    /**
    * Gets the value <code>amount</code>.
    * @return The current value of amount.
    */
    public int getAmount() {
        return amount;
    }
    
    /**
    * Sets the value <code>amount</code>.
    * @param a The new value for amount.
    */
    public void setAmount(int a) {
        amount = a;
    }
    
    /**
    * Gets the value <code>type</code>. Note that type of goods should NEVER change.
    * @return The current value of type.
    */
    public int getType() {
        return type;
    }
    
    /**
    * Prepares the <code>Goods</code> for a new turn.
    */
    public void newTurn() {
    
    }

    /**
    * Loads the cargo onto a carrier that is on the same tile.
    *
    * @param carrier The carrier this unit shall embark.
    * @exception IllegalStateException If the carrier is on another tile than this unit.
    */
    public void Load(Unit carrier) {
        if ((getLocation() == null) || (getLocation().getTile() == carrier.getTile())) {
            setLocation(carrier);
        } else {
            throw new IllegalStateException("It is not allowed to board a ship on another tile.");
        }
    }


    /**
    * Unload the goods from the ship. This method should only be invoked if the ship is in a harbour.
    *
    * @exception IllegalStateException If not in harbour.
    * @exception ClassCastException If not located on a ship.
    */
    public void Unload() {
        Location l = ((Unit) getLocation()).getLocation();

        logger.warning("Unloading cargo from a ship.");
        if (l instanceof Europe) {
            //TODO: sell the goods. But for now...
            setLocation(l);
        } else if (l.getTile().getSettlement() != null && l.getTile().getSettlement() instanceof Colony) {
            setLocation(l.getTile().getSettlement());
        } else {
            throw new IllegalStateException("Goods may only leave a ship while in a harbour.");
        }
    }    
    
    /**
    * Removes all references to this object.
    */
    public void dispose() {
        if (location != null) {
            location.remove(this);
        }

        super.dispose();
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Goods".
    */
    public Element toXMLElement(Player player, Document document) {
        Element goodsElement = document.createElement(getXMLElementTagName());

        goodsElement.setAttribute("ID", getID());
        goodsElement.setAttribute("type", Integer.toString(type));
        goodsElement.setAttribute("amount", Integer.toString(amount));

        if (location != null) {
            goodsElement.setAttribute("location", location.getID());
        }
        
        return goodsElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param goodsElement The DOM-element ("Document Object Model") made to represent this "Goods".
    */
    public void readFromXMLElement(Element goodsElement) {
        setID(goodsElement.getAttribute("ID"));

        type = Integer.parseInt(goodsElement.getAttribute("type"));
        amount = Integer.parseInt(goodsElement.getAttribute("amount"));

        if (goodsElement.hasAttribute("location")) {
            location = (Location) getGame().getFreeColGameObject(goodsElement.getAttribute("location"));
        }

    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "goods".
    */
    public static String getXMLElementTagName() {
        return "goods";
    }

}
