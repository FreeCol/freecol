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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when summarizing a nation.
 */
public class GetNationSummaryMessage extends DOMMessage {

    public static final String TAG = "getNationSummary";

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
        super(getTagName());

        this.playerId = player.getId();
        this.summary = null;
    }

    /**
     * Create a new <code>GetNationSummaryMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> containing the nation to summarize.
     * @param element The <code>Element</code> to use to create the message.
     */
    public GetNationSummaryMessage(Game game, Element element) {
        super(getTagName());

        this.playerId = element.getAttribute("player");
        this.summary = getChild(game, element, 0, NationSummary.class);
    }


    // Public interface

    /**
     * Client side helper to get the player.
     *
     * @return The player.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(playerId, Player.class);
    }

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

        Player player = getPlayer(game);
        if (player == null) {
            return serverPlayer.clientError("Not a player: " + playerId)
                .build(serverPlayer);
        } else if (player.isIndian() && !serverPlayer.hasContacted(player)) {
            return null;
        }

        // Proceed to get the summary.
        this.summary = new NationSummary(player, serverPlayer);
        return toXMLElement();
    }

    /**
     * Convert this GetNationSummaryMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        DOMMessage result = new DOMMessage(getTagName(),
            "player", playerId);
        if (summary != null) result.add(summary);
        return result.toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "getNationSummary".
     */
    public static String getTagName() {
        return TAG;
    }
}
