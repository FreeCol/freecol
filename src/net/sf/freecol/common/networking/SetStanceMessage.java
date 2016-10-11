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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
     * Create a new {@code SetStanceMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SetStanceMessage(Game game, Element element) {
        super(TAG, STANCE_TAG, getStringAttribute(element, STANCE_TAG),
              FIRST_TAG, getStringAttribute(element, FIRST_TAG),
              SECOND_TAG, getStringAttribute(element, SECOND_TAG));
    }


    // Public interface

    /**
     * Get the stance that changed.
     *
     * @return The {@code Stance} value.
     */
    public Stance getStance() {
        return Enum.valueOf(Stance.class, getAttribute(STANCE_TAG));
    }

    /**
     * Which player is changing stance?
     *
     * @param game The {@code Game} the player is in.
     * @return The player whose stance changes.
     */
    public Player getFirstPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(FIRST_TAG), Player.class);
    }

    /**
     * Which player is the stance changed with respect to?
     *
     * @param game The {@code Game} the player is in.
     * @return The player the stance changed to.
     */
    public Player getSecondPlayer(Game game) {
        return game.getFreeColGameObject(getAttribute(SECOND_TAG), Player.class);
    }

    // No server handle() method required.
    // This message is only sent to clients.
}
