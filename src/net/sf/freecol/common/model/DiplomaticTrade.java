/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Player.Stance;

import org.w3c.dom.Element;


/**
 * The class <code>DiplomaticTrade</code> represents an offer one player can
 * make another.
 */
public class DiplomaticTrade extends FreeColObject {

    /**
     * A type for the trade status.
     */
    public static enum TradeStatus {
        PROPOSE_TRADE,
        ACCEPT_TRADE,
        REJECT_TRADE
    }


    /**
     * The individual items the trade consists of.
     */
    private List<TradeItem> items;

    private final Game game;

    /**
     * The player who proposed agreement.
     */
    private Player sender;

    /**
     * The player who is to accept this agreement.
     */
    private Player recipient;

    /**
     * The status of this agreement.
     */
    private TradeStatus status;


    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param sender a <code>Player</code> value
     * @param recipient a <code>Player</code> value
     */
    public DiplomaticTrade(Game game, Player sender, Player recipient) {
        this(game, sender, recipient, new ArrayList<TradeItem>());
    }

    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game The <code>Game</code> containing the trade.
     * @param sender The sending <code>Player</code>.
     * @param recipient The recipient <code>Player</code>.
     * @param items A list of items to trade.
     */
    public DiplomaticTrade(Game game, Player sender, Player recipient,
                           List<TradeItem> items) {
        this.game = game;
        this.sender = sender;
        this.recipient = recipient;
        this.items = items;
        this.status = TradeStatus.PROPOSE_TRADE;
    }

    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game The <code>Game</code> containing the trade.
     * @param element an <code>Element</code> value
     */
    public DiplomaticTrade(Game game, Element element) {
        this.game = game;
        readFromXMLElement(element);
    }


    /**
     * Get the trade status.
     *
     * @return The status of this agreement.
     */
    public TradeStatus getStatus() {
        return status;
    }

    /**
     * Set the trade status.
     *
     * @param status The new <code>TradeStatus</code> for this agreement.
     */
    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    /**
     * Get the sending player.
     *
     * @return The sending <code>Player</code>.
     */
    public final Player getSender() {
        return sender;
    }

    /**
     * Set the sending player.
     *
     * @param newSender The new sending <code>Player</code>.
     */
    public final void setSender(final Player newSender) {
        this.sender = newSender;
    }

    /**
     * Get the recipient player.
     *
     * @return The recipient <code>Player</code>.
     */
    public final Player getRecipient() {
        return recipient;
    }

    /**
     * Set the recieving player.
     *
     * @param newRecipient The new recipient <code>Player</code>.
     */
    public final void setRecipient(final Player newRecipient) {
        this.recipient = newRecipient;
    }


    /**
     * Add to the DiplomaticTrade.
     *
     * @param newItem The <code>TradeItem</code> to add.
     */
    public void add(TradeItem newItem) {
        if (newItem.isUnique()) {
            removeType(newItem);
        }
        items.add(newItem);
    }

    /**
     * Remove a from the DiplomaticTrade.
     *
     * @param newItem The <code>TradeItem</code> to remove.
     */
    public void remove(TradeItem newItem) {
        items.remove(newItem);
    }

    /**
     * Remove from the DiplomaticTrade.
     *
     * @param index The index of the <code>TradeItem</code> to remove
     */
    public void remove(int index) {
        items.remove(index);
    }

    /**
     * Removes all trade items of the same class as the given argument.
     *
     * @param someItem a <code>TradeItem</code> value
     */
    public void removeType(TradeItem someItem) {
        Iterator<TradeItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            if (itemIterator.next().getClass() == someItem.getClass()) {
                itemIterator.remove();
            }
        }
    }


    /**
     * Get a list of all items to trade.
     *
     * @return A list of all the TradeItems.
     */
    public List<TradeItem> getTradeItems() {
        return items;
    }

    /**
     * Get an iterator for all the TradeItems.
     *
     * @return An iterator for all TradeItems.
     */
    public Iterator<TradeItem> iterator() {
        return items.iterator();
    }


    /**
     * Get the stance being offered.
     *
     * @return The <code>Stance</code> offered in this trade, or null if none.
     */
    public Stance getStance() {
        Iterator<TradeItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof StanceTradeItem) {
                return ((StanceTradeItem) item).getStance();
            }
        }
        return null;
    }

    /**
     * Get the goods being offered.
     *
     * @return A list of <code>Goods</code> offered in this trade.
     */
    public List<Goods> getGoodsGivenBy(Player player){
        List<Goods> goodsList = new ArrayList<Goods>();
        Iterator<TradeItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof GoodsTradeItem && player == item.getSource()) {
                goodsList.add(((GoodsTradeItem) item).getGoods());
            }
        }
        return goodsList;
    }

    /**
     * Get a list of colonies offered in this trade.
     *
     * @return A list of <code>Colony</code>s offered in this trade.
     */
    public List<Colony> getColoniesGivenBy(Player player){
        List<Colony> colonyList = new ArrayList<Colony>();
        Iterator<TradeItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof ColonyTradeItem && player==item.getSource()) {
                colonyList.add(((ColonyTradeItem) item).getColony());
            }
        }
        return colonyList;
    }


    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("sender", sender.getId());
        out.writeAttribute("recipient", recipient.getId());
        out.writeAttribute("status", status.toString());
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        for (TradeItem item : items) item.toXML(out);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        String senderString = in.getAttributeValue(null, "sender");
        sender = (Player) game.getFreeColGameObject(senderString);

        String recipientString = in.getAttributeValue(null, "recipient");
        recipient = (Player) game.getFreeColGameObject(recipientString);

        status = Enum.valueOf(TradeStatus.class,
                              in.getAttributeValue(null, "status"));

        items = new ArrayList<TradeItem>();
        TradeItem item;
        while (in.hasNext()) {
            if (in.next() != XMLStreamConstants.START_ELEMENT) continue;
            if (in.getLocalName().equals(StanceTradeItem.getXMLElementTagName())) {
                item = new StanceTradeItem(game, in);
            } else if (in.getLocalName().equals(GoodsTradeItem.getXMLElementTagName())) {
                item = new GoodsTradeItem(game, in);
            } else if (in.getLocalName().equals(GoldTradeItem.getXMLElementTagName())) {
                item = new GoldTradeItem(game, in);
            } else if (in.getLocalName().equals(ColonyTradeItem.getXMLElementTagName())) {
                item = new ColonyTradeItem(game, in);
            } else if (in.getLocalName().equals(UnitTradeItem.getXMLElementTagName())) {
                item = new UnitTradeItem(game, in);
            } else {
                logger.warning("Unknown TradeItem: " + in.getLocalName());
                continue;
            }
            items.add(item);
        }

    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "diplomaticTrade".
     */
    public static String getXMLElementTagName() {
        return "diplomaticTrade";
    }
}
