
package net.sf.freecol.common.model;


import net.sf.freecol.FreeCol;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.OptionMap;

import org.w3c.dom.Element;


/**
* Keeps track of the available game options. New options should be added to
* {@link #addDefaultOptions()} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class GameOptions extends OptionMap {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /** The amount of money each player will receive before the game starts. */
    public static final String STARTING_MONEY = "startingMoney";

    /** The cost of a single hammer when buying a building in a colony. */
    public static final String HAMMER_PRICE = "hammerPrice";

    /** The Stock the custom house should keep when selling goods. */
    public static final String CUSTOM_STOCK = "customStock";

    /** Does the Custom House sell boycotted goods **/
    public static final String CUSTOM_IGNORE_BOYCOTT = "customIgnoreBoycott";

    /** Enables/disables fog of war. */
    public static final String FOG_OF_WAR = "fogOfWar";

    /** No units are hidden on carriers or settlements if this option is set to <code>false</code>. */
    public static final String UNIT_HIDING = "unitHiding";
    
    /** 
     * Victory condition: Should the <code>Player</code> who first defeats the
     * Royal Expeditionary Force win the game?
     */
    public static final String VICTORY_DEFEAT_REF = "victoryDefeatREF";
    
    /** 
     * Victory condition: Should a <code>Player</code> who first defeats all
     * other european players win the game?
     */
    public static final String VICTORY_DEFEAT_EUROPEANS = "victoryDefeatEuropeans";    

    /** 
     * Victory condition: Should a <code>Player</code> who first defeats all
     * other human players win the game?
     */
    public static final String VICTORY_DEFEAT_HUMANS = "victoryDefeatHumans";
    
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
        if (FreeCol.isInDebugMode()) {
            starting.add(new IntegerOption(STARTING_MONEY, "gameOptions.startingMoney.name", "gameOptions.startingMoney.shortDescription", 0, 50000, 10000));
        } else {
            starting.add(new IntegerOption(STARTING_MONEY, "gameOptions.startingMoney.name", "gameOptions.startingMoney.shortDescription", 0, 50000, 0));
        }
        add(starting);

        /* Map options: */
        OptionGroup map = new OptionGroup("gameOptions.map.name", "gameOptions.map.shortDescription");
        map.add(new BooleanOption(FOG_OF_WAR, "gameOptions.fogOfWar.name", "gameOptions.fogOfWar.shortDescription", true));
        map.add(new BooleanOption(UNIT_HIDING, "gameOptions.unitHiding.name", "gameOptions.unitHiding.shortDescription", true));
        add(map);        

        /* Colony options: */
        OptionGroup colony = new OptionGroup("gameOptions.colony.name", "gameOptions.colony.shortDescription");
        colony.add(new IntegerOption(HAMMER_PRICE, "gameOptions.hammerPrice.name", "gameOptions.hammerPrice.shortDescription", 0, 50, 20));
        colony.add(new IntegerOption(CUSTOM_STOCK, "gameOptions.customStock.name", "gameOptions.customStock.shortDescription", 0, 300, 50));
        colony.add(new BooleanOption(CUSTOM_IGNORE_BOYCOTT, "gameOptions.customIgnoreBoycott.name", "gameOptions.customIgnoreBoycott.shortDescription", false));

        add(colony);

        /* Victory Conditions */
        OptionGroup victoryConditions = new OptionGroup("gameOptions.victoryConditions.name", "gameOptions.victoryConditions.shortDescription");
        victoryConditions.add(new BooleanOption(VICTORY_DEFEAT_REF, "gameOptions.victoryDefeatREF.name", "gameOptions.victoryDefeatREF.shortDescription", true));
        victoryConditions.add(new BooleanOption(VICTORY_DEFEAT_EUROPEANS, "gameOptions.victoryDefeatEuropeans.name", "gameOptions.victoryDefeatEuropeans.shortDescription", true));
        victoryConditions.add(new BooleanOption(VICTORY_DEFEAT_HUMANS, "gameOptions.victoryDefeatHumans.name", "gameOptions.victoryDefeatHumans.shortDescription", false));
        add(victoryConditions);        
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "gameOptions".
    */
    public static String getXMLElementTagName() {
        return "gameOptions";
    }

}
