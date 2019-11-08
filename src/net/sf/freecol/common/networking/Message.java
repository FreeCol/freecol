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

package net.sf.freecol.common.networking;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Constructor;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Introspector;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;

import org.xml.sax.SAXException;


/**
 * Base abstract class for all messages.
 */
public abstract class Message {

    protected static final Logger logger = Logger.getLogger(Message.class.getName());

    /**
     * A map of message name to message constructors, built on the fly
     * as new messages are encountered and suitable constructors found.
     */
    private final static Map<String, Constructor<? extends Message>> builders
        = Collections.synchronizedMap(new HashMap<String,
            Constructor<? extends Message>>());

    /** Classes used by Message.read() */
    private static final Class[] readClasses = {
        Game.class, FreeColXMLReader.class };

    // Convenient way to specify the relative priorities of the messages
    // types in one place.
    public static enum MessagePriority {
        ATTRIBUTE(-1), // N/A
        ANIMATION(0),  // Do animations first
        REMOVE(100),   // Do removes last
        STANCE(5),     // Do stance before updates
        OWNED(20),     // Do owned changes after updates
        PARTIAL(9),    // There are a lot of partial updates
        UPDATE(10),    // There are a lot of updates
        // Symbolic priorities used by various non-fixed types
        EARLY(1),
        NORMAL(15),
        LATE(90),
        LAST(99);

        private final int level;

        MessagePriority(int level) {
            this.level = level;
        }

        public int getValue() {
            return this.level;
        }
    }

    /** Comparator comparing by message priority. */
    public static final Comparator<Message> messagePriorityComparator
        = Comparator.comparingInt(Message::getPriorityLevel);


    /**
     * Deliberately trivial constructor.
     */
    protected Message() {
        // empty constructor
    }


    /**
     * Get the message tag.
     *
     * @return The message tag.
     */
    abstract public String getType();
    
    /**
     * Set the message tag.
     *
     * @param type The new message tag.
     */
    abstract protected void setType(String type);
    
    /**
     * Checks if an attribute is present in this message.
     * 
     * @param key The attribute to look for.
     * @return True if the attribute is present.
     */
    abstract protected boolean hasAttribute(String key);

    /**
     * Get a string attribute value.
     *
     * @param key The attribute to look for.
     * @return The string value found, or null if the attribute was absent.
     */
    abstract protected String getStringAttribute(String key);

    /**
     * Sets an attribute in this message.
     * 
     * @param key The attribute to set.
     * @param value The new value of the attribute.
     */
    abstract protected void setStringAttribute(String key, String value);

    /**
     * Get a map of all the attributes in this message.
     *
     * @return A {@code Map} of the attributes.
     */
    abstract protected Map<String,String> getStringAttributeMap();

    /**
     * Get the number of child objects.
     *
     * @return The child count.
     */
    abstract protected int getChildCount();

    /**
     * Get the child objects of this message.
     *
     * @return A list of child {@code FreeColObject}s.
     */
    abstract protected List<FreeColObject> getChildren();
        
    /**
     * Set the list of objects attached to this message.
     *
     * @param fcos The new list of attached {@code FreeColObject}s.
     */
    abstract protected void setChildren(List<? extends FreeColObject> fcos);

    /**
     * Append a new child.
     *
     * @param <T> The child type.
     * @param fco The new child object.
     */
    abstract protected <T extends FreeColObject> void appendChild(T fco);
    
    /**
     * Append a multiple new children.
     *
     * @param <T> The child type.
     * @param fcos The new child objects.
     */
    abstract protected <T extends FreeColObject> void appendChildren(Collection<T> fcos);

    /**
     * Should this message only be sent to a server by the current player?
     *
     * @return True if this is a current-player-only message.
     */
    abstract public boolean currentPlayerMessage();

    /**
     * Get the priority of this type of message.
     *
     * @return The message priority.
     */
    abstract public MessagePriority getPriority();


    /**
     * Does this message consist only of mergeable attributes?
     *
     * @return True if this message is trivially mergeable.
     */
    public boolean canMerge() {
        return false;
    }

    /**
     * AI-side handler for this message.
     *
     * AI handlers always return null.
     * FIXME: One day the FreeColServer should devolve to AIMain.
     * 
     * @param freeColServer The {@code FreeColServer} handling the request.
     * @param aiPlayer The {@code AIPlayer} the message was sent to.
     * @exception FreeColException if there is a problem handling the message.
     */
    abstract public void aiHandler(FreeColServer freeColServer,
                                   AIPlayer aiPlayer) throws FreeColException;

