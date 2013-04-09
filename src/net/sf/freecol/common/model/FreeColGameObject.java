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

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;


/**
 * The superclass of all game objects in FreeCol.
 *
 * All FreeColGameObjects need to be able to refer to the game they belong
 * to.  Therefore, the game attribute must not be null.
 */
abstract public class FreeColGameObject extends FreeColObject {

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());

    /** The game this object belongs to. */
    private Game game;

    /** Has this object been disposed. */
    private boolean disposed = false;

    /** Has this object been initialized. */
    private boolean uninitialized;


    /**
     * Empty constructor for subclasses.
     */
    protected FreeColGameObject() {
        logger.info("FreeColGameObject with null id.");

        uninitialized = false;
    }

    /**
     * Creates a new <code>FreeColGameObject</code>.  Automatically
     * assign an id and register this object at the specified
     * <code>Game</code>, unless this object is a <code>Game</code>
     * in which case it is given the zero id.
     *
     * @param game The <code>Game</code> in which this object belongs.
     */
    public FreeColGameObject(Game game) {
        this.game = game;

        if (game != null && game instanceof Game) {
            setDefaultId(game);
        } else if (this instanceof Game) {
            setId("0");
        } else {
            logger.warning("FreeColGameObject with null game: " + getId());
        }

        uninitialized = getId() == null;
    }

    /**
     * Initiates a new <code>FreeColGameObject</code>.
     * If an id is supplied, use that, otherwise leave the id undefined.
     *
     * This routine should be used when we intend later to call one of:
     *
     * - {@link #readFromXML(XMLStreamReader)}
     * - {@link #readFromXMLElement(Element)}
     *
     * @param game The <code>Game</code> in which this object belongs.
     * @param id An optional identifier for this object.
     */
    public FreeColGameObject(Game game, String id) {
        this.game = game;
        if (game == null && !(this instanceof Game)) {
            logger.warning("FreeColGameObject with null game: " + this
                           + "/" + id);
        }

        if (id != null) setId(id);
        uninitialized = true;
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
     * Sets the id from the real type and the next id in the server.
     * Split out only to help out a backward compatibility reader.
     *
     * @param game The <code>Game</code> this object is in.
     */
    protected void setDefaultId(Game game) {
        setId(getRealXMLElementTagName() + ":" + game.getNextId());
    }

    /**
     * Get the actual tag name for this object.
     *
     * @return The real tag name.
     */
    private String getRealXMLElementTagName() {
        String tagName = "";
        try {
            Method m = getClass().getMethod("getXMLElementTagName",
                                            (Class[]) null);
            tagName = (String) m.invoke((Object) null, (Object[]) null);
        } catch (Exception e) {}
        return tagName;
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
        String stringPart = getRealXMLElementTagName() + ":";
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
     * Override this in subclasses.
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
     * Gets a <code>FreeColGameObject</code> of a given class.
     * If the object does not exist, create it.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param returnClass The class to expect.
     * @return The <code>FreeColGameObject</code> found, or null if not found.
     */
    public <T extends FreeColGameObject> T getFreeColGameObject(XMLStreamReader in,
        String attributeName, Class<T> returnClass) {
        final String id =
        // @compat 0.10.7
            (ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId(in) :
        // end @compat
            in.getAttributeValue(null, attributeName);
        if (id == null) return null;

        Game game = getGame();
        T ret = game.getFreeColGameObject(id, returnClass);
        try {
            if (ret == null) {
                Constructor<T> c = returnClass.getConstructor(Game.class,
                                                              String.class);
                ret = returnClass.cast(c.newInstance(game, id));
            }
            return ret;

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create FCGO with id: " + id,
                       e);
        }
        return null;
    }

    /**
     * Gets a <code>FreeColGameObject</code> of a given class.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param returnClass The class to expect.
     * @param defaultValue A default value to return if not found.
     * @return The <code>FreeColGameObject</code> found, or the default
     *     value if not found.
     */
    public <T extends FreeColGameObject> T getFreeColGameObject(XMLStreamReader in,
        String attributeName, Class<T> returnClass, T defaultValue) {
        final String id =
        // @compat 0.10.7
            (ID_ATTRIBUTE_TAG.equals(attributeName)) ? readId(in) :
        // end @compat
            in.getAttributeValue(null, attributeName);
        return (id == null) ? defaultValue
            : getGame().getFreeColGameObject(id, returnClass);
    }

    /**
     * Updates an existing <code>FreeColGameObject</code> from a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param returnClass The class to expect.
     * @return The <code>FreeColGameObject</code> found, or null if not found.
     */
    public <T extends FreeColGameObject> T updateFreeColGameObject(XMLStreamReader in,
        Class<T> returnClass) {
        T ret = getFreeColGameObject(in, ID_ATTRIBUTE_TAG, returnClass);
        if (ret != null) {
            try {
                ret.readFromXML(in);
            } catch (XMLStreamException xse) {
                logger.log(Level.WARNING, "Failed to update " + ret.getId()
                    + " from stream.", xse);
                return null;
            }
        }
        return ret;
    }

    /**
     * "Clone" this FreeColGameObject by serializing it and creating a
     * new object from the resulting XML.  We need to pass the result
     * class, since the object we are about to "clone" is likely a
     * server object.
     *
     * @param resultClass The class to clone.
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
     * Gets a <code>FreeColGameObject</code> value or the default value.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param attributeName The attribute name.
     * @param returnType The <code>FreeColObject</code> type to expect.
     * @param defaultValue The default value.
     * @return The <code>FreeColGameObject</code> found, or the
     *     default value if not.
     */
    public <T extends FreeColGameObject> T getAttribute(XMLStreamReader in,
        String attributeName, Class<T> returnType, T defaultValue) {
        return getAttribute(in, attributeName, getGame(),
                            returnType, defaultValue);
    }

    /**
     * FreeColGameObjects are equal if the two fcgos are in the same
     * game and have the same id. 
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
     * Sets the unique id of this object.  When setting a new id to
     * this object, it it automatically registered at the
     * corresponding <code>Game</code> with the new id.
     *
     * @param newId The unique id of this object.
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

    private static final String NEW_WORLD_TAG = "newWorld";
    // Several classes use this for lists of units.
    public static final String UNITS_TAG = "units";


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The output <code>XMLStreamWriter</code>.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or null if <code>showAll == true</code>.
     * @param showAll Show all attributes.
     * @param toSavedGame Also show some extra attributes when saving the game.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    @Override
    public final void toXML(XMLStreamWriter out, Player player,
                            boolean showAll,
                            boolean toSavedGame) throws XMLStreamException {
        if (!showAll && toSavedGame) {
            throw new IllegalArgumentException("'showAll' should be true when saving a game.");
        }
        toXMLImpl(out, player, showAll, toSavedGame);
    }

    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @exception XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out, null, false, false);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * Implement this in the subclasses.
     *
     * @param out The output <code>XMLStreamWriter</code>.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or null if <code>showAll == true</code>.
     * @param showAll Show all attributes.
     * @param toSavedGame Also show some extra attributes when saving the game.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    abstract protected void toXMLImpl(XMLStreamWriter out, Player player,
                                      boolean showAll, boolean toSavedGame)
        throws XMLStreamException;

    /**
     * Common routine for FreeColGameObject descendants to write a
     * partial XML-representation of this object to the given stream,
     * including only the mandatory and specified fields.
     *
     * All attributes are considered visible as this is
     * server-to-owner-client functionality, but it depends ultimately
     * on the presence of a getFieldName() method that returns a type
     * compatible with String.valueOf.
     *
     * @param out The output <code>XMLStreamWriter</code>.
     * @param theClass The real class of this object, required by the
     *     <code>Introspector</code>.
     * @param fields The fields to write.
     * @exception XMLStreamException if there are problems writing the stream.
     */
    protected void toXMLPartialByClass(XMLStreamWriter out, Class<?> theClass, 
                                       String[] fields) throws XMLStreamException {
        try {
            Introspector tag = new Introspector(theClass, "XMLElementTagName");

            out.writeStartElement(tag.getter(this));

            writeAttribute(out, ID_ATTRIBUTE_TAG, getId());

            writeAttribute(out, PARTIAL_ATTRIBUTE, String.valueOf(true));

            for (int i = 0; i < fields.length; i++) {
                Introspector intro = new Introspector(theClass, fields[i]);
                writeAttribute(out, fields[i], intro.getter(this));
            }

            out.writeEndElement();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Partial write failed for "
                + theClass.getName(), e);
        }
    }

    /**
     * Common routine for FreeColGameObject descendants to update an
     * object from a partial XML-representation which includes only
     * mandatory and server-supplied fields.
     *
     * All attributes are considered visible as this is
     * server-to-owner-client functionality.  It depends ultimately on
     * the presence of a setFieldName() method that takes a parameter
     * type T where T.valueOf(String) exists.
     *
     * @param in The input <code>XMLStreamReader</code>.
     * @param theClass The real class of this object, required by the
     *     <code>Introspector</code>.
     * @exception XMLStreamException If there are problems reading the stream.
     */
    public void readFromXMLPartialByClass(XMLStreamReader in,
                                          Class<?> theClass) throws XMLStreamException {
        int n = in.getAttributeCount();

        setId(readId(in));

        for (int i = 0; i < n; i++) {
            String name = in.getAttributeLocalName(i);

            if (name.equals(ID_ATTRIBUTE_TAG)
                || name.equals(ID_ATTRIBUTE)
                || name.equals(PARTIAL_ATTRIBUTE)) continue;

            try {
                Introspector intro = new Introspector(theClass, name);
                intro.setter(this, in.getAttributeValue(i));

            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not set field " + name, e);
            }
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT);
    }

    /**
     * Get a new location from a given id.
     *
     * @param in The input <code>XMLStreamReader</code>.
     * @param attrib The attribute to check.
     * @return The <code>Location</code> found.
     */
    protected Location getLocationAttribute(XMLStreamReader in, String attrib) {
        if (attrib == null) return null;

        String id = getAttribute(in, attrib, (String)null);
        if (id == null) return null;

        FreeColGameObject fcgo = game.getFreeColGameObject(id);
        if (fcgo instanceof Location) return (Location)fcgo;

        final String tag = id.substring(0, id.indexOf(':'));
        if (NEW_WORLD_TAG.equals(tag)) {
            // do nothing

        } else if (Building.getXMLElementTagName().equals(tag)) {
            return new Building(game, id);

        } else if (Colony.getXMLElementTagName().equals(tag)) {
            return new Colony(game, id);

        } else if (ColonyTile.getXMLElementTagName().equals(tag)) {
            return new ColonyTile(game, id);

        } else if (Europe.getXMLElementTagName().equals(tag)) {
            return new Europe(game, id);

        } else if (HighSeas.getXMLElementTagName().equals(tag)) {
            return new HighSeas(game, id);

        } else if (IndianSettlement.getXMLElementTagName().equals(tag)) {
            return new IndianSettlement(game, id);

        } else if (Map.getXMLElementTagName().equals(tag)) {
            return new Map(game, id);

        } else if (Tile.getXMLElementTagName().equals(tag)) {
            return new Tile(game, id);

        } else if (Unit.getXMLElementTagName().equals(tag)) {
            return new Unit(game, id);

        } else {
            throw new IllegalStateException("Unknown type of Location: " + id);
        }
        return null;
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
