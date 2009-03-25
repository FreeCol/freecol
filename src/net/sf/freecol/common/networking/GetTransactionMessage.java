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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
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
     * @return A reply encapsulating the possibilities for this transaction.
     * @throws IllegalStateException if there is problem with the message
     *         arguments.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = player.getGame();
        Unit unit = server.getUnitSafely(unitId, serverPlayer);
        Settlement settlement = server.getAdjacentIndianSettlementSafely(settlementId, unit);
        InGameController controller = (InGameController) server.getController();

        // if starting a transaction session, the unit needs movement points
        if (!controller.isTransactionSessionOpen(unit, settlement)
            && unit.getMovesLeft() <= 0) {
            throw new IllegalStateException("No moves left!");
        }
        // Sets unit moves to zero to avoid cheating
        // If no action was done, the moves will be restored when closing
        // the session
        unit.setMovesLeft(0);

        java.util.Map<String,Object> session = controller.getTransactionSession(unit, settlement);
        Element reply = Message.createNewRootElement("getTransactionAnswer");
        reply.setAttribute("canBuy", ((Boolean) session.get("canBuy")).toString());
        reply.setAttribute("canSell", ((Boolean) session.get("canSell")).toString());
        reply.setAttribute("canGift", ((Boolean) session.get("canGift")).toString());
        reply.setAttribute("hasSpaceLeft", ((Boolean) session.get("hasSpaceLeft")).toString());
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
