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
    
    private boolean initialized = false;

    /**
     * Constant for specifying the access to this <code>Market</code>
     * when {@link #buy(GoodsType, int, Player) buying} and
     * {@link #sell(GoodsType, int, Player, int) selling} goods.
     */
    public static final int EUROPE = 0, CUSTOM_HOUSE = 1;
    
    private final Map<GoodsType, MarketData> marketData = new HashMap<GoodsType, MarketData>();

    private ArrayList<TransactionListener> transactionListeners =
        new ArrayList<TransactionListener>();
    
    // ----------------------------------------------------------- constructors

    public Market(Game game, Player player) {
        super(game);
        this.owner = player;
        
        
        /* create the objects to hold the market data for each type of
         * goods and seed these objects with the initial amount of
         * each type of goods
         */
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            MarketData data = new MarketData();
            data.setId(goodsType.getId());
            if (goodsType.isStorable()) {
                data.setAmountInMarket(goodsType.getInitialAmount());
                data.setPaidForSale(goodsType.getInitialSellPrice());
                data.setCostToBuy(goodsType.getInitialBuyPrice());
                data.setOldPrice(goodsType.getInitialBuyPrice());
                data.setInitialPrice(goodsType.getInitialSellPrice());
            }
            marketData.put(goodsType, data);
        }
        priceGoods();

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
     * Initiates a new <code>Market</code> 
     * with the given ID. The object should later be
     * initialized by calling either
     * {@link #readFromXML(XMLStreamReader)} or
     * {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Market(Game game, String id) {
        super(game, id);
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
     * @see #setOwner
     */
    public Player getOwner() {
        return owner;
    }

    
    /**
     * Sets the owner of this <code>Market</code>.
     *
     * @param owner The <code>Player</code> that shall own this <code>Market</code>.
     * @see #getOwner
     */
    public void setOwner(Player owner) {
        this.owner = owner;
    }

    /**
     * Determines the cost to buy a single unit of a particular type of good.
     *
     * @param type a <code>GoodsType</code> value
     * @return The cost to buy a single unit of the given type of goods.
     */
    public int costToBuy(GoodsType type) {
        MarketData data = marketData.get(type);
        if (data == null) {
            return 0;
        } else {
            return data.getCostToBuy();
        }
    }

    /**
     * Determines the price paid for the sale of a single unit of a particular
     * type of good.
     *
     * @param type a <code>GoodsType</code> value
     * @return The price for a single unit of the given type of goods
     *      if being sold. 
     */
    public int paidForSale(GoodsType type) {
        MarketData data = marketData.get(type);
        if (data == null) {
            return 0;
        } else {
            return data.getPaidForSale();
        }
    }


    /**
     * Sells a particular amount of a particular type of good with the proceeds
     * of the sale being paid to a particular player. The goods is
     * sold using {@link #EUROPE} as the accesspoint for this market.
     *
     * @param  type   The type of goods to be sold.
     * @param  amount The amount of goods to be sold.
     * @param  player The player selling the goods
     */
    public void sell(GoodsType type, int amount, Player player) {
        sell(type, amount, player, Market.EUROPE);
    }
    
    /**
     * Sells a particular amount of a particular type of good with the proceeds
     * of the sale being paid to a particular player.
     *
     * @param  type   The type of goods to be sold.
     * @param  amount The amount of goods to be sold.
     * @param  player The player selling the goods
     * @param  marketAccess Place where goods are traded
     */
    public void sell(GoodsType type, int amount, Player player, int marketAccess) {
        if (player.canTrade(type, marketAccess)) {
            int unitPrice = paidForSale(type);
            int tax = player.getTax();
            
            int incomeBeforeTaxes = getSalePrice(type, amount);
            int incomeAfterTaxes = ((100 - tax) * incomeBeforeTaxes) / 100;
            player.modifyGold(incomeAfterTaxes);
            player.modifySales(type, amount);
            player.modifyIncomeBeforeTaxes(type, incomeBeforeTaxes);
            player.modifyIncomeAfterTaxes(type, incomeAfterTaxes);
            
            for(TransactionListener listener : transactionListeners) {
                listener.logSale(type, amount, unitPrice, tax);
            }
            amount = (int) player.getFeatureContainer()
                .applyModifier(amount, "model.modifier.tradeBonus",
                               type, getGame().getTurn());
            add(type, amount);
        } else {
            addModelMessage(this, ModelMessage.MessageType.WARNING,
                            "model.europe.market", "%goods%", type.getName());
        }
    }

    /**
     * Sells a particular amount of a particular type of good with the proceeds
     * of the sale being paid to a particular player. The goods is
     * sold using {@link #EUROPE} as the accesspoint for this market.
     *
     * @param  goods  The Goods object being sold.
     * @param  player      the player selling the goods
     * @throws  NullPointerException  if either (goods) or (player) is
     *                                <TT>null</TT>
     */
    public void sell(Goods goods, Player player) {
        GoodsType type = goods.getType();
        int amount = goods.getAmount();

        goods.setLocation(null);

        sell(type, amount, player, Market.EUROPE);
    }

    /**
     * Buys a particular amount of a particular type of good with the cost
     * being met by a particular player.
     *
     * @param  goodsType   the type of the good that is being bought
     * @param  amount      the number of units of goods that are being bought
     * @param  player      the player buying the goods
     * @throws IllegalStateException If the <code>player</code> cannot afford
     *                               to buy the goods.
     */
    public void buy(GoodsType goodsType, int amount, Player player) {
        int price = getBidPrice(goodsType, amount);
        if (price > player.getGold()) {
            throw new IllegalStateException();
        }

        int unitPrice = costToBuy(goodsType);
        player.modifyGold(-price);
        player.modifySales(goodsType, -amount);
        player.modifyIncomeBeforeTaxes(goodsType, -price);
        player.modifyIncomeAfterTaxes(goodsType, -price);

        for(TransactionListener listener : transactionListeners) {
            listener.logPurchase(goodsType, amount, unitPrice);
        }

        amount = (int) player.getFeatureContainer()
            .applyModifier(amount, "model.modifier.tradeBonus",
                           goodsType, getGame().getTurn());
        remove(goodsType, amount);
    }


    /**
     * Add the given <code>Goods</code> to this <code>Market</code>.
     * 
     * @param goodsType a <code>GoodsType</code> value
     * @param amount The amount of goods.
     */
    public void add(GoodsType goodsType, int amount) {
        MarketData data = marketData.get(goodsType);
        if (data == null) {
            data = new MarketData();
            marketData.put(goodsType, data);
        }
        int oldAmount = data.getAmountInMarket();
        data.setAmountInMarket(oldAmount + amount);
        priceGoods(goodsType, true);
    }

    /**
     * Remove the given <code>Goods</code> from this <code>Market</code>.
     * @param goodsType a <code>GoodsType</code> value
     * @param amount The amount of goods.
     */
    public void remove(GoodsType goodsType, int amount) {
        MarketData data = marketData.get(goodsType);
        if (data == null) {
            data = new MarketData();
            marketData.put(goodsType, data);
        }
        int oldAmount = data.getAmountInMarket();
        /* this is a bottomless market: goods cannot be owed by the
         * market */
        data.setAmountInMarket(Math.max(oldAmount - amount, 100));
        priceGoods(goodsType, true);
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
        if (data == null) {
            return 0;
        } else {
            return (amount * data.getCostToBuy());
        }
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
        if (data == null) {
            return 0;
        } else {
            return (amount * data.getPaidForSale());
        }
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

    // -------------------------------------------------------- support methods

    /**
     * Adjusts the prices for all goods.
     */
    private void priceGoods() {
        priceGoods(initialized);
        initialized = true;
    }

    /**
     * Adjusts the prices for all goods.
     */
    private void priceGoods(boolean addMessages) {        
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                priceGoods(goodsType, addMessages);
            }
        }
    }

    /**
     * Adjust the price for a particular type of goods.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param addMessages a <code>boolean</code> value
     */
    private void priceGoods(GoodsType goodsType, boolean addMessages) {
        MarketData data = marketData.get(goodsType);
        if (data != null) {
            data.setOldPrice(data.getCostToBuy());
            int newPrice = Math.round(goodsType.getInitialAmount() * data.getInitialPrice() /
                                      (float) data.getAmountInMarket());
            if (newPrice + goodsType.getPriceDifference() > MAXIMUM_PRICE) {
                data.setCostToBuy(MAXIMUM_PRICE);
                data.setPaidForSale(MAXIMUM_PRICE - goodsType.getPriceDifference());
            } else {
                if (newPrice < MINIMUM_PRICE) {
                    newPrice = MINIMUM_PRICE;
                }
                data.setPaidForSale(newPrice);
                data.setCostToBuy(data.getPaidForSale() + goodsType.getPriceDifference());
            }
            if (addMessages && owner != null && owner.getEurope() != null && 
                goodsType.isStorable()) {
                if (data.getOldPrice() > data.getCostToBuy()) {
                    addModelMessage(owner.getEurope(), ModelMessage.MessageType.MARKET_PRICES, goodsType,
                                    "model.market.priceDecrease",
                                    "%europe%", owner.getEurope().getName(),
                                    "%goods%", goodsType.getName(),
                                    "%buy%", String.valueOf(data.getCostToBuy()),
                                    "%sell%", String.valueOf(data.getPaidForSale()));
                             
                } else if (data.getOldPrice() < data.getCostToBuy()) {
                    addModelMessage(owner.getEurope(), ModelMessage.MessageType.MARKET_PRICES, goodsType,
                                    "model.market.priceIncrease",
                                    "%europe%", owner.getEurope().getName(),
                                    "%goods%", goodsType.getName(),
                                    "%buy%", String.valueOf(data.getCostToBuy()),
                                    "%sell%", String.valueOf(data.getPaidForSale()));
                }
            }
        }
    }

    public void newTurn() {
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                MarketData data = marketData.get(goodsType);
                if (data != null && data.getAmountInMarket() > goodsType.getInitialAmount()) {
                    remove(goodsType, 10);
                }
            }
        }
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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
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
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        
        owner = getFreeColGameObject(in, "owner", Player.class);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(MarketData.getXMLElementTagName())) {
                MarketData data = new MarketData();
                data.readFromXML(in);    
                marketData.put(FreeCol.getSpecification().getGoodsType(data.getId()), data);
            }
        }
        
        priceGoods();

    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "market";
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

    
    // ---------------------------------------------------------- inner classes

}
