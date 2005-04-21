
package net.sf.freecol.common.model;

import net.sf.freecol.common.option.*;

import java.util.logging.Logger;
import java.util.*;

import org.w3c.dom.*;


/**
* Keeps track of the available game options. New options should be added to
* {@link #addDefaultOptions} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class GameOptions extends OptionMap {
    private static Logger logger = Logger.getLogger(GameOptions.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /** The amount of money each player will receive before the game starts. */
    public static final String STARTING_MONEY = "startingMoney";



    /**
    * Creates a new <code>GameOptions</code>.
    */
    public GameOptions() {
        super(getXMLElementTagName(), "gameOptions.name", "gameOptions.shortDescription");
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
        super(element, getXMLElementTagName(), "gameOptions.name", "gameOptions.shortDescription");
    }




    /**
    * Adds the options to this <code>GameOptions</code>.
    */
    protected void addDefaultOptions() {
        /* Add options here: */

        /* Initial values: */
        OptionGroup starting = new OptionGroup("gameOptions.starting.name", "gameOptions.starting.shortDescription");
        starting.add(new IntegerOption(STARTING_MONEY, "gameOptions.startingMoney.name", "gameOptions.startingMoney.shortDescription", 0, 50000, 0));
        add(starting);
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "gameOptions".
    */
    public static String getXMLElementTagName() {
        return "gameOptions";
    }

}
