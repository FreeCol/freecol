/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * This class implements a simple economic model where a market holds
 * all goods to be sold and the price of a particular goods type is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject implements Ownable {

    private static final Logger logger = Logger.getLogger(Market.class.getName());

    /**
     * European markets are bottomless.  Goods present never decrease
     * below this threshold.
     */
    public static final int MINIMUM_AMOUNT = 100;

    /**
     * Constant for specifying the access to this <code>Market</code>
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

    /** Watching listeners.  Do not serialize. */
    private final ArrayList<TransactionListener> transactionListeners
        = new ArrayList<>();


    /**
     * Main constructor for creating a market for a new player.
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
     * Creates a new <code>Market</code> with the given identifier.
     * The object should later be initialized by calling either
     * {@link #readFromXML(FreeColXMLReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Market(Game game, String id) {
        super(game, id);
    }


    private void putMarketData(GoodsType goodsType, MarketData data) {
        marketData.put(goodsType, data);
    }

    /**
     * Gets the market data for a specified goods type, creating it
     * if it does not exist yet.
     *
     * @param goodsType The <code>GoodsType</code> to query.
     * @return The <code>MarketData</code> required.
     */
    private MarketData requireMarketData(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(getGame(), goodsType);
            putMarketData(goodsType, data);
        }
        return data;
    }

    // ------------------------------------------------------------ API methods

    /**
     * Get the market data values.
     *
     * @return The market data in this market.
     */
    public Collection<MarketData> getMarketData() {
        return this.marketData.values();
    }

    /**
     * Gets the market data for a type of goods.
     *
     * Public so the server can send individual MarketData updates.
     *
     * @param goodsType The <code>GoodsType</code> to look for.
     * @return The corresponding <code>MarketData</code>, or null if none.
     */
    public MarketData getMarketData(GoodsType goodsType) {
        return marketData.get(goodsType);
    }

    /**
     * Has a type of goods been traded in this market?
     *
     * @param type The type of goods to consider.
     * @return True if the goods type has been traded.
     */
    public boolean hasBeenTraded(GoodsType type) {
        MarketData data = getMarketData(type);
        return data != null && data.getTraded();
    }

    /**
     * Determines the cost to buy a single unit of a particular type of good.
     *
     * @param type A <code>GoodsType</code> value.
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
     * @param type A <code>GoodsType</code> value.
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
     * @param goodsType The <code>GoodsType</code> to add.
     * @param amount The amount of goods.
     * @return True if the price changes as a result of this addition.
     */
    public boolean addGoodsToMarket(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);

        // Markets are bottomless, amount can not go below the threshold
        data.setAmountInMarket(Math.max(MINIMUM_AMOUNT,
                                        data.getAmountInMarket() + amount));
        data.setTraded(true);
        return data.price();
    }

    /**
     * Gets the initial price of a given goods type.
     *
     * @param goodsType The <code>GoodsType</code> to get the initial price of.
     * @return The initial price.
     */
    public int getInitialPrice(GoodsType goodsType) {
        MarketData data = requireMarketData(goodsType);
        return data.getInitialPrice();
    }

    /**
     * Sets the initial price of a given goods type.
     *
     * @param goodsType The <code>GoodsType</code> to set the initial price of.
     * @param amount The new initial price.
     */
    public void setInitialPrice(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);
        data.setInitialPrice(amount);
    }

    /**
     * Gets the price of a given goods when the <code>Player</code> buys.
     *
     * @param type a <code>GoodsType</code> value
     * @param amount The amount of goods.
     * @return The bid price of the given goods.
     */
    public int getBidPrice(GoodsType type, int amount) {
        MarketData data = getMarketData(type);
        return (data == null) ? 0 : amount * data.getCostToBuy();
    }

    /**
     * Gets the price of a given goods when the <code>Player</code> sells.
     *
     * @param type a <code>GoodsType</code> value
     * @param amount The amount of goods.
     * @return The sale price of the given goods.
     */
    public int getSalePrice(GoodsType type, int amount) {
        MarketData data = getMarketData(type);
        return (data == null) ? 0 : amount * data.getPaidForSale();
    }

    /**
     * Gets the price of a given goods when the <code>Player</code> sells.
     *
     * @param goods a <code>Goods</code> value
     * @return an <code>int</code> value
     */
    public int getSalePrice(Goods goods) {
        return getSalePrice(goods.getType(), goods.getAmount());
    }

    /**
     * Gets the arrears for of a given goods type.
     *
     * @param goodsType The <code>GoodsType</code> to get arrears for.
     * @return The arrears.
     */
    public int getArrears(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getArrears();
    }

    /**
     * Sets the arrears associated with a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to set the arrears for.
     * @param value The amount of arrears to set.
     */
    public void setArrears(GoodsType goodsType, int value) {
        MarketData data = requireMarketData(goodsType);
        data.setArrears(value);
    }

    /**
     * Gets the sales of a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to get the sales for.
     * @return The current sales amount.
     */
    public int getSales(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getSales();
    }

    /**
     * Modifies the sales of a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to set the sales for.
     * @param amount The amount of sales to add to the current amount.
     */
    public void modifySales(GoodsType goodsType, int amount) {
        if (amount != 0) {
            MarketData data = requireMarketData(goodsType);
            data.setSales(data.getSales() + amount);
            data.setTraded(true);
        }
    }

    /**
     * Gets the income before taxes for a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to get the income for.
     * @return The current income before taxes.
     */
    public int getIncomeBeforeTaxes(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getIncomeBeforeTaxes();
    }

    /**
     * Modifies the income before taxes on sales of a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to set the income for.
     * @param amount The amount of tax income to add to the current amount.
     */
    public void modifyIncomeBeforeTaxes(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);
        data.setIncomeBeforeTaxes(data.getIncomeBeforeTaxes() + amount);
    }

    /**
     * Gets the income after taxes for a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to get the income for.
     * @return The current income after taxes.
     */
    public int getIncomeAfterTaxes(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getIncomeAfterTaxes();
    }

    /**
     * Modifies the income after taxes on sales of a type of goods.
     *
     * @param goodsType The <code>GoodsType</code> to set the income for.
     * @param amount The amount of tax income to add to the current amount.
     */
    public void modifyIncomeAfterTaxes(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);
        data.setIncomeAfterTaxes(data.getIncomeAfterTaxes() + amount);
    }

    /**
     * Gets the amount of a goods type in the market.
     *
     * @param goodsType The <code>GoodsType</code> to get the amount of.
     * @return The current amount of the goods in the market.
     */
    public int getAmountInMarket(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return (data == null) ? 0 : data.getAmountInMarket();
    }

    /**
     * Has the price of a type of goods changed in this market?
     *
     * @param goodsType The type of goods to consider.
     * @return True if the price has changed.
     */
    public boolean hasPriceChanged(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        return data != null && data.getOldPrice() != 0
            && data.getOldPrice() != data.getCostToBuy();
    }

    /**
     * Clear any price changes for a type of goods.
     *
     * @param goodsType The type of goods to consider.
     */
    public void flushPriceChange(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        if (data != null) {
            data.setOldPrice(data.getCostToBuy());
        }
    }

    /**
     * Make up a <code>ModelMessage</code> describing the change in this
     * <code>Market</code> for a specified type of goods.
     *
     * @param goodsType The <code>GoodsType</code> that has changed price.
     * @return A message describing the change.
     */
    public ModelMessage makePriceChangeMessage(GoodsType goodsType) {
        MarketData data = getMarketData(goodsType);
        int oldPrice = data.getOldPrice();
        int newPrice = data.getCostToBuy();
        return (oldPrice == newPrice) ? null
            : new ModelMessage(ModelMessage.MessageType.MARKET_PRICES,
                               ((newPrice > oldPrice)
                                ? "model.market.priceIncrease"
                                : "model.market.priceDecrease"),
                               this, goodsType)
                .addStringTemplate("%market%", owner.getMarketName())
                .addNamed("%goods%", goodsType)
                .addAmount("%buy%", newPrice)
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
     * @param goodsType The <code>GoodsType</code> to update.
     */
    public void update(GoodsType goodsType) {
        MarketData data = requireMarketData(goodsType);
        if (data != null) data.update();
    }

    /**
     * Get a sale price comparator for this market.
     *
     * @return A suitable <code>Comparator</code>.
     */
    public <T extends AbstractGoods> Comparator<T> getSalePriceComparator() {
        return Comparator.comparingInt((T t)
            -> getSalePrice(t.getType(), t.getAmount())).reversed();
    }

    /**
     * Adds a transaction listener for notification of any transaction
     *
     * @param listener The <code>TransactionListener</code> to add.
     */
    public void addTransactionListener(TransactionListener listener) {
        transactionListeners.add(listener);
    }

    /**
     * Removes a transaction listener
     *
     * @param listener The <code>TransactionListener</code> to remove.
     */
    public void removeTransactionListener(TransactionListener listener) {
        transactionListeners.remove(listener);
    }

    /**
     * Gets the listeners added to this market.
     *
     * @return An array of all the <code>TransactionListener</code>s
     *     added, or an empty array if none found.
     */
    public TransactionListener[] getTransactionListener() {
        return transactionListeners.toArray(new TransactionListener[0]);
    }


    // Interface Ownable

    /**
     * Gets the owner of this <code>Market</code>.
     *
     * @return The owner of this <code>Market</code>.
     */
    @Override
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this <code>Market</code>.
     *
     * @param owner The <code>Player</code> to own this <code>Market</code>.
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

            for (MarketData data : getSortedCopy(marketData.values())) {
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
        marketData.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (MarketData.getXMLElementTagName().equals(tag)) {
            MarketData data = xr.readFreeColGameObject(getGame(), 
                                                       MarketData.class);
            putMarketData(data.getGoodsType(), data);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" owner=").append(owner.getId());
        for (MarketData md : getSortedCopy(marketData.values())) {
            sb.append(" ").append(md);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "market".
     */
    public static String getXMLElementTagName() {
        return "market";
    }
}
