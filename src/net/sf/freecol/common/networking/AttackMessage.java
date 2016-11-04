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
public class AttackMessage extends AttributeMessage {

    public static final String TAG = "attack";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code AttackMessage} for the supplied unit and
     * direction.
     *
     * @param unit The {@code Unit} attacking.
     * @param direction The {@code Direction} to attack in.
     */
    public AttackMessage(Unit unit, Direction direction) {
        super(TAG, UNIT_TAG, unit.getId(),
              DIRECTION_TAG, String.valueOf(direction));
    }

    /**
     * Create a new {@code AttackMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AttackMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              DIRECTION_TAG, getStringAttribute(element, DIRECTION_TAG));
    }


    /**
     * Handle a "attack"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param serverPlayer The {@code ServerPlayer} the message applies to.
     * @return An update encapsulating the attack or an error
     *     {@code Element} on failure.
     */
    public Element handle(FreeColServer server, ServerPlayer serverPlayer) {
        final String unitId = getAttribute(UNIT_TAG);
        final String directionString = getAttribute(DIRECTION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        MoveType moveType = unit.getMoveType(tile);
        if (moveType == MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT
            || moveType == MoveType.ENTER_FOREIGN_COLONY_WITH_SCOUT
            || moveType.isAttack()) {
            ; // OK
        } else {
            return serverPlayer.clientError("Illegal attack move for: "
                + unitId
                + " type: " + moveType
                + " from: " + unit.getLocation().getId()
                + " to: " + tile.getId())
                .build(serverPlayer);
        }

        Unit defender = tile.getDefendingUnit(unit);
        if (defender == null) {
            return serverPlayer.clientError("Could not find defender"
                + " in tile: " + tile.getId()
                + " from: " + unit.getLocation().getId())
                .build(serverPlayer);
        }

        // Proceed to attack.
        return server.getInGameController()
            .combat(serverPlayer, unit, defender, null)
            .build(serverPlayer);
    }
}
