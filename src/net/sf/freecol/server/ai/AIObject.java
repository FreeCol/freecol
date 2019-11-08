/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.LogBuilder;


/**
 * An {@code AIObject} contains AI-related information and methods.
 * Each {@code FreeColGameObject}, that is owned by an AI-controlled
 * player, can have a single {@code AIObject} attached to it.
 */
public abstract class AIObject extends FreeColObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(AIObject.class.getName());

    /** The AI this object exists within. */
    private final AIMain aiMain;

    /** Whether the object is initialized. */
    protected boolean initialized;


    /**
     * Creates a new uninitialized {@code AIObject}.
     *
     * @param aiMain The main AI-object.
     */
    protected AIObject(AIMain aiMain) {
        this.aiMain = aiMain;
        this.initialized = false;
    }

    /**
     * Creates a new uninitialized {@code AIObject} with a registerable
     * AI identifier.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     * @see AIMain#addAIObject(String, AIObject)
     */
    protected AIObject(AIMain aiMain, String id) {
        this(aiMain);

        if (id != null) {
            setId(id);
            aiMain.addAIObject(id, this);
        }
    }

    /**
     * Creates a new {@code AIObject}.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     * @see AIObject#readFromXML
     */
    protected AIObject(AIMain aiMain, FreeColXMLReader xr)
        throws XMLStreamException {
        this(aiMain);

        readFromXML(xr);
        addAIObjectWithId();
    }


    /**
     * Is this AI object initialized?
     *
     * That is: it has been fully initialized by a detailed
     * constructor, or built with a simple constructor due to being
     * referenced by another object, but then updated with
     * {@link #readFromXML}.
     *
     * @return True if this {@code AIObject} is initialized.
     */
    private final boolean isInitialized() {
        return this.initialized;
    }

    /**
     * Set the initialized flag in this object.
     *
     * To be implemented by leaf classes, and called in their constructors
     * plus the special case in readChild below where we resolve forward
     * references.
     */
    public abstract void setInitialized();
    
    /**
     * Convenience accessor for the main AI-object.
     *
     * @return The {@code AIMain}.
     */
    public final AIMain getAIMain() {
        return this.aiMain;
    }

    /**
     * Disposes this {@code AIObject} by removing the reference
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


    // Other low level

    /**
     * AIObjects need integrity checking too.
     *
     * @param fix If true, fix problems if possible.
     * @param lb A {@code LogBuilder} to log to.
     * @return -1 if there are problems remaining, zero if problems
     *     were fixed, +1 if no problems found at all.
     */
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = IntegrityType.INTEGRITY_GOOD;
        if (!isInitialized()) {
            lb.add("\n  Uninitialized AI Object: ", getId());
            result = result.fail();
        }
        return result;
    }

    /**
     * Just do the check, short cut the logging.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    public IntegrityType checkIntegrity(boolean fix) {
        return checkIntegrity(fix, new LogBuilder(-1));
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public final Specification getSpecification() {
        return getAIMain().getSpecification();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setSpecification(Specification specification) {
        throw new RuntimeException("Can not set specification: " + this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final Game getGame() {
        return getAIMain().getGame();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setGame(Game game) {
        throw new RuntimeException("Can not set game: " + this);
    }

    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        AIObject o = copyInCast(other, AIObject.class);
        if (o == null || !super.copyIn(o)) return false;
        //this.aiMain is fixed
        this.initialized = o.isInitialized();
        return true;
    }
}
