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
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.FreeColServer.ServerState;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to check the game state.
 */
public class GameStateMessage extends AttributeMessage {

    public static final String TAG = "gameState";
    private static final String STATE_TAG = "state";


    /**
     * Create a new {@code GameStateMessage}.
     */
    public GameStateMessage() {
        super(TAG);
    }

    /**
     * Create a new {@code GameStateMessage} with a given state.
     *
     * @param serverState The {@code serverState} to send.
     */
    public GameStateMessage(ServerState serverState) {
        super(TAG);

        if (serverState != null) {
            setAttribute(STATE_TAG, serverState.toString());
        }
    }

    /**
     * Create a new {@code GameStateMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public GameStateMessage(Game game, Element element) {
        this(getEnumAttribute(element, STATE_TAG,
                              ServerState.class, (ServerState)null));
    }


    // Public interface

    public ServerState getState() {
        return Enum.valueOf(ServerState.class, getAttribute(STATE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
        @SuppressWarnings("unused") ServerPlayer serverPlayer) {
        // Called from UserConnectionHandler, without serverPlayer being defined
        return ChangeSet.simpleChange((ServerPlayer)null,
            new GameStateMessage(freeColServer.getServerState()));
    }
}
