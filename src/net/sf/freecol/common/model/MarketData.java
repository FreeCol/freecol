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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Objects of this class hold the market data for a particular type of
 * good.
 */
public class MarketData extends FreeColObject {

    /**
     * Describe costToBuy here.
     */
    private int costToBuy;

    /**
     * Describe paidForSale here.
     */
    private int paidForSale;

    /**
     * Describe amountInMarket here.
     */
    private int amountInMarket;

    /**
     * Describe initialPrice here.
     */
    private int initialPrice;

    /**
     * Describe arrears here.
     */
    private int arrears;

    /**
     * Describe sales here.
     */
    private int sales;

    /**
     * Describe incomeBeforeTaxes here.
     */
    private int incomeBeforeTaxes;

    /**
     * Describe incomeAfterTaxes here.
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
     * Package constructor: This class is only supposed to be constructed
     * by {@link Market}.
     * 
     */
    public MarketData() {
        traded = false;
    }
    
    /**
     * Creates a new <code>MarketData</code> instance.
     *
     * @param goodsType a <code>GoodsType</code> value
     */
    public MarketData(GoodsType goodsType) {
        setId(goodsType.getId());
        traded = false;
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
        this.traded |= this.sales != newSales;
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
        return (oldPrice != 0) ? oldPrice : costToBuy;
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
     * Get the type of goods of this <code>MarketData</code>.
     *
     * @return The goods type for this data.
     */
    public final GoodsType getGoodsType() {
        return Specification.getSpecification().getGoodsType(getId());
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        out.writeAttribute("amount", Integer.toString(amountInMarket));
        out.writeAttribute("initialPrice", Integer.toString(initialPrice));
        out.writeAttribute("arrears", Integer.toString(arrears));
        out.writeAttribute("sales", Integer.toString(sales));
        out.writeAttribute("incomeBeforeTaxes", Integer.toString(incomeBeforeTaxes));
        out.writeAttribute("incomeAfterTaxes", Integer.toString(incomeAfterTaxes));
        out.writeAttribute("traded", Boolean.toString(traded));
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        amountInMarket = Integer.parseInt(in.getAttributeValue(null, "amount"));
        initialPrice = getAttribute(in, "initialPrice", -1);
        // support for older savegames
        if (initialPrice < 0) {
            initialPrice = getGoodsType().getInitialSellPrice();
        }
        arrears = Integer.parseInt(in.getAttributeValue(null, "arrears"));
        sales = Integer.parseInt(in.getAttributeValue(null, "sales"));
        incomeBeforeTaxes = Integer.parseInt(in.getAttributeValue(null, "incomeBeforeTaxes"));
        incomeAfterTaxes = Integer.parseInt(in.getAttributeValue(null, "incomeAfterTaxes"));
        traded = getAttribute(in, "traded", sales != 0);
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "marketData";
    }

} 
