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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * Objects of this class hold the market data for a particular type of
 * goods.
 */
public class MarketData extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(MarketData.class.getName());

    public static final String TAG = "marketData";

    /** Inclusive lower bound on goods price. */
    public static final int MINIMUM_PRICE = 1;

    /** Inclusive upper bound on goods price. */
    public static final int MAXIMUM_PRICE = 19;


    /** What type of goods is this. */
    private GoodsType goodsType;

    /** Current purchase price. */
    private int costToBuy;

    /** Current selling price. */
    private int paidForSale;

    /** Amount of this goods in the market. */
    private int amountInMarket;

    /** The initial price. */
    private int initialPrice;

    /** Arrears owed to the crown. */
    private int arrears;

    /** Total sales. */
    private int sales;

    /** Total income before taxes. */
    private int incomeBeforeTaxes;

    /** Total income after taxes. */
    private int incomeAfterTaxes;

    /**
     * Place to save to old price so as to be able to tell when a price change
     * message should be generated.  Not necessary to serialize.
     */
    private int oldPrice;

    /** Has this good been traded? */
    private boolean traded;


    /**
     * Creates a new {@code MarketData} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param goodsType The {@code GoodsType} this market data describes.
     */
    public MarketData(Game game, GoodsType goodsType) {
        super(game);

        this.goodsType = goodsType;
        paidForSale = goodsType.getInitialSellPrice();
        costToBuy = goodsType.getInitialBuyPrice();
        amountInMarket = goodsType.getInitialAmount();
        initialPrice = goodsType.getInitialSellPrice();
        arrears = 0;
        sales = 0;
        incomeBeforeTaxes = 0;
        incomeAfterTaxes = 0;
        oldPrice = costToBuy;
        traded = false;
    }

    /**
     * Creates a new {@code MarketData} with the given identifier.
     *
     * The object should be initialized later.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public MarketData(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the type of goods of this {@code MarketData}.
     *
     * @return The goods type for this data.
     */
    public final GoodsType getGoodsType() {
        return goodsType;
    }

    /**
     * Get the current purchase price.
     *
     * @return The purchase price.
     */
    public final int getCostToBuy() {
        return costToBuy;
    }

    /**
     * Set the current purchase price.
     *
     * @param newCostToBuy The new purchase price.
     */
    public final void setCostToBuy(final int newCostToBuy) {
        this.costToBuy = newCostToBuy;
    }

    /**
     * Get the current sale price.
     *
     * @return The sale price.
     */
    public final int getPaidForSale() {
        return paidForSale;
    }

    /**
     * Set the current sale price.
     *
     * @param newPaidForSale The new sale price.
     */
    public final void setPaidForSale(final int newPaidForSale) {
        this.paidForSale = newPaidForSale;
    }

    /**
     * Get the amount of the goods type in the market.
     *
     * @return The amount of goods.
     */
    public final int getAmountInMarket() {
        return amountInMarket;
    }

    /**
     * Set the amount of goods in the market.
     *
     * @param newAmountInMarket The new amount of goods in the market.
     */
    public final void setAmountInMarket(final int newAmountInMarket) {
        this.amountInMarket = newAmountInMarket;
    }

    /**
     * Get the initial price of these goods.
     *
     * @return The initial price.
     */
    public final int getInitialPrice() {
        return initialPrice;
    }

    /**
     * Set the initial price value.
     *
     * @param newInitialPrice The new initial price value.
     */
    public final void setInitialPrice(final int newInitialPrice) {
        this.initialPrice = newInitialPrice;
    }

    /**
     * Get the amount of arrears owned to the crown for this goods type.
     *
     * @return The arrears amount.
     */
    public final int getArrears() {
        return arrears;
    }

    /**
     * Set the amount of arrears owed to the crown.
     *
     * @param newArrears The new amount of arrears.
     */
    public final void setArrears(final int newArrears) {
        this.arrears = newArrears;
    }

    /**
     * Get the total sales.
     *
     * @return The total sales.
     */
    public final int getSales() {
        return sales;
    }

    /**
     * Set the total sales value.
     *
     * @param newSales The new total sales value.
     */
    public final void setSales(final int newSales) {
        this.sales = newSales;
    }

    /**
     * Get the income before taxes from trading in this goods type.
     *
     * @return The income before taxes.
     */
    public final int getIncomeBeforeTaxes() {
        return incomeBeforeTaxes;
    }

    /**
     * Set the income before taxes value.
     *
     * @param newIncomeBeforeTaxes The new income before taxes value.
     */
    public final void setIncomeBeforeTaxes(final int newIncomeBeforeTaxes) {
        this.incomeBeforeTaxes = newIncomeBeforeTaxes;
    }

    /**
     * Get the income after taxes from trading in this goods type.
     *
     * @return The income after taxes.
     */
    public final int getIncomeAfterTaxes() {
        return incomeAfterTaxes;
    }

    /**
     * Set the income after taxes value.
     *
     * @param newIncomeAfterTaxes The new income after taxes value.
     */
    public final void setIncomeAfterTaxes(final int newIncomeAfterTaxes) {
        this.incomeAfterTaxes = newIncomeAfterTaxes;
    }

    /**
     * Get the old price.
     *
     * @return The old price.
     */
    public final int getOldPrice() {
        return oldPrice;
    }

    /**
     * Set the old price.
     *
     * @param oldPrice A `new' old price.
     */
    public void setOldPrice(int oldPrice) {
        this.oldPrice = oldPrice;
    }

    /**
     * Set the trading status of this {@code MarketData}.
     *
     * @param traded The trade status to set.
     **/
    public void setTraded(boolean traded) {
        this.traded = traded;
    }

    /**
     * Has the price of this goods type changed?
     *
     * @return True if the price has changed.
     */
    public boolean hasPriceChanged() {
        return oldPrice != 0 && oldPrice != costToBuy;
    }

    /**
     * Has this goods type been traded in the market?
     *
     * @return True if the goods type has been traded.
     */
    public boolean hasBeenTraded() {
        return traded;
    }

    /**
     * Checks if the price has increased since the last flush.
     *
     * @return True if the current cost to buy is greater than the old price.
     */
    public boolean isPriceIncrease() {
        return costToBuy > oldPrice;
    }

    /**
     * Clear any price changes for this goods type by 
     * updating the old price to the current price.
     */
    public void flushPriceChange() {
        oldPrice = costToBuy;
    }

    /**
     * Gets the price of a given amount of these goods when the player buys.
     *
     * @param amount The amount of goods.
     * @return The bid price.
     */
    public int getBidPrice(int amount) {
        return amount * costToBuy;
    }

    /**
     * Gets the price of a given amount of these goods when the player sells.
     *
     * @param amount The amount of goods.
     * @return The sale price.
     */
    public int getSalePrice(int amount) {
        return amount * paidForSale;
    }

    /**
     * Records a sale of these goods.
     *
     * @param amount The quantity sold.
     */
    public void addSales(int amount) {
        this.sales += amount;
        this.traded = true;
    }

    /**
     * Adds to the accumulated income before taxes.
     *
     * @param amount The income amount to add.
     */
    public void addIncomeBeforeTaxes(int amount) {
        this.incomeBeforeTaxes += amount;
    }

    /**
     * Adds to the accumulated income after taxes.
     *
     * @param amount The income amount to add.
     */
    public void addIncomeAfterTaxes(int amount) {
        this.incomeAfterTaxes += amount;
    }

    /**
     * Adds an amount of goods to this market data, ensuring the 
     * market amount does not drop below a specified threshold.
     *
     * @param amount The amount of goods to add (can be negative).
     * @param minimumAmount The floor for the goods amount in market.
     * @return True if the price changed as a result.
     */
    public boolean addGoods(int amount, int minimumAmount) {
        // Markets are bottomless, amount can not go below the threshold
        this.amountInMarket = Math.max(minimumAmount, this.amountInMarket + amount);
        this.traded = true;
        return price(); // Recalculate and return if price changed
    }

    /**
     * Utility to keep a price within the allowed market bounds.
     * * @param price The calculated price.
     * @return The price clamped between MINIMUM_PRICE and MAXIMUM_PRICE.
     */
    private int clampPrice(int price) {
        return Math.max(MINIMUM_PRICE, Math.min(MAXIMUM_PRICE, price));
    }

    /**
     * Limits price fluctuations to prevent market exploitation.
     * If the new price exceeds the 'diff' threshold from the current cost,
     * the market supply is adjusted to maintain economic balance.
     *
     * @param newPrice The newly calculated price.
     * @param diff The allowed price difference.
     * @return The stabilized price.
     */
    private int applyStabilityLimit(int newPrice, int diff) {
        if (costToBuy <= 0) return newPrice;

        float amountPrice = initialPrice * (goodsType.getInitialAmount() / (float) amountInMarket);

        // Price rising too fast
        if (newPrice > costToBuy + diff) {
            float adjustedAmountPrice =
                amountPrice - (newPrice - (costToBuy + diff));

            adjustMarketAmountForPrice(adjustedAmountPrice);

            int stabilizedPrice = costToBuy + diff;
            logger.info("Clamped price rise for " + getId()
                + " from " + newPrice
                + " to " + stabilizedPrice);
            return stabilizedPrice;
        }

        // Price falling too fast
        if (newPrice < costToBuy - diff) {
            float adjustedAmountPrice =
                amountPrice + ((costToBuy - diff) - newPrice);

            adjustMarketAmountForPrice(adjustedAmountPrice);

            int stabilizedPrice = costToBuy - diff;
            logger.info("Clamped price fall for " + getId()
                + " from " + newPrice
                + " to " + stabilizedPrice);
            return stabilizedPrice;
        }

        return newPrice;
    }

    /**
     * Recalculates amountInMarket to match the adjusted amountPrice
     * used in the original stability clamp logic.
     *
     * This method preserves the original behavior exactly:
     *
     *     amountInMarket = round(initialAmount * (initialPrice / amountPrice))
     *
     * @param adjustedAmountPrice The modified amountPrice value
     *                            after applying the stability correction.
     */
    private void adjustMarketAmountForPrice(float adjustedAmountPrice) {
        this.amountInMarket = Math.round(
            goodsType.getInitialAmount() * (initialPrice / adjustedAmountPrice)
        );
    }

    /**
     * Updates the internal price state and notifies listeners if a change occurred.
     *
     * @param newPrice The final calculated purchase price.
     * @param newSalePrice The final calculated sale price.
     * @return True if either price changed.
     */
    private boolean updatePriceState(int newPrice, int newSalePrice) {
        int oldCostToBuy = costToBuy;
        int oldPaidForSale = paidForSale;

        this.costToBuy = newPrice;
        this.paidForSale = newSalePrice;

        if (costToBuy != oldCostToBuy) {
            firePropertyChange(goodsType.getId(), oldCostToBuy, costToBuy);
            return true;
        } else if (paidForSale != oldPaidForSale) {
            firePropertyChange(goodsType.getId(), oldPaidForSale, paidForSale);
            return true;
        }
        
        return false;
    }

    private boolean isNewWorldPriceCapped(int newSalePrice) {
        return newSalePrice > initialPrice + 2
            && (goodsType.isNewWorldGoodsType() 
                || (goodsType.getInputType() != null && goodsType.getInputType().isNewWorldGoodsType()));
    }

    /**
     * Adjust the prices.
     *
     * Sets the costToBuy and paidForSale fields from the amount in
     * the market, initial price and goods-type specific information.
     * Ensures that prices change incrementally with a clamping
     * mechanism.
     *
     * @return True if the price changes.
     */
    public boolean price() {
        if (!goodsType.isStorable()) return false;
        
        int diff = goodsType.getPriceDifference();
        
        // Calculate base price from market supply
        float amountPrice = initialPrice * (goodsType.getInitialAmount() / (float) amountInMarket);
        int newSalePrice = Math.round(amountPrice);
        int newPrice = newSalePrice + diff;

        // Apply New World goods ceiling
        if (isNewWorldPriceCapped(newSalePrice)) {
            newSalePrice = initialPrice + 2;
            newPrice = newSalePrice + diff;
        }

        // Apply stability hack (prevents rapid exploitation)
        newPrice = applyStabilityLimit(newPrice, diff);

        // Absolute clamping (MIN/MAX)
        newPrice = clampPrice(newPrice);
        newSalePrice = newPrice - diff;

        // Floor protection (ensure sale price >= 1)
        if (newSalePrice < MINIMUM_PRICE) {
            newSalePrice = MINIMUM_PRICE;
            newPrice = newSalePrice + diff;
        }

        // Commit changes and return result
        return updatePriceState(newPrice, newSalePrice);
    }

    /**
     * Update the pricing of this datum, ignoring the price change clamp.
     */
    public void update() {
        costToBuy = -1; // Disable price change clamping
        price();
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        MarketData o = copyInCast(other, MarketData.class);
        if (o == null || !super.copyIn(o)) return false;
        this.goodsType = o.getGoodsType();
        this.costToBuy = o.getCostToBuy();
        this.paidForSale = o.getPaidForSale();
        this.amountInMarket = o.getAmountInMarket();
        this.initialPrice = o.getInitialPrice();
        this.arrears = o.getArrears();
        this.sales = o.getSales();
        this.incomeBeforeTaxes = o.getIncomeBeforeTaxes();
        this.incomeAfterTaxes = o.getIncomeAfterTaxes();
        this.oldPrice = o.getOldPrice();
        this.traded = o.hasBeenTraded();
        return true;
    }


    // Serialization

    private static final String AMOUNT_TAG = "amount";
    private static final String ARREARS_TAG = "arrears";
    private static final String GOODS_TYPE_TAG = "goods-type";
    private static final String INCOME_AFTER_TAXES_TAG = "incomeAfterTaxes";
    private static final String INCOME_BEFORE_TAXES_TAG = "incomeBeforeTaxes";
    private static final String INITIAL_PRICE_TAG = "initialPrice";
    private static final String SALES_TAG = "sales";
    private static final String TRADED_TAG = "traded";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(GOODS_TYPE_TAG, goodsType);

        xw.writeAttribute(AMOUNT_TAG, amountInMarket);

        xw.writeAttribute(INITIAL_PRICE_TAG, initialPrice);

        xw.writeAttribute(ARREARS_TAG, arrears);

        xw.writeAttribute(SALES_TAG, sales);

        xw.writeAttribute(INCOME_AFTER_TAXES_TAG, incomeAfterTaxes);

        xw.writeAttribute(INCOME_BEFORE_TAXES_TAG, incomeBeforeTaxes);

        xw.writeAttribute(TRADED_TAG, traded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        goodsType = xr.getType(spec, GOODS_TYPE_TAG, GoodsType.class,
                               (GoodsType)null);

        amountInMarket = xr.getAttribute(AMOUNT_TAG, 0);

        initialPrice = xr.getAttribute(INITIAL_PRICE_TAG, -1);

        arrears = xr.getAttribute(ARREARS_TAG, 0);

        sales = xr.getAttribute(SALES_TAG, 0);

        incomeBeforeTaxes = xr.getAttribute(INCOME_BEFORE_TAXES_TAG, 0);

        incomeAfterTaxes = xr.getAttribute(INCOME_AFTER_TAXES_TAG, 0);

        traded = xr.getAttribute(TRADED_TAG, sales != 0);

        update();
        oldPrice = costToBuy;
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
            .append(' ').append((goodsType == null) ? "null"
                : goodsType.getId())
            .append(" costToBuy=").append(costToBuy)
            .append(" paidForSale=").append(paidForSale)
            .append(" amountInMarket=").append(amountInMarket)
            .append(" initialPrice=").append(initialPrice)
            .append(" arrears=").append(arrears)
            .append(" sales=").append(sales)
            .append(" incomeBeforeTaxes=").append(incomeBeforeTaxes)
            .append(" incomeAfterTaxes=").append(incomeAfterTaxes)
            .append(" oldPrice=").append(oldPrice)
            .append(" traded=").append(traded)
            .append(']');
        return sb.toString();
    }
}