    /**
     * Client-side handler for this message.
     *
     * Client handlers always return null.
     *
     * @param freeColClient The {@code FreeColClient} to handle this message.
     * @exception FreeColException if there is a problem building the message.
     */
    abstract public void clientHandler(FreeColClient freeColClient)
        throws FreeColException;

    /**
     * Server-side handler for this message.
     *
     * @param freeColServer The {@code FreeColServer} handling the request.
     * @param serverPlayer The {@code ServerPlayer} that sent the request.
     * @return A {@code ChangeSet} defining the response.
     */
    abstract public ChangeSet serverHandler(FreeColServer freeColServer,
                                            ServerPlayer serverPlayer);


    // Utilities derived from the above primitives

    /**
     * Checks if this message is of a given type.
     * 
     * @param type The type you wish to test against.
     * @return True if the type of this message equals the given type.
     */
    public boolean isType(String type) {
        return getType().equals(type);
    }
        
    /**
     * Get a boolean attribute value.
     *
     * @param key The attribute to look for.
     * @param defaultValue The fallback result.
     * @return The boolean value, or the default value if no boolean is found.
     */
    protected Boolean getBooleanAttribute(String key, Boolean defaultValue) {
        if (hasAttribute(key)) {
            try {
                return Boolean.valueOf(getStringAttribute(key));
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    /**
     * Get an integer attribute value.
     *
     * @param key The attribute to look for.
     * @param defaultValue The fallback result.
     * @return The integer value, or the default value if no integer is found.
     */
    protected Integer getIntegerAttribute(String key, int defaultValue) {
        if (hasAttribute(key)) {
            try {
                return Integer.valueOf(getStringAttribute(key));
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    /**
     * Gets an enum attribute value.
     *
     * @param <T> The expected enum type.
     * @param key The attribute name.
     * @param returnClass The class of the return value.
     * @param defaultValue The default value.
     * @return The enum attribute value, or the default value if none found.
     */
    protected <T extends Enum<T>> T getEnumAttribute(String key,
                                                     Class<T> returnClass,
                                                     T defaultValue) {
        T result = defaultValue;
        if (hasAttribute(key)) {
            String kv = upCase(getStringAttribute(key));
            try {
                result = Enum.valueOf(returnClass, kv);
            } catch (Exception e) {
                logger.warning("Not a " + defaultValue.getClass().getName()
                    + ": " + kv);
            }
        }
        return result;
    }
            
    /**
     * Sets an attribute in this message with a boolean value.
     *
     * @param key The attribute to set.
     * @param value The value of the attribute.
     */
    protected void setBooleanAttribute(String key, Boolean value) {
        if (value != null) setStringAttribute(key, Boolean.toString(value));
    }

    /**
     * Sets an attribute in this message with an enum value.
     *
     * @param key The attribute to set.
     * @param value The value of the attribute.
     */
    protected void setEnumAttribute(String key, Enum<?> value) {
        if (value != null) setStringAttribute(key, downCase(value.toString()));
    }

    /**
     * Sets an attribute in this message with an integer value.
     *
     * @param key The attribute to set.
     * @param value The value of the attribute.
     */
    protected void setIntegerAttribute(String key, int value) {
        setStringAttribute(key, Integer.toString(value));
    }

    /**
     * Set all the attributes in a map.
     *
     * @param attributeMap The map of key,value pairs to set.
     */
    protected void setStringAttributeMap(Map<String, String> attributeMap) {
        forEachMapEntry(attributeMap,
                        e -> setStringAttribute(e.getKey(), e.getValue()));
    }

    /**
     * Set all the attributes from a list.
     *
     * @param attributes A list of alternating key,value pairs.
     */
    protected void setStringAttributes(List<String> attributes) {
        for (int i = 0; i < attributes.size()-1; i += 2) {
            String k = attributes.get(i);
            String v = attributes.get(i+1);
            if (k != null && v != null) setStringAttribute(k, v);
        }
    }

    /**
     * Set all the attributes from an array.
     *
     * @param attributes An array of alternating key,value pairs.
     */
    protected void setStringAttributes(String[] attributes) {
        for (int i = 0; i < attributes.length; i += 2) {
            if (attributes[i+1] != null) {
                setStringAttribute(attributes[i], attributes[i+1]);
            }
        }
    }

    /**
     * Get the array attributes of this message.
     *
     * @return A list of the array attributes found.
     */
    protected List<String> getArrayAttributes() {
        List<String> ret = new ArrayList<>();
        int n = getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, -1);
        for (int i = 0; i < n; i++) {
            String key = FreeColObject.arrayKey(i);
            if (!hasAttribute(key)) break;
            ret.add(getStringAttribute(key));
        }
        return ret;
    }

    /**
     * Set a list of attributes as an array.
     *
     * @param attributes A list of attribute values.
     */
    protected void setArrayAttributes(List<String> attributes) {
        if (attributes != null) {
            int i = 0;
            for (String a : attributes) {
                String key = FreeColObject.arrayKey(i);
                i++;
                setStringAttribute(key, a);
            }
            setIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG, i);
        }
    }

    /**
     * Set an array of attributes.
     *
     * @param attributes The array of attributes.
     */
    protected void setArrayAttributes(String[] attributes) {
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                setStringAttribute(FreeColObject.arrayKey(i), attributes[i]);
            }
        }
    }

    /**
     * Get a child object.
     *
     * @param <T> The actual class of {@code FreeColObject} to get.
     * @param index The index of the child to get.
     * @param returnClass The expected class of child.
     * @return The child object found, or null if the index is invalid or
     *     return class incorrect.
     */
    protected <T extends FreeColObject> T getChild(int index,
                                                   Class<T> returnClass) {
        if (index >= getChildCount()) return (T)null;
        FreeColObject fco = getChildren().get(index);
        try {
            return returnClass.cast(fco);
        } catch (ClassCastException cce) {
            logger.log(Level.WARNING, "Cast fail", cce);
            return null;
        }
    }

    /**
     * Get the child objects.
     *
     * @param <T> The actual class of {@code FreeColObject} to get.
     * @param returnClass The expected class of children.
     * @return The children with the expected class.
     */
    protected <T extends FreeColObject> List<T> getChildren(Class<T> returnClass) {
        List<T> ret = new ArrayList<>();
        for (FreeColObject fco : getChildren()) {
            try {
                ret.add(returnClass.cast(fco));
            } catch (ClassCastException cce) {}
        }
        return ret;
    }
            
    /**
     * Is this message vacuous?
     *
     * @return True if there are no attributes or children present.
     */
    protected boolean isEmpty() {
        return getStringAttributeMap().isEmpty() && getChildren().isEmpty();
    }

    /**
     * Get the priority level of this type of message.
     *
     * @return The message priority level.
     */
    public final int getPriorityLevel() {
        return getPriority().getValue();
    }

    /**
     * Merge another message into this message if possible.
     *
     * @param message The {@code Message} to merge.
     * @return True if the other message was merged.
     */
    public boolean merge(Message message) {
        if (!message.canMerge()) return false;

        Map<String,String> map = this.getStringAttributeMap();
        map.putAll(message.getStringAttributeMap());
        this.setStringAttributeMap(map);
        Set<FreeColObject> objs = new HashSet<>(this.getChildren());
        objs.addAll(message.getChildren());
        this.setChildren(toList(objs));
        return true;
    }

    /**
     * Write any attributes of this message.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there is a problem writing the stream.
     */
    protected void writeAttributes(FreeColXMLWriter xw)
        throws XMLStreamException {
        for (Entry<String,String> e : getStringAttributeMap().entrySet()) {
            xw.writeAttribute(e.getKey(), e.getValue());
        }
    }

    /**
     * Write any children of this message.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there is a problem writing the stream.
     */
    protected void writeChildren(FreeColXMLWriter xw)
        throws XMLStreamException {
        for (FreeColObject fco : getChildren()) {
            fco.toXML(xw);
        }
    }

    /**
     * Write this message as XML.
     *
     * @param xw The {@code FreeColXMLWriter} to write with.
     * @exception XMLStreamException if there is a problem writing the stream.
     */
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeStartElement(getType());

        writeAttributes(xw);

        writeChildren(xw);

        xw.writeEndElement();
    }


    // Convenience methods for the subclasses

    protected net.sf.freecol.client.control.InGameController
        igc(FreeColClient freeColClient) {
        return freeColClient.getInGameController();
    }

    protected net.sf.freecol.server.control.InGameController
        igc(FreeColServer freeColServer) {
        return freeColServer.getInGameController();
    }

    protected void invokeAndWait(FreeColClient freeColClient,
                                 Runnable runnable) {
        freeColClient.getGUI().invokeNowOrWait(runnable);
    }

    protected void invokeLater(FreeColClient freeColClient,
                               Runnable runnable) {
        freeColClient.getGUI().invokeNowOrLater(runnable);
    }

    protected net.sf.freecol.client.control.PreGameController
        pgc(FreeColClient freeColClient) {
        return freeColClient.getPreGameController();
    }

    protected net.sf.freecol.server.control.PreGameController
        pgc(FreeColServer freeColServer) {
        return freeColServer.getPreGameController();
    }

    /**
     * Do some generic client checks that can apply to any message.
     *
     * @param freeColClient The client.
     */
    protected void clientGeneric(final FreeColClient freeColClient) {
        if (freeColClient.currentPlayerIsMyPlayer()) {
            // Play a sound if specified
            String sound = getStringAttribute("sound");
            if (sound != null && !sound.isEmpty()) {
                freeColClient.getGUI().playSound(sound);
            }
            // If there is a "flush" attribute present, encourage the
            // client to display any new messages.
            if (getBooleanAttribute("flush", Boolean.FALSE)) {
                final Runnable displayModelMessagesRunnable = () -> {
                    freeColClient.getInGameController()
                        .displayModelMessages(false);
                };
                invokeLater(freeColClient, displayModelMessagesRunnable);
            }
        }
    }    

    /**
     * Complain about finding the wrong XML element.
     *
     * @param wanted The tag we wanted.
     * @param got The tag we got.
     * @exception XMLStreamException is always thrown.
     */
    protected void expected(String wanted, String got)
        throws XMLStreamException {
        throw new XMLStreamException("In " + getClass().getName()
            + ", expected " + wanted + " but read: " + got);
    }


    // Fundamental utility for the input handlers to call

    /**
     * Read a new message from a stream.
     *
     * @param game The {@code Game} within which to construct the message.
     * @param xr A {@code FreeColXMLReader} to read from.
     * @return The new {@code Message}.
     * @exception FreeColException if there is problem reading the message.
     */
    public static Message read(Game game, FreeColXMLReader xr)
        throws FreeColException {
        final String tag = xr.getLocalName();
        Message ret = null;
        Constructor<? extends Message> mb = builders.get(tag);
        if (mb == null) {
            final String className = "net.sf.freecol.common.networking."
                + capitalize(tag) + "Message";
            @SuppressWarnings("unchecked")
            final Class<? extends Message> cl
                = (Class<? extends Message>)Introspector.getClassByName(className);
            if (cl == null) {
                throw new FreeColException("No class for: " + tag)
                    .preserveDebug();
            }

            mb = Introspector.getConstructor(cl, readClasses);
            if (mb == null) {
                throw new FreeColException("No constructor for: " + tag)
                    .preserveDebug();
            }
            builders.put(tag, mb);
        }

        try {
            ret = Introspector.construct(mb, new Object[] { game, xr });
        } catch (Introspector.IntrospectorException ie) {
            throw new FreeColException(ie);
        }
        return ret;
    }


    // Override Object
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[');
        pretty(sb, getType(), getStringAttributeMap(), getChildren());
        sb.append(']');
        return sb.toString();
    }

    /**
     * Pretty print the components of a message to a string builder.
     *
     * @param sb The {@code StringBuilder} to print to.
     * @param type The type of the message.
     * @param attributeMap A map of key,value attribute pairs.
     * @param children The attached child {@code FreeColObject}s.
     */
    protected static void pretty(StringBuilder sb, String type,
                                 Map<String, String> attributeMap,
                                 List<FreeColObject> children) {
        sb.append(type);
        if (attributeMap != null) {
            for (Entry<String,String> e : attributeMap.entrySet()) {
                sb.append(' ').append(e.getKey())
                    .append('=').append(e.getValue());
            }
        }
        if (children != null) {
            for (FreeColObject fco : children) {
                sb.append(' ').append(fco.getId());
            }
        }
    }
}
