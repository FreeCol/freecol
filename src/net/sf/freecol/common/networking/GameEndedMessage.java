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
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message to signal the end of the game.
 */
public class GameEndedMessage extends AttributeMessage {

    public static final String TAG = "gameEnded";
    private static final String HIGH_SCORE_TAG = "highScore";
    private static final String WINNER_TAG = "winner";


    /**
     * Create a new {@code GameEndedMessage} with the supplied winner.
     *
     * @param winner The {@code Player} that has won.
     * @param highScore True if a new high score was reached.
     */
    public GameEndedMessage(Player winner, boolean highScore) {
        super(TAG, WINNER_TAG, winner.getId(),
              HIGH_SCORE_TAG, String.valueOf(highScore));
    }

    /**
     * Create a new {@code GameEndedMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public GameEndedMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, WINNER_TAG, HIGH_SCORE_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
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
        final Player winner = getWinner(game);
        final String highScore = getScore();

        if (winner == null) {
            logger.warning("Invalid player for gameEnded");
            return;
        }
        FreeColDebugger.finishDebugRun(freeColClient, true);
        if (winner != freeColClient.getMyPlayer()) return;
        
        igc(freeColClient).gameEndedHandler(highScore);
    }

    
    // Public interface

    /**
     * Who won?
     *
     * @param game The {@code Game} the winner is in.
     * @return The {@code Player} that won.
     */
    public Player getWinner(Game game) {
        return game.getFreeColGameObject(getStringAttribute(WINNER_TAG), Player.class);
    }

    /**
     * Get the high score attribute.
     *
     * Note: *not* a boolean, due to a kludge in client.IGIH.
     *
     * @return The score attribute.
     */
    public String getScore() {
        return getStringAttribute(HIGH_SCORE_TAG);
    }

    // No server handler method required.
    // This message is only sent to clients.
}
