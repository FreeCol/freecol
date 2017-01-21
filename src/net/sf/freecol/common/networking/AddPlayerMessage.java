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

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent to add or update players to the game.
 */
public class AddPlayerMessage extends DOMMessage {

    public static final String TAG = "addPlayer";

    /** The players to add. */
    private final List<Player> players = new ArrayList<>();


    /**
     * Create a new {@code AddPlayerMessage}.
     */
    public AddPlayerMessage() {
        super(TAG);

        this.players.clear();
    }

    /**
     * Create a new {@code AddPlayerMessage} from a supplied
     * element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AddPlayerMessage(Game game, Element element) {
        this();

        // Making this message implicitly updates the game.
        // TODO: should this do a non-interning read and have the client
        // handlers do more checking?
        this.players.addAll(DOMUtils.mapChildren(element, (e) ->
                DOMUtils.readGameElement(game, e, true, Player.class)));
    }


    // Public interface

    /**
     * Get the attached players.
     *
     * @return The list of {@code Player}s to add.
     */
    public List<Player> getPlayers() {
        return this.players;
    }

    /**
     * Add a player to the message.
     *
     * @param p The {@code Player} to add.
     * @return This message.
     */
    public AddPlayerMessage addPlayer(Player p) {
        this.players.add(p);
        return this;
    }


    /**
     * Handle a "addPlayer"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param serverPlayer The {@code ServerPlayer} the message applies to.
     * @return Null.
     */
    public Element handle(FreeColServer server, ServerPlayer serverPlayer) {
        // Only sent by the server to the clients.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG)
            .add(this.players).toXMLElement();
    }
}
