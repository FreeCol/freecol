/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when natives are making demands of a colony.
 */
public class IndianDemandMessage extends Message {

    // The id of the unit that is demanding.
    private String unitId;

    // The id of the colony being demanded of.
    private String colonyId;

    // The goods being demanded.
    private Goods goods;

    // The gold being demanded.
    private String goldString;


    /**
     * Create a new <code>IndianDemandMessage</code> with the
     * supplied unit, colony and demands.
     *
     * @param unit The <code>Unit</code> that is demanding.
     * @param colony The <code>Colony</code> being demanded of.
     * @param goods The <code>Goods</code> being demanded.
     * @param gold The gold being demanded.
     */
    public IndianDemandMessage(Unit unit, Colony colony, Goods goods,
                               int gold) {
        this.unitId = unit.getId();
        this.colonyId = colony.getId();
        this.goods = goods;
        this.goldString = (gold == 0) ? null : Integer.toString(gold);
    }

    /**
     * Create a new <code>IndianDemandMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public IndianDemandMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.colonyId = element.getAttribute("colony");
        this.goldString = (!element.hasAttribute("gold")) ? null
            : element.getAttribute("gold");
        this.goods = (!element.hasChildNodes()) ? null
            : new Goods(game, Message.getChildElement(element,
                    Goods.getXMLElementTagName()));
    }

    /**
     * Client-side convenience function to get the unit in this message.
     *
     * @param game The <code>Game</code> to look for the unit in.
     */
    public Unit getUnit(Game game) {
        return (Unit) game.getFreeColGameObjectSafely(unitId);
    }

    /**
     * Client-side convenience function to get the colony in this message.
     *
     * @param game The <code>Game</code> to look for the colony in.
     */
    public Colony getColony(Game game) {
        return (Colony) game.getFreeColGameObjectSafely(colonyId);
    }

    /**
     * Client-side convenience function to get the goods in this message.
     */
    public Goods getGoods() {
        return goods;
    }

    /**
     * Client-side convenience function to get the gold in this message.
     */
    public int getGold() {
        return (goldString == null) ? 0 : Integer.parseInt(goldString);
    }

    /**
     * Handle a "indianDemand"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the indianDemandd unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        if (unit.getMovesLeft() <= 0) {
            return Message.clientError("Unit has no moves left: " + unitId);
        }
        Colony colony;
        try {
            Settlement settlement
                = server.getAdjacentSettlementSafely(colonyId, unit);
            if (settlement instanceof Colony) {
                colony = (Colony) settlement;
            } else {
                return Message.clientError("Not a colony: " + colonyId);
            }
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }
        int gold = 0;
        if (goods != null) {
            if (goods.getLocation() != colony) {
                return Message.clientError("Demand of goods that are not in colony: " + colonyId);
            }
        } else if (goldString != null) {
            try {
                gold = Integer.parseInt(goldString);
            } catch (NumberFormatException e) {
                return Message.clientError(e.getMessage());
            }
            if (gold <= 0) {
                return Message.clientError("Bad gold: " + goldString);
            }
        } else {
            return Message.clientError("Both goods and gold can not be empty");
        }

        // Proceed to indianDemand.
        return server.getInGameController()
            .indianDemand(serverPlayer, unit, colony, goods, gold);
    }

    /**
     * Convert this IndianDemandMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("colony", colonyId);
        if (goldString != null) result.setAttribute("gold", goldString);
        if (goods != null) {
            result.appendChild(goods.toXMLElement(null, result.getOwnerDocument()));
        }
        return result;
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
