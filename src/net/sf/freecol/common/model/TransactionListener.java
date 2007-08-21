
package net.sf.freecol.common.model;


/**
* Interface for classes which listen to transactions in market
*/
public interface TransactionListener {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";
    
    /**
     * Logs a purchase
     *
     * @param goodsType The type of goods which have been purchased
     * @param amount The amount of goods which have been purchased
     * @param price The unit price of the goods
     */
    public void logPurchase(GoodsType goodsType, int amount, int price);

    /**
     * Logs a sale
     *
     * @param goodsType The type of goods which have been sold
     * @param amount The amount of goods which have been sold
     * @param price The unit price of the goods
     * @param tax The tax which has been applied
     */
    public void logSale(GoodsType goodsType, int amount, int price, int tax);
}
