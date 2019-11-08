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
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to the clients to signal a stance change.
 */
public class SetStanceMessage extends AttributeMessage {

    public static final String TAG = "setStance";
    private static final String FIRST_TAG = "first";
    private static final String SECOND_TAG = "second";
    private static final String STANCE_TAG = "stance";


    /**
     * Create a new {@code SetStanceMessage} with the given stance and players.
     *
     * @param stance The new {@code Stance}.
     * @param first The {@code Player} whose stance is changing.
     * @param second The {@code Player} the stance is changed with respect to.
     */
    public SetStanceMessage(Stance stance, Player first, Player second) {
        super(TAG, STANCE_TAG, String.valueOf(stance),
              FIRST_TAG, first.getId(),
              SECOND_TAG, second.getId());
    }

    /**
     * Create a new {@code SetStanceMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetStanceMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, STANCE_TAG, FIRST_TAG, SECOND_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.STANCE;
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
        final Stance stance = getStance();
        final Player p1 = getFirstPlayer(game);
        final Player p2 = getSecondPlayer(game);

        if (p1 == null) {
            logger.warning("Invalid player1 for setStance");
            return;
        }
        if (p2 == null) {
            logger.warning("Invalid player2 for setStance");
            return;
        }

        igc(freeColClient).setStanceHandler(stance, p1, p2);
        clientGeneric(freeColClient);
    }


    // Public interface

    /**
     * Get the stance that changed.
     *
     * @return The {@code Stance} value.
     */
    public Stance getStance() {
        return Enum.valueOf(Stance.class, getStringAttribute(STANCE_TAG));
    }

    /**
     * Which player is changing stance?
     *
     * @param game The {@code Game} the player is in.
     * @return The player whose stance changes.
     */
    public Player getFirstPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(FIRST_TAG), Player.class);
    }

    /**
     * Which player is the stance changed with respect to?
     *
     * @param game The {@code Game} the player is in.
     * @return The player the stance changed to.
     */
    public Player getSecondPlayer(Game game) {
        return game.getFreeColGameObject(getStringAttribute(SECOND_TAG), Player.class);
    }
}
