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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * The message sent when doing a monarch action.
 */
public class MonarchActionMessage extends Message {

    // The monarch action.
    private MonarchAction action;

    // The tax amount (optional).
    private String amountString;

    // The id of the goods to use in a tea party.
    private String goodsTypeId;

    // The id of a player the monarch has declared war on.
    private String enemyId;

    // Units added.
    private List<AbstractUnit> additions;


    /**
     * Create a new <code>MonarchActionMessage</code> with the given action.
     *
     * @param action The <code>MonarchAction</code> to do.
     */
    public MonarchActionMessage(MonarchAction action) {
        this.action = action;
        this.amountString = null;
        this.goodsTypeId = null;
        this.enemyId = null;
        this.additions = new ArrayList<AbstractUnit>();
    }

    /**
     * Create a new <code>MonarchActionMessage</code> for a RAISE_TAX action.
     *
     * @param tax The new tax rate.
     * @param goodsType The <code>GoodsType</code> to use in a tea party.
     */
    public MonarchActionMessage(int tax, GoodsType goodsType) {
        this(MonarchAction.RAISE_TAX);
        this.amountString = Integer.toString(tax);
        this.goodsTypeId = goodsType.getId();
    }

    /**
     * Create a new <code>MonarchActionMessage</code> for a LOWER_TAX action.
     *
     * @param tax The new tax rate.
     */
    public MonarchActionMessage(int tax) {
        this(MonarchAction.LOWER_TAX);
        this.amountString = Integer.toString(tax);
    }

    /**
     * Create a new <code>MonarchActionMessage</code> for a DECLARE_WAR action.
     *
     * @param enemy The new enemy.
     */
    public MonarchActionMessage(Player enemy) {
        this(MonarchAction.DECLARE_WAR);
        this.enemyId = enemy.getId();
    }

    /**
     * Create a new <code>MonarchActionMessage</code> for a the actions that
     * may add units.
     *
     * @param additions The extra units.
     */
    public MonarchActionMessage(MonarchAction action,
                                List<AbstractUnit> additions) {
        this(action);
        this.additions = additions;
    }

    /**
     * Create a new <code>MonarchActionMessage</code> for an OFFER_MERCENARIES
     * action.
     *
     * @param price The price to pay for the mercenaries.
     * @param additions The extra units.
     */
    public MonarchActionMessage(int price, List<AbstractUnit> additions) {
        this(MonarchAction.OFFER_MERCENARIES, additions);
        this.amountString = Integer.toString(price);
    }

    /**
     * Create a new <code>MonarchActionMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public MonarchActionMessage(Game game, Element element) {
        this.action = Enum.valueOf(MonarchAction.class,
                                   element.getAttribute("action"));
        this.amountString = element.getAttribute("amount");
        this.goodsTypeId = element.getAttribute("goods");
        this.enemyId = element.getAttribute("enemy");
        this.additions = new ArrayList<AbstractUnit>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            AbstractUnit au = new AbstractUnit();
            au.readFromXMLElement((Element) children.item(i));
            additions.add(au);
        }
    }

    /**
     * Gets the monarch action type of this message.
     *
     * @return The monarch action type.
     */
    public MonarchAction getAction() {
        return action;
    }

    /**
     * Gets the tax/price for this message.
     *
     * @return The amount, or negative on error.
     */
    public int getAmount() {
        int amount;
        try {
            amount = Integer.parseInt(amountString);
        } catch (NumberFormatException e) {
            amount = -1;
        }
        return amount;
    }

    /**
     * Gets the tea party goods type.
     *
     * @param game The <code>Game</code> to find the goods type in.
     * @return The tea party goods type.
     */
    public GoodsType getGoodsType(Game game) {
        return (goodsTypeId == null) ? null
            : game.getSpecification().getGoodsType(goodsTypeId);
    }

    /**
     * Gets the new enemy.
     *
     * @param game The <code>Game</code> to find the enemy in.
     * @return The new enemy.
     */
    public Player getEnemy(Game game) {
        return (enemyId == null) ? null
            : (game.getFreeColGameObject(enemyId) instanceof Player)
            ? (Player) game.getFreeColGameObject(enemyId)
            : null;
    }

    /**
     * Gets the additional units added.
     *
     * @return The additional units.
     */
    public List<AbstractUnit> getAdditions() {
        return additions;
    }

    /**
     * Handle a "monarchAction"-message.
     * This method is not needed as MonarchActionMessages are sent
     * from server to client, and only returned with an annotation attached.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return Null.
     */
    @SuppressWarnings("unused")
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        return null;
    }

    /**
     * Convert this MonarchMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        Document doc = result.getOwnerDocument();
        result.setAttribute("action", action.toString());
        if (amountString != null) result.setAttribute("amount", amountString);
        if (goodsTypeId != null) result.setAttribute("goods", goodsTypeId);
        if (enemyId != null) result.setAttribute("enemy", enemyId);
        for (AbstractUnit au : additions) {
            result.appendChild(au.toXMLElement(null, doc));
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "monarchAction".
     */
    public static String getXMLElementTagName() {
        return "monarchAction";
    }
}
