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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.util.DOMUtils;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The basic message with optional attributes.
 */
public class AttributeMessage extends TrivialMessage {

    /** The key,value pairs. */
    private final Map<String,String> attributes = new HashMap<>();


    /**
     * Create a new {@code AttributeMessage} of a given type.
     *
     * @param type The message type.
     */
    public AttributeMessage(String type) {
        super(type);

        this.attributes.clear();
    }

    /**
     * Create a new {@code AttributeMessage} of a given type and attributes.
     *
     * @param type The message type.
     * @param attributes The key,value pairs.
     */
    public AttributeMessage(String type, String... attributes) {
        this(type);

        setStringAttributes(attributes);
    }

    /**
     * Create a new {@code AttributeMessage} of a given type and attributes.
     *
     * @param type The message type.
     * @param attributes A map of key,value pairs.
     */
    public AttributeMessage(String type, Map<String, String> attributes) {
        this(type);

        setStringAttributes(attributes);
    }

    /**
     * Create a new {@code AttributeMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AttributeMessage(Game game, Element element) {
        this(element.getTagName());

        this.attributes.putAll(DOMUtils.getAttributeMap(element));
    }


    // Public interface

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
    public String getStringAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * Set an attribute value.
     *
     * @param key The {@code key} to look up.
     * @param value The value to set.
     */
    @Override
    public void setStringAttribute(String key, String value) {
        if (value != null) this.attributes.put(key, value);
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return DOMUtils.createElement(getType(), this.attributes);
    }
}
