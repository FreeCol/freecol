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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to discover vacant players in a game.
 */
public class VacantPlayersMessage extends AttributeMessage {

    public static final String TAG = "vacantPlayers";
    

    /**
     * Create a new {@code VacantPlayersMessage}.
     */
    public VacantPlayersMessage() {
        super(TAG);
    }

    /**
     * Create a new {@code VacantPlayersMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public VacantPlayersMessage(Game game, FreeColXMLReader xr) {
        super(TAG, xr.getArrayAttributeMap());
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
        final List<String> vacant = getVacantPlayers();

        freeColClient.setVacantPlayerNames(vacant);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
        @SuppressWarnings("unused") ServerPlayer serverPlayer) {
        // Called from UserConnectionHandler, without serverPlayer being defined
        return pgc(freeColServer)
            .vacantPlayers();
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

    /**
     * Set the vacant players in this message from a given game.
     *
     * @param game The {@code Game} to find players in.
     * @return This message.
     */
    public VacantPlayersMessage setVacantPlayers(Game game) {
        if (game == null) return this;
        final Predicate<Player> vacantPred = p ->
            !p.isREF() && (p.isAI() || !p.isConnected());
        setArrayAttributes(transform(game.getLiveEuropeanPlayers(),
                                     vacantPred, Player::getNationId));
        return this;
    }
}
