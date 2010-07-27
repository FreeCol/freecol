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

import net.sf.freecol.common.option.OptionMap;
import org.w3c.dom.Element;


/**
* Keeps track of the available game options. New options should be added to
* {@link #addDefaultOptions()} and each option should be given an unique
* identifier (defined as a constant in this class).
*/
public class GameOptions extends OptionMap {

    /**
     * Describe specification here.
     */
    private Specification specification;

    /** The amount of money each player will receive before the game starts. */
    public static final String STARTING_MONEY = "model.option.startingMoney";

    /** The cost of a single hammer when buying a building in a colony. */
    //Unused at the moment
    // public static final String HAMMER_PRICE = "hammerPrice";

    /** Does the Custom House sell boycotted goods **/
    public static final String CUSTOM_IGNORE_BOYCOTT = "model.option.customIgnoreBoycott";

    /** Whether experts have connections, producing without raw materials in factories */
    public static final String EXPERTS_HAVE_CONNECTIONS = "model.option.expertsHaveConnections";

    public static final String SAVE_PRODUCTION_OVERFLOW = "model.option.saveProductionOverflow";

    /** Whether to award exploration points or not. */
    public static final String EXPLORATION_POINTS = "model.option.explorationPoints";

    /** Enables/disables fog of war. */
    public static final String FOG_OF_WAR = "model.option.fogOfWar";

    /** 
     * Victory condition: Should the <code>Player</code> who first defeats the
     * Royal Expeditionary Force win the game?
     */
    public static final String VICTORY_DEFEAT_REF = "model.option.victoryDefeatREF";
    
    /** 
     * Victory condition: Should a <code>Player</code> who first defeats all
     * other european players win the game?
     */
    public static final String VICTORY_DEFEAT_EUROPEANS = "model.option.victoryDefeatEuropeans";    

    /** 
     * Victory condition: Should a <code>Player</code> who first defeats all
     * other human players win the game?
     */
    public static final String VICTORY_DEFEAT_HUMANS = "model.option.victoryDefeatHumans";

    /**
     * Whether to educate the least skilled unit first. This is the
     * behaviour of the original game and disallows manually assigning
     * students to teachers.
     */
    public static final String ALLOW_STUDENT_SELECTION =
        "model.option.allowStudentSelection";

    /**
     * The difficulty of the game.
     */
    public static final String DIFFICULTY = "model.option.difficulty";
    
    /**
     * Creates a new <code>GameOptions</code>.
     * @param specification a <code>Specification</code> value
     */
    public GameOptions(Specification specification) {
        super(getXMLElementTagName());
        setSpecification(specification);
    }


    /**
     * Creates an <code>GameOptions</code> from an XML representation.
     *
     * <br><br>
     *
     * @param in The input stream containing the XML.
     * @param specification a <code>Specification</code> value
     * @exception XMLStreamException if an error occured during parsing.
     */
    public GameOptions(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        super(in, getXMLElementTagName());
        setSpecification(specification);
    }
    
    /**
     * Creates an <code>GameOptions</code> from an XML representation.
     *
     * <br><br>
     *
     * @param e An XML-element that will be used to initialize
     *      this object.
     * @param specification a <code>Specification</code> value
     */
    public GameOptions(Element e, Specification specification) {
        super(e, getXMLElementTagName());
        setSpecification(specification);
    }

    /**
     * Get the <code>Specification</code> value.
     *
     * @return a <code>Specification</code> value
     */
    public final Specification getSpecification() {
        return specification;
    }

    /**
     * Set the <code>Specification</code> value.
     *
     * @param newSpecification The new Specification value.
     */
    public final void setSpecification(final Specification newSpecification) {
        this.specification = newSpecification;
    }

    /**
    * Adds the options to this <code>GameOptions</code>.
    */
    protected void addDefaultOptions() {
        Specification spec = Specification.getSpecification();
        
        /* Map options: */
        add(spec.getOptionGroup("gameOptions.map"));
        /* Colony options: */
        add(spec.getOptionGroup("gameOptions.colony"));
        /* Victory Conditions */
        add(spec.getOptionGroup("gameOptions.victoryConditions"));
        /* Difficulty settings */
        //add(spec.getOptionGroup("gameOptions.difficultySettings"));

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
