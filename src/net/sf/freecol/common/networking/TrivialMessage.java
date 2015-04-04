/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import org.w3c.dom.Element;


/**
 * A trivial message.  That is, it contains only simple string
 * attributes.
 */
class TrivialMessage extends DOMMessage {

    /** The tag for the trivial message. */
    private final String tag;

    /** An array of (attribute,value) pairs. */
    private final String[] attributes;


    public TrivialMessage(String tag, String... attributes) {
        super(tag);

        this.tag = tag;
        this.attributes = attributes;
    }


    @Override
    public Element toXMLElement() {
        return DOMMessage.createMessage(tag, attributes);
    }
}
