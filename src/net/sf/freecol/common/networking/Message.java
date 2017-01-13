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
     * Gets an attribute from this message.
     * 
     * @param key The attribute to look for.
     * @return The value of the attribute with the given key.
     */
    abstract public String getAttribute(String key);

    /**
     * Sets an attribute in this message.
     * 
     * @param key The attribute to set.
     * @param value The new value of the attribute.
     */
    abstract public void setAttribute(String key, String value);

    /**
     * Get all the attributes in this message.
     *
     * @param element The {@code Element} to extract from.
     * @return A {@code Map} of the attributes.
     */
    abstract public Map<String,String> getAttributes();
    
    /**
     * Set all the attributes in a map.
     *
     * @param attributes The map of key, value pairs to set.
     */
    public void setAttributes(Map<String, String> attributes) {
        forEachMapEntry(attributes,
                        e -> setAttribute(e.getKey(), e.getValue()));
    }

    /**
     * Get the array attrubutes of this message.
     *
     * @return A list of the array attributes found.
     */
    public List<String> getArrayAttributes() {
        List<String> result = new ArrayList<>();
        String key;
        int i = 0;
        for (;;) {
            key = FreeColObject.arrayKey(i);
            if (!hasAttribute(key)) break;
            result.add(getAttribute(key));
            i++;
        }
        return result;
    }

    /**
     * Set a list of attributes as an array.
     *
     * @param attributes The list of attributes.
     */
    public void setArrayAttributes(List<String> attributes) {
        int i = 0;
        for (String a : attributes) {
            setAttribute(FreeColObject.arrayKey(i), a);
            i++;
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
                setAttribute(FreeColObject.arrayKey(i), attributes[i]);
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
}
