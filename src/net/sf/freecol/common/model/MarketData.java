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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * Objects of this class hold the market data for a particular type of
 * goods.
 */
public class MarketData extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(MarketData.class.getName());

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
     * Creates a new <code>MarketData</code> instance.
     *
     * @param goodsType The <code>GoodsType</code> this market data describes.
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
     * Creates a new <code>MarketData</code> with the given
     * identifier.  The object should later be initialized by calling
     * either {@link #readFromXML(FreeColXMLReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public MarketData(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the type of goods of this <code>MarketData</code>.
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
     * Has there been trading in this <code>MarketData</code>?
     *
     * @return True if trading has occurred.
     **/
    public final boolean getTraded() {
        return traded;
    }

    /**
     * Set the trading status of this <code>MarketData</code>.
     *
     * @param traded The trade status to set.
     **/
    public void setTraded(boolean traded) {
        this.traded = traded;
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
        float amountPrice = initialPrice * (goodsType.getInitialAmount()
            / (float) amountInMarket);
        int newSalePrice = Math.round(amountPrice);
        int newPrice = newSalePrice + diff;

        // Work-around to limit prices of new world goods
        // and related manufactured goods.
        if ((goodsType.isNewWorldGoodsType()
             || (goodsType.getInputType() != null
                 && goodsType.getInputType().isNewWorldGoodsType()))
            && newSalePrice > initialPrice + 2) {
            newSalePrice = initialPrice + 2;
            newPrice = newSalePrice + diff;
        }

        // Another hack to prevent price changing too fast in one hit.
        // Push the amount in market back as well to keep this stable.
        //
        // Prices that change by more than the buy/sell difference
        // allow big traders to exploit the market and extract free
        // money... not sure I want to be fighting economic reality
        // but game balance demands it here.
        if (costToBuy > 0) {
            if (newPrice > costToBuy + diff) {
                amountPrice -= newPrice - (costToBuy + diff);
                amountInMarket = Math.round(goodsType.getInitialAmount()
                    * (initialPrice / amountPrice));
                logger.info("Clamped price rise for " + getId()
                    + " from " + newPrice
                    + " to " + (costToBuy + diff));
                newPrice = costToBuy + diff;
            } else if (newPrice < costToBuy - diff) {
                amountPrice += (costToBuy - diff) - newPrice;
                amountInMarket = Math.round(goodsType.getInitialAmount()
                    * (initialPrice / amountPrice));
                logger.info("Clamped price fall for " + getId()
                    + " from " + newPrice
                    + " to " + (costToBuy - diff));
                newPrice = costToBuy - diff;
            }
            newSalePrice = newPrice - diff;
        }

        // Clamp extremes.
        if (newPrice > MAXIMUM_PRICE) {
            newPrice = MAXIMUM_PRICE;
            newSalePrice = newPrice - diff;
        } else if (newSalePrice < MINIMUM_PRICE) {
            newSalePrice = MINIMUM_PRICE;
            newPrice = newSalePrice + diff;
        }

        int oldCostToBuy = costToBuy, oldPaidForSale = paidForSale;
        costToBuy = newPrice;
        paidForSale = newSalePrice;
        if (costToBuy != oldCostToBuy) {
            firePropertyChange(goodsType.getId(), oldCostToBuy, costToBuy);
        } else if (paidForSale != oldPaidForSale) {
            firePropertyChange(goodsType.getId(), oldPaidForSale, paidForSale);
        }            
        return costToBuy != oldCostToBuy || paidForSale != oldPaidForSale;
    }

    /**
     * Update the pricing of this datum, ignoring the price change clamp.
     */
    public void update() {
        costToBuy = -1; // Disable price change clamping
        price();
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
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[").append(getId())
            .append(" ").append(goodsType.getId())
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
            .append("]");
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
     * @return "marketData"
     */
    public static String getXMLElementTagName() {
        return "marketData";
    }
}
