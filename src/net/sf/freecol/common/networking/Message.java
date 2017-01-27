/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColObject;
import static net.sf.freecol.common.util.CollectionUtils.*;

import org.xml.sax.SAXException;


/**
 * Base abstract class for all messages.
 */
public abstract class Message {

    protected static final Logger logger = Logger.getLogger(Message.class.getName());

    // Convenient way to specify the relative priorities of the messages
    // types in one place.
    public static enum MessagePriority {
        ATTRIBUTE(-1), // N/A
        ANIMATION(0),  // Do animations first
        REMOVE(100),   // Do removes last
        STANCE(5),     // Do stance before updates
        OWNED(20),     // Do owned changes after updates
        UPDATE(10),    // There are a lot of updates
        // Symbolic priorities used by various non-fixed types
        EARLY(1),
        NORMAL(15),
        LATE(90);

        private final int level;

        MessagePriority(int level) {
            this.level = level;
        }

        public int getValue() {
            return this.level;
        }
    }


    /**
     * Deliberately trivial constructor.
     */
    protected Message() {
        // empty constructor
    }

    /**
     * Constructs a new message with data from the given input stream.
     * 
     * @param inputStream The {@code InputStream} to read.
     * @exception IOException if thrown by the {@code InputStream}.
     * @exception SAXException if thrown during parsing.
     */
    public Message(InputStream inputStream) throws SAXException, IOException {
        readInputStream(inputStream);
    }

    /**
     * Build a new message with the given type.
     * 
     * @param type The main message type.
     */
    public Message(String type) {
        setType(type);
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
     * Checks if this message is of a given type.
     * 
     * @param type The type you wish to test against.
     * @return True if the type of this message equals the given type.
     */
    public boolean isType(String type) {
        return getType().equals(type);
    }

    /**
     * Checks if an attribute is present in this message.
     * 
     * @param key The attribute to look for.
     * @return True if the attribute is present.
     */
    abstract public boolean hasAttribute(String key);

    /**
     * Get a boolean attribute value.
     *
     * @param key The attribute to look for.
     * @param defaultValue The fallback result.
     * @return The boolean value, or the default value if no boolean is found.
     */
    public Boolean getBooleanAttribute(String key, Boolean defaultValue) {
        if (hasAttribute(key)) {
            try {
                return Boolean.parseBoolean(getStringAttribute(key));
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
    public Integer getIntegerAttribute(String key, int defaultValue) {
        if (hasAttribute(key)) {
            try {
                return Integer.parseInt(getStringAttribute(key));
            } catch (NumberFormatException nfe) {}
        }
        return defaultValue;
    }

    /**
     * Get a string attribute value.
     *
     * @param key The attribute to look for.
     * @return The string value found, or null if the attribute was absent.
     */
    abstract public String getStringAttribute(String key);

    /**
     * Sets an attribute in this message with n boolean value.
     *
     * @param key The attribute to set.
     * @param value The value of the attribute.
     */
    public void setBooleanAttribute(String key, Boolean value) {
        if (value != null) setStringAttribute(key, Boolean.toString(value));
    }

    /**
     * Sets an attribute in this message with an integer value.
     *
     * @param key The attribute to set.
     * @param value The value of the attribute.
     */
    public void setIntegerAttribute(String key, int value) {
        setStringAttribute(key, Integer.toString(value));
    }

    /**
     * Sets an attribute in this message.
     * 
     * @param key The attribute to set.
     * @param value The new value of the attribute.
     */
    abstract public void setStringAttribute(String key, String value);

    /**
     * Get all the attributes in this message.
     *
     * @param element The {@code Element} to extract from.
     * @return A {@code Map} of the attributes.
     */
    abstract public Map<String,String> getStringAttributes();
    
    /**
     * Set all the attributes in a map.
     *
     * @param attributes The map of key,value pairs to set.
     */
    public void setStringAttributes(Map<String, String> attributes) {
        forEachMapEntry(attributes,
                        e -> setStringAttribute(e.getKey(), e.getValue()));
    }

    /**
     * Set all the attributes from a list.
     *
     * @param attributes A list of alternating key,value pairs.
     */
    public void setStringAttributes(List<String> attributes) {
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
    public void setStringAttributes(String[] attributes) {
        for (int i = 0; i < attributes.length; i += 2) {
            if (attributes[i+1] != null) {
                setStringAttribute(attributes[i], attributes[i+1]);
            }
        }
    }

    /**
     * Get the array attrubutes of this message.
     *
     * @return A list of the array attributes found.
     */
    public List<String> getArrayAttributes() {
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
    public void setArrayAttributes(List<String> attributes) {
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
    public void setArrayAttributes(String[] attributes) {
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                setStringAttribute(FreeColObject.arrayKey(i), attributes[i]);
            }
        }
    }

    /**
     * Build this message from an input stream.
     *
     * @param inputStream The {@code InputStream} to read.
     */
    abstract public void readInputStream(InputStream inputStream)
        throws IOException, SAXException;


    /**
     * Get the priority of this type of message.
     *
     * May need to be overridden by specific message types if priority
     * varies due to the specifics of the message.
     *
     * @return The message priority.
     */
    public MessagePriority getPriority() {
        return getMessagePriority();
    }
    
    /**
     * Get the priority of this type of message.
     *
     * To be overridden by specific message types.
     *
     * @return The message priority.
     */
    public static MessagePriority getMessagePriority() {
        return MessagePriority.NORMAL;
    }
}
