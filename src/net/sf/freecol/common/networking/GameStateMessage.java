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
import net.sf.freecol.server.FreeColServer.GameState;

import org.w3c.dom.Element;


/**
 * The message sent to check the game state.
 */
public class GameStateMessage extends DOMMessage {

    public static final String TAG = "gameState";
    private static final String STATE_TAG = "state";

    /** The string value of the game state. */
    public String state;


    /**
     * Create a new {@code GameStateMessage}.
     */
    public GameStateMessage() {
        super(getTagName());

        this.state = null;
    }

    /**
     * Create a new {@code GameStateMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public GameStateMessage(Game game, Element element) {
        this();

        this.state = getStringAttribute(element, STATE_TAG);
    }


    // Public interface

    public GameState getGameState() {
        return Enum.valueOf(GameState.class, this.state);
    }


    /**
     * Handle a "gameState"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param connection The {@code Connection} message was received on.
     * @return An {@code Element} to update the originating player
     *     with the result of the query.
     */
    public Element handle(FreeColServer server, Connection connection) {
        this.state = server.getGameState().toString();
        return this.toXMLElement();
    }


    /**
     * Convert this GameStateMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            STATE_TAG, this.state).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "gameState".
     */
    public static String getTagName() {
        return TAG;
    }
}
