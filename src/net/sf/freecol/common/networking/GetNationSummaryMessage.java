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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when summarizing a nation.
 */
public class GetNationSummaryMessage extends DOMMessage {

    /** The identifier of the player to summarize. */
    private final String playerId;

    /** The summary. */
    private NationSummary summary;


    /**
     * Create a new <code>GetNationSummaryMessage</code> for the
     * specified player.
     *
     * @param player The <code>Player</code> to summarize.
     */
    public GetNationSummaryMessage(Player player) {
        super(getXMLElementTagName());

        playerId = player.getId();
        summary = null;
    }

    /**
     * Create a new <code>GetNationSummaryMessage</code> from a
     * supplied element.
     *
     * @param element The <code>Element</code> to use to create the message.
     */
    public GetNationSummaryMessage(Element element) {
        super(getXMLElementTagName());

        playerId = element.getAttribute("player");
        NodeList nodes = element.getChildNodes();
        summary = (nodes == null || nodes.getLength() != 1) ? null
            : new NationSummary((Element) nodes.item(0));
    }


    // Public interface

    /**
     * Client side helper to get the summary.
     *
     * @return The summary.
     */
    public NationSummary getNationSummary() {
        return summary;
    }


    /**
     * Handle a "getNationSummary"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the nation summaries, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = serverPlayer.getGame();

        Player player = game.getFreeColGameObject(playerId, Player.class);
        if (player == null) {
            return DOMMessage.clientError("Not a player: " + playerId);
        } else if (player.isIndian() && !serverPlayer.hasContacted(player)) {
            return null;
        }

        // Proceed to get the summary.
        summary = server.getInGameController()
            .getNationSummary(serverPlayer, player);
        return toXMLElement();
    }

    /**
     * Convert this GetNationSummaryMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "player", playerId);
        if (summary != null) {
            result.appendChild(summary.toXMLElement(result.getOwnerDocument()));
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "getNationSummary".
     */
    public static String getXMLElementTagName() {
        return "getNationSummary";
    }
}
