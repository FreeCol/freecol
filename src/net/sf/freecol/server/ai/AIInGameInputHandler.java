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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.MessageHandler;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.FreeColServerHolder;


/**
 * Handles the network messages that arrives while in the game.
 */
public final class AIInGameInputHandler extends FreeColServerHolder
    implements MessageHandler {

    private static final Logger logger = Logger.getLogger(AIInGameInputHandler.class.getName());

    /** The player for whom I work. */
    private final Player player;

    /** The main AI object. */
    private final AIMain aiMain;


    /**
     * The constructor to use.
     *
     * @param freeColServer The main server.
     * @param player The {@code Player} to manage.
     * @param aiMain The main AI-object.
     */
    public AIInGameInputHandler(FreeColServer freeColServer,
                                Player player, AIMain aiMain) {
        super(freeColServer);
        
        if (player == null) {
            throw new NullPointerException("serverPlayer == null: " + this);
        } else if (!player.isAI()) {
            throw new RuntimeException("Applying AIInGameInputHandler to a non-AI player: " + player);
        } else if (aiMain == null) {
            throw new NullPointerException("aiMain == null: " + this);
        }

        // FIXME: Do not precalculate the AIPlayer, it may still be
        // being initialized
        this.player = player;
        this.aiMain = aiMain;
    }

    /**
     * Get the AI player using this handler.
     *
     * @return The {@code AIPlayer}.
     */
    private AIPlayer getMyAIPlayer() {
        return this.aiMain.getAIPlayer(this.player);
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
