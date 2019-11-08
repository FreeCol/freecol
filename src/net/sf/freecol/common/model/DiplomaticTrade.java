/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Stance;

import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The class {@code DiplomaticTrade} represents an offer one
 * player can make another.
 *
 * This has to be a FCGO so that it can be serialized, but instances are not
 * interned.
 */
public class DiplomaticTrade extends FreeColGameObject {

    public static final String TAG = "diplomaticTrade";

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
     * Simple constructor, used in Game.newInstance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The identifier (ignored).
     */
    public DiplomaticTrade(Game game, String id) {
        super(game, ""); // Identifier not required
    }
        
    /**
     * Creates a new {@code DiplomaticTrade} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param context The {@code TradeContext} for this agreement.
     * @param sender The sending {@code Player}.
     * @param recipient The recipient {@code Player}.
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
     * Make a new diplomatic trade for a given context that establishes
     * peace between two given players.
     *
     * @param context The {@code TradeContext} the peace arises in.
     * @param sender The sending {@code Player}.
     * @param recipient The recipient {@code Player}.
     * @return A suitable {@code DiplomaticTrade}.
     */
    public static DiplomaticTrade makePeaceTreaty(TradeContext context,
                                                  Player sender,
                                                  Player recipient) {
        final Game game = sender.getGame();
        DiplomaticTrade dt = new DiplomaticTrade(game, context,
                                                 sender, recipient, null, 0);
        dt.add(new StanceTradeItem(game, sender, recipient, Stance.PEACE));
        dt.add(new StanceTradeItem(game, recipient, sender, Stance.PEACE));
        return dt;
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
     * @param status The new {@code TradeStatus} for this agreement.
     */
    public void setStatus(TradeStatus status) {
        this.status = status;
    }

    /**
     * Get the sending player.
     *
     * @return The sending {@code Player}.
     */
    public final Player getSender() {
        return this.sender;
    }

    /**
     * Set the sending player.
     *
     * @param newSender The new sending {@code Player}.
     */
    public final void setSender(final Player newSender) {
        this.sender = newSender;
    }

    /**
     * Get the recipient player.
     *
     * @return The recipient {@code Player}.
     */
    public final Player getRecipient() {
        return this.recipient;
    }

    /**
     * Set the recieving player.
     *
     * @param newRecipient The new recipient {@code Player}.
     */
    public final void setRecipient(final Player newRecipient) {
        this.recipient = newRecipient;
    }

    /**
     * Get the other player in a trade.
     *
     * @param player The known {@code Player}.
     * @return The other player, not the supplied known one.
     */
    public Player getOtherPlayer(Player player) {
        return (this.sender == player) ? this.recipient : this.sender;
    }

    /**
     * Handy utility to get the message associated with sending this
     * agreement from a player to a settlement owner.
     *
     * @param player The sending {@code Player}.
     * @param settlement The {@code Settlement} to send to.
     * @return A {@code StringTemplate} for the message.
     */
    public StringTemplate getSendMessage(Player player, Settlement settlement) {
        return StringTemplate.template("model.diplomaticTrade.send."
            + getContext().getKey())
            .addStringTemplate("%nation%",
                settlement.getOwner().getCountryLabel())
            .addStringTemplate("%settlement%",
                settlement.getLocationLabelFor(player));
    }

    /**
     * Handy utility to get the message associated with sending this
     * agreement from a player to a settlement owner.
     *
     * @param player The {@code Player} the offer came from.
     * @return A {@code StringTemplate} for the message.
     */
    public StringTemplate getReceiveMessage(Player player) {
        return StringTemplate.template("model.diplomaticTrade.receive."
            + getContext().getKey())
            .addStringTemplate("%nation%", player.getCountryLabel());
    }

    /**
     * Add to the DiplomaticTrade.
     *
     * @param newItem The {@code TradeItem} to add.
     */
    public void add(TradeItem newItem) {
        if (newItem.isUnique()) removeType(newItem.getClass());
        this.items.add(newItem);
    }

    /**
     * Remove a from the DiplomaticTrade.
     *
     * @param newItem The {@code TradeItem} to remove.
     */
    public void remove(TradeItem newItem) {
        this.items.remove(newItem);
    }

    /**
     * Remove from the DiplomaticTrade.
     *
     * @param index The index of the {@code TradeItem} to remove
     */
    public void remove(int index) {
        this.items.remove(index);
    }

    /**
     * Removes all trade items of the same class as the given argument.
     *
     * @param itemClass The {@code Class} of
     *     {@code TradeItem} to remove.
     */
    public void removeType(Class<? extends TradeItem> itemClass) {
        removeInPlace(this.items, matchKey(itemClass, TradeItem::getClass));
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
     * @return A list of all the {@code TradeItems}.
     */
    public final List<TradeItem> getItems() {
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
     * Get the items offered by a particular player.
     *
     * @param player The {@code Player} to check.
     * @return A list of {@code TradeItem}s offered by the player.
     */
    public List<TradeItem> getItemsGivenBy(Player player) {
        return transform(this.items, matchKey(player, TradeItem::getSource));
    }

    /**
     * Get the stance being offered.
     *
     * @return The {@code Stance} offered in this trade, or null if none.
     */
    public Stance getStance() {
        TradeItem ti = find(this.items, i -> i instanceof StanceTradeItem);
        return (ti == null) ? null : ti.getStance();
    }

    /**
     * Get a list of colonies offered in this trade.
     *
     * @param player The {@code Player} offering the colonies.
     * @return A list of {@code Colony}s offered in this trade.
     */
    public List<Colony> getColoniesGivenBy(final Player player) {
        return transform(this.items,
                         ti -> ti instanceof ColonyTradeItem
                             && ti.getSource() == player,
                         ti -> ti.getColony(player.getGame()));
    }

    /**
     * Get the gold offered in this trade by a given player.
     *
     * @param player The {@code Player} to check.
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
     * @param player The {@code Player} offering the goods.
     * @return A list of {@code Goods} offered in this trade.
     */
    public List<Goods> getGoodsGivenBy(Player player) {
        return transform(this.items,
                         ti -> ti instanceof GoodsTradeItem
                             && ti.getSource() == player,
                         TradeItem::getGoods);
    }

    /**
     * Get the player being incited against.
     *
     * @return The {@code Player} to be incited against.
     */
    public Player getVictim() {
        TradeItem ti = find(this.items, i -> i instanceof InciteTradeItem);
        return (ti == null) ? null : ti.getVictim();
    }

    /**
     * Get a list of units offered in this trade.
     *
     * @param player The {@code Player} offering the units.
     * @return A list of {@code Unit}s offered in this trade.
     */
    public List<Unit> getUnitsGivenBy(Player player) {
        return transform(this.items,
                         ti -> ti instanceof UnitTradeItem
                             && ti.getSource() == player,
                         TradeItem::getUnit);
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


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        DiplomaticTrade o = copyInCast(other, DiplomaticTrade.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.context = o.getContext();
        this.status = o.getStatus();
        this.sender = game.updateRef(o.getSender());
        this.recipient = game.updateRef(o.getRecipient());
        this.items.clear();
        this.items.addAll(game.updateRef(o.getItems()));
        this.version = o.getVersion();
        return true;
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

        if (ColonyTradeItem.TAG.equals(tag)) {
            add(new ColonyTradeItem(getGame(), xr));

        } else if (GoldTradeItem.TAG.equals(tag)) {
            add(new GoldTradeItem(getGame(), xr));

        } else if (GoodsTradeItem.TAG.equals(tag)) {
            add(new GoodsTradeItem(getGame(), xr));

        } else if (InciteTradeItem.TAG.equals(tag)) {
            add(new InciteTradeItem(getGame(), xr));

        } else if (StanceTradeItem.TAG.equals(tag)) {
            add(new StanceTradeItem(getGame(), xr));

        } else if (UnitTradeItem.TAG.equals(tag)) {
            add(new UnitTradeItem(getGame(), xr));

        } else {
            super.readChild(xr);
        }
    }


    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append('[').append(getId())
            .append(' ').append(getContext())
            .append(' ').append(getStatus())
            .append(" from=").append(getSender().getId())
            .append(" to=").append(getRecipient().getId())
            .append(" version=").append(getVersion())
            .append(" [");
        for (TradeItem item : getItems()) sb.append(' ').append(item);
        sb.append(" ]]");
        return sb.toString();
    }
}
