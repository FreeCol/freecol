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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message that contains other messages.
 */
public class MultipleMessage extends DOMMessage {

    public static final String TAG = "multiple";

    /** The list of messages. */
    private List<Element> elements = new ArrayList<>();


    /**
     * Create a new <code>MultipleMessage</code>.
     *
     * @param elements A list of sub-<code>Element</code>s.
     */
    public MultipleMessage(List<Element> elements) {
        super(getTagName());

        this.elements.clear();
        if (elements != null) this.elements.addAll(elements);
    }

    /**
     * Create a new <code>MultipleMessage</code>.
     *
     * @param element An element containing the sub-<code>Element</code>s.
     */
    public MultipleMessage(Element element) {
        this(mapChildren(element, e -> e));
    }

    /**
     * Create a new <code>MultipleMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MultipleMessage(Game game, Element element) {
        this((List<Element>)null);

        this.elements.addAll(mapChildren(element, e -> e));
    }


    // Public interface

    /**
     * Extract an element chosen by tag from the elements list.
     *
     * @param tag The message tag to choose.
     * @return The chosen <code>Element</code> if found, or null if not.
     */
    public Element extract(String tag) {
        for (Element e : this.elements) {
            if (e.getTagName().equals(tag)) {
                this.elements.remove(e);
                return e;
            }
        }
        return null;
    }

    /**
     * Apply a handler to this message.
     *
     * @param handler A <code>MessageHandler</code> to apply.
     * @param connection The <code>Connection</code> message was received on.
     * @return A collapsed resolution of the submessages.
     */
    public Element applyHandler(MessageHandler handler,
                                Connection connection) {
        return handleList(handler, connection, this.elements);
    }


    /**
     * Handle a "multiple"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return A collapsed resolution of the submessages.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        return applyHandler(server.getInGameInputHandler(), connection);
    }


    /**
     * Convert this MultipleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(getTagName());
        for (Element e : this.elements) result.add(e);
        return result.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "multiple".
     */
    public static String getTagName() {
        return TAG;
    }
}
