/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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


/**
 * Interface for classes which listen to transactions in market
 */
public interface TransactionListener {

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
