/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.networking.DOMMessage;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The class <code>DiplomaticTrade</code> represents an offer one
 * player can make another.
 *
 * This has to be a FCGO so that it can be serialized, but instances are not
 * interned.
 */
public class DiplomaticTrade extends FreeColGameObject {

    /** A context for the trade. */
    public static enum TradeContext {
        CONTACT,    /** First contact between Europeans */
        DIPLOMATIC, /** Scout negotiating */
        TRADE,      /** Carrier trading */
        TRIBUTE;    /** Offensive unit demanding */

        /**
         * Get a message key for this trade context.
         *
         * @return A message key.
         */
        public String getKey() {
            return getEnumKey(this);
        }
    }

    /** A type for the trade status. */
    public static enum TradeStatus {
        PROPOSE_TRADE,
        ACCEPT_TRADE,
        REJECT_TRADE
    }


    /** The context of this agreement. */
    private TradeContext context;

    /** The status of this agreement. */
    private TradeStatus status;

    /** The player who proposed agreement. */
    private Player sender;

    /** The player who is to accept this agreement. */
    private Player recipient;

    /** The individual items the trade consists of. */
    private final List<TradeItem> items = new ArrayList<>();

    /** Counter for the number of iterations on this attempt to agree. */
    private int version;


    /**
     * Simple constructor, used in FreeColGameObject.newInstance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The identifier (ignored).
     */
    public DiplomaticTrade(Game game, String id) {
        super(game, ""); // Identifier not required
    }
        
    /**
     * Creates a new <code>DiplomaticTrade</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param context The <code>TradeContext</code> for this agreement.
     * @param sender The sending <code>Player</code>.
     * @param recipient The recipient <code>Player</code>.
     * @param items A list of items to trade.
     * @param version The trade version number.
     */
    public DiplomaticTrade(Game game, TradeContext context,
                           Player sender, Player recipient,
                           List<TradeItem> items, int version) {
        this(game, "");
        this.context = context;
        this.sender = sender;
        this.recipient = recipient;
        this.status = TradeStatus.PROPOSE_TRADE;
        this.items.clear();
        if (items != null) this.items.addAll(items);
        this.version = version;
    }


    /**
     * Get the trade context.
     *
     * @return The context of this agreement.
     */
    public TradeContext getContext() {
        return this.context;
    }

