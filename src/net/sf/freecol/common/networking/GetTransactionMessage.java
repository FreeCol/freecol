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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to initiate a transaction.
 */
public class GetTransactionMessage extends DOMMessage {

    /** The object identifier of the unit performing the transaction. */
    private final String unitId;

    /**
     * The object identifier of the settlement at which the
     * transaction occurs.
     */
    private final String settlementId;


    /**
     * Create a new <code>GetTransactionMessage</code> with the
     * supplied unit and settlement.
     *
     * @param unit The <code>Unit</code> performing the transaction.
     * @param settlement The <code>Settlement</code> where the
     *        transaction occurs.
     */
    public GetTransactionMessage(Unit unit, Settlement settlement) {
        super(getXMLElementTagName());

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
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
    }


    /**
     * Handle a "getTransaction"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return A reply encapsulating the possibilities for this
     *     transaction, or an error <code>Element</code> on failure.
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

        Settlement settlement;
        try {
            settlement = unit.getAdjacentSettlementSafely(settlementId);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        return server.getInGameController()
            .getTransaction(serverPlayer, unit, settlement);
    }

    /**
     * Convert this GetTransactionMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", unitId,
            "settlement", settlementId);
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
