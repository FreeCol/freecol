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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


/**
 * Objects of this class hold the market data for a particular type of
 * good.
 */
public class MarketData extends FreeColGameObject {

    private static final Logger logger = Logger.getLogger(MarketData.class.getName());

    /**
     * Bounds on price movements.
     */
    public static final int MINIMUM_PRICE = 1;
    public static final int MAXIMUM_PRICE = 19;

    /**
     * What type of goods is this.
     */
    private GoodsType goodsType;

    /**
     * Purchase price.
     */
    private int costToBuy;

    /**
     * Sale price.
     */
    private int paidForSale;

    /**
     * Amount of this goods in the market.
     */
    private int amountInMarket;

    /**
     * The initial price.
     */
    private int initialPrice;

    /**
     * Arrears owed to the crown.
     */
    private int arrears;

    /**
     * Total sales.
     */
    private int sales;

    /**
     * Total income before taxes.
     */
    private int incomeBeforeTaxes;

    /**
     * Total income after taxes.
     */
    private int incomeAfterTaxes;

    /**
     * Place to save to old price so as to be able to tell when a price change
     * message should be generated.  Not necessary to serialize.
     */
    private int oldPrice;

    /**
     * Has this good been traded?
     */
    private boolean traded;


    /**
     * Creates a new <code>MarketData</code> instance.
     *
     * @param goodsType a <code>GoodsType</code> value
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
     * Instantiate a new <code>MarketData</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if an error occured during parsing.
     */
    public MarketData(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);

