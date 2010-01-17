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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;

import org.w3c.dom.Element;

/**
 * This class implements a simple economic model whereby a market holds all
 * goods that have been sold and the price of a particular type of good is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject implements Ownable {

    public static final int MINIMUM_PRICE = 1, MAXIMUM_PRICE = 19;

    private Player owner;
    
    /**
     * Constant for specifying the access to this <code>Market</code>
     * when {@link #buy(GoodsType, int, Player) buying} and
     * {@link #sell(GoodsType, int, Player, int) selling} goods.
     */
    public static final int EUROPE = 0, CUSTOM_HOUSE = 1;
    
    private final Map<GoodsType, MarketData> marketData
        = new HashMap<GoodsType, MarketData>();

    private ArrayList<TransactionListener> transactionListeners
        = new ArrayList<TransactionListener>();
    
    // ----------------------------------------------------------- constructors

    public Market(Game game, Player player) {
        super(game);
        this.owner = player;
        
        /*
         * Create the objects to hold the market data for each type of
         * goods and seed these objects with the initial amount of
         * each type of goods.
         */
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            MarketData data = new MarketData(goodsType);
            if (goodsType.isStorable()) {
                data.setAmountInMarket(goodsType.getInitialAmount());
                data.setPaidForSale(goodsType.getInitialSellPrice());
                data.setCostToBuy(goodsType.getInitialBuyPrice());
                data.setInitialPrice(goodsType.getInitialSellPrice());
                priceGoods(goodsType);
                data.setOldPrice(data.getCostToBuy());
            }
            marketData.put(goodsType, data);
        }
    }

    /**
     * Initiates a new <code>Market</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Market(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>Market</code> from an
     * XML representation.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize
     *      this object.
     */
    public Market(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Market</code> with the given ID.
     * The object should later be initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Market(Game game, String id) {
        super(game, id);
    }
    
    /**
     * Adjust the price for a particular type of goods.
     *
     * @param goodsType The type of goods to consider.
     */
    private void priceGoods(GoodsType goodsType) {
        MarketData data = marketData.get(goodsType);
        if (data != null) {
            int newSalePrice = data.getInitialPrice();
            newSalePrice = Math.round(newSalePrice * goodsType.getInitialAmount()
                                      / (float) data.getAmountInMarket());
            int newPrice = newSalePrice + goodsType.getPriceDifference();

            // dirty work-around to limit prices of new world goods
            // and related manufactured goods
            if ((goodsType.isNewWorldGoodsType()
                 || (goodsType.getRawMaterial() != null
                     && goodsType.getRawMaterial().isNewWorldGoodsType()))
                && newSalePrice > data.getInitialPrice() + 2) {
                newSalePrice = data.getInitialPrice() + 2;
                newPrice = newSalePrice + goodsType.getPriceDifference();
            }

            if (newPrice > MAXIMUM_PRICE) {
                newPrice = MAXIMUM_PRICE;
                newSalePrice = newPrice - goodsType.getPriceDifference();
            } else if (newSalePrice < MINIMUM_PRICE) {
                newSalePrice = MINIMUM_PRICE;
                newPrice = newSalePrice + goodsType.getPriceDifference();
            }

            data.setCostToBuy(newPrice);
            data.setPaidForSale(newSalePrice);
        }
    }

    // ------------------------------------------------------------ API methods

    /**
     * Return the market data for a type of goods.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>MarketData</code> value
     */
    public MarketData getMarketData(GoodsType goodsType) {
        return marketData.get(goodsType);
    }

    /**
     * Describe <code>putMarketData</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param data a <code>MarketData</code> value
     */
    public void putMarketData(GoodsType goodsType, MarketData data) {
        marketData.put(goodsType, data);
    }

    /**
     * Gets the owner of this <code>Market</code>.
     *
     * @return The owner of this <code>Market</code>.
     */
    public Player getOwner() {
        return owner;
    }

    /**
     * Sets the owner of this <code>Market</code>.
     *
     * @param owner The <code>Player</code> that shall own this <code>Market</code>.
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    /**
     * Determines the cost to buy a single unit of a particular type of good.
     *
     * @param type A <code>GoodsType</code> value.
     * @return The cost to buy a single unit of the given type of goods.
     */
    public int costToBuy(GoodsType type) {
        MarketData data = marketData.get(type);
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
    public int paidForSale(GoodsType type) {
        MarketData data = marketData.get(type);
        return (data == null) ? 0 : data.getPaidForSale();
    }


    /**
     * Sells an amount of a particular type of good with the proceeds
     * of the sale being paid to a supplied player. The goods is sold
     * using {@link #EUROPE} as the accesspoint for this market.
     * Note that post-independence this no longer refers to a specific port.
     *
     * @param goods The <code>Goods</code> object being sold.
     * @param player The <code>Player</code> selling the goods.
     */
    public void sell(Goods goods, Player player) {
        sell(goods.getType(), goods.getAmount(), player, Market.EUROPE);
    }

    /**
     * Sells an amount of a particular type of good with the proceeds
     * of the sale being paid to a supplied player. The goods is
     * sold using {@link #EUROPE} as the accesspoint for this market.
     *
     * @param type The type of goods to be sold.
     * @param amount The amount of goods to be sold.
     * @param player The <code>Player</code> selling the goods.
     */
    public void sell(GoodsType type, int amount, Player player) {
        sell(type, amount, player, Market.EUROPE);
    }

    /**
     * Sells an amount of a particular type of good with the proceeds
     * of the sale being paid to a supplied player.
     *
     * @param type   The type of goods to be sold.
     * @param amount The amount of goods to be sold.
     * @param player The <code>Player</code> selling the goods.
     * @param marketAccess The place where goods are traded.
     */
    public void sell(GoodsType type, int amount, Player player, int marketAccess) {
        if (player.canTrade(type, marketAccess)) {
            int tax = player.getTax();
            int incomeBeforeTaxes = getSalePrice(type, amount);
            int incomeAfterTaxes = ((100 - tax) * incomeBeforeTaxes) / 100;
            player.modifyGold(incomeAfterTaxes);
            player.modifySales(type, amount);
            player.modifyIncomeBeforeTaxes(type, incomeBeforeTaxes);
            player.modifyIncomeAfterTaxes(type, incomeAfterTaxes);
            
            amount = (int) player.getFeatureContainer()
                .applyModifier(amount, "model.modifier.tradeBonus",
                               type, getGame().getTurn());
            addGoodsToMarket(type, amount);
        } else {
            addModelMessage(this, ModelMessage.MessageType.WARNING,
                            "model.europe.market", "%goods%", type.getName());
        }
    }

    /**
     * Buys an amount of a particular type of good with the cost being
     * met by a supplied player.
     *
     * @param goodsType The type of the good that is being bought.
     * @param amount The amount of goods that are being bought.
     * @param player The <code>Player</code> buying the goods.
     * @throws IllegalStateException If the <code>player</code> cannot afford
     *                               to buy the goods.
     */
    public void buy(GoodsType goodsType, int amount, Player player) {
        int price = getBidPrice(goodsType, amount);
        if (price > player.getGold()) {
            throw new IllegalStateException("Player " + player.getName()
                                            + " tried to buy " + Integer.toString(amount)
                                            + " " + goodsType.toString()
                                            + " for " + Integer.toString(price)
                                            + " but has " + Integer.toString(player.getGold())
                                            + " gold.");
        }
        player.modifyGold(-price);
        player.modifySales(goodsType, -amount);
        player.modifyIncomeBeforeTaxes(goodsType, -price);
        player.modifyIncomeAfterTaxes(goodsType, -price);

        amount = (int) player.getFeatureContainer()
            .applyModifier(amount, "model.modifier.tradeBonus",
                           goodsType, getGame().getTurn());
        addGoodsToMarket(goodsType, -amount);
    }

    /**
     * Add (or remove) some goods to this market.
     * 
     * @param goodsType The <code>GoodsType</code> to add.
     * @param amount The amount of goods.
     */
    public void addGoodsToMarket(GoodsType goodsType, int amount) {
        MarketData data = getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(goodsType);
            marketData.put(goodsType, data);
        }

        /*
         * Markets are bottomless, amount can not go below the
         * threshold of 100.
         */
        data.setAmountInMarket(Math.max(100, data.getAmountInMarket() + amount));
        data.setTraded(true);
        priceGoods(goodsType);
    }

    /**
     * Gets the price of a given goods when the <code>Player</code> buys.
     *
     * @param type a <code>GoodsType</code> value
     * @param amount The amount of goods.
     * @return The bid price of the given goods.
     */
    public int getBidPrice(GoodsType type, int amount) {
        MarketData data = marketData.get(type);
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
        MarketData data = marketData.get(type);
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
     * Has the price of a type of goods changed in this market?
     *
     * @param goodsType The type of goods to consider.
     * @return True if the price has changed.
     */
    public boolean hasPriceChanged(GoodsType goodsType) {
        MarketData data = marketData.get(goodsType);
        return data != null && data.getOldPrice() != data.getCostToBuy();
    }

    /**
     * Clear any price changes for a type of goods.
     *
     * @param goodsType The type of goods to consider.
     */
    public void flushPriceChange(GoodsType goodsType) {
        MarketData data = marketData.get(goodsType);
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
        MarketData data = marketData.get(goodsType);
        int oldPrice = data.getOldPrice();
        int newPrice = data.getCostToBuy();
        int newSalePrice = data.getPaidForSale();
        return (oldPrice == newPrice) ? null
            : new ModelMessage(this,
                               ModelMessage.MessageType.MARKET_PRICES,
                               goodsType,
                               ((newPrice > oldPrice)
                                ? "model.market.priceIncrease"
                                : "model.market.priceDecrease"),
                               "%market%", owner.getMarketName(),
                               "%goods%", goodsType.getName(),
                               "%buy%", String.valueOf(newPrice),
                               "%sell%", String.valueOf(newSalePrice));
    }

    /**
     * Adds a transaction listener for notification of any transaction
     *
     * @param listener the listener
     */
    public void addTransactionListener(TransactionListener listener) {
        transactionListeners.add(listener);
    }

    /**
     * Removes a transaction listener
     *
     * @param listener the listener
     */
    public void removeTransactionListener(TransactionListener listener) {
        transactionListeners.remove(listener);
    }

    /**
     * Returns an array of all the TransactionListener added to this Market.
     *
     * @return all of the TransactionListener added or an empty array if no
     * listeners have been added
     */
    public TransactionListener[] getTransactionListener() {
        return transactionListeners.toArray(new TransactionListener[0]);
    }


    // -------------------------------------------------------- support methods

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

        out.writeAttribute("ID", getId());
        out.writeAttribute("owner", owner.getId());
        
        for (MarketData data : marketData.values()) {
            data.toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException on problems with the stream.
     * @todo Get rid of the for-priceGoods() when the server sends all
     * price changes.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        owner = getFreeColGameObject(in, "owner", Player.class);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(MarketData.getXMLElementTagName())) {
                MarketData data = new MarketData();
                data.readFromXML(in);
                marketData.put(data.getGoodsType(), data);
            }
        }
        
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                priceGoods(goodsType);
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "market";
    }
}
