/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when naming a new land.
 */
public class NewLandNameMessage extends Message {
    /**
     * The name to use.
     */
    private String newLandName;

    /**
     * Create a new <code>NewLandNameMessage</code> with the
     * supplied name.
     *
     * @param newLandName The new land name.
     */
    public NewLandNameMessage(String newLandName) {
        this.newLandName = newLandName;
    }

    /**
     * Create a new <code>NewLandNameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NewLandNameMessage(Game game, Element element) {
        this.newLandName = element.getAttribute("newLandName");
    }

    /**
     * Handle a "newLandName"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update setting the new land name,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        HistoryEvent h = new HistoryEvent(player.getGame().getTurn().getNumber(),
                                          HistoryEvent.EventType.DISCOVER_NEW_WORLD)
            .addName("%name%", newLandName);
        player.getHistory().add(h);
        player.setNewLandName(newLandName);
        // TODO: send land name to other players?

        Element reply = Message.createNewRootElement("multiple");
        Document doc = reply.getOwnerDocument();
        Element update = doc.createElement("update");
        Element history = doc.createElement("addHistory");
        reply.appendChild(update);
        reply.appendChild(history);
        update.appendChild(player.toXMLElementPartial(doc, "newLandName"));
        history.appendChild(h.toXMLElement(player, doc));
        return reply;
    }

    /**
     * Convert this NewLandNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("newLandName", newLandName);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "newLandName".
     */
    public static String getXMLElementTagName() {
        return "newLandName";
    }
}
