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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when training a unit in Europe.
 */
public class TrainUnitInEuropeMessage extends AttributeMessage {

    public static final String TAG = "trainUnitInEurope";
    private static final String UNIT_TYPE_TAG = "unitType";


    /**
     * Create a new {@code TrainUnitInEuropeMessage} with the
     * supplied type.
     *
     * @param type The {@code UnitType} to train.
     */
    public TrainUnitInEuropeMessage(UnitType type) {
        super(TAG, UNIT_TYPE_TAG, type.getId());
    }

    /**
     * Create a new {@code TrainUnitInEuropeMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public TrainUnitInEuropeMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TYPE_TAG);
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
        final String typeId = getStringAttribute(UNIT_TYPE_TAG);
        
        UnitType type = freeColServer.getSpecification().getUnitType(typeId);
        if (type == null) {
            return serverPlayer.clientError("Not a unit type: " + typeId);
        }

        // Proceed to train a unit.
        return igc(freeColServer)
            .trainUnitInEurope(serverPlayer, type);
    }
}
