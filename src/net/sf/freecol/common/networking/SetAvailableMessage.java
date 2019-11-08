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
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that changes the player availability.
 */
public class SetAvailableMessage extends AttributeMessage {

    public static final String TAG = "setAvailable";
    private static final String NATION_TAG = "nation";
    private static final String STATE_TAG = "state";
    

    /**
     * Create a new {@code SetAvailableMessage}.
     *
     * @param nation The {@code Nation} to make available.
     * @param state The {@code NationState} for that nation.
     */
    public SetAvailableMessage(Nation nation, NationState state) {
        super(TAG, NATION_TAG, nation.getId(),
              STATE_TAG, state.toString());
    }

    /**
     * Create a new {@code SetAvailableMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetAvailableMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, NATION_TAG, STATE_TAG);
    }
    

    /**
     * {@inheritDoc}
     */
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final Nation nation = getNation(spec);
        final NationState nationState = getNationState();

        pgc(freeColClient).setAvailableHandler(nation, nationState);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            logger.warning("setAvailable from unknown player");
        }
        
        final Game game = freeColServer.getGame();
        final Specification spec = game.getSpecification();
        final Nation nation = getNation(spec);
        final NationState state = getNationState();

        return pgc(freeColServer)
            .setAvailable(serverPlayer, nation, state);
    }


    // Public interface

    /**
     * Get the nation whose availability is changing.
     *
     * @param spec The {@code Specification} to look up the nation in.
     * @return The {@code Nation} found.
     */
    public Nation getNation(Specification spec) {
        return spec.getNation(getStringAttribute(NATION_TAG));
    }

    /**
     * Get the new nation availabiility.
     *
     * @return The new {@code NationState}.
     */
    public NationState getNationState() {
        return Enum.valueOf(NationState.class, getStringAttribute(STATE_TAG));
    }
}
