/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when declining to investigate strange mounds.
 */
public class DeclineMoundsMessage extends DOMMessage {

    /** The identifier of the unit that is exploring. */
    private final String unitId;

    /** The direction of exploration. */
    private final String directionString;


    /**
     * Create a new <code>DeclineMoundsMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is exploring.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public DeclineMoundsMessage(Unit unit, Direction direction) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>DeclineMoundsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DeclineMoundsMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }


    /**
     * Handle a "declineMounds"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> that sent the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return An <code>Element</code> to update the originating
     *     player with the result of the demand.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        LostCityRumour rumour = tile.getLostCityRumour();
        if (rumour == null
            || rumour.getType() != LostCityRumour.RumourType.MOUNDS) {
            return DOMMessage.clientError("No mounds rumour on tile: "
                + tile.getId());
        }

        // Clear the mounds.
        return server.getInGameController()
            .declineMounds(serverPlayer, tile);
    }

    /**
     * Convert this DemandTributeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", unitId,
            "direction", directionString);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "declineMounds".
     */
    public static String getXMLElementTagName() {
        return "declineMounds";
    }
}
