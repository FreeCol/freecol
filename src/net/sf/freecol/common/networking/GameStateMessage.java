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
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.FreeColServer.ServerState;
import net.sf.freecol.server.model.ServerPlayer;


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
            setStringAttribute(STATE_TAG, serverState.toString());
        }
    }

    /**
     * Create a new {@code GameStateMessage} from a stream.
     *
     * @param game The {@code Game} to read within (unused, no game
     *     exists at this point).
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public GameStateMessage(Game game, FreeColXMLReader xr) {
        this(xr.getAttribute(STATE_TAG, ServerState.class, (ServerState)null));
    }
        

    /**
     * Get the game state.
     *
     * @return The game state, if present.
     */
    private ServerState getState() {
        return getEnumAttribute(STATE_TAG, ServerState.class,
                                (ServerState)null);
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
    public void clientHandler(FreeColClient freeColClient) {
        final ServerState state = getState();
        if (state != null) freeColClient.setServerState(state);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
        @SuppressWarnings("unused") ServerPlayer serverPlayer) {
        // Called from UserConnectionHandler, without serverPlayer
        // being defined
        return igc(freeColServer)
            .gameState();
    }
}
