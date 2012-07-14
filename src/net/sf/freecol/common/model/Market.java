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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;


/**
 * This class implements a simple economic model where a market holds
 * all goods to be sold and the price of a particular goods type is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject implements Ownable {

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

    private final Map<GoodsType, MarketData> marketData
        = new HashMap<GoodsType, MarketData>();

    private Player owner;

    private ArrayList<TransactionListener> transactionListeners
        = new ArrayList<TransactionListener>();


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
        for (GoodsType goodsType : getSpecification().getGoodsTypeList()) {
            if (goodsType.isStorable()) {
                marketData.put(goodsType, new MarketData(game, goodsType));
            }
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
     * Describe <code>putMarketData</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param data a <code>MarketData</code> value
     */
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
     * Makes a slight randomization to all the new world and luxury goods
     * types.  Used at the start of game by the PreGameController.
     *
     * @param random A pseudo-random number source.
     */
    public void randomizeInitialPrice(Random random) {
        Specification spec = getGame().getSpecification();
        for (GoodsType type : spec.getGoodsTypeList()) {
            String prefix = "model.option." + type.getSuffix("model.goods.");
            // these options are not available for all goods types
            if (spec.hasOption(prefix + ".minimumPrice")
                && spec.hasOption(prefix + ".maximumPrice")) {
                int min = spec.getInteger(prefix + ".minimumPrice");
                int max = spec.getInteger(prefix + ".maximumPrice");
                int value = min;
                if (max > min) {
                    value += Utils.randomInt(null, null, random, max - min);
                } else if (max < min) {
                    // user error
                    value = max + Utils.randomInt(null, null, random, min - max);
                }
                setInitialPrice(type, value);
            }
        }
    }

    /**
     * Return the market data for a type of goods.  This one is public
     * so the server can send individual MarketData updates.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return a <code>MarketData</code> value
     */
    public MarketData getMarketData(GoodsType goodsType) {
        return marketData.get(goodsType);
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
     * @param owner The <code>Player</code> to own this <code>Market</code>.
     */
    public void setOwner(Player owner) {
        this.owner = owner;
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
     */
    public void addGoodsToMarket(GoodsType goodsType, int amount) {
        MarketData data = requireMarketData(goodsType);

        // Markets are bottomless, amount can not go below the threshold
        data.setAmountInMarket(Math.max(MINIMUM_AMOUNT,
                                        data.getAmountInMarket() + amount));
        data.setTraded(true);
        data.price();
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
            .add("%goods%", goodsType.getNameKey())
            .addAmount("%buy%", newPrice)
            .addAmount("%sell%", data.getPaidForSale());
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
        out.writeAttribute("owner", owner.getId());

        if (player == owner || showAll || toSavedGame) {
            for (MarketData data : marketData.values()) {
                data.toXML(out, player, showAll, toSavedGame);
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException on problems with the stream.
     * TODO: Get rid of the price() when the server sends all
     * price changes.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        Game game = getGame();

        setId(in.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE));

        owner = getFreeColGameObject(in, "owner", Player.class);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(MarketData.getXMLElementTagName())) {
                String id = in.getAttributeValue(null, FreeColObject.ID_ATTRIBUTE);
                MarketData data = game.getFreeColGameObject(id, MarketData.class);
                if (data == null) {
                    data = new MarketData(game, in);
                } else {
                    data.readFromXML(in);
                }
                putMarketData(data.getGoodsType(), data);
            }
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "market".
     */
    public static String getXMLElementTagName() {
        return "market";
    }
}
