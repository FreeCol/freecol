/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.common.model;


import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.OptionMap;
import net.sf.freecol.common.option.RangeOption;

import org.w3c.dom.Element;


/**
* Keeps track of the available game options. New options should be added to
* {@link #addDefaultOptions()} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class GameOptions extends OptionMap {



    /** The amount of money each player will receive before the game starts. */
    public static final String STARTING_MONEY = "startingMoney";

    /** The cost of a single hammer when buying a building in a colony. */
    public static final String HAMMER_PRICE = "hammerPrice";

    /** Does the Custom House sell boycotted goods **/
    public static final String CUSTOM_IGNORE_BOYCOTT = "customIgnoreBoycott";

    /** Whether experts have connections, producing without raw materials in factories */
    public static final String EXPERTS_HAVE_CONNECTIONS = "expertsHaveConnections";

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
     * The difficulty of the game.
     */
    public static final String DIFFICULTY = "difficulty";
    
    /**
    * Creates a new <code>GameOptions</code>.
    */
    public GameOptions() {
        super(getXMLElementTagName());
    }


    /**
    * Creates an <code>GameOptions</code> from an XML representation.
    *
    * <br><br>
    *
    * @param in The input stream containing the XML.
    * @throws XMLStreamException if an error occured during parsing.
    */
    public GameOptions(XMLStreamReader in) throws XMLStreamException {
        super(in, getXMLElementTagName());
    }
    
    /**
     * Creates an <code>GameOptions</code> from an XML representation.
     *
     * <br><br>
     *
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public GameOptions(Element e) {
        super(e, getXMLElementTagName());
    }




    /**
    * Adds the options to this <code>GameOptions</code>.
    */
    protected void addDefaultOptions() {
        /* Add options here: */

        /* Initial values: */
        OptionGroup starting = new OptionGroup("gameOptions.starting");
        if (FreeCol.isInDebugMode()) {
            new IntegerOption(STARTING_MONEY, starting, 0, 50000, 10000);
        } else {
            new IntegerOption(STARTING_MONEY, starting, 0, 50000, 0);
        }
        add(starting);

        /* Map options: */
        OptionGroup map = new OptionGroup("gameOptions.map");
        new BooleanOption(FOG_OF_WAR, map, true);
        new BooleanOption(UNIT_HIDING, map, true);
        add(map);        

        /* Colony options: */
        OptionGroup colony = new OptionGroup("gameOptions.colony");
        new IntegerOption(HAMMER_PRICE, colony, 0, 50, 20);
        new BooleanOption(CUSTOM_IGNORE_BOYCOTT, colony, false);
        new BooleanOption(EXPERTS_HAVE_CONNECTIONS, colony, false);

        add(colony);

        /* Victory Conditions */
        OptionGroup victoryConditions = new OptionGroup("gameOptions.victoryConditions");
        new BooleanOption(VICTORY_DEFEAT_REF, victoryConditions, true);
        new BooleanOption(VICTORY_DEFEAT_EUROPEANS, victoryConditions, true);
        new BooleanOption(VICTORY_DEFEAT_HUMANS, victoryConditions, false);
        add(victoryConditions);

        /* Difficulty settings */
        OptionGroup difficultySettings = new OptionGroup("gameOptions.difficultySettings");
        new RangeOption(DIFFICULTY, difficultySettings,
                         new String[] {"veryEasy", 
                                       "easy", 
                                       "normal", 
                                       "hard", 
                                       "veryHard"}, 
                         2);
        add(difficultySettings);
    }

    protected boolean isCorrectTagName(String tagName) {
        return getXMLElementTagName().equals(tagName);
    }

    /**
    * Gets the tag name of the root element representing this object.
    * @return "gameOptions".
    */
    public static String getXMLElementTagName() {
        return "gameOptions";
    }

}