        readFromXML(in);
    }

    /**
     * Instantiates a new <code>MarketData</code> with the given
     * ID. The object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
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
     * Get the <code>CostToBuy</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getCostToBuy() {
        return costToBuy;
    }

    /**
     * Set the <code>CostToBuy</code> value.
     *
     * @param newCostToBuy The new CostToBuy value.
     */
    public final void setCostToBuy(final int newCostToBuy) {
        this.costToBuy = newCostToBuy;
    }

    /**
     * Get the <code>PaidForSale</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getPaidForSale() {
        return paidForSale;
    }

    /**
     * Set the <code>PaidForSale</code> value.
     *
     * @param newPaidForSale The new PaidForSale value.
     */
    public final void setPaidForSale(final int newPaidForSale) {
        this.paidForSale = newPaidForSale;
    }

    /**
     * Get the <code>AmountInMarket</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getAmountInMarket() {
        return amountInMarket;
    }

    /**
     * Set the <code>AmountInMarket</code> value.
     *
     * @param newAmountInMarket The new AmountInMarket value.
     */
    public final void setAmountInMarket(final int newAmountInMarket) {
        this.amountInMarket = newAmountInMarket;
    }

    /**
     * Get the <code>InitialPrice</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getInitialPrice() {
        return initialPrice;
    }

    /**
     * Set the <code>InitialPrice</code> value.
     *
     * @param newInitialPrice The new InitialPrice value.
     */
    public final void setInitialPrice(final int newInitialPrice) {
        this.initialPrice = newInitialPrice;
    }

    /**
     * Get the <code>Arrears</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getArrears() {
        return arrears;
    }

    /**
     * Set the <code>Arrears</code> value.
     *
     * @param newArrears The new Arrears value.
     */
    public final void setArrears(final int newArrears) {
        this.arrears = newArrears;
    }

    /**
     * Get the <code>Sales</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getSales() {
        return sales;
    }

    /**
     * Set the <code>Sales</code> value.
     *
     * @param newSales The new Sales value.
     */
    public final void setSales(final int newSales) {
        this.sales = newSales;
    }

    /**
     * Get the <code>IncomeBeforeTaxes</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getIncomeBeforeTaxes() {
        return incomeBeforeTaxes;
    }

    /**
     * Set the <code>IncomeBeforeTaxes</code> value.
     *
     * @param newIncomeBeforeTaxes The new IncomeBeforeTaxes value.
     */
    public final void setIncomeBeforeTaxes(final int newIncomeBeforeTaxes) {
        this.incomeBeforeTaxes = newIncomeBeforeTaxes;
    }

    /**
     * Get the <code>IncomeAfterTaxes</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getIncomeAfterTaxes() {
        return incomeAfterTaxes;
    }

    /**
     * Set the <code>IncomeAfterTaxes</code> value.
     *
     * @param newIncomeAfterTaxes The new IncomeAfterTaxes value.
     */
    public final void setIncomeAfterTaxes(final int newIncomeAfterTaxes) {
        this.incomeAfterTaxes = newIncomeAfterTaxes;
    }

    /**
     * Get the old price in this <code>MarketData</code>.
     *
     * @return The old price.
     */
    public final int getOldPrice() {
        return oldPrice;
    }

    /**
     * Set the old price in this <code>MarketData</code>.
     *
     * @param oldPrice A `new' old price.
     */
    public void setOldPrice(int oldPrice) {
        this.oldPrice = oldPrice;
    }

    /**
     * Has there been trading in this <code>MarketData</code>?
     *
     * @return Whether trading has occurred.
     **/
    public final boolean getTraded() {
        return traded;
    }

    /**
     * Set the trade status of this <code>MarketData</code>.
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
     */
    public void price() {
        if (!goodsType.isStorable()) return;
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

        costToBuy = newPrice;
        paidForSale = newSalePrice;
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
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        writeAttributes(out);

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        writeAttribute(out, ID_ATTRIBUTE_TAG, getId());

        writeAttribute(out, GOODS_TYPE_TAG, goodsType);

        writeAttribute(out, AMOUNT_TAG, amountInMarket);

        writeAttribute(out, INITIAL_PRICE_TAG, initialPrice);

        writeAttribute(out, ARREARS_TAG, arrears);

        writeAttribute(out, SALES_TAG, sales);

        writeAttribute(out, INCOME_AFTER_TAXES_TAG, incomeAfterTaxes);

        writeAttribute(out, INCOME_BEFORE_TAXES_TAG, incomeBeforeTaxes);

        writeAttribute(out, TRADED_TAG, traded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        goodsType = spec.getType(in, GOODS_TYPE_TAG, GoodsType.class,
                                 (GoodsType)null);
        // @compat 0.9.x
        if (goodsType == null) {
            setDefaultId(getGame());
            goodsType = spec.getType(in, ID_ATTRIBUTE_TAG, GoodsType.class,
                                     (GoodsType)null);
        }
        // end compatibility code

        amountInMarket = getAttribute(in, AMOUNT_TAG, 0);

        initialPrice = getAttribute(in, INITIAL_PRICE_TAG, -1);

        arrears = getAttribute(in, ARREARS_TAG, 0);

        sales = getAttribute(in, SALES_TAG, 0);

        incomeBeforeTaxes = getAttribute(in, INCOME_BEFORE_TAXES_TAG, 0);

        incomeAfterTaxes = getAttribute(in, INCOME_AFTER_TAXES_TAG, 0);

        traded = getAttribute(in, TRADED_TAG, sales != 0);

        update();
        oldPrice = costToBuy;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(getId());
        sb.append(" ").append(goodsType.getId());
        sb.append(" costToBuy=").append(costToBuy);
        sb.append(" paidForSale=").append(paidForSale);
        sb.append(" amountInMarket=").append(amountInMarket);
        sb.append(" initialPrice=").append(initialPrice);
        sb.append(" arrears=").append(arrears);
        sb.append(" sales=").append(sales);
        sb.append(" incomeBeforeTaxes=").append(incomeBeforeTaxes);
        sb.append(" incomeAfterTaxes=").append(incomeAfterTaxes);
        sb.append(" oldPrice=").append(oldPrice);
        sb.append(" traded=").append(traded);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "marketData"
     */
    public static String getXMLElementTagName() {
        return "marketData";
    }
}
