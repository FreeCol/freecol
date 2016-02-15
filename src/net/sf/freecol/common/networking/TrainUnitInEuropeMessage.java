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
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when training a unit in Europe.
 */
public class TrainUnitInEuropeMessage extends DOMMessage {

    public static final String TAG = "trainUnitInEurope";
    private static final String UNIT_TYPE_TAG = "unitType";

    /** The identifier of the unit type. */
    private final String typeId;


    /**
     * Create a new <code>TrainUnitInEuropeMessage</code> with the
     * supplied type.
     *
     * @param type The <code>UnitType</code> to train.
     */
    public TrainUnitInEuropeMessage(UnitType type) {
        super(getTagName());

        this.typeId = type.getId();
    }

    /**
     * Create a new <code>TrainUnitInEuropeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public TrainUnitInEuropeMessage(Game game, Element element) {
        super(getTagName());

        this.typeId = getStringAttribute(element, UNIT_TYPE_TAG);
    }


    /**
     * Handle a "trainUnitInEurope"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the trainUnitInEuroped unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        UnitType type = server.getSpecification().getUnitType(this.typeId);
        if (type == null) {
            return serverPlayer.clientError("Not a unit type: " + this.typeId)
                .build(serverPlayer);
        }

        // Proceed to train a unit.
        return server.getInGameController()
            .trainUnitInEurope(serverPlayer, type)
            .build(serverPlayer);
    }

    /**
     * Convert this TrainUnitInEuropeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TYPE_TAG, typeId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "trainUnitInEurope".
     */
    public static String getTagName() {
        return TAG;
    }
}
