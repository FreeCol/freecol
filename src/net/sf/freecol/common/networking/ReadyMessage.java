/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
 * The message that signals a player is ready.
 */
public class ReadyMessage extends AttributeMessage {

    public static final String TAG = "ready";
    private static final String PLAYER_TAG = "player";
    private static final String VALUE_TAG = "value";


    /**
     * Create a new {@code ReadyMessage} with the given player and state.
     *
     * @param player The {@code Player} to set.
     * @param ready True if the player is ready.
     */
    public ReadyMessage(Player player, boolean ready) {
        super(TAG, PLAYER_TAG, (player == null) ? null : player.getId(),
              VALUE_TAG, String.valueOf(ready));
    }

    /**
     * Create a new {@code ReadyMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ReadyMessage(Game game, Element element) {
        super(TAG,
              PLAYER_TAG, getStringAttribute(element, PLAYER_TAG),
              VALUE_TAG, getStringAttribute(element, VALUE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }


    // Public interface

    /**
     * Which player is to be set?
     *
     * @param game The {@code Game} the player is in.
     * @return The {@code Player} to set the AI state of.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(PLAYER_TAG), Player.class);
    }

    /**
     * Get the ready state.
     * 
     * @return True if the player is ready.
     */
    public boolean getValue() {
        return getBooleanAttribute(VALUE_TAG, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer != null) {
            boolean ready = getValue();
            serverPlayer.setReady(ready);
            freeColServer.sendToAll(new ReadyMessage(serverPlayer, ready),
                                    serverPlayer);
        } else {
            logger.warning("Ready from unknown connection.");
        }
        return null;
    }
}
