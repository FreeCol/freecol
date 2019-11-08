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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when moving a unit across the high seas.
 */
public class MoveToMessage extends AttributeMessage {

    public static final String TAG = "moveTo";
    private static final String DESTINATION_TAG = "destination";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code MoveToMessage} for the supplied unit
     * and destination.
     *
     * @param unit The {@code Unit} to move.
     * @param destination The {@code Location} to move to.
     */
    public MoveToMessage(Unit unit, Location destination) {
        super(TAG, UNIT_TAG, unit.getId(),
              DESTINATION_TAG, destination.getId());
    }

    /**
     * Create a new {@code MoveToMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public MoveToMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, DESTINATION_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
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
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = serverPlayer.getGame();
        final String unitId = getStringAttribute(UNIT_TAG);
        final String destinationId = getStringAttribute(DESTINATION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Location destination = game.findFreeColLocation(destinationId);
        if (destination == null) {
            return serverPlayer.clientError("Not a location: "
                + destinationId);
        }

        // Proceed to move.
        return igc(freeColServer)
            .moveTo(serverPlayer, unit, destination);
    }
}
