
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
                            MUSKETS = 15;
    
    public static final int NUMBER_OF_TYPES = 16;


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
        this.location.remove(this);
        
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
        return 1;
    }
    
    
    /**
    * Prepares the <code>Goods</code> for a new turn.
    */
    public void newTurn() {
    
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
