/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

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
            setDefaultId(game);
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
     * - {@link #readFromXML(XMLStreamReader)}
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
     * Sets the identifier from the real type and the next identifier
     * in the server.  Split out only to help out a backward
     * compatibility reader.
     *
     * @param game The <code>Game</code> this object is in.
     */
    protected void setDefaultId(Game game) {
        setId(getXMLTagName() + ":" + game.getNextId());
    }

    // @compat 0.9.x
    /**
     * Gets the identifiers integer part.  The age of two
     * FreeColGameObjects can be compared by comparing their integer
     * identifiers.
     *
     * @return The integer identifier.
     */
    public Integer getIntegerId() {
        String stringPart = getXMLTagName() + ":";
        return new Integer(getId().substring(stringPart.length()));
    }
    // end @compat

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
     * Find a <code>FreeColGameObject</code> of a given class
     * from a stream attribute.
     *
     * Use this routine when the object is optionally already be
     * present in the game.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param returnClass The class to expect.
     * @param defaultValue A default value to return if not found.
     * @param required If true a null result should throw an exception.
     * @return The <code>FreeColGameObject</code> found, or the default
     *     value if not found.
     * @exception XMLStreamException if the attribute is missing.
     */
    public <T extends FreeColGameObject> T findFreeColGameObject(XMLStreamReader in,
        String attributeName, Class<T> returnClass, T defaultValue,
        boolean required) throws XMLStreamException {
        T ret = getAttribute(in, attributeName, getGame(),
                             returnClass, (T)null);
        if (ret == (T)null) {
            if (required) {
                throw new XMLStreamException(getXMLTagName()
                    + " missing " + attributeName + ": " + currentTag(in));
            } else {
                ret = defaultValue;
            }
        }
        return ret;
    }

    /**
     * Either get an existing <code>FreeColGameObject</code> from a stream
     * attribute or create it if it does not exist.
     *
     * Use this routine when the object may not necessarily already be
     * present in the game, but is expected to be defined eventually.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The required attribute name.
     * @param returnClass The class of object.
     * @param required If true a null result should throw an exception.
     * @return The <code>FreeColGameObject</code> found or made, or null
     *     if the attribute was not present.
     * @exception XMLStreamError if there was a problem reading the stream.
     */
    public <T extends FreeColGameObject> T makeFreeColGameObject(XMLStreamReader in,
        String attributeName, Class<T> returnClass,
        boolean required) throws XMLStreamException {
        final String id =
            // @compat 0.10.7
            (ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId(in) :
            // end @compat
            in.getAttributeValue(null, attributeName);
        T ret = null;

        if (id == null) {
            if (required) {
                throw new XMLStreamException(getXMLTagName()
                    + " missing " + attributeName + ": " + currentTag(in));
            }
        } else {
            ret = getGame().getFreeColGameObject(id, returnClass);
            if (ret == null) {
                try {
                    Constructor<T> c = returnClass.getConstructor(Game.class,
                                                                  String.class);
                    ret = returnClass.cast(c.newInstance(game, id));
                    if (required && ret == null) {
                        throw new XMLStreamException(getXMLTagName()
                            + " constructed null " + returnClass.getName()
                            + " for " + id + ": " + currentTag(in));
                    }
                } catch (Exception e) {
                    if (required) {
                        throw new XMLStreamException(e.getCause());
                    } else {
                        logger.log(Level.WARNING, "Failed to create FCGO: "
                            + id, e);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Reads a <code>FreeColGameObject</code> from a stream.
     * Expects the object to be identified by the standard ID_ATTRIBUTE_TAG.
     *
     * Use this routine when the object may or may not have been
     * referenced and created-by-id in this game, but this is the
     * point where it is authoritatively defined.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param returnClass The class to expect.
     * @return The <code>FreeColGameObject</code> found, or null there
     *     was no ID_ATTRIBUTE_TAG present.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public <T extends FreeColGameObject> T readFreeColGameObject(XMLStreamReader in,
        Class<T> returnClass) throws XMLStreamException {
        T ret = makeFreeColGameObject(in, ID_ATTRIBUTE_TAG, returnClass, false);
        if (ret != null) ret.readFromXML(in);
        return ret;
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
            String xml = this.serialize(owner, true, true);

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
    // FreeColGameObjects use the 4-arg toXML


    /**
     * {@inheritDoc}
     */
    @Override
    public final void toXML(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, null, false, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void toXML(XMLStreamWriter out, Player player,
                            boolean showAll,
                            boolean toSavedGame) throws XMLStreamException {
        if (!showAll && toSavedGame) {
            throw new IllegalArgumentException("'showAll' should be true when saving a game.");
        }
        toXML(out, getXMLTagName(), player, showAll, toSavedGame);
    }

    /**
     * This method writes an XML-representation of this object with
     * a specified tag to the given stream.
     *
     * Almost all FreeColGameObjects end up calling this, and implementing
     * their own write{Attributes,Children} methods which begin by
     * calling their superclass.  This allows a clean nesting of the
     * serialization routines throughout the class hierarchy.
     *
     * Attribute and child visibility are controlled by the player, showAll,
     * and toSavedGame arguments.
     *
     * @param out The target stream.
     * @param tag The tag to use.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or null if <code>showAll == true</code>.
     * @param showAll Show all attributes.
     * @param toSavedGame Also show some extra attributes when saving the game.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected final void toXML(XMLStreamWriter out, String tag, Player player,
                               boolean showAll,
                               boolean toSavedGame) throws XMLStreamException {
        out.writeStartElement(tag);

        writeAttributes(out, player, showAll, toSavedGame);

        writeChildren(out, player, showAll, toSavedGame);

        out.writeEndElement();
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * To be overridden if required by any object that has attributes
     * and uses the toXML(XMLStreamWriter, String, Player, boolean,
     * boolean) call.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or null if <code>showAll == true</code>.
     * @param showAll Show all attributes.
     * @param toSavedGame Also show some extra attributes when saving the game.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeAttributes(XMLStreamWriter out, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
        super.writeAttributes(out);
    }

    /**
     * Write the children of this object to a stream.
     *
     * To be overridden if required by any object that has children
     * and uses the toXML(XMLStreamWriter, String) call.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or null if <code>showAll == true</code>.
     * @param showAll Show all attributes.
     * @param toSavedGame Also show some extra attributes when saving the game.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    protected void writeChildren(XMLStreamWriter out, Player player,
                                 boolean showAll,
                                 boolean toSavedGame) throws XMLStreamException {
        super.writeChildren(out);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input <code>XMLStreamReader</code>.
     * @exception XMLStreamException if there problems reading the stream.
     */
    @Override
    public final void readFromXML(XMLStreamReader in) throws XMLStreamException {
        uninitialized = false;
        super.readFromXML(in);
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
