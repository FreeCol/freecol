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
import java.util.List;
import java.util.function.Function;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Element;


/**
 * The message that contains other messages.
 */
public class MultipleMessage extends DOMMessage {

    public static final String TAG = "multiple";

    /** The list of messages. */
    private List<Element> elements = new ArrayList<>();


    /**
     * Create a new {@code MultipleMessage}.
     *
     * @param elements A list of sub-{@code Element}s.
     */
    public MultipleMessage(List<Element> elements) {
        super(TAG);

        this.elements.clear();
        if (elements != null) this.elements.addAll(elements);
    }

    /**
     * Create a new {@code MultipleMessage}.
     *
     * @param element An element containing the sub-{@code Element}s.
     */
    public MultipleMessage(Element element) {
        this(mapChildren(element, Function.identity()));
    }

    /**
     * Create a new {@code MultipleMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public MultipleMessage(Game game, Element element) {
        this((List<Element>)null);

        this.elements.addAll(mapChildren(element, Function.identity()));
    }


    // Public interface

    /**
     * Apply a handler to this message.
     *
     * @param handler A {@code MessageHandler} to apply.
     * @param connection The {@code Connection} message was received on.
     * @return A collapsed resolution of the submessages.
     */
    public Element applyHandler(MessageHandler handler,
                                Connection connection) {
        return handleList(handler, connection, this.elements);
    }


    /**
     * Handle a "multiple"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param connection The {@code Connection} message was received on.
     * @return A collapsed resolution of the submessages.
     */
    public Element handle(FreeColServer server, Connection connection) {
        return applyHandler(server.getInGameInputHandler(), connection);
    }


    /**
     * Convert this MultipleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(TAG);
        for (Element e : this.elements) result.add(e);
        return result.toXMLElement();
    }
}
