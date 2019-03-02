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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;


/**
 * The basic message with optional attributes.
 */
public class AttributeMessage extends TrivialMessage {

    public static final String TAG = "attribute";

    /** The key,value attribute pairs. */
    protected final Map<String,String> attributes = new HashMap<>();

    /** Whether this message is trivially mergeable. */
    private boolean mergeable = false;


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
     * @param attributeMap A map of key,value pairs.
     */
    protected AttributeMessage(String type, Map<String, String> attributeMap) {
        this(type);

        setStringAttributeMap(attributeMap);
    }

    /**
     * Create a new {@code AttributeMessage} from a stream.
     *
     * Should only be called by direct subclasses of AttributeMessage
     * as the entire message is consumed.
     * 
     * @param type The message type.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param attributes The attributes to read.
     * @exception XMLStreamException if the stream is corrupt.
     */
    protected AttributeMessage(String type, FreeColXMLReader xr,
                               String... attributes)
        throws XMLStreamException {
        this(type, xr.getAttributeMap(attributes));

        xr.closeTag(type);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasAttribute(String key) {
        return this.attributes.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getStringAttribute(String key) {
        return this.attributes.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setStringAttribute(String key, String value) {
        if (key != null && value != null) this.attributes.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String,String> getStringAttributeMap() {
        return this.attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.ATTRIBUTE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canMerge() {
        return this.mergeable;
    }


    // Public interface

    /**
     * Set the mergeable state of this message.
     *
     * @param mergeable The new mergeable state.
     * @return This message.
     */
    public AttributeMessage setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
        return this;
    }
}
