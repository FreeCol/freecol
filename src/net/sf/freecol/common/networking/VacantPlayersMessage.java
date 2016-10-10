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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to discover the vacant players.
 */
public class VacantPlayersMessage extends TrivialMessage {

    public static final String TAG = "vacantPlayers";
    

    /**
     * Create a new {@code VacantPlayersMessage}.
     */
    public VacantPlayersMessage() {
        super(TAG);
    }

    /**
     * Create a new {@code VacantPlayersMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public VacantPlayersMessage(Game game, Element element) {
        super(TAG);

        setArrayAttributes(getArrayAttributes(element));
    }


    // Public interface

    /**
     * Get the vacant players.
     *
     * @return A list of vacant code player identifiers.
     */
    public List<String> getVacantPlayers() {
        return getArrayAttributes();
    }


    // Implement MessageHandler

    /**
     * Handle a "vacantPlayers"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param connection The {@code Connection} message was received on.
     *
     * @return An {@code Element} to update the originating player
     *     with the result of the query.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final Game game = server.getGame();

        final Predicate<Player> vacantPred = p ->
            !p.isREF() && (p.isAI() || !((ServerPlayer)p).isConnected());
        setArrayAttributes(transform(game.getLiveEuropeanPlayers(),
                                     vacantPred, Player::getNationId));
        return this.toXMLElement();
    }
}
