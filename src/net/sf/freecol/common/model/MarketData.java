/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


/**
 * Objects of this class hold the market data for a particular type of
 * good.
 */
public class MarketData extends FreeColGameObject {

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
     * Instantiates a new <code>MarketData</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public MarketData(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
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
     * Adjust the prices.  Sets the costToBuy and paidForSale fields
     * from the amount in the market, initial price and goods-type
     * specific information.
     */
    public void price() {
        if (!goodsType.isStorable()) return;
        int newSalePrice = Math.round(goodsType.getInitialAmount()
                                      * initialPrice
                                      / (float) amountInMarket);
        int newPrice = newSalePrice + goodsType.getPriceDifference();

        // dirty work-around to limit prices of new world goods
        // and related manufactured goods
        if ((goodsType.isNewWorldGoodsType()
             || (goodsType.getRawMaterial() != null
                 && goodsType.getRawMaterial().isNewWorldGoodsType()))
            && newSalePrice > initialPrice + 2) {
            newSalePrice = initialPrice + 2;
            newPrice = newSalePrice + goodsType.getPriceDifference();
        }

        if (newPrice > MAXIMUM_PRICE) {
            newPrice = MAXIMUM_PRICE;
            newSalePrice = newPrice - goodsType.getPriceDifference();
        } else if (newSalePrice < MINIMUM_PRICE) {
            newSalePrice = MINIMUM_PRICE;
            newPrice = newSalePrice + goodsType.getPriceDifference();
        }

        costToBuy = newPrice;
        paidForSale = newSalePrice;
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
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code>
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE, getId());
        out.writeAttribute("goods-type", goodsType.getId());
        out.writeAttribute("amount", Integer.toString(amountInMarket));
        out.writeAttribute("initialPrice", Integer.toString(initialPrice));
        out.writeAttribute("arrears", Integer.toString(arrears));
        out.writeAttribute("sales", Integer.toString(sales));
        out.writeAttribute("incomeBeforeTaxes",
            Integer.toString(incomeBeforeTaxes));
        out.writeAttribute("incomeAfterTaxes",
            Integer.toString(incomeAfterTaxes));
        out.writeAttribute("traded", Boolean.toString(traded));
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        String goodsTypeStr = in.getAttributeValue(null, "goods-type");
        if (goodsTypeStr == null) {
            // TODO: backward compatibility, remove in 0.11.x
            goodsTypeStr = in.getAttributeValue(null, ID_ATTRIBUTE);
            setDefaultId(getGame());
        } else {
            setId(in.getAttributeValue(null, ID_ATTRIBUTE));
        }
        if (goodsTypeStr == null) {
            throw new XMLStreamException("Missing goods-type");
        }
        goodsType = getSpecification().getGoodsType(goodsTypeStr);
        amountInMarket = getAttribute(in, "amount", 0);
        initialPrice = getAttribute(in, "initialPrice", -1);
        arrears = getAttribute(in, "arrears", 0);
        sales = getAttribute(in, "sales", 0);
        incomeBeforeTaxes = getAttribute(in, "incomeBeforeTaxes", 0);
        incomeAfterTaxes = getAttribute(in, "incomeAfterTaxes", 0);
        traded = getAttribute(in, "traded", sales != 0);
        price();
        oldPrice = costToBuy;
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "marketData"
     */
    public static String getXMLElementTagName() {
        return "marketData";
    }
} 
