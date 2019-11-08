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
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when paying for a building.
 */
public class PayForBuildingMessage extends AttributeMessage {

    public static final String TAG = "payForBuilding";
    private static final String COLONY_TAG = "colony";


    /**
     * Create a new {@code PayForBuildingMessage} with the
     * supplied colony.
     *
     * @param colony The {@code Colony} that is building.
     */
    public PayForBuildingMessage(Colony colony) {
        super(TAG, COLONY_TAG, colony.getId());
    }

    /**
     * Create a new {@code PayForBuildingMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public PayForBuildingMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, COLONY_TAG);
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
        final String colonyId = getStringAttribute(COLONY_TAG);
        
        Colony colony;
        try {
            colony = serverPlayer.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        // Proceed to pay.
        return igc(freeColServer)
            .payForBuilding(serverPlayer, colony);
    }
}
