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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
    public LogoutMessage(Player player, String reason) {
        super(TAG, PLAYER_TAG, player.getId(), REASON_TAG, reason);
    }

    /**
     * Create a new {@code LogoutMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public LogoutMessage(Game game, Element element) {
        super(TAG, PLAYER_TAG, getStringAttribute(element, PLAYER_TAG),
              REASON_TAG, getStringAttribute(element, REASON_TAG));
    }


    // Public interface

    /**
     * Get the player logging out.
     *
     * @param game A {@code Game} to find the player in.
     * @return The player found.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the reason for logging out.
     *
     * @return The logout reason.
     */
    public String getReason() {
        return getAttribute(REASON_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) return null;
        logger.info("Handling logout by " + serverPlayer.getName());

        ChangeSet cs = null;

        // FIXME: Setting the player dead directly should be a server
        // option, but for now allow the player to reconnect.
        serverPlayer.setConnected(false);

        switch (freeColServer.getServerState()) {
        case PRE_GAME: case LOAD_GAME:
            LogoutMessage message
                = new LogoutMessage(serverPlayer, "User has logged out");
            freeColServer.sendToAll(message, serverPlayer);
            break;
        case IN_GAME:
            Game game = freeColServer.getGame();
            if (game.getCurrentPlayer() == serverPlayer
                && !freeColServer.getSinglePlayer()) {
                cs = freeColServer.getInGameController().endTurn(serverPlayer);
            }
            break;
        case END_GAME:
            break;
        }

        // Withdraw from the metaserver
        freeColServer.updateMetaServer(false);

        return cs;
    }
}
