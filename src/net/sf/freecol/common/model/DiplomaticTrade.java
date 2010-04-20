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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import net.sf.freecol.common.model.Player.Stance;

/**
 * The class <code>DiplomaticTrade</code> represents an offer one player can
 * make another.
 * 
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
     * Whether this is the accept instance.
     */
    private boolean accept;

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
     * @param game a <code>Game</code> value
     * @param sender a <code>Player</code> value
     * @param recipient a <code>Player</code> value
     */
    public DiplomaticTrade(Game game, Player sender, Player recipient, List<TradeItem> items) {
        this.game = game;
        this.sender = sender;
        this.recipient = recipient;
        this.items = items;
    }

    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param element an <code>Element</code> value
     */
    public DiplomaticTrade(Game game, Element element) {
        this.game = game;
        readFromXMLElement(element);
    }

    /**
     * Gets the game object this <code>DiplomaticTrade</code> belongs to.
     * @return The <code>game</code>.
     */
    public Game getGame() {
        return game;
    }

    /**
     * Get the <code>Accept</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isAccept() {
        return accept;
    }

    /**
     * Set the <code>Accept</code> value.
     *
     * @param newAccept The new Accept value.
     */
    public final void setAccept(final boolean newAccept) {
        this.accept = newAccept;
    }

    /**
     * Get the <code>Sender</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getSender() {
        return sender;
    }

    /**
     * Set the <code>Sender</code> value.
     *
     * @param newSender The new Sender value.
     */
    public final void setSender(final Player newSender) {
        this.sender = newSender;
    }

    /**
     * Get the <code>Recipient</code> value.
     *
     * @return a <code>Player</code> value
     */
    public final Player getRecipient() {
        return recipient;
    }

    /**
     * Set the <code>Recipient</code> value.
     *
     * @param newRecipient The new Recipient value.
     */
    public final void setRecipient(final Player newRecipient) {
        this.recipient = newRecipient;
    }

    /**
     * Add a TradeItem to the DiplomaticTrade.
     * 
     * @param newItem
     *            a <code>TradeItem</code> value
     */
    public void add(TradeItem newItem) {
        if (newItem.isUnique()) {
            removeType(newItem);
        }
        items.add(newItem);
    }

    /**
     * Remove a TradeItem from the DiplomaticTrade.
     * 
     * @param newItem
     *            a <code>TradeItem</code> value
     */
    public void remove(TradeItem newItem) {
        items.remove(newItem);
    }


    /**
     * Remove a TradeItem from the DiplomaticTrade.
     * 
     * @param index
     *            the index of the <code>TradeItem</code> to remove
     */
    public void remove(int index) {
        items.remove(index);
    }


    /**
     * Removes all trade items of the same class as the given
     * argument.
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
     * Returns the stance being offered, or null if none
     * is being offered.
     *
     * @return an <code>int</code> value
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
     * Returns a list of goods given by <code>Player</code>
     *
     * @return a list of <code>Goods</code> offered by the player, empty if none given
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
     * Returns a list of colonies given by <code>Player</code>
     *
     * @return a list of <code>Colony</code> offered by the player, empty if none given
     */
    public List<Colony> getColoniesGivenBy(Player player){
    	List<Colony> colonyList = new ArrayList<Colony>();
    	Iterator<TradeItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof ColonyTradeItem && player == item.getSource()) {
            	colonyList.add(((ColonyTradeItem) item).getColony());
            }
        }
        return colonyList;
    }
    
    
    /**
     * Calls the <code>makeTrade</code> method of all TradeItems.
     *
     * @return A list of all objects traded.
     */
    public List<FreeColGameObject> makeTrade() {
        ArrayList<FreeColGameObject> all = new ArrayList<FreeColGameObject>();

        for (TradeItem item : items) {
            all.addAll(item.makeTrade());
        }
        return all;
    }


    /**
     * Returns all TradeItems.
     * 
     * @return a List of TradeItems    
     */
    public List<TradeItem> getTradeItems() {
        return items;
    }    


    /**
     * Returns an iterator for all TradeItems.
     * 
     * @return an iterator for all TradeItems.
     */
    public Iterator<TradeItem> iterator() {
        return items.iterator();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in
     *            The input stream with the XML.
     * @throws XMLStreamException
     *             if a problem was encountered during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        String acceptString = in.getAttributeValue(null, "accept");
        if ("accept".equals(acceptString)) {
            accept = true;
        }

        String senderString = in.getAttributeValue(null, "sender");
        sender = (Player) getGame().getFreeColGameObject(senderString);

        String recipientString = in.getAttributeValue(null, "recipient");
        recipient = (Player) getGame().getFreeColGameObject(recipientString);

        items = new ArrayList<TradeItem>();
        TradeItem item;
        while (in.hasNext()){
        	if(in.next() != XMLStreamConstants.START_ELEMENT)
        		continue;
            if (in.getLocalName().equals(StanceTradeItem.getXMLElementTagName())) {
                item = new StanceTradeItem(getGame(), in);
            } else if (in.getLocalName().equals(GoodsTradeItem.getXMLElementTagName())) {
                item = new GoodsTradeItem(getGame(), in);
            } else if (in.getLocalName().equals(GoldTradeItem.getXMLElementTagName())) {
                item = new GoldTradeItem(getGame(), in);
            } else if (in.getLocalName().equals(ColonyTradeItem.getXMLElementTagName())) {
                item = new ColonyTradeItem(getGame(), in);
            } else if (in.getLocalName().equals(UnitTradeItem.getXMLElementTagName())) {
                item = new UnitTradeItem(getGame(), in);
            } else {
                logger.warning("Unknown TradeItem: " + in.getLocalName());
                continue;
            }
            items.add(item);
        }

    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     * 
      * @param out
     *            The target stream.
      * @throws XMLStreamException
     *             if there are any problems writing to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("accept", accept ? "accept" : "");
        out.writeAttribute("sender", sender.getId());
        out.writeAttribute("recipient", recipient.getId());
        for (TradeItem item : items) {
            item.toXML(out);
        }
        out.writeEndElement();
    }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "goods".
     */
    public static String getXMLElementTagName() {
        return "diplomaticTrade";
    }

}
