/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.util.Map;

import org.w3c.dom.Element;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to initiate a transaction.
 */
public class GetTransactionMessage extends Message {
    /**
     * The ID of the unit performing the transaction.
     */
    private String unitId;

    /**
     * The ID of the settlement at which the transaction occurs.
     */
    private String settlementId;

    /**
     * Create a new <code>GetTransactionMessage</code> with the
     * supplied unit and settlement.
     *
     * @param unit The <code>Unit</code> performing the transaction.
     * @param settlement The <code>Settlement</code> where the
     *        transaction occurs.
     */
    public GetTransactionMessage(Unit unit, Settlement settlement) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
    }

    /**
     * Create a new <code>GetTransactionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public GetTransactionMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
    }

    /**
     * Handle a "getTransaction"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return A reply encapsulating the possibilities for this transaction,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        IndianSettlement settlement;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
            settlement = server.getAdjacentIndianSettlementSafely(settlementId, unit);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }

        MoveType type = unit.getSimpleMoveType(settlement.getTile());
        if (type != MoveType.ENTER_SETTLEMENT_WITH_CARRIER_AND_GOODS) {
            return Message.clientError("Unable to enter "
                                       + settlement.getName()
                                       + ": " + type.whyIllegal());
        }

        // If starting a transaction session, the unit needs movement points
        InGameController igc = server.getInGameController();
        if (!igc.isTransactionSessionOpen(unit, settlement)
            && unit.getMovesLeft() <= 0) {
            return Message.clientError("Unit " + unitId + "has 0 moves left.");
        }

        java.util.Map<String,Object> session
            = igc.getTransactionSession(unit, settlement);
        Element reply = Message.createNewRootElement("getTransactionAnswer");
        reply.setAttribute("canBuy", ((Boolean) session.get("canBuy")).toString());
        reply.setAttribute("canSell", ((Boolean) session.get("canSell")).toString());
        reply.setAttribute("canGift", ((Boolean) session.get("canGift")).toString());

        // Sets unit moves to zero to avoid cheating.
        // If no action was done, the moves will be restored when closing
        // the session
        unit.setMovesLeft(0);

        return reply;
    }

    /**
     * Convert this GetTransactionMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "getTransaction".
     */
    public static String getXMLElementTagName() {
        return "getTransaction";
    }
}
