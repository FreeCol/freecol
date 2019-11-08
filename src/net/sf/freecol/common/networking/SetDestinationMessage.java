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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when the client requests setting a unit destination.
 */
public class SetDestinationMessage extends AttributeMessage {

    public static final String TAG = "setDestination";
    private static final String DESTINATION_TAG = "destination";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code SetDestinationMessage} with the supplied unit
     * and destination.
     *
     * @param unit The {@code Unit} whose destination is to be set
     * @param destination The destination to set (may be null)
     */
    public SetDestinationMessage(Unit unit, Location destination) {
        super(TAG, UNIT_TAG, unit.getId(),
              DESTINATION_TAG, ((destination == null) ? null : destination.getId()));
    }

    /**
     * Create a new {@code SetDestinationMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetDestinationMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, DESTINATION_TAG);
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
        final Game game = freeColServer.getGame();
        final String unitId = getStringAttribute(UNIT_TAG);
        final String destinationId = getStringAttribute(DESTINATION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        // destination == null is OK.
        Location destination = (destinationId == null) ? null
            : game.findFreeColLocation(destinationId);

        // Set destination
        return igc(freeColServer)
            .setDestination(serverPlayer, unit, destination);
    }
}
