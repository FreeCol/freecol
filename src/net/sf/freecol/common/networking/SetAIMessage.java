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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that changes the AI state of a player.
 */
public class SetAIMessage extends AttributeMessage {

    public static final String TAG = "setAI";
    private static final String AI_TAG = "ai";
    private static final String PLAYER_TAG = "player";


    /**
     * Create a new {@code SetAIMessage} with the given player and state.
     *
     * @param player The {@code Player} to set.
     * @param ai The new AI state.
     */
    public SetAIMessage(Player player, boolean ai) {
        super(TAG, PLAYER_TAG, player.getId(), AI_TAG, String.valueOf(ai));
    }

    /**
     * Create a new {@code SetAIMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr A {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetAIMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, PLAYER_TAG, AI_TAG);
    }
    

    /**
     * {@inheritDoc}
     */
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        final Player player = getPlayer(freeColServer.getGame());
        final boolean ai = getAI();

        if (player == null) return;

        aiPlayer.setAIHandler(player, ai);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Player player = getPlayer(freeColClient.getGame());
        final boolean ai = getAI();

        if (player == null) return;

        igc(freeColClient).setAIHandler(player, ai);
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
     * What AI state should be set?
     *
     * @return The AI state.
     */
    public boolean getAI() {
        return getBooleanAttribute(AI_TAG, Boolean.FALSE);
    }
}
