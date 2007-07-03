package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * The class <code>DiplomaticTrade</code> represents an offer one player can
 * make another.
 * 
 */
public class DiplomaticTrade extends PersistentObject {

    public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";


    /** 
     * The individual items the trade consists of.
     */
    private List<TradeItem> items;

    @SuppressWarnings("unused")
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
     * @param newItem
     *            a <code>TradeItem</code> value
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
     * Returns the stance being offered, or Integer.MIN_VALUE if none
     * is being offered.
     *
     * @return an <code>int</code> value
     */
    public int getStance() {
        Iterator<TradeItem> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            TradeItem item = itemIterator.next();
            if (item instanceof StanceTradeItem) {
                return ((StanceTradeItem) item).getStance();
            }
        }
        return Integer.MIN_VALUE;
    }


    /**
     * Calls the <code>makeTrade</code> method of all TradeItems.
     *
     */
    public void makeTrade() {
        for (TradeItem item : items) {
            item.makeTrade();
        }
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
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
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
     * <br>
     * <br>
     * 
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     * 
     * @param out
     *            The target stream.
     * @param player
     *            The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @throws XMLStreamException
     *             if there are any problems writing to the stream.
     */
    public void toXML(XMLStreamWriter out, Player player) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("accept", accept ? "accept" : "");
        out.writeAttribute("sender", sender.getID());
        out.writeAttribute("recipient", recipient.getID());
        for (TradeItem item : items) {
            item.toXML(out, player);
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
