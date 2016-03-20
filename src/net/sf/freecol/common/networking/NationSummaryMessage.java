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
public class NationSummaryMessage extends DOMMessage {

    public static final String TAG = "nationSummary";
    private static final String PLAYER_TAG = "player";

    /** The identifier of the player to summarize. */
    private final String playerId;

    /** The summary. */
    private NationSummary summary;


    /**
     * Create a new <code>NationSummaryMessage</code> for the
     * specified player.
     *
     * @param player The <code>Player</code> to summarize.
     */
    public NationSummaryMessage(Player player) {
        super(getTagName());

        this.playerId = player.getId();
        this.summary = null;
    }

    /**
     * Create a new <code>NationSummaryMessage</code> from a supplied
     * element.
     *
     * @param game The <code>Game</code> containing the nation to summarize.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NationSummaryMessage(Game game, Element element) {
        super(getTagName());

        this.playerId = getStringAttribute(element, PLAYER_TAG);
        this.summary = getChild(game, element, 0, NationSummary.class);
    }


    // Public interface

    /**
     * Client side helper to get the player.
     *
     * @param game The <code>Game</code> to look for a player within.
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
        return this.summary;
    }

    /**
     * Set the nation summary.
     *
     * @param ns The new <code>NationSummary</code>.
     * @return This message.
     */
    public NationSummaryMessage setNationSummary(NationSummary ns) {
        this.summary = ns;
        return this;
    }
    
    
    /**
     * Handle a "nationSummary"-message.
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
            return serverPlayer.clientError("Not a player: " + this.playerId)
                .build(serverPlayer);
        } else if (player.isIndian() && !serverPlayer.hasContacted(player)) {
            return null;
        }

        // Proceed to get the summary.
        setNationSummary(new NationSummary(player, serverPlayer));
        return toXMLElement();
    }

    /**
     * Convert this NationSummaryMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            PLAYER_TAG, this.playerId)
            .add(summary).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "nationSummary".
     */
    public static String getTagName() {
        return TAG;
    }
}
