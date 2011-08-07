/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

package net.sf.freecol.server.ai;

import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;


/**
 * An <code>AIObject</code> contains AI-related information and methods.
 * Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
 * player, can have a single <code>AIObject</code> attached to it.
 */
public abstract class AIObject extends FreeColObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColObject.class.getName());

    private final AIMain aiMain;
    protected boolean uninitialized = false;


    /**
     * Creates a new <code>AIObject</code>.
     * @param aiMain The main AI-object.
     */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
    }

    /**
     * Creates a new <code>AIObject</code> and registers
     * this object with <code>AIMain</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The unique identifier.
     * @see AIMain#addAIObject(String, AIObject)
     */
    public AIObject(AIMain aiMain, String id) {
        this.aiMain = aiMain;
        setId(id);
        aiMain.addAIObject(id, this);
    }


    /**
     * Returns the main AI-object.
     * @return The <code>AIMain</code>.
     */
    public AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Checks if this <code>AIObject</code>
     * is uninitialized. That is: it has been referenced
     * by another object, but has not yet been updated with
     * {@link #readFromXML}.
     *
     * @return <code>true</code> if this object is not initialized.
     */
    public boolean isUninitialized() {
        return uninitialized;
    }

    /**
     * Gets the random number generator to use in the AI.
     *
     * @return The AI random number generator.
     */
    protected Random getAIRandom() {
        return aiMain.getAIRandom();
    }


    /**
     * Disposes this <code>AIObject</code> by removing
     * any referances to this object.
     */
    public void dispose() {
        getAIMain().removeAIObject(getId());
    }


    /**
    * Returns the game.
    * @return The <code>Game</code>.
    */
    public Game getGame() {
        return aiMain.getGame();
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXML(XMLStreamReader in)
        throws XMLStreamException {
        super.readFromXML(in);
        uninitialized = false;
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "AIObject".
     */
    public static String getXMLElementTagName() {
        return "AIObject";
    }
}
