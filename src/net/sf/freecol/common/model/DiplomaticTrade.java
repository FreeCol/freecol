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

    /** A type for the trade status. */
    public static enum TradeStatus {
        PROPOSE_TRADE,
        ACCEPT_TRADE,
        REJECT_TRADE
    }


    /** The game in play. */
    private final Game game;

    /** The player who proposed agreement. */
    private Player sender;

    /** The player who is to accept this agreement. */
    private Player recipient;

    /** The status of this agreement. */
    private TradeStatus status;

    /** The individual items the trade consists of. */
    private final List<TradeItem> items = new ArrayList<TradeItem>();


    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param sender The sending <code>Player</code>.
     * @param recipient The recipient <code>Player</code>.
     */
    public DiplomaticTrade(Game game, Player sender, Player recipient) {
        this(game, sender, recipient, new ArrayList<TradeItem>());
    }

    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param sender The sending <code>Player</code>.
     * @param recipient The recipient <code>Player</code>.
     * @param items A list of items to trade.
     */
    public DiplomaticTrade(Game game, Player sender, Player recipient,
                           List<TradeItem> items) {
        setId("");
        this.game = game;
        this.sender = sender;
        this.recipient = recipient;
        this.status = TradeStatus.PROPOSE_TRADE;
        this.items.clear();
        this.items.addAll(items);
    }

    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
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
    public final List<TradeItem> getTradeItems() {
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
     * Get the items offered by a particular player.
     *
     * @param player The <code>Player</code> to check.
     * @return A list of <code>TradeItem</code>s offered by the player.
     */
    public List<TradeItem> getItemsGivenBy(Player player) {
        List<TradeItem> goodsList = new ArrayList<TradeItem>();
        for (TradeItem ti : items) {
            if (player == ti.getSource()) goodsList.add(ti);
        }
        return goodsList;
    }

    /**
     * Get the stance being offered.
     *
     * @return The <code>Stance</code> offered in this trade, or null if none.
     */
    public Stance getStance() {
        for (TradeItem ti : items) {
            if (ti instanceof StanceTradeItem) {
                return ((StanceTradeItem)ti).getStance();
            }
        }
        return null;
    }

    /**
     * Get the goods being offered.
     *
     * @return A list of <code>Goods</code> offered in this trade.
     */
    public List<Goods> getGoodsGivenBy(Player player) {
        List<Goods> goodsList = new ArrayList<Goods>();
        for (TradeItem ti : items) {
            if (ti instanceof GoodsTradeItem && player == ti.getSource()) {
                goodsList.add(((GoodsTradeItem)ti).getGoods());
            }
        }
        return goodsList;
    }

    /**
     * Get a list of colonies offered in this trade.
     *
     * @return A list of <code>Colony</code>s offered in this trade.
     */
    public List<Colony> getColoniesGivenBy(Player player) {
        List<Colony> colonyList = new ArrayList<Colony>();
        for (TradeItem ti : items) {
            if (ti instanceof ColonyTradeItem && player == ti.getSource()) {
                colonyList.add(((ColonyTradeItem)ti).getColony());
            }
        }
        return colonyList;
    }


    // Serialization

    private static final String RECIPIENT_TAG = "recipient";
    private static final String SENDER_TAG = "sender";
    private static final String STATUS_TAG = "status";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, SENDER_TAG, sender);

        writeAttribute(out, RECIPIENT_TAG, recipient);

        writeAttribute(out, STATUS_TAG, status.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (TradeItem item : items) item.toXML(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        sender = getAttribute(in, SENDER_TAG, game,
                              Player.class, (Player)null);

        recipient = getAttribute(in, RECIPIENT_TAG, game,
                                 Player.class, (Player)null);

        status = getAttribute(in, STATUS_TAG, TradeStatus.class,
                              TradeStatus.REJECT_TRADE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers
        items.clear();

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();
        TradeItem item = null;

        if (ColonyTradeItem.getXMLElementTagName().equals(tag)) {
            item = new ColonyTradeItem(game, in);

        } else if (GoldTradeItem.getXMLElementTagName().equals(tag)) {
            item = new GoldTradeItem(game, in);

        } else if (GoodsTradeItem.getXMLElementTagName().equals(tag)) {
            item = new GoodsTradeItem(game, in);

        } else if (StanceTradeItem.getXMLElementTagName().equals(tag)) {
            item = new StanceTradeItem(game, in);

        } else if (UnitTradeItem.getXMLElementTagName().equals(tag)) {
            item = new UnitTradeItem(game, in);

        } else {
            super.readChild(in);
        }

        if (item != null) items.add(item);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "diplomaticTrade".
     */
    public static String getXMLElementTagName() {
        return "diplomaticTrade";
    }
}
