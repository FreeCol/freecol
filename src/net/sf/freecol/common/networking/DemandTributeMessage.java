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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when demanding tribute from a native settlement.
 */
public class DemandTributeMessage extends DOMMessage {

    public static final String TAG = "demandTribute";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the object demanding tribute. */
    private final String unitId;

    /** The direction the demand is made. */
    private final String directionString;


    /**
     * Create a new <code>DemandTributeMessage</code> with the
     * supplied unit and direction.
     *
     * @param unit The <code>Unit</code> that is demanding.
     * @param direction The <code>Direction</code> the unit is looking.
     */
    public DemandTributeMessage(Unit unit, Direction direction) {
        super(getTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
    }

    /**
     * Create a new <code>DemandTributeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DemandTributeMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.directionString = getStringAttribute(element, DIRECTION_TAG);
    }


    /**
     * Handle a "demandTribute"-message.
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
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (unit.isArmed()
            || unit.hasAbility(Ability.DEMAND_TRIBUTE)) {
            ; // ok
        } else {
            return serverPlayer.clientError("Unit is neither armed"
                + " nor able to demand tribute: " + unitId)
                .build(serverPlayer);
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(this.directionString);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        ServerIndianSettlement settlement
            = (ServerIndianSettlement)tile.getIndianSettlement();
        if (settlement == null) {
            return serverPlayer.clientError("There is native settlement at: "
                + tile.getId())
                .build(serverPlayer);
        }

        MoveType type = unit.getMoveType(tile);
        if (type != MoveType.ATTACK_SETTLEMENT
            && type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
            return serverPlayer.clientError("Unable to demand tribute at: "
                + settlement.getName() + ": " + type.whyIllegal())
                .build(serverPlayer);
        }

        // Do the demand
        return server.getInGameController()
            .demandTribute(serverPlayer, unit, settlement)
            .build(serverPlayer);
    }

    /**
     * Convert this DemandTributeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            DIRECTION_TAG, this.directionString).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "demandTribute".
     */
    public static String getTagName() {
        return "demandTribute";
    }
}
