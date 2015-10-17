/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;

import org.w3c.dom.Element;


/**
 * An <code>AIObject</code> contains AI-related information and methods.
 * Each <code>FreeColGameObject</code>, that is owned by an AI-controlled
 * player, can have a single <code>AIObject</code> attached to it.
 */
public abstract class AIObject extends FreeColObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(FreeColObject.class.getName());

    /** The AI this object exists within. */
    private final AIMain aiMain;

    /** Whether the object is uninitialized. */
    protected boolean uninitialized = false;


    /**
     * Creates a new uninitialized <code>AIObject</code>.
     *
     * @param aiMain The main AI-object.
     */
    public AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
        uninitialized = true;
    }

    /**
     * Creates a new uninitialized <code>AIObject</code> with a registerable
     * AI identifier.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     * @see AIMain#addAIObject(String, AIObject)
     */
    public AIObject(AIMain aiMain, String id) {
        this(aiMain);

        if (id != null) {
            setId(id);
            aiMain.addAIObject(id, this);
        }
        uninitialized = true;
    }

    /**
     * Creates a new <code>AIObject</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public AIObject(AIMain aiMain, Element element) {
        this(aiMain);

        readFromXMLElement(element);
        addAIObjectWithId();
    }

    /**
     * Creates a new <code>AIObject</code>.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     * @see AIObject#readFromXML
     */
    public AIObject(AIMain aiMain,
                    FreeColXMLReader xr) throws XMLStreamException {
        this(aiMain);

        readFromXML(xr);
        addAIObjectWithId();
    }


    /**
     * Convenience accessor for the main AI-object.
     *
     * @return The <code>AIMain</code>.
     */
    public final AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Convenience accessor for the game.
     *
     * @return The <code>Game</code>.
     */
    public final Game getGame() {
        return aiMain.getGame();
    }

    /**
     * Checks if this <code>AIObject</code>
     * is uninitialized. That is: it has been referenced
     * by another object, but has not yet been updated with
     * {@link #readFromXML}.
     *
     * @return <code>true</code> if this object is not initialized.
     */
    public final boolean isUninitialized() {
        return uninitialized;
    }

    /**
     * Disposes this <code>AIObject</code> by removing the reference
     * to this object from the enclosing AIMain.
     */
    public void dispose() {
        getAIMain().removeAIObject(getId());
    }

    /**
     * Has this AIObject been disposed?
     *
     * @return True if this AIObject was disposed.
     */
    public final boolean isDisposed() {
        return getAIMain().getAIObject(getId()) == null;
    }

    /**
     * Adds this object to the AI main if it has a non-null identifier.
     */
    protected final void addAIObjectWithId() {
        if (getId() != null) aiMain.addAIObject(getId(), this);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public final Specification getSpecification() {
        return getGame().getSpecification();
    }


    // Other low level

    /**
     * Checks the integrity of this AI object.
     * Subclasses should extend.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    public int checkIntegrity(boolean fix) {
        return (isUninitialized()) ? -1 : 1;
    }
}
