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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.Introspector;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.server.model.ServerGame;

import org.w3c.dom.Element;


/**
 * The superclass of all game objects in FreeCol.
 */
abstract public class FreeColGameObject extends FreeColObject {

    private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());

    public static final String UNITS_TAG_NAME = "units";

    private Game game;
    private boolean disposed = false;
    private boolean uninitialized;

    protected FreeColGameObject() {
        logger.info("FreeColGameObject without ID created.");
        uninitialized = false;
    }


    /**
     * Creates a new <code>FreeColGameObject</code> with an automatically assigned
     * ID and registers this object at the specified <code>Game</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     */
    public FreeColGameObject(Game game) {
        this.game = game;

        if (game != null && game instanceof Game) {
            setDefaultId(game);
        } else if (this instanceof Game) {
            setId("0");
        } else {
            logger.warning("Created 'FreeColGameObject' with 'game == null':"
                + this.getId());
        }

        uninitialized = false;
    }


    /**
     * Initiates a new <code>FreeColGameObject</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public FreeColGameObject(Game game, XMLStreamReader in) throws XMLStreamException {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

        uninitialized = false;
    }

    /**
     * Initiates a new <code>FreeColGameObject</code> from an <code>Element</code>.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public FreeColGameObject(Game game, Element e) {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

        uninitialized = false;
    }

    /**
     * Initiates a new <code>FreeColGameObject</code>
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public FreeColGameObject(Game game, String id) {
        this.game = game;

        if (game == null && !(this instanceof Game)) {
            logger.warning("Created 'FreeColGameObject' with 'game == null': " + this);
        }

        setId(id);

        uninitialized = true;
    }

    /**
     * Sets the Id from the real type and the next Id in the server.
     * Split out only to help out a backward compatibility reader.
     *
     * @param game The <code>Game</code> this object is in.
     */
    protected void setDefaultId(Game game) {
        setId(getRealXMLElementTagName() + ":"
              + ((ServerGame)game).getNextID());
    }

    /**
     * Gets the game object this <code>FreeColGameObject</code> belongs to.
     * @return The <code>game</code>.
     */
    public Game getGame() {
        return game;
    }


    /**
     * Describe <code>getSpecification</code> method here.
     *
     * @return a <code>Specification</code> value
     */
    @Override
    public Specification getSpecification() {
        if (game == null) {
            return null;
        } else {
            return game.getSpecification();
        }
    }

    /**
     * Sets the game object this <code>FreeColGameObject</code> belongs to.
     * @param game The <code>game</code>.
     */
    public void setGame(Game game) {
        this.game = game;
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
     * Checks if this object has been disposed.
     * @return <code>true</code> if this object has been disposed.
     * @see #dispose
     */
    public boolean isDisposed() {
        return disposed;
    }

    /**
     * Checks if this <code>FreeColGameObject</code>
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
     * Gets the ID's integer part of this object. The age of two
     * FreeColGameObjects can be compared by comparing their integer
     * IDs.
     *
     * @return The unique ID of this object.
     */
    // TODO: remove compatibility code: use established instead
    public Integer getIntegerID() {
        String stringPart = getRealXMLElementTagName() + ":";
        return new Integer(getId().substring(stringPart.length()));
    }

    /**
     * Sets the unique ID of this object. When setting a new ID to this object,
     * it it automatically registered at the corresponding <code>Game</code>
     * with the new ID.
     *
     * @param newID the unique ID of this object,
     */
    @Override
    public final void setId(String newID) {
        if (game != null && !(this instanceof Game)) {
            if (!newID.equals(getId())) {
                if (getId() != null) {
                    game.removeFreeColGameObject(getId());
                }

                super.setId(newID);
                game.setFreeColGameObject(newID, this);
            }
        } else {
            super.setId(newID);
        }
    }

    /**
     * Checks if the given <code>FreeColGameObject</code> equals this object.
     *
     * @param o The <code>FreeColGameObject</code> to compare against this object.
     * @return <i>true</i> if the two <code>FreeColGameObject</code> are equal and <i>false</i> otherwise.
     */
    public boolean equals(FreeColGameObject o) {
        if (o != null) {
            return Utils.equals(this.getGame(), o.getGame()) && getId().equals(o.getId());
        } else {
            return false;
        }
    }

    /**
     * Checks if the given <code>FreeColGameObject</code> equals this object.
     *
     * @param o The <code>FreeColGameObject</code> to compare against this object.
     * @return <i>true</i> if the two <code>FreeColGameObject</code> are equal and <i>false</i> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return (o instanceof FreeColGameObject) ? equals((FreeColGameObject) o) : false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


    public <T extends FreeColGameObject> T getFreeColGameObject(XMLStreamReader in, String attributeName,
                                                                Class<T> returnClass) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString == null) {
            return null;
        } else {
            T returnValue = returnClass.cast(getGame().getFreeColGameObject(attributeString));
            try {
                if (returnValue == null) {
                    Constructor<T> c = returnClass.getConstructor(Game.class, String.class);
                    returnValue = returnClass.cast(c.newInstance(getGame(), attributeString));
                }
                return returnValue;
            } catch(Exception e) {
                logger.warning("Failed to create FreeColGameObject with ID " + attributeString);
                return null;
            }
        }
    }

    public <T extends FreeColGameObject> T getFreeColGameObject(XMLStreamReader in, String attributeName,
                                                                Class<T> returnClass, T defaultValue) {
        final String attributeString = in.getAttributeValue(null, attributeName);
        if (attributeString != null) {
            return returnClass.cast(getGame().getFreeColGameObject(attributeString));
        } else {
            return defaultValue;
        }
    }

    public <T extends FreeColGameObject> T updateFreeColGameObject(XMLStreamReader in, Class<T> returnClass) {
        String idString = in.getAttributeValue(null, ID_ATTRIBUTE);
        if (idString == null) {
            idString = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        }
        if (idString == null) {
            return null;
        }
        FreeColGameObject fcgo = getGame().getFreeColGameObject(idString);
        T returnValue = (fcgo == null) ? null : returnClass.cast(fcgo);
        try {
            if (returnValue == null) {
                Constructor<T> c = returnClass.getConstructor(Game.class, XMLStreamReader.class);
                returnValue = returnClass.cast(c.newInstance(getGame(), in));
            } else {
                returnValue.readFromXML(in);
            }
            return returnValue;
        } catch (Exception e) {
            logger.warning("Failed to update FreeColGameObject with ID "
                           + idString);
            e.printStackTrace();
            return null;
        }
    }

    protected Location newLocation(String locationString) {
        Location destination = null;
        if (locationString != null) {
            FreeColGameObject fcgo = game.getFreeColGameObject(locationString);
            if (fcgo instanceof Location) {
                destination = (Location)fcgo;
            } else {
                String XMLElementTag = locationString.substring(0, locationString.indexOf(':'));
                if (XMLElementTag.equals(Tile.getXMLElementTagName())) {
                    return new Tile(game, locationString);
                } else if (XMLElementTag.equals(ColonyTile.getXMLElementTagName())) {
                    return new ColonyTile(game, locationString);
                } else if (XMLElementTag.equals(Colony.getXMLElementTagName())) {
                    return new Colony(game, locationString);
                } else if (XMLElementTag.equals(IndianSettlement.getXMLElementTagName())) {
                    return new IndianSettlement(game, locationString);
                } else if (XMLElementTag.equals(Europe.getXMLElementTagName())) {
                    return new Europe(game, locationString);
                } else if (XMLElementTag.equals(Building.getXMLElementTagName())) {
                    return new Building(game, locationString);
                } else if (XMLElementTag.equals(Unit.getXMLElementTagName())) {
                    return new Unit(game, locationString);
                } else if (XMLElementTag.equals(HighSeas.getXMLElementTagName())) {
                    return new HighSeas(game, locationString);
                } else if (XMLElementTag.equals(Map.getXMLElementTagName())) {
                    return new Map(game, locationString);
                } else if (XMLElementTag.equals("newWorld")) {
                    // do nothing
                } else {
                    logger.warning("Unknown type of Location: " + locationString);
                    return new Tile(game, locationString);
                }
            }
        }
        return destination;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    @Override
    public final void toXML(XMLStreamWriter out, Player player,
                            boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        if (toSavedGame && !showAll) {
            throw new IllegalArgumentException("'showAll' should be true when saving a game.");
        }
        toXMLImpl(out, player, showAll, toSavedGame);
    }

    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXMLImpl(out, null, false, false);
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * <br><br>
     *
     * Only attributes visible to the given <code>Player</code> will
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    abstract protected void toXMLImpl(XMLStreamWriter out, Player player,
                                      boolean showAll, boolean toSavedGame)
        throws XMLStreamException;

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    @Override
    public final void readFromXML(XMLStreamReader in) throws XMLStreamException {
        uninitialized = false;
        super.readFromXML(in);
    }

    private String getRealXMLElementTagName() {
        String tagName = "";
        try {
            Method m = getClass().getMethod("getXMLElementTagName", (Class[]) null);
            tagName = (String) m.invoke((Object) null, (Object[]) null);
        } catch (Exception e) {}
        return tagName;
    }
    // end TODO

    /**
     * Common routine for FreeColGameObject descendants to write an
     * XML-representation of this object to the given stream,
     * including only the mandatory and specified fields.
     * All attributes are considered visible as this is
     * server-to-owner-client functionality, but it depends ultimately
     * on the presence of a getFieldName() method that returns a type
     * compatible with String.valueOf.
     *
     * @param out The target stream.
     * @param theClass The real class of this object, required by the
     *                 <code>Introspector</code>.
     * @param fields The fields to write.
     * @throws XMLStreamException if there are problems writing the stream.
     */
    protected void toXMLPartialByClass(XMLStreamWriter out,
                                       Class<?> theClass, String[] fields)
        throws XMLStreamException {
        // Start element
        try {
            Introspector tag = new Introspector(theClass, "XMLElementTagName");
            out.writeStartElement(tag.getter(this));
        } catch (IllegalArgumentException e) {
            logger.warning("Could not get tag field: " + e.toString());
        }

        // Partial element
        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute(PARTIAL_ATTRIBUTE, String.valueOf(true));

        // All the fields
        for (int i = 0; i < fields.length; i++) {
            try {
                Introspector intro = new Introspector(theClass, fields[i]);
                out.writeAttribute(fields[i], intro.getter(this));
            } catch (IllegalArgumentException e) {
                logger.warning("Could not get field " + fields[i]
                               + ": " + e.toString());
            }
        }

        out.writeEndElement();
    }

    /**
     * Common routine for FreeColGameObject descendants to update an
     * object from a partial XML-representation which includes only
     * mandatory and server-supplied fields.
     * All attributes are considered visible as this is
     * server-to-owner-client functionality.  It depends ultimately on
     * the presence of a setFieldName() method that takes a parameter
     * type T where T.valueOf(String) exists.
     *
     * @param in The input stream with the XML.
     * @param theClass The real class of this object, required by the
     *                 <code>Introspector</code>.
     * @throws XMLStreamException If there are problems reading the stream.
     */
    protected void readFromXMLPartialByClass(XMLStreamReader in,
                                             Class<?> theClass)
        throws XMLStreamException {
        int n = in.getAttributeCount();

        setId(in.getAttributeValue(null, ID_ATTRIBUTE));

        for (int i = 0; i < n; i++) {
            String name = in.getAttributeLocalName(i);

            if (name.equals(ID_ATTRIBUTE)
                || name.equals(PARTIAL_ATTRIBUTE)) continue;

            try {
                Introspector intro = new Introspector(theClass, name);
                intro.setter(this, in.getAttributeValue(i));
            } catch (IllegalArgumentException e) {
                logger.warning("Could not set field " + name
                               + ": " + e.toString());
            }
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT);
    }

    /**
     * Gets a string representation of the object.
     *
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        return getClass().getName() + ": "
            + getId() + " (super's hash code: "
            + Integer.toHexString(super.hashCode()) + ")";
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
