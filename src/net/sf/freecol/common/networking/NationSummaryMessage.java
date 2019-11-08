/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when summarizing a nation.
 */
public class NationSummaryMessage extends ObjectMessage {

    public static final String TAG = "nationSummary";
    private static final String PLAYER_TAG = "player";


    /**
     * Create a new {@code NationSummaryMessage} for the
     * specified player.
     *
     * @param player The {@code Player} to summarize.
     * @param nationSummary An optional {@code NationSummary} to attach.
     */
    public NationSummaryMessage(Player player, NationSummary nationSummary) {
        super(TAG, PLAYER_TAG, player.getId());

        appendChild(nationSummary);
    }

    /**
     * Create a new {@code NationSummaryMessage} from a stream.
     *
     * @param game The {@code Game} containing the nation to summarize.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public NationSummaryMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, PLAYER_TAG);

        NationSummary ns = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (NationSummary.TAG.equals(tag)) {
                if (ns == null) {
                    ns = xr.readFreeColObject(game, NationSummary.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(NationSummary.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
        appendChild(ns);
    }


    /**
     * Client side helper to get the player.
     *
     * @param game The {@code Game} to look for a player within.
     * @return The player.
     */
    private Player getPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(PLAYER_TAG),
                                         Player.class);
    }

    /**
     * Client side helper to get the summary.
     *
     * @return The summary.
     */
    private NationSummary getNationSummary() {
        return getChild(0, NationSummary.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Player other = getPlayer(freeColServer.getGame());
        final NationSummary ns = getNationSummary();

        aiPlayer.nationSummaryHandler(other, ns);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Player other = getPlayer(game);
        final NationSummary ns = getNationSummary();

        igc(freeColClient).nationSummaryHandler(other, ns);
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();

        Player player = getPlayer(game);
        if (player == null) {
            return serverPlayer.clientError("Not a player: "
                + getStringAttribute(PLAYER_TAG));
        } else if (player.isIndian() && !serverPlayer.hasContacted(player)) {
            return null;
        }

        return igc(freeColServer)
            .nationSummary(serverPlayer, player);
    }
}
