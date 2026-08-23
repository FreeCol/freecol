/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.sort;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * This class implements a simple economic model where a market holds
 * all goods to be sold and the price of a particular goods type is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject implements Ownable {
    
    public static final String TAG = "market";

    /**
     * European markets are bottomless.  Goods present never decrease
     * below this threshold.
     */
    public static final int MINIMUM_AMOUNT = 100;

    public static final String PRICE_CHANGE = "priceChange";
    
    /**
     * Constant for specifying the access to this {@code Market}
     * when selling goods.
     */
    public static enum Access {
        EUROPE,
        CUSTOM_HOUSE,
    }


    /** The contents of the market, keyed by goods type. */
    private final Map<GoodsType, MarketData> marketData = new HashMap<>();

    /** The owning player. */
    private Player owner;

    // Do not serialize below

    /** Watching listeners. */
    private final ArrayList<TransactionListener> transactionListeners
        = new ArrayList<>();


    /**
     * Main constructor for creating a market for a new player.
     *
     * @param game The enclosing {@code Game}.
     * @param player The {@code Player} to own the market.
     */
    public Market(Game game, Player player) {
        super(game);

        this.owner = player;

        /*
         * Create the market data containers for each type of goods
         * and seed these objects with the initial amount of each type
         * of goods.
         */
        for (GoodsType goodsType : getSpecification().getStorableGoodsTypeList()) {
            putMarketData(goodsType, new MarketData(game, goodsType));
        }
    }

    /**
     * Creates a new {@code Market} with the given identifier.
     *
     * The object should be initialized later.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public Market(Game game, String id) {
        super(game, id);
    }
    

    /**
     * Get the market data values.
     *
     * @return The market data in this market.
     */
    public Collection<MarketData> getMarketDataValues() {
        synchronized (this.marketData) {
            return this.marketData.values();
        }
    }

    /**
     * Gets the market data for a type of goods.
     *
     * Public so the server can send individual MarketData updates.
     *
     * @param goodsType The {@code GoodsType} to look for.
     * @return The corresponding {@code MarketData}, or null if none.
     */
    public MarketData getMarketData(GoodsType goodsType) {
        synchronized (this.marketData) {
            return this.marketData.get(goodsType);
        }
    }

    /**
     * Set the market data for a given goods type.
     *
     * @param goodsType The {@code GoodsType} to set the market data for.
     * @param data The new {@code MarketData}.
     */
    private void putMarketData(GoodsType goodsType, MarketData data) {
        synchronized (this.marketData) {
            this.marketData.put(goodsType, data);
        }
    }

    /**
     * Gets the market data for a specified goods type, creating it
     * if it does not exist yet.
     *
     * @param goodsType The {@code GoodsType} to query.
     * @return The {@code MarketData} required.
     */
    private MarketData requireMarketData(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(getGame(), goodsType);
            putMarketData(goodsType, data);
        }
        return data;
    }

    /**
     * Clear the market data.
     */
    private void clearMarketData() {
        synchronized (this.marketData) {
            this.marketData.clear();
        }
    }

    /**
     * Has a type of goods been traded in this market?
     *
     * @param type The type of goods to consider.
     * @return True if the goods type has been traded.
     */
    public boolean hasBeenTraded(GoodsType type) {
        MarketData data = getMarketData(type);
        return data != null && data.hasBeenTraded();
    }

    /**
     * Determines the cost to buy a single unit of a particular type of good.
     *
     * @param type A {@code GoodsType} value.
     * @return The cost to buy a single unit of the given type of goods.
     */
    public int getCostToBuy(GoodsType type) {
        MarketData data = getMarketData(type);
        return (data == null) ? 0 : data.getCostToBuy();
    }

    /**
     * Determines the price paid for the sale of a single unit of a particular
     * type of goods.
     *
     * @param type A {@code GoodsType} value.
     * @return The price for a single unit of the given type of goods
     *         if it is for sale.
     */
    public int getPaidForSale(GoodsType type) {
        MarketData data = getMarketData(type);
        return (data == null) ? 0 : data.getPaidForSale();
    }

    /**
     * Add (or remove) some goods to this market.
     *
     * @param goodsType The {@code GoodsType} to add.
     * @param amount The amount of goods.
     * @return True if the price changes as a result of this addition.
     */
    public boolean addGoodsToMarket(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);
        return data.addGoods(amount, MINIMUM_AMOUNT);
    }

    /**
     * Sets a new initial price for a goods type and forces a price update.
     * This is primarily used during game randomization/initialization.
     *
     * @param type The GoodsType to modify.
     * @param price The new initial price.
     */
    public void resetInitialPrice(GoodsType type, int price) {
        MarketData data = getMarketData(type);
        if (data != null) {
            data.setInitialPrice(price);
            data.update();           // Calculate costToBuy/paidForSale based on new initial
            data.flushPriceChange();  // Ensure Turn 1 doesn't show a "price change" alert
        }
    }

    /**
     * Gets the price of a given goods when the {@code Player} buys.
     *
     * @param type a {@code GoodsType} value
     * @param amount The amount of goods.
     * @return The bid price of the given goods.
     */
    public int getBidPrice(GoodsType type, int amount) {
        MarketData data = getMarketData(type);
        return (data == null) ? 0 : data.getBidPrice(amount);
    }

    /**
     * Gets the price of a given goods when the {@code Player} sells.
     *
     * @param type a {@code GoodsType} value
     * @param amount The amount of goods.
     * @return The sale price of the given goods.
     */
    public int getSalePrice(GoodsType type, int amount) {
        MarketData data = getMarketData(type);
        return (data == null) ? 0 : data.getSalePrice(amount);
    }

    /**
     * Gets the price of a given goods when the {@code Player} sells.
     *
     * @param <T> The base type of the goods.
     * @param goods The {@code Goods} to evaluate.
     * @return The price.
     */
    public <T extends AbstractGoods> int getSalePrice(T goods) {
        return getSalePrice(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the arrears for of a given goods type.
     *
     * @param goodsType The {@code GoodsType} to get arrears for.
     * @return The arrears.
     */
    public int getArrears(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getArrears();
    }

    /**
     * Sets the arrears associated with a type of goods.
     *
     * @param goodsType The {@code GoodsType} to set the arrears for.
     * @param value The amount of arrears to set.
     */
    public void setArrears(GoodsType goodsType, int value) {
        MarketData data = requireMarketData(goodsType);
        data.setArrears(value);
    }

    /**
     * Gets the sales of a type of goods.
     *
     * @param goodsType The {@code GoodsType} to get the sales for.
     * @return The current sales amount.
     */
    public int getSales(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getSales();
    }

    /**
     * Modifies the sales of a type of goods.
     *
     * @param goodsType The {@code GoodsType} to set the sales for.
     * @param amount The amount of sales to add to the current amount.
     */
    public void modifySales(GoodsType goodsType, int amount) {
        if (amount != 0) {
            MarketData data = requireMarketData(goodsType);
            data.addSales(amount);
        }
    }

    /**
     * Gets the income before taxes for a type of goods.
     *
     * @param goodsType The {@code GoodsType} to get the income for.
     * @return The current income before taxes.
     */
    public int getIncomeBeforeTaxes(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getIncomeBeforeTaxes();
    }

    /**
     * Modifies the income before taxes on sales of a type of goods.
     *
     * @param goodsType The {@code GoodsType} to set the income for.
     * @param amount The amount of tax income to add to the current amount.
     */
    public void modifyIncomeBeforeTaxes(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);
        data.addIncomeBeforeTaxes(amount);
    }

    /**
     * Gets the income after taxes for a type of goods.
     *
     * @param goodsType The {@code GoodsType} to get the income for.
     * @return The current income after taxes.
     */
    public int getIncomeAfterTaxes(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getIncomeAfterTaxes();
    }

    /**
     * Modifies the income after taxes on sales of a type of goods.
     *
     * @param goodsType The {@code GoodsType} to set the income for.
     * @param amount The amount of tax income to add to the current amount.
     */
    public void modifyIncomeAfterTaxes(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);
        data.addIncomeAfterTaxes(amount);
    }

    /**
     * Gets the amount of a goods type in the market.
     *
     * @param goodsType The {@code GoodsType} to get the amount of.
     * @return The current amount of the goods in the market.
     */
    public int getAmountInMarket(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getAmountInMarket();
    }

    /**
     * Has the price of a type of goods changed in this market?
     *
     * @param goodsType The {@code GoodsType} to consider.
     * @return True if the price has changed.
     */
    public boolean hasPriceChanged(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return data != null && data.hasPriceChanged();
    }

    /**
     * Clear any price changes for a type of goods.
     *
     * @param goodsType The {@code GoodsType} to consider.
     */
    public void flushPriceChange(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        if (data != null) {
            data.flushPriceChange();
        }
    }

    /**
     * Make up a {@code ModelMessage} describing the change in this
     * {@code Market} for a specified type of goods.
     *
     * @param goodsType The {@code GoodsType} that has changed price.
     * @return A message describing the change.
     */
    public ModelMessage makePriceChangeMessage(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        if (data == null || !data.hasPriceChanged()) return null;

        String messageId = data.isPriceIncrease()
            ? "model.market.priceIncrease"
            : "model.market.priceDecrease";

        return new ModelMessage(ModelMessage.MessageType.MARKET_PRICES,
                                messageId, this, goodsType)
                .addStringTemplate("%market%", owner.getMarketName())
                .addNamed("%goods%", goodsType)
                .addAmount("%buy%", data.getCostToBuy())
                .addAmount("%sell%", data.getPaidForSale());
    }

    /**
     * Update the price for a type of goods, bypassing the price change
     * clamping.
     *
     * Used to reset the prices when the initial price is randomized.  Do
     * not use during the game, the price change clamping mechanism should
     * remain in effect.
     *
     * @param goodsType The {@code GoodsType} to update.
     */
    public void update(GoodsType goodsType) {
        MarketData data = requireMarketData(goodsType);
        data.update();
    }

    /**
     * Get a sale price comparator for this market.
     *
     * @param <T> The {@code AbstractGoods} type to compare.
     * @return A suitable {@code Comparator}.
     */
    public <T extends AbstractGoods> Comparator<T> getSalePriceComparator() {
        return Comparator.comparingInt((T t)
            -> getSalePrice(t.getType(), t.getAmount())).reversed();
    }

    // TransactionListener support

    /**
     * Adds a transaction listener for notification of any transaction
     *
     * @param listener The {@code TransactionListener} to add.
     */
    public void addTransactionListener(TransactionListener listener) {
        transactionListeners.add(listener);
    }

    /**
     * Removes a transaction listener
     *
     * @param listener The {@code TransactionListener} to remove.
     */
    public void removeTransactionListener(TransactionListener listener) {
        transactionListeners.remove(listener);
    }

    /**
     * Gets the listeners added to this market.
     *
     * @return An array of all the {@code TransactionListener}s
     *     added, or an empty array if none found.
     */
    public TransactionListener[] getTransactionListener() {
        return transactionListeners.toArray(new TransactionListener[0]);
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getOwner() {
        return owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColGameObject getLinkTarget(Player player) {
        return (player == getOwner()) ? getOwner().getEurope() : null;
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Market o = copyInCast(other, Market.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.marketData.clear();
        for (MarketData md : game.update(o.getMarketDataValues(), true)) {
            this.marketData.put(md.getGoodsType(), md);
        }
        this.owner = game.updateRef(o.getOwner());
        return true;
    }


    // Serialization

    private static final String OWNER_TAG = "owner";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(OWNER_TAG, owner);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (xw.validFor(owner)) {

            for (MarketData data : sort(getMarketDataValues())) {
                data.toXML(xw);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        owner = xr.findFreeColGameObject(getGame(), OWNER_TAG,
                                         Player.class, (Player)null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearMarketData();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (MarketData.TAG.equals(tag)) {
            MarketData data = xr.readFreeColObject(getGame(), MarketData.class);
            putMarketData(data.getGoodsType(), data);

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
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getId())
            .append(" owner=").append((owner==null) ? "null" : owner.getId());
        for (MarketData md : sort(getMarketDataValues())) {
            sb.append(' ').append(md);
        }
        sb.append(']');
        return sb.toString();
    }
}
