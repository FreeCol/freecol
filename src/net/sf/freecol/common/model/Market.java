
package net.sf.freecol.common.model;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class implements a simple economic model whereby a market holds all
 * goods that have been sold and the price of a particular type of good is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    private final Data[]  dataForGoodType = new Data[Goods.NUMBER_OF_TYPES];


    // ----------------------------------------------------------- constructors

    public Market(Game game) {
        super(game);

        /* create the objects to hold the market data for each type of good */
        for (int i = 0; i < dataForGoodType.length; i++) {
            dataForGoodType[i] = new Data(getGame());
        }

        /* seed these objects with the initial amount of each type of good */
        int[]  initialAmounts = {
          10000, // FOOD
           2000, // SUGAR
           2000, // TOBACCO
           2000, // COTTON
           2000, // FURS
           8000, // LUMBER
           5000, // ORE
            500, // SILVER
           8000, // HORSES
           1000, // RUM
           1000, // CIGARS
           1000, // CLOTH
           1000, // COATS
          10000, // TRADE_GOODS
           4000, // TOOLS
           3000  // MUSKETS
        };
        for (int i = 0; i < initialAmounts.length; i++) {
            dataForGoodType[i].amountInMarket = initialAmounts[i];
        };

        /* price the goods in the newly set-up market */
        priceGoods();
    }

    public Market(Game game, Element e) {
        super(game, e);

        readFromXMLElement(e);
    }


    // ------------------------------------------------------------ API methods

    /**
     * Determines the cost to buy a single unit of a particular type of good.
     *
     * @param  typeOfGood  the type of good for which to obtain a price
     * @throws  NullPointerException  if the argument is <TT>null</TT>
     */
    public int costToBuy(int typeOfGood) {

        return dataForGoodType[typeOfGood].costToBuy;
    }


    /**
     * Determines the price paid for the sale of a single unit of a particular
     * type of good.
     *
     * @param  typeOfGood  The Goods for which to obtain a price.
     * @throws  NullPointerException  if the argument is <TT>null</TT>
     */
    public int paidForSale(int typeOfGood) {
        return dataForGoodType[typeOfGood].paidForSale;
    }


    /**
     * Sells a particular amount of a particular type of good with the proceeds
     * of the sale being paid to a particular player.
     *
     * @param  type   The type of goods to be sold.
     * @param  amount The amount of goods to be sold.
     * @param  player The player selling the goods
     */
    public void sell(int type, int amount, Player player) {
        if (player.canTrade(type)) {
            int incomeBeforeTaxes = getSalePrice(type, amount);
            int incomeAfterTaxes = ((100 - player.getTax()) * incomeBeforeTaxes) / 100;
            player.modifyGold(incomeAfterTaxes);
            player.modifySales(type, amount);
            player.modifyIncomeBeforeTaxes(type, incomeBeforeTaxes);
            player.modifyIncomeAfterTaxes(type, incomeAfterTaxes);
            add(type, (player.getNation() == Player.DUTCH) ? (amount/2) : amount);
        } else {
            addModelMessage(this, "model.europe.market",
                            new String [][] {{"%goods%", Goods.getName(type)}});
        }
    }


    /**
     * Sells a particular amount of a particular type of good with the proceeds
     * of the sale being paid to a particular player.
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

        sell(type, amount, player);
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

        int price = getBidPrice(type, amount);
        player.modifyGold(-price);
        player.modifySales(type, -amount);
        player.modifyIncomeBeforeTaxes(type, -price);
        player.modifyIncomeAfterTaxes(type, -price);
        remove(type, ((player.getNation() == Player.DUTCH) ? (amount / 2) : amount));
    }


    /**
    * Add the given <code>Goods</code> to this <code>Market</code>.
    */
    public void add(int type, int amount) {
        Data  data = dataForGoodType[type];

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

        data.amountInMarket += amount;
        priceGoods();
    }


    /**
    * Remove the given <code>Goods</code> from this <code>Market</code>.
    */
    public void remove(int type, int amount) {
        Data data = dataForGoodType[type];
        data.amountInMarket -= amount;

        /* this is a bottomless market: goods cannot be owed by the market */
        if (data.amountInMarket < 0) {
            data.amountInMarket = 0;
        }

        priceGoods();
    }


    /**
    * Gets the price of a given goods when the <code>Player</code> buys.
    *
    * @return The bid price of the given goods.
    */
    public int getBidPrice(int type, int amount) {
        Data data = dataForGoodType[type];
        return (amount * data.costToBuy);
    }


    /**
    * Gets the price of a given goods when the <code>Player</code> sales.
    *
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

    private void priceGoods() {

        /* first, count the total units of all goods in the market */
        int  totalGoods = 0;

        for (int i = 0; i < dataForGoodType.length; i++) {
            totalGoods += dataForGoodType[i].amountInMarket;
        }

        /* next, choose a price for each type of good in the market based on
         * the relative availability of that type of good */
        for (int i = 0; i < dataForGoodType.length; i++) {

            Data  data = dataForGoodType[i];

            /* scale sale prices from 1 to 19 and place buy prices one above */
            data.paidForSale = (10 * ((totalGoods / 30) + 1)) / (data.amountInMarket + 1);

            //data.paidForSale = 19 - 18 * (data.amountInMarket / (totalGoods + 1));

            if (data.paidForSale > 19) data.paidForSale = 19;
            if (data.paidForSale < 1) data.paidForSale = 1;

            if (i == Goods.FOOD) {
              data.costToBuy = data.paidForSale + 7;
            } else if ((i == Goods.LUMBER) || (i == Goods.ORE)) {
              data.costToBuy = data.paidForSale + 2;
            } else {
              data.costToBuy = data.paidForSale + 1;
            }
        }
    }

    /**
    * Make a XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Map".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element marketElement = document.createElement(getXMLElementTagName());

        marketElement.setAttribute("ID", getID());

        for (int i=0; i<dataForGoodType.length;i++) {
            marketElement.appendChild(dataForGoodType[i].toXMLElement(player, document, showAll, toSavedGame));
        }

        return marketElement;
    }

    public void newTurn() {
        for(int i = 0; i < dataForGoodType.length; i++) {
            int subtract = (int) (dataForGoodType[i].amountInMarket*0.015);
            dataForGoodType[i].amountInMarket -= subtract;
        }
        priceGoods();
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param marketElement The DOM-element ("Document Object Model") made to represent this "Map".
    */
    public void readFromXMLElement(Element marketElement) {
        setID(marketElement.getAttribute("ID"));

        NodeList dataList = marketElement.getElementsByTagName(Data.getXMLElementTagName());

        for (int i=0; i<dataList.getLength(); i++) {
            Node node = dataList.item(i);
            if (!(node instanceof Element)) {
                continue;
            }        
            Element dataElement = (Element) node;

            dataForGoodType[i] = new Data(getGame(), dataElement);
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
         */
        Data(Game game) {
            super(game);
        }

        Data(Game game, Element e) {
            super(game, e);

            readFromXMLElement(e);
        }

        /**
        * Make a XML-representation of this object.
        *
        * @param document The document to use when creating new componenets.
        * @return The DOM-element ("Document Object Model") made to represent this "Data".
        */
        public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
            Element dataElement = document.createElement(getXMLElementTagName());

            dataElement.setAttribute("ID", getID());

            dataElement.setAttribute("amount", Integer.toString(amountInMarket));

            return dataElement;
        }


        /**
        * Initialize this object from an XML-representation of this object.
        *
        * @param dataElement The DOM-element ("Document Object Model") made to represent this "Data".
        */
        public void readFromXMLElement(Element dataElement) {
            setID(dataElement.getAttribute("ID"));

            amountInMarket = Integer.parseInt(dataElement.getAttribute("amount"));
        }

        /**
        * Returns the tag name of the root element representing this object.
        *
        * @return the tag name.
        */
        public static String getXMLElementTagName() {
            return "marketdata";
        }

        public void newTurn() {
            // Shift market goods around a bit?
        }
    } // class Data

}
