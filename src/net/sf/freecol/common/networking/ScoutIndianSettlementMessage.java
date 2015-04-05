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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when scouting a native settlement.
 */
public class ScoutIndianSettlementMessage extends DOMMessage {

    /** The identifier of the unit that is scouting. */
    private final String unitId;

    /** The direction of the settlement from the unit. */
    private final String directionString;


    /**
     * Create a new <code>ScoutIndianSettlementMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is learning.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public ScoutIndianSettlementMessage(Unit unit, Direction direction) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>ScoutIndianSettlementMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ScoutIndianSettlementMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
    }


    /**
     * Handle a "scoutIndianSettlement"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An element containing the result of the scouting
     *     action, or an error <code>Element</code> on failure.
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
        if (!unit.hasAbility(Ability.SPEAK_WITH_CHIEF)) {
            return DOMMessage.clientError("Unit lacks ability"
                + " to speak to chief: " + unitId);
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        IndianSettlement is = tile.getIndianSettlement();
        if (is == null) {
            return DOMMessage.clientError("There is no native settlement at: "
                + tile.getId());
        }

        MoveType type = unit.getMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
            return DOMMessage.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        // Valid request, do the scouting.
        return server.getInGameController()
            .scoutIndianSettlement(serverPlayer, unit, is);
    }

    /**
     * Convert this ScoutIndianSettlementMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unitId", unitId,
            "direction", directionString);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "scoutIndianSettlement".
     */
    public static String getXMLElementTagName() {
        return "scoutIndianSettlement";
    }
}
