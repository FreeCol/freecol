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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
     * Create a new {@code TrainUnitInEuropeMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public TrainUnitInEuropeMessage(Game game, Element element) {
        super(TAG, UNIT_TYPE_TAG, getStringAttribute(element, UNIT_TYPE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String typeId = getAttribute(UNIT_TYPE_TAG);
        
        UnitType type = freeColServer.getSpecification().getUnitType(typeId);
        if (type == null) {
            return serverPlayer.clientError("Not a unit type: " + typeId);
        }

        // Proceed to train a unit.
        return freeColServer.getInGameController()
            .trainUnitInEurope(serverPlayer, type);
    }
}
