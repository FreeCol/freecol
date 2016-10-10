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


    /**
     * Create a new {@code TrivialMessage} of a given type.
     *
     * @param type The message type.
     */
    public TrivialMessage(String type) {
        super(type);

        this.type = type;
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
}
