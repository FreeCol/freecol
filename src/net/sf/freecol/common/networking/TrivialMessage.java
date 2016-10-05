/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The basic trivial message, with just a name and possibly some attributes.
 */
public class TrivialMessage extends DOMMessage {

    private static final String TRIVIAL_TAG = "trivial";

    /** The actual message type. */
    private final String type;

    /** The key,value pairs. */
    private final Map<String,String> attributes = new HashMap<>();


    /**
     * Create a new {@code TrivialMessage} of a given type.
     *
     * @param type The message type.
     */
    public TrivialMessage(String type) {
        super(TRIVIAL_TAG);

        this.type = type;
        this.attributes.clear();
    }

    /**
     * Create a new {@code TrivialMessage} of a given type and attributes.
     *
     * @param type The message type.
     * @param attributes The key,value pairs.
     */
    public TrivialMessage(String type, String... attributes) {
        this(type);

        setAttributes(attributes);
    }

    /**
     * Create a new {@code TrivialMessage} of a given type and attributes.
     *
     * @param type The message type.
     * @param attributes A map of key,value pairs.
     */
    public TrivialMessage(String type, Map<String, String> attributes) {
        this(type);

        setAttributes(attributes);
    }

    /**
     * Create a new {@code TrivialMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public TrivialMessage(Game game, Element element) {
        this(element.getTagName());

        this.attributes.putAll(getAttributeMap(element));
    }


    // Public interface

    /**
     * Get the type of the message.
     *
     * @return The type name.
     */
    @Override
    public String getType() {
        return this.type;
    }
    
    /**
     * Check if an attribute is present.
     *
     * @param key The {@code key} to look up.
     * @return True if key is present.
     */
    @Override
    public boolean hasAttribute(String key) {
        return this.attributes.containsKey(key);
    }

    /**
     * Get an attribute value.
     *
     * @param key The {@code key} to look up.
     * @return The value found.
     */
    @Override
    public String getAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * Set an attribute value.
     *
     * @param key The {@code key} to look up.
     * @param value The value to set.
     * @return This message.
     */
    @Override
    public void setAttribute(String key, String value) {
        if (value != null) this.attributes.put(key, value);
    }

    /**
     * Get a boolean attribute value.
     *
     * @param key The {@code key} to look up.
     * @return The boolean value found, or null if the attribute was absent.
     * @exception NumberFormatException if the value is ill-formed.
     */
    public Boolean getBooleanAttribute(String key)
        throws NumberFormatException {
        String value = getAttribute(key);
        return (value == null) ? null : Boolean.parseBoolean(value);
    }

    /**
     * Get an integer attribute value.
     *
     * @param key The {@code key} to look up.
     * @return The integer value found, or null if the attribute was absent.
     * @exception NumberFormatException if the value is ill-formed.
     */
    public Integer getIntegerAttribute(String key)
        throws NumberFormatException {
        String value = getAttribute(key);
        return (value == null) ? null : Integer.parseInt(getAttribute(key));
    }

    /**
     * Set the attribute pairs in an array.
     *
     * @param attributes An array of key,value pairs.
     * @return This message.
     */
    public TrivialMessage setAttributes(String[] attributes) {
        if (attributes != null) {
            for (int i = 0; i < attributes.length-1; i += 2) {
                this.setAttribute(attributes[i], attributes[i+1]);
            }
        }
        return this;
    }

    /**
     * Set the attributes in a map.
     *
     * @param attributes A map of key,value pairs.
     * @return This message.
     */
    public TrivialMessage setAttributes(Map<String, String> attributes) {
        if (attributes != null) this.attributes.putAll(attributes);
        return this;
    }

    /**
     * Get a list of array attributes.
     *
     * @param attributes A list of attribute values.
     */
    public List<String> getArrayAttributes() {
        List<String> ret = new ArrayList<>();
        int n;
        try {
            n = getIntegerAttribute(FreeColObject.ARRAY_SIZE_TAG);
        } catch (NumberFormatException nfe) {
            n = 0;
        }
        for (int i = 0; i < n; i++) {
            String key = FreeColObject.arrayKey(i);
            if (!hasAttribute(key)) break;
            ret.add(getAttribute(key));
        }
        return ret;
    }

    /**
     * Set a list of attributes as an array.
     *
     * @param attributes A list of attribute values.
     */
    public TrivialMessage setArrayAttributes(List<String> attributes) {
        if (attributes != null) {
            int i = 0;
            for (String a : attributes) {
                String key = FreeColObject.arrayKey(i);
                i++;
                setAttribute(key, a);
            }
            setAttribute(FreeColObject.ARRAY_SIZE_TAG, String.valueOf(i));
        }
        return this;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return createElement(this.type, this.attributes);
    }

    /**
     * {@inheritDoc}
    @Override
    public Element toXMLElement(ServerPlayer serverPlayer) {
        return toXMLElement(); // Does not specialize
    }
     */
    
    /**
     * The tag name of this object, which is not actually statically available.
     *
     * @return Fails!
     */
    public static String getTagName() {
        return TRIVIAL_TAG;
    }
}
