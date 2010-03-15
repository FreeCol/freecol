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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when naming a new land.
 */
public class NewLandNameMessage extends Message {

    /**
     * The name to use.
     */
    private String newLandName;

    /**
     * An optional welcoming player.
     */
    private String welcomerId;

    /**
     * Has a treaty been accepted with the welcomer?
     */
    private String acceptString;

    /**
     * Create a new <code>NewLandNameMessage</code> with the
     * supplied name.
     *
     * @param newLandName The new land name.
     */
    public NewLandNameMessage(String newLandName, Player welcomer,
                              boolean accept) {
        this.newLandName = newLandName;
        this.welcomerId = (welcomer == null) ? null : welcomer.getId();
        this.acceptString = Boolean.toString(accept);
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
        this.welcomerId = (element.hasAttribute("welcomer"))
            ? element.getAttribute("welcomer") : null;
        this.acceptString = element.getAttribute("accept");
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
        Game game = server.getGame();
        ServerPlayer serverPlayer = server.getPlayer(connection);

        if (newLandName == null || newLandName.length() == 0) {
            return Message.clientError("Empty new land name");
        }
        ServerPlayer welcomer = null;
        boolean accept = false;
        if (welcomerId != null) {
            if (game.getFreeColGameObjectSafely(welcomerId) instanceof ServerPlayer) {
                welcomer = (ServerPlayer) game.getFreeColGameObjectSafely(welcomerId);
                accept = Boolean.valueOf(acceptString);
            } else {
                return Message.clientError("Not a player: " + welcomerId);
            }
        }

        // Set name.
        return server.getInGameController()
            .setNewLandName(serverPlayer, newLandName, welcomer, accept);
    }

    /**
     * Convert this NewLandNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("newLandName", newLandName);
        if (welcomerId != null) result.setAttribute("welcomer", welcomerId);
        result.setAttribute("accept", acceptString);
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
