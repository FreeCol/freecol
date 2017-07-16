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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.FreeColServerHolder;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * Handles the network messages that arrives while in the game.
 */
public final class AIInGameInputHandler extends FreeColServerHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    /** The player for whom I work. */
    private final ServerPlayer serverPlayer;

    /** The main AI object. */
    private final AIMain aiMain;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server.
     * @param serverPlayer The {@code ServerPlayer} that is being
     *     managed by this AIInGameInputHandler.
     * @param aiMain The main AI-object.
     */
    public AIInGameInputHandler(FreeColServer freeColServer,
                                ServerPlayer serverPlayer,
                                AIMain aiMain) {
        super(freeColServer);
        
        if (serverPlayer == null) {
            throw new NullPointerException("serverPlayer == null");
        } else if (!serverPlayer.isAI()) {
            throw new RuntimeException("Applying AIInGameInputHandler to a non-AI player!");
        } else if (aiMain == null) {
            throw new NullPointerException("aiMain == null");
        }

        // FIXME: Do not precalculate the AIPlayer, it may still be being initialized
        this.serverPlayer = serverPlayer;
        this.aiMain = aiMain;
    }

    /**
     * Get the AI player using this handler.
     *
     * @return The {@code AIPlayer}.
     */
    private AIPlayer getMyAIPlayer() {
        return this.aiMain.getAIPlayer(this.serverPlayer);
    }


    // Implement MessageHandler

    /**
     * {@inheritDoc}
     */
    public Message handle(Connection connection, Message message)
        throws FreeColException {
        message.aiHandler(getFreeColServer(), getMyAIPlayer());
        return null; 
    }

    /**
     * {@inheritDoc}
     */
    public Message read(Connection connection)
        throws FreeColException, XMLStreamException {
        return Message.read(getGame(), connection.getFreeColXMLReader());
    }
}
