/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.server.control;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.DOMMessage;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The control object that is responsible for setting parameters
 * and starting a new game. {@link PreGameInputHandler} is used
 * to receive and handle network messages from the clients.
 *
 * The game enters the state
 * {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME}, when the
 * {@link #startGame} has successfully been invoked.
 *
 * @see InGameInputHandler
 */
public final class PreGameController extends Controller {

    private static final Logger logger = Logger.getLogger(PreGameController.class.getName());

    /**
     * The constructor to use.
     *
     * @param freeColServer The main <code>FreeColServer</code> object.
     */
    public PreGameController(FreeColServer freeColServer) {
        super(freeColServer);
    }

    /**
     * Updates and starts the new game.
     *
     * Called in response to a requestLaunch message arriving at the 
     * PreGameInputHandler.
     *
     * <ol>
     *   <li>Creates the game.
     *   <li>Sends updated game information to the clients.
     *   <li>Changes the game state to {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME}.
     *   <li>Sends the "startGame"-message to the clients.
     * </ol>
     *
     * @exception FreeColException if there is an error building the game.
     */
    public void startGame() throws FreeColException {
        final FreeColServer freeColServer = getFreeColServer();
        Game game = freeColServer.buildGame();

        // Inform the clients.
        for (Player player : game.getLivePlayers(null)) {
            if (player.isAI()) continue;

            player.invalidateCanSeeTiles();//Send clean copy of the game
            Connection conn = ((ServerPlayer)player).getConnection();
            Element update = DOMMessage.createMessage("updateGame");
            update.appendChild(game.toXMLElement(update.getOwnerDocument(),
                                                 player));
            try {
                conn.ask(update);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Unable to updateGame", e);
            }
        }

        // Start the game:
        freeColServer.setGameState(FreeColServer.GameState.IN_GAME);
        freeColServer.updateMetaServer();
        freeColServer.getServer()
            .sendToAll(DOMMessage.createMessage("startGame"));
        freeColServer.getServer().setMessageHandlerToAllConnections(freeColServer.getInGameInputHandler());
    }
}
