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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to resolve natives making demands of a colony.
 */
public class IndianDemandMessage extends DOMMessage {

    /** The identifier of the unit that is demanding. */
    private final String unitId;

    /** The identifier of the colony being demanded of. */
    private final String colonyId;

    /** The type of goods being demanded, null implies gold. */
    private final String typeId;

    /** The amount of goods being demanded. */
    private final String amount;

    /** The result of this demand: null implies not decided yet. */
    private String result;


    /**
     * Create a new <code>IndianDemandMessage</code> with the
     * supplied unit, colony and demands.
     *
     * @param unit The <code>Unit</code> that is demanding.
     * @param colony The <code>Colony</code> being demanded of.
     * @param type The <code>GoodsType</code> being demanded.
     * @param amount The amount of goods being demanded.
     */
    public IndianDemandMessage(Unit unit, Colony colony,
                               GoodsType type, int amount) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.colonyId = colony.getId();
        this.typeId = (type == null) ? null : type.getId();
        this.amount = Integer.toString(amount);
        this.result = null;
    }

    /**
     * Create a new <code>IndianDemandMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public IndianDemandMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.colonyId = element.getAttribute("colony");
        this.typeId = (!element.hasAttribute("type")) ? null
            : element.getAttribute("type");
        this.amount = element.getAttribute("amount");
        this.result = element.getAttribute("result");
    }


    // Public interface

    /**
     * Client-side convenience function to get the unit in this message.
     *
     * @param game The <code>Game</code> to look for the unit in.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(unitId, Unit.class);
    }

    /**
     * Client-side convenience function to get the colony in this message.
     *
     * @param game The <code>Game</code> to look for the colony in.
     */
    public Colony getColony(Game game) {
        return game.getFreeColGameObject(colonyId, Colony.class);
    }

    /**
     * Client-side convenience function to get the goods type in this message.
     */
    public GoodsType getType(Game game) {
        return (typeId == null) ? null
            : game.getSpecification().getGoodsType(typeId);
    }

    /**
     * Client-side convenience function to get the gold in this message.
     */
    public int getAmount() {
        try {
            return Integer.parseInt(amount);
        } catch (NumberFormatException nfe) {}
        return -1;
    }

    /**
     * Client-side convenience function to set the result of this message.
     *
     * @return The result of this demand.
     */
    public boolean getResult() {
        return Boolean.parseBoolean(result);
    }

    /**
     * Client-side convenience function to set the result of this message.
     *
     * @param result The new result of this demand.
     */
    public void setResult(boolean result) {
        this.result = Boolean.toString(result);
    }


    /**
     * Handle a "indianDemand"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the indianDemandd unit, or an
     *     error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = player.getGame();

        Unit unit;
        try {
            if (result == null) { // Initial demand
                unit = player.getOurFreeColGameObject(unitId, Unit.class);
                if (unit.getMovesLeft() <= 0) {
                    return DOMMessage.clientError("Unit has no moves left: "
                        + unitId);
                }
            } else { // Reply from colony
                unit = game.getFreeColGameObject(unitId, Unit.class);
                if (unit == null) {
                    return DOMMessage.clientError("Not a unit: " + unitId);
                }
            }
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Colony colony;
        try {
            Settlement settlement
                = unit.getAdjacentSettlementSafely(colonyId);
            if (!(settlement instanceof Colony)) {
                return DOMMessage.clientError("Not a colony: " + colonyId);
            }
            colony = (Colony)settlement;
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        if (getAmount() <= 0) {
            return DOMMessage.clientError("Bad amount: " + amount);
        }

        // Proceed to demand.
        return server.getInGameController()
            .indianDemand(serverPlayer, unit, colony,
                          getType(game), getAmount());
    }

    /**
     * Convert this IndianDemandMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element ret = createMessage(getXMLElementTagName(),
            "unit", unitId,
            "colony", colonyId,
            "amount", amount);
        if (typeId != null) ret.setAttribute("type", typeId);
        if (result != null) ret.setAttribute("result", result);
        return ret;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "indianDemand".
     */
    public static String getXMLElementTagName() {
        return "indianDemand";
    }
}
