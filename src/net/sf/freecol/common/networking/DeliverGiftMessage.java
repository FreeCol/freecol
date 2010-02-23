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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when delivering a gift to a Settlement.
 */
public class DeliverGiftMessage extends Message {
    /**
     * The ID of the unit that is delivering the gift.
     */
    private String unitId;

    /**
     * The ID of the settlement the gift is going to.
     */
    private String settlementId;

    /**
     * The goods to be delivered.
     */
    private Goods goods;

    /**
     * Create a new <code>DeliverGiftMessage</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param settlement The <code>Settlement</code> that is trading.
     * @param goods The <code>Goods</code> to deliverGift.
     */
    public DeliverGiftMessage(Unit unit, Settlement settlement, Goods goods) {
        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.goods = goods;
    }

    /**
     * Create a new <code>DeliverGiftMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public DeliverGiftMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.settlementId = element.getAttribute("settlement");
        this.goods = new Goods(game, Message.getChildElement(element, Goods.getXMLElementTagName()));
    }

    /**
     * Get the <code>Unit</code> which is delivering the gift.
     * This is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @return The unit, or null if none.
     */
    public Unit getUnit() {
        try {
            return (Unit) goods.getGame().getFreeColGameObject(unitId);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Get the <code>Settlement</code> which is receiving the gift.
     * This is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @return The settlement, or null if none.
     */
    public Settlement getSettlement() {
        try {
            return (Settlement) goods.getGame().getFreeColGameObject(settlementId);
        } catch (Exception e) {
        }
        return null;
    }

    /**
     * Get the <code>Goods</code> delivered as a gift.
     * This is a helper routine to be called in-client as it blindly trusts
     * its field.
     *
     * @return The goods, or null if none.
     */
    public Goods getGoods() {
        return goods;
    }

    /**
     * Handle a "deliverGift"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the unit and settlement,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();
        Unit unit;
        Settlement settlement;

        try {
            unit = server.getUnitSafely(unitId, serverPlayer);
            settlement = server.getAdjacentSettlementSafely(settlementId, unit);
        } catch (Exception e) {
            return Message.clientError(e.getMessage());
        }

        // Make sure we are trying to deliver something that is there
        if (goods.getLocation() != unit) {
            return Message.createError("server.trade.noGoods", "deliverGift of non-existent goods");
        }

        InGameController controller = (InGameController) server.getController();
        if (!controller.isTransactionSessionOpen(unit, settlement)) {
            return Message.clientError("Trying to deliverGift without opening a transaction session");
        }
        java.util.Map<String,Object> session = controller.getTransactionSession(unit, settlement);

        IndianSettlement indianSettlement = null;
        if (settlement instanceof IndianSettlement) {
            indianSettlement = (IndianSettlement) settlement;
            indianSettlement.modifyAlarm(player, -indianSettlement.getPrice(goods) / 50);
        }

        server.getInGameController().moveGoods(goods, settlement);
        if (indianSettlement != null) {
            indianSettlement.updateWantedGoods();
            indianSettlement.getTile().updateIndianSettlementInformation(player);
        }

        session.put("actionTaken", true);
        session.put("canGift", false);
        session.put("hasSpaceLeft", unit.getSpaceLeft() != 0);

        ServerPlayer receiver = (ServerPlayer) settlement.getOwner();
        if (!receiver.isAI() && receiver.isConnected()
            && settlement instanceof Colony) {
            Element gift = Message.createNewRootElement("multiple");
            Document doc = gift.getOwnerDocument();
            Element update = doc.createElement("update");
            gift.appendChild(update);
            update.appendChild(unit.toXMLElement(receiver, doc, false, false));
            update.appendChild(settlement.toXMLElement(receiver, doc));
            Element messages = doc.createElement("addMessages");
            gift.appendChild(messages);
            ModelMessage m
                = new ModelMessage(ModelMessage.MessageType.GIFT_GOODS,
                                   "model.unit.gift", settlement, goods.getType())
                .addStringTemplate("%player%", player.getNationName())
                .add("%type%", goods.getNameKey())
                .addAmount("%amount%", goods.getAmount())
                .addName("%colony%", settlement.getName());
            m.addToOwnedElement(messages, receiver);
            try {
                receiver.getConnection().send(gift);
            } catch (IOException e) {
                logger.warning(e.getMessage());
            }
        }

        Element reply = Message.createNewRootElement("update");
        Document doc = reply.getOwnerDocument();
        reply.appendChild(unit.toXMLElement(player, doc));
        reply.appendChild(settlement.getTile().toXMLElement(player, doc, false, false));
        return reply;
    }

    /**
     * Convert this DeliverGiftMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("unit", unitId);
        result.setAttribute("settlement", settlementId);
        result.appendChild(goods.toXMLElement(null, result.getOwnerDocument()));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "deliverGift".
     */
    public static String getXMLElementTagName() {
        return "deliverGift";
    }
}
