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
 * The message to signal a player has died.
 */
public class SetDeadMessage extends AttributeMessage {

    public static final String TAG = "setDead";
    private static final String PLAYER_TAG = "player";


    /**
     * Create a new {@code SetDeadMessage} for a given player.
     *
     * @param player The {@code Player} that has died.
     */
    public SetDeadMessage(Player player) {
        super(TAG, PLAYER_TAG, player.getId());
    }

    /**
     * Create a new {@code SetDeadMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SetDeadMessage(Game game, Element element) {
        super(TAG, PLAYER_TAG, getStringAttribute(element, PLAYER_TAG));
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return Message.MessagePriority.EARLY;
    }


    // Public interface

    /**
     * Who died?
     *
     * @param game The {@code Game} the player is in.
     * @return The {@code Player} that has died.
     */
    public Player getPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(PLAYER_TAG), Player.class);
    }

    // No server handler required.
    // This message is only sent to clients.
}
