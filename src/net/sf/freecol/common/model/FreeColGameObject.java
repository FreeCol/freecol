/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;


/**
 * The superclass of all game objects in FreeCol.
 *
 * All FreeColGameObjects need to be able to refer to the game they belong
 * to.  Therefore, the game attribute must not be null.
 */
public abstract class FreeColGameObject extends FreeColObject {

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());

    /** The game this object belongs to. */
    private Game game;

    /** Has this object been disposed. */
    private boolean disposed = false;

    /** Has this object been initialized. */
    private boolean uninitialized;


    /**
     * Creates a new <code>FreeColGameObject</code>.  Automatically
     * assign an object identifier and register this object at the
     * specified <code>Game</code>, unless this object is a
     * <code>Game</code> in which case it is given an identifier of
     * zero.
     *
     * @param game The <code>Game</code> in which this object belongs.
     */
    public FreeColGameObject(Game game) {
        if (game != null) {
            this.game = game;
            setId(getXMLTagName() + ":" + game.getNextId());

        } else if (this instanceof Game) {
            this.game = (Game)this;
            setId("0");

        } else {
            throw new IllegalArgumentException("FCGO with null game.");
        }
        this.uninitialized = getId() == null;
    }

    /**
     * Creates a new <code>FreeColGameObject</code>.
     * If an identifier is supplied, use that, otherwise leave it undefined.
     *
     * This routine should be used when we intend later to call one of:
     * - {@link #readFromXML(FreeColXMLReader)}
     * - {@link #readFromXMLElement(Element)}
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param id The object identifier.
     */
    public FreeColGameObject(Game game, String id) {
        if (game == null) {
            throw new IllegalArgumentException("FCGO(id=" + id
                + ") with null game");
        }

        this.game = game;
        if (id != null) setId(id);
        this.uninitialized = true;
    }


    /**
     * Gets the game object this <code>FreeColGameObject</code> belongs to.
     *
     * @return The <code>Game</code> this object belongs to.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Sets the game object this <code>FreeColGameObject</code> belongs to.
     *
     * @param game The <code>Game</code> to set.
     */
    public void setGame(Game game) {
        this.game = game;
    }

    /**
     * Has this object been disposed?
     *
     * @return True if this object has been disposed.
     * @see #dispose
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Has this object not yet been initialized?
     *
     * @return True if this object is not initialized.
     */
    public boolean isUninitialized() {
        return uninitialized;
    }

    /**
     * Low level base dispose.
     */
    public void fundamentalDispose() {
        disposed = true;
        getGame().removeFreeColGameObject(getId());
    }

    /**
     * Removes all references to this object.
     *
     * To be overridden in subclasses, but reference this routine.
     *
     * @return A list of disposed objects.
     */
    public List<FreeColGameObject> disposeList() {
        fundamentalDispose();

        List<FreeColGameObject> objects = new ArrayList<FreeColGameObject>();
        objects.add(this);
        return objects;
    }

    /**
     * Removes all references to this object.
     */
    public void dispose() {
        disposeList();
    }

    /**
     * "Clone" this FreeColGameObject by serializing it and creating a
     * new object from the resulting XML.  We need to pass the result
     * class, since the object we are about to "clone" is likely a
     * server object.
     *
     * @param returnClass The class to clone.
     * @return The "clone" of the <code>FreeColGameObject</code>.
     */
    public <T extends FreeColGameObject> T cloneFreeColGameObject(Class<T> returnClass) {
        final Game game = getGame();
        final Player owner = (this instanceof Ownable)
            ? ((Ownable)this).getOwner()
            : null;
        try {
            String xml = this.serialize();

            Field nextId = Game.class.getDeclaredField("nextId");
            nextId.setAccessible(true);
            int id = nextId.getInt(game);
            nextId.setInt(game, id + 1);
            xml = xml.replace(getId(), T.getXMLElementTagName() + ":" + id);

            return unserialize(xml, game, returnClass);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to clone " + getId(), e);
        }
        return null;
    }

    /**
     * FreeColGameObjects are equal if the two fcgos are in the same
     * game and have the same identifier.
     *
     * @param o The <code>FreeColGameObject</code> to compare against
     *     this object.
     * @return True if the <code>FreeColGameObject</code> is equal to this one.
     */
    public boolean equals(FreeColGameObject o) {
        if (o == null) return false;
        return Utils.equals(this.getGame(), o.getGame())
            && getId().equals(o.getId());
    }


    // Override FreeColObject

    /**
     * Sets the unique identifier of this object.  When setting a new
     * identifier, the object is automatically registered at the
     * corresponding <code>Game</code> with that identifier.
     *
     * @param newId The unique identifier of this object.
     */
    @Override
    public final void setId(String newId) {
        if (game != null && !(this instanceof Game)) {
            if (!newId.equals(getId())) {
                if (getId() != null) {
                    game.removeFreeColGameObject(getId());
                }

                super.setId(newId);
                game.setFreeColGameObject(newId, this);
            }
        } else {
            super.setId(newId);
        }
    }

    /**
     * Get the specification for this game object.
     *
     * @return The <code>Specification</code> of this game.
     */
    @Override
    public Specification getSpecification() {
        return (game == null) ? null : game.getSpecification();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof FreeColGameObject) ? equals((FreeColGameObject)o)
            : false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return getId().hashCode();
    }


    // Serialization


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param xr The input <code>FreeColXMLReader</code>.
     * @exception XMLStreamException if there problems reading the stream.
     */
    @Override
    public final void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        uninitialized = false;
        super.readFromXML(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + ":" + getId()
            + " (super hashcode: " + Integer.toHexString(super.hashCode())
            + ")";
    }

    /**
     * Gets the tag name of the root element representing this object.
     * This method should be overwritten by any sub-class, preferably
     * with the name of the class with the first letter in lower case.
     *
     * @return "unknown".
     */
    public static String getXMLElementTagName() {
        return "unknown";
    }
}
