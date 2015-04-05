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
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when spying on a settlement.
 */
public class SpySettlementMessage extends DOMMessage {

    /** The identifier of the object doing the spying. */
    private final String unitId;

    /** The direction the spy is looking. */
    private final String directionString;


    /**
     * Create a new <code>SpySettlementMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is spying.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public SpySettlementMessage(Unit unit, Direction direction) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>SpySettlementMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SpySettlementMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }


    /**
     * Handle a "spySettlement"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling
     *     the message.
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @return An <code>Element</code> containing a representation of
     *     the settlement being spied upon and any units at that
     *     position, or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (!unit.hasAbility(Ability.SPY_ON_COLONY)) {
            return DOMMessage.clientError("Unit lacks ability"
                + " to spy on colony: " + unitId);
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Settlement settlement = tile.getSettlement();
        if (settlement == null) {
            return DOMMessage.clientError("There is no settlement at: "
                + tile.getId());
        }

        MoveType type = unit.getMoveType(settlement.getTile());
        if (type != MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT) {
            return DOMMessage.clientError("Unable to enter at: "
                + settlement.getName() + ": " + type.whyIllegal());
        }

        // Spy on the settlement
        return server.getInGameController()
            .spySettlement(serverPlayer, unit, settlement);
    }

    /**
     * Convert this SpySettlementMessage to XML.
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
     * @return "spySettlement".
     */
    public static String getXMLElementTagName() {
        return "spySettlement";
    }
}
