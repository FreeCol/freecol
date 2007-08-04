
package net.sf.freecol.common.model;

import java.util.ArrayList;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * This class implements a simple economic model whereby a market holds all
 * goods that have been sold and the price of a particular type of good is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject implements Ownable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private Player owner;

    /**
     * Constant for specifying the access to this <code>Market</code>
     * when {@link #buy(int, int, Player) buying} and
     * {@link #sell(int, int, Player, int) selling} goods.
     */
    public static final int EUROPE = 0,
                            CUSTOM_HOUSE = 1;
    
    private static final int GOODS_STABILIZER = 750;

    private final Data[]  dataForGoodType = new Data[Goods.NUMBER_OF_TYPES];

    private static final int[] initialAmounts = {
        10000, // FOOD
        1500, // SUGAR
        1500, // TOBACCO
        1500, // COTTON
        1500, // FURS
        3000, // LUMBER
        2000, // ORE
        500, // SILVER
        1000, // HORSES
        1000, // RUM
        1000, // CIGARS
        1000, // CLOTH
        1000, // COATS
        1000, // TRADE_GOODS
        500, // TOOLS
        500  // MUSKETS
    };

    private static final int[] priceDifferences = {
        7, // FOOD
        2, // SUGAR
        2, // TOBACCO
        2, // COTTON
        2, // FURS
        5, // LUMBER
        3, // ORE
        1, // SILVER
        1, // HORSES
        1, // RUM
        1, // CIGARS
        1, // CLOTH
        1, // COATS
        1, // TRADE_GOODS
        1, // TOOLS
        1  // MUSKETS
    };

    private int[] initialPrices = {
        1, // FOOD
        4, // SUGAR
        4, // TOBACCO
        4, // COTTON
        4, // FURS
        1, // LUMBER
        4, // ORE
        16, // SILVER
        1, // HORSES
        10, // RUM
        10, // CIGARS
        10, // CLOTH
        10, // COATS
        1, // TRADE_GOODS
        1, // TOOLS
        1  // MUSKETS
    };

    private ArrayList<TransactionListener> transactionListeners =
            new ArrayList<TransactionListener>();
    
    // ----------------------------------------------------------- constructors

    public Market(Game game, Player player) {
        super(game);
        this.owner = player;
        
        /* create the objects to hold the market data for each type of
         * goods and seed these objects with the initial amount of
         * each type of goods */
        for (int goodsType = 0; goodsType < dataForGoodType.length; goodsType++) {
            Data data = new Data(getGame());
            data.amountInMarket = initialAmounts[goodsType];
            data.paidForSale = initialPrices[goodsType];
            data.costToBuy = initialPrices[goodsType] + priceDifferences[goodsType];
            dataForGoodType[goodsType] = data;
        }

    }

    /**
     * Randomizes the initial prices.
     *
     */
    public void randomizeInitialPrices() {
        for (int goodsType = 0; goodsType < dataForGoodType.length; goodsType++) {
            switch(goodsType) {
            case Goods.TOOLS:
            case Goods.MUSKETS:
            case Goods.TRADE_GOODS:
            case Goods.HORSES:
                continue;
            default:
                switch (getGame().getModelController().getPseudoRandom().nextInt(5)) {
                case 1:
                    initialPrices[goodsType]++;
                    break;
                case 2:
                    initialPrices[goodsType] += 2;
                    break;
                default:
                }
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
     * @param typeOfGood  the type of good for which to obtain a price
     * @return The cost to buy a single unit of the given type of goods.
     * @throws NullPointerException  if the argument is <TT>null</TT>
     */
    public int costToBuy(int typeOfGood) {
        return dataForGoodType[typeOfGood].costToBuy;
    }

    /**
     * Determines the price paid for the sale of a single unit of a particular
     * type of good.
     *
     * @param typeOfGood  The Goods for which to obtain a price.
     * @return The price for a single unit of the given type of goods
     *      if being sold. 
     * @throws NullPointerException  if the argument is <TT>null</TT>
     */
    public int paidForSale(int typeOfGood) {
        return dataForGoodType[typeOfGood].paidForSale;
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
    public void sell(int type, int amount, Player player) {
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
    public void sell(int type, int amount, Player player, int marketAccess) {
        if (player.canTrade(type, marketAccess)) {
            int unitPrice = paidForSale(type);
            int tax = player.getTax();
            
            int incomeBeforeTaxes = getSalePrice(type, amount);
            int incomeAfterTaxes = ((100 - tax) * incomeBeforeTaxes) / 100;
            player.modifyGold(incomeAfterTaxes);
            player.modifySales(type, amount);
            player.modifyIncomeBeforeTaxes(type, incomeBeforeTaxes);
            player.modifyIncomeAfterTaxes(type, incomeAfterTaxes);
            add(type, (player.getNation() == Player.DUTCH) ? (amount/2) : amount);

            for(TransactionListener listener : transactionListeners) {
                listener.logSale(type, amount, unitPrice, tax);
            }
        } else {
            addModelMessage(this, "model.europe.market",
                            new String [][] {{"%goods%", Goods.getName(type)}},
                            ModelMessage.WARNING);
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
        int type = goods.getType();
        int amount = goods.getAmount();

        goods.setLocation(null);

        sell(type, amount, player, Market.EUROPE);
    }

    /**
     * Buys a particular amount of a particular type of good with the cost
     * being met by a particular player.
     *
     * @param  type  the type of good that is being bought
     * @param  amount      the number of units of goods that are being bought
     * @param  player      the player buying the goods
     * @throws IllegalStateException If the <code>player</code> cannot afford
     *                               to buy the goods.
     */
    public void buy(int type, int amount, Player player) {
        if (getBidPrice(type, amount) > player.getGold()) {
            throw new IllegalStateException();
        }

        int unitPrice = costToBuy(type);
        int price = getBidPrice(type, amount);
        player.modifyGold(-price);
        player.modifySales(type, -amount);
        player.modifyIncomeBeforeTaxes(type, -price);
        player.modifyIncomeAfterTaxes(type, -price);
        remove(type, ((player.getNation() == Player.DUTCH) ? (amount / 2) : amount));
        
        for(TransactionListener listener : transactionListeners) {
            listener.logPurchase(type, amount, unitPrice);
        }
    }


    /**
     * Add the given <code>Goods</code> to this <code>Market</code>.
     * 
     * @param type The type of goods.
     * @param amount The amount of goods.
     */
    public void add(int type, int amount) {
        Data  data = dataForGoodType[type];
        /*
        switch(type) {
          case Goods.SILVER:
               amount *= 4; // No bitshifts in Java? Should be << 2 - sjm
               break;
          case Goods.SUGAR:
          case Goods.TOBACCO:
          case Goods.COTTON:
          case Goods.FURS:
          case Goods.RUM:
          case Goods.CIGARS:
          case Goods.CLOTH:
          case Goods.COATS:
              amount *= 2;
          break;
          default:
              break;
        }
        */
        data.amountInMarket += amount;
        priceGoods();
    }

    /**
     * Remove the given <code>Goods</code> from this <code>Market</code>.
     * @param type The type of goods.
     * @param amount The amount of goods.
     */
    public void remove(int type, int amount) {
        Data data = dataForGoodType[type];
        data.amountInMarket -= amount;

        /* this is a bottomless market: goods cannot be owed by the market */
        if (data.amountInMarket < 100) {
            data.amountInMarket = 100;
        }

        priceGoods();
    }

    /**
     * Gets the price of a given goods when the <code>Player</code> buys.
     *
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @return The bid price of the given goods.
     */
    public int getBidPrice(int type, int amount) {
        Data data = dataForGoodType[type];
        return (amount * data.costToBuy);
    }

    /**
     * Gets the price of a given goods when the <code>Player</code> sells.
     *
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @return The sale price of the given goods.
     */
    public int getSalePrice(int type, int amount) {
        Data data = dataForGoodType[type];
        return (amount * data.paidForSale);
    }

    public int getSalePrice(Goods goods) {
        return getSalePrice(goods.getType(), goods.getAmount());
    }

    // -------------------------------------------------------- support methods

    /**
     * Adjusts the prices for all goods.
     */
    private void priceGoods() {
        priceGoods(true);
    }

    /**
     * Adjusts the prices for all goods.
     */
    private void priceGoods(boolean addMessages) {        
        int[] oldPrices = new int[dataForGoodType.length];
        for(int i = 0; i < dataForGoodType.length; i++) {
            if (dataForGoodType[i] != null) {
                oldPrices[i] = dataForGoodType[i].costToBuy;
            }
        }

        /* choose a price for each type of good in the market based on
         * the relative availability of that type of goods */
        for (int goodsType = 0; goodsType < dataForGoodType.length; goodsType++) {
            Data data = dataForGoodType[goodsType];
            int newPrice = Math.round(initialAmounts[goodsType] * initialPrices[goodsType] /
                                      (float) data.amountInMarket);
            if (newPrice + priceDifferences[goodsType] > 19) {
                data.costToBuy = 19;
                data.paidForSale = 19 - priceDifferences[goodsType];
            } else {
                if (newPrice < 1) {
                    newPrice = 1;
                }
                data.paidForSale = newPrice;
                data.costToBuy = data.paidForSale + priceDifferences[goodsType];
            }
            if (addMessages && owner != null && owner.getEurope() != null) {
                if (oldPrices[goodsType] > dataForGoodType[goodsType].costToBuy) {
                    addModelMessage(owner.getEurope(), "model.market.priceDecrease",
                                    new String[][] {
                                        {"%europe%", owner.getEurope().getName()},
                                        {"%goods%", Goods.getName(goodsType)},
                                        {"%buy%", String.valueOf(dataForGoodType[goodsType].costToBuy)},
                                        {"%sell%", String.valueOf(dataForGoodType[goodsType].paidForSale)}},
                                    ModelMessage.MARKET_PRICES,
                                    new Goods(goodsType));
                } else if (oldPrices[goodsType] < dataForGoodType[goodsType].costToBuy) {
                    addModelMessage(owner.getEurope(), "model.market.priceIncrease",
                                    new String[][] {
                                        {"%europe%", owner.getEurope().getName()},
                                        {"%goods%", Goods.getName(goodsType)},
                                        {"%buy%", String.valueOf(dataForGoodType[goodsType].costToBuy)},
                                        {"%sell%", String.valueOf(dataForGoodType[goodsType].paidForSale)}},
                                    ModelMessage.MARKET_PRICES,
                                    new Goods(goodsType));
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
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getID());
        out.writeAttribute("owner", owner.getID());
        toArrayElement("initialPrices", initialPrices, out);
        
        for (int i=0; i<dataForGoodType.length;i++) {
            dataForGoodType[i].toXML(out, player, showAll, toSavedGame);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setID(in.getAttributeValue(null, "ID"));
        
        owner = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "owner"));
        if (owner == null) {
            owner = new Player(getGame(), in.getAttributeValue(null, "owner"));
        }

        int i = 0;
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("initialPrices")) {
                initialPrices = readFromArrayElement("initialPrices", in, new int[0]);
            } else if (in.getLocalName().equals(Data.getXMLElementTagName())) {
                dataForGoodType[i] = (Data) getGame().getFreeColGameObject(in.getAttributeValue(null, "ID"));
                if (dataForGoodType[i] != null) {
                    dataForGoodType[i].readFromXML(in);    
                } else {
                    dataForGoodType[i] = new Data(getGame(), in);
                }
                i++;
            }
        }
        
        priceGoods(false);

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

    /**
     * Objects of this class hold the market data for a particular type of
     * good.
     */
    private static final class Data extends FreeColGameObject {

        int  costToBuy;
        int  paidForSale;
        int  amountInMarket;

        /**
         * Package constructor: This class is only supposed to be constructed
         * by {@link Market}.
         * 
         * @param game The game this object should be created within.
         */
        Data(Game game) {
            super(game);
        }

        Data(Game game, XMLStreamReader in) throws XMLStreamException {
            super(game, in);

            readFromXML(in);
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
        protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
            // Start element:
            out.writeStartElement(getXMLElementTagName());

            out.writeAttribute("ID", getID());
            out.writeAttribute("amount", Integer.toString(amountInMarket));

            out.writeEndElement();
        }

        /**
         * Initialize this object from an XML-representation of this object.
         * @param in The input stream with the XML.
         */
        protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
            setID(in.getAttributeValue(null, "ID"));

            amountInMarket = Integer.parseInt(in.getAttributeValue(null, "amount"));
            
            in.nextTag();
        }

        /**
         * Returns the tag name of the root element representing this object.
         *
         * @return the tag name.
         */
        public static String getXMLElementTagName() {
            return "marketdata";
        }

    } // class Data
}
