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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.ChangeSet.See;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.ServerState;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when logging out.
 */
public class LogoutMessage extends AttributeMessage {

    public static final String TAG = "logout";
    private static final String PLAYER_TAG = "player";
    private static final String REASON_TAG = "reason";


    /**
     * Create a new {@code LogoutMessage}.
     *
     * Note: The logout reason is non-i18n for now as it is just logged.
     *
     * @param player The {@code Player} that has logged out.
     * @param reason A reason for logging out.
     */
    public LogoutMessage(Player player, LogoutReason reason) {
        super(TAG, PLAYER_TAG, player.getId(),
              REASON_TAG, String.valueOf(reason));
    }

    /**
     * Create a new {@code LogoutMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public LogoutMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, PLAYER_TAG, REASON_TAG);
    }


    /**
     * {@inheritDoc}
     */
    public MessagePriority getPriority() {
        return MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Player player = getPlayer(game);
        final LogoutReason reason = getReason();

        if (player == null) return;

        if (freeColClient.isInGame()) {
            igc(freeColClient).logoutHandler(player, reason);

        } else {
            pgc(freeColClient).logoutHandler(player, reason);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) return null;
        logger.info("Handling logout by " + serverPlayer.getName());

        LogoutReason reason = getReason();
        ChangeSet cs = null;
        switch (freeColServer.getServerState()) {
        case PRE_GAME: case LOAD_GAME:
            break;
        case IN_GAME:
            boolean endTurn = false;
            switch (reason) {
            case DEFEATED:
                endTurn = true;
                break;
            case LOGIN: // FIXME: should go away
                break;
            case MAIN_TITLE: case NEW_GAME: case QUIT:
                if (freeColServer.getSinglePlayer() || serverPlayer.isAdmin()) {
                    // End completely for single player, or multiplayer admin
                    freeColServer.endGame();
                } else {
                    endTurn = true;
                    // FIXME: turn multiplayer withdrawing player into an AI?
                }
                break;
            case RECONNECT:
                break;
            }
            if (endTurn
                && freeColServer.getGame().getCurrentPlayer() == serverPlayer) {
                cs = freeColServer.getInGameController().endTurn(serverPlayer);
            }
            break;
        case END_GAME:
            return null;
        }

        // Confirm the logout with the given reason.
        if (cs == null) cs = new ChangeSet();
        cs.add(See.only(serverPlayer), new LogoutMessage(serverPlayer, reason));

        // Update the metaserver
        freeColServer.updateMetaServer();

        return cs;
    }


    // Public interface

    /**
     * Get the player logging out.
     *
     * @param game A {@code Game} to find the player in.
     * @return The player found.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the reason for logging out.
     *
     * @return The {@code LogoutReason}.
     */
    public LogoutReason getReason() {
        return Enum.valueOf(LogoutReason.class, getStringAttribute(REASON_TAG));
    }
}