    /**
     * Get the trade status.
     *
     * @return The status of this agreement.
     */
    public TradeStatus getStatus() {
        return this.status;
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
        return this.sender;
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
        return this.recipient;
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
     * Get the other player in a trade.
     *
     * @param player The known <code>Player</code>.
     * @return The other player, not the supplied known one.
     */
    public Player getOtherPlayer(Player player) {
        return (this.sender == player) ? this.recipient : this.sender;
    }

    /**
     * Handy utility to get the message associated with sending this
     * agreement from a player to a settlement owner.
     *
     * @param player The sending <code>Player</code>.
     * @param settlement The <code>Settlement</code> to send to.
     * @return A <code>StringTemplate</code> for the message.
     */
    public StringTemplate getSendMessage(Player player, Settlement settlement) {
        return StringTemplate.template("model.diplomaticTrade.send."
            + getContext().getKey())
            .addStringTemplate("%nation%",
                settlement.getOwner().getNationLabel())
            .addStringTemplate("%settlement%",
                settlement.getLocationLabelFor(player));
    }

    /**
     * Handy utility to get the message associated with sending this
     * agreement from a player to a settlement owner.
     *
     * @param player The <code>Player</code> the offer came from.
     * @return A <code>StringTemplate</code> for the message.
     */
    public StringTemplate getReceiveMessage(Player player) {
        return StringTemplate.template("model.diplomaticTrade.receive."
            + getContext().getKey())
            .addStringTemplate("%nation%", player.getNationLabel());
    }

    /**
     * Add to the DiplomaticTrade.
     *
     * @param newItem The <code>TradeItem</code> to add.
     */
    public void add(TradeItem newItem) {
        if (newItem.isUnique()) removeType(newItem.getClass());
        this.items.add(newItem);
    }

    /**
     * Remove a from the DiplomaticTrade.
     *
     * @param newItem The <code>TradeItem</code> to remove.
     */
    public void remove(TradeItem newItem) {
        this.items.remove(newItem);
    }

    /**
     * Remove from the DiplomaticTrade.
     *
     * @param index The index of the <code>TradeItem</code> to remove
     */
    public void remove(int index) {
        this.items.remove(index);
    }

    /**
     * Removes all trade items of the same class as the given argument.
     *
     * @param itemClass The <code>Class</code> of
     *     <code>TradeItem</code> to remove.
     */
    public void removeType(Class<? extends TradeItem> itemClass) {
        Iterator<TradeItem> itemIterator = this.items.iterator();
        while (itemIterator.hasNext()) {
            if (itemIterator.next().getClass() == itemClass) {
                itemIterator.remove();
            }
        }
    }

    /**
     * Remove all trade items from this agreement.
     */
    public void clear() {
        this.items.clear();
    }

    /**
     * Get a list of all items to trade.
     *
     * @return A list of all the TradeItems.
     */
    public final List<TradeItem> getTradeItems() {
        return this.items;
    }

    /**
     * Are there no trade items present?
     *
     * @return True if there are no trade items present.
     */
    public final boolean isEmpty() {
        return this.items.isEmpty();
    }

    /**
     * Get an iterator for all the TradeItems.
     *
     * @return An iterator for all TradeItems.
     */
    public Iterator<TradeItem> iterator() {
        return this.items.iterator();
    }

    /**
     * Get the items offered by a particular player.
     *
     * @param player The <code>Player</code> to check.
     * @return A list of <code>TradeItem</code>s offered by the player.
     */
    public List<TradeItem> getItemsGivenBy(Player player) {
        return transform(this.items, ti -> ti.getSource() == player,
                         Collectors.toList());
    }

    /**
     * Get the stance being offered.
     *
     * @return The <code>Stance</code> offered in this trade, or null if none.
     */
    public Stance getStance() {
        TradeItem ti = find(this.items, i -> i instanceof StanceTradeItem);
        return (ti == null) ? null : ti.getStance();
    }

    /**
     * Get a list of colonies offered in this trade.
     *
     * @param player The <code>Player</code> offering the colonies.
     * @return A list of <code>Colony</code>s offered in this trade.
     */
    public List<Colony> getColoniesGivenBy(final Player player) {
        return transform(this.items,
            ti -> ti instanceof ColonyTradeItem && ti.getSource() == player,
            ti -> ti.getColony(player.getGame()), Collectors.toList());
    }

    /**
     * Get the gold offered in this trade by a given player.
     *
     * @param player The <code>Player</code> to check.
     * @return The gold offered in this trade.
     */
    public int getGoldGivenBy(Player player) {
        TradeItem ti = find(this.items, i -> i instanceof GoldTradeItem
            && player == i.getSource());
        return (ti == null) ? -1 : ti.getGold();
    }

    /**
     * Get the goods being offered.
     *
     * @param player The <code>Player</code> offering the goods.
     * @return A list of <code>Goods</code> offered in this trade.
     */
    public List<Goods> getGoodsGivenBy(Player player) {
        return transform(this.items,
            ti -> ti instanceof GoodsTradeItem && ti.getSource() == player,
            TradeItem::getGoods, Collectors.toList());
    }

    /**
     * Get the player being incited against.
     *
     * @return The <code>Player</code> to be incited against.
     */
    public Player getVictim() {
        TradeItem ti = find(this.items, i -> i instanceof InciteTradeItem);
        return (ti == null) ? null : ti.getVictim();
    }

    /**
     * Get a list of units offered in this trade.
     *
     * @param player The <code>Player</code> offering the units.
     * @return A list of <code>Unit</code>s offered in this trade.
     */
    public List<Unit> getUnitsGivenBy(Player player) {
        return transform(this.items,
            ti -> ti instanceof UnitTradeItem && ti.getSource() == player,
            TradeItem::getUnit, Collectors.toList());
    }

    /**
     * Gets the version of this agreement.
     *
     * @return The version number.
     */
    public int getVersion() {
        return this.version;
    }

    /**
     * Increment the version of this agreement.
     */
    public void incrementVersion() {
        this.version++;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    public boolean isInternable() {
        return false;
    }


    // Serialization

    private static final String CONTEXT_TAG = "context";
    private static final String RECIPIENT_TAG = "recipient";
    private static final String SENDER_TAG = "sender";
    private static final String STATUS_TAG = "status";
    private static final String VERSION_TAG = "version";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(CONTEXT_TAG, this.context);

        xw.writeAttribute(SENDER_TAG, this.sender);

        xw.writeAttribute(RECIPIENT_TAG, this.recipient);

        xw.writeAttribute(STATUS_TAG, this.status);

        xw.writeAttribute(VERSION_TAG, this.version);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (TradeItem item : this.items) item.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.context = xr.getAttribute(CONTEXT_TAG, TradeContext.class,
                                       (TradeContext)null);

        this.sender = xr.getAttribute(getGame(), SENDER_TAG,
                                      Player.class, (Player)null);

        this.recipient = xr.getAttribute(getGame(), RECIPIENT_TAG,
                                         Player.class, (Player)null);

        this.status = xr.getAttribute(STATUS_TAG, TradeStatus.class,
                                      TradeStatus.REJECT_TRADE);

        this.version = xr.getAttribute(VERSION_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        this.items.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (ColonyTradeItem.getTagName().equals(tag)) {
            add(new ColonyTradeItem(getGame(), xr));

        } else if (GoldTradeItem.getTagName().equals(tag)) {
            add(new GoldTradeItem(getGame(), xr));

        } else if (GoodsTradeItem.getTagName().equals(tag)) {
            add(new GoodsTradeItem(getGame(), xr));

        } else if (InciteTradeItem.getTagName().equals(tag)) {
            add(new InciteTradeItem(getGame(), xr));

        } else if (StanceTradeItem.getTagName().equals(tag)) {
            add(new StanceTradeItem(getGame(), xr));

        } else if (UnitTradeItem.getTagName().equals(tag)) {
            add(new UnitTradeItem(getGame(), xr));

        } else {
            super.readChild(xr);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[").append(getId())
            .append(" ").append(getContext())
            .append(" ").append(getStatus())
            .append(" from=").append(getSender().getId())
            .append(" to=").append(getRecipient().getId())
            .append(" version=").append(getVersion())
            .append(" [");
        for (TradeItem item : getTradeItems()) sb.append(" ").append(item);
        sb.append(" ]]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "diplomaticTrade".
     */
    public static String getTagName() {
        return "diplomaticTrade";
    }
}
