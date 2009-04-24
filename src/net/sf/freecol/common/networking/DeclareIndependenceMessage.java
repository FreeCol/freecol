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

import java.util.List;

import org.w3c.dom.Element;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when a player declares independence.
 */
public class DeclareIndependenceMessage extends Message {
    /**
     * The new name for the rebelling nation
     */
    private String nationName;


    /**
     * Create a new <code>DeclareIndependenceMessage</code> with the
     * supplied name.
     *
     * @param nationName The new name for the rebelling nation.
     */
    public DeclareIndependenceMessage(String nationName) {
        this.nationName = nationName;
    }

    /**
     * Create a new <code>DeclareIndependenceMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DeclareIndependenceMessage(Game game, Element element) {
        this.nationName = (String) element.getAttribute("nationName");
    }

    /**
     * Handle a "declareIndependence"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message is from.
     *
     * @return An <code>Element</code> containing the client view of the
     *         REF raised to punish rebel insolence,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        if (nationName == null || nationName.length() == 0) {
            return Message.createError("server.declareIndependence.badName", null);
        }
        ServerPlayer serverplayer = server.getPlayer(connection);
        ServerPlayer refPlayer = server.getInGameController().createREFPlayer(serverplayer);
        List<Unit> refUnits = server.getInGameController().createREFUnits(serverplayer, refPlayer);
        player.setIndependentNationName(nationName);
        player.declareIndependence();

        Element reply = Message.createNewRootElement("update");
        reply.appendChild(refPlayer.toXMLElement(null, reply.getOwnerDocument()));
        for (Unit unit : refUnits) {
            reply.appendChild(unit.toXMLElement(null, reply.getOwnerDocument()));
        }
        return reply;
    }

    /**
     * Convert this DeclareIndependenceMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("nationName", nationName);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "declareIndependence".
     */
    public static String getXMLElementTagName() {
        return "declareIndependence";
    }
}
