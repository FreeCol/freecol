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
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when attacking.
 */
public class AttackMessage extends DOMMessage {

    /** The identifier of the attacker. */
    private final String unitId;

    /** The direction to attack. */
    private final String directionString;


    /**
     * Create a new <code>AttackMessage</code> for the supplied unit and
     * direction.
     *
     * @param unit The <code>Unit</code> attacking.
     * @param direction The <code>Direction</code> to attack in.
     */
    public AttackMessage(Unit unit, Direction direction) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>AttackMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public AttackMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.directionString = element.getAttribute("direction");
    }


    /**
     * Handle a "attack"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update encapsulating the attack or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        
        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        MoveType moveType = unit.getMoveType(tile);
        if (moveType == MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT
            || moveType == MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT
            || moveType.isAttack()) {
            ; // OK
        } else {
            return DOMMessage.clientError("Illegal attack move for: " + unitId
                + " type: " + moveType
                + " from: " + unit.getLocation().getId()
                + " to: " + tile.getId());
        }

        Unit defender = tile.getDefendingUnit(unit);
        if (defender == null) {
            return DOMMessage.clientError("Could not find defender"
                + " in tile: " + tile.getId()
                + " from: " + unit.getLocation().getId());
        }

        // Proceed to attack.
        return server.getInGameController()
            .combat(serverPlayer, unit, defender, null);
    }

    /**
     * Convert this AttackMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", this.unitId,
            "direction", this.directionString);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "attack".
     */
    public static String getXMLElementTagName() {
        return "attack";
    }
}
