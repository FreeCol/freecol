
package net.sf.freecol.common.model;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;

import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 * This class implements a simple economic model whereby a market holds all
 * goods that have been sold and the price of a particular type of good is
 * determined solely by its availability in that market.
 */
public final class Market extends FreeColGameObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
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
          1000, // FOOD
           200, // SUGAR
           200, // TOBACCO
           200, // COTTON
           200, // FURS
           800, // LUMBER
           500, // ORE
             0, // SILVER
           800, // HORSES
           100, // RUM
           100, // CIGARS
           100, // CLOTH
           100, // COATS
          1000, // TRADE_GOODS
           400, // TOOLS
           300  // MUSKETS
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
     * @param  typeOfGood  The Goods object being sold.
     * @param  player      the player selling the goods
     * @throws  NullPointerException  if either (typeOfGood) or (player) is
     *                                <TT>null</TT>
     */
    public void sell(Goods typeOfGood, Player player) {

        Data  data = dataForGoodType[typeOfGood.getType()];
	int originalamount = typeOfGood.getAmount(), amount = originalamount;
        switch(typeOfGood.getType()) {
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
        if (player.getNation() == Player.DUTCH) amount /= 2;
        data.amountInMarket += amount;
        player.modifyGold(originalamount * data.paidForSale);

        typeOfGood.getLocation().remove(typeOfGood);

        priceGoods();
    }


    /**
     * Buys a particular amount of a particular type of good with the cost
     * being met by a particular player.
     * 
     * @param  typeOfGood  the type of good that is being bought
     * @param  amount      the number of units of goods that are being bought
     * @param  player      the player buying the goods
     * @throws  NullPointerException  if either (typeOfGood) or (player) is
     *                                <TT>null</TT>
     */
    public Goods buy(int typeOfGood, int amount, Player player) {

        Data  data = dataForGoodType[typeOfGood];
        data.amountInMarket -= ((player.getNation() == Player.DUTCH) ? (amount / 2) : amount);

        /* this is a bottomless market: goods cannot be owed by the market */
        if (data.amountInMarket < 0) {
            data.amountInMarket = 0;
        }

        player.modifyGold( -(amount * data.costToBuy));

        priceGoods();
        
        return new Goods(player.getGame(), null, typeOfGood, amount);
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
            data.paidForSale =
                (10 * ((totalGoods / 30) + 1)) / (data.amountInMarket + 1);

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
    public Element toXMLElement(Player player, Document document) {
        Element marketElement = document.createElement(getXMLElementTagName());

        marketElement.setAttribute("ID", getID());

        for (int i=0; i<dataForGoodType.length;i++) {
            marketElement.appendChild(dataForGoodType[i].toXMLElement(player, document));
        }

        return marketElement;
    }
    
    public void newTurn() {
        // Shift market goods around a bit?
    }


    /**
    * Initialize this object from an XML-representation of this object.
    *
    * @param mapElement The DOM-element ("Document Object Model") made to represent this "Map".
    */
    public void readFromXMLElement(Element marketElement) {
        setID(marketElement.getAttribute("ID"));

        NodeList dataList = marketElement.getElementsByTagName(Data.getXMLElementTagName());

        for (int i=0; i<dataList.getLength(); i++) {
            Element dataElement = (Element) dataList.item(i);

            dataForGoodType[i] = new Data(getGame(), dataElement);
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
        Data(Game game) {super(game);}

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
        public Element toXMLElement(Player player, Document document) {
            Element dataElement = document.createElement(getXMLElementTagName());

            dataElement.setAttribute("ID", getID());
            dataElement.setAttribute("buy", Integer.toString(costToBuy));
            dataElement.setAttribute("sell", Integer.toString(paidForSale));
            dataElement.setAttribute("amount", Integer.toString(paidForSale));

            return dataElement;
        }


        /**
        * Initialize this object from an XML-representation of this object.
        *
        * @param marketDataElement The DOM-element ("Document Object Model") made to represent this "Data".
        */
        public void readFromXMLElement(Element dataElement) {
            setID(dataElement.getAttribute("ID"));

            costToBuy = Integer.parseInt(dataElement.getAttribute("buy"));
	    costToBuy = Integer.parseInt(dataElement.getAttribute("sell"));
	    costToBuy = Integer.parseInt(dataElement.getAttribute("amount"));
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
