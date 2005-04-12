
package net.sf.freecol.common.model;

import net.sf.freecol.common.option.*;

import java.util.logging.Logger;
import java.util.*;

import org.w3c.dom.*;


/**
* Keeps track of the available game options.
*/
public final class GameOptions extends OptionGroup {
    private static Logger logger = Logger.getLogger(GameOptions.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    public static final String STARTING_MONEY = "startingMoney";


    private HashMap values;


    /**
    * Creates a new <code>GameOptions</code>.
    */
    public GameOptions() {
        super("gameOptions", "gameOptions.name", "gameOptions.shortDescription");

        /* Add options here: */
        add(new IntegerOption(STARTING_MONEY, "gameOptions.startingMoney.name", "gameOptions.startingMoney.shortDescription", 0));

        createMap();
    }


    /**
    * Creates an <code>GameOptions</code> from an XML representation.
    *
    * <br><br>
    *
    * @param element The XML <code>Element</code> from which this object
    *                should be constructed.
    */
    public GameOptions(Element element) {
        super(element);
        readFromXMLElement(element);
    }




    /**
    * Gets the integer value of an option.
    *
    * @param id The id of the option.
    * @return The value.
    * @exception IllegalArgumentException If there is no integer
    *            value associated with the specified option.
    */
    public int getInteger(String id) {
        try {
            return ((IntegerOption) values.get(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }


    private void createMap() {
        if (values == null) {
            values = new HashMap();
        } else {
            values.clear();
        }
        addToMap(this);
    }


    private void addToMap(OptionGroup og) {
        Iterator it = iterator();
        while (it.hasNext()) {
            Option option = (Option) it.next();
            if (option instanceof OptionGroup) {
                addToMap((OptionGroup) option);
            }
            values.put(option.getId(), option);
        }
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "GameOptions".
    */
    public Element toXMLElement(Document document) {
        Element gameOptionsElement = document.createElement(getXMLElementTagName());

        writeAttributes(gameOptionsElement);

        return gameOptionsElement;
    }


    /**
    * Initializes this object from an XML-representation of this object.
    * @param gameOptionsElement The DOM-element ("Document Object Model") made to represent this "GameOptions".
    */
    public void readFromXMLElement(Element gameOptionsElement) {
        readAttributes(gameOptionsElement);
        createMap();
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "gameOptions".
    */
    public static String getXMLElementTagName() {
        return "gameOptions";
    }

}
