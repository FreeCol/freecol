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
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public final class GoodsType extends FreeColGameObjectType {

    /** Is this a farmed goods type. */
    private boolean isFarmed;

    /** Is this a food type. */
    private boolean isFood;

    /** Does this goods type ignore warehouse limits. */
    private boolean ignoreLimit;

    /** Is this goods type native to the New World. */
    private boolean newWorldGoods;

    /** Whether this type of goods is required for building. */
    private boolean buildingMaterial;

    /**
     * Whether this type of goods is required for building equipment
     * that grants an offence bonus or defence bonus.
     */
    private boolean militaryGoods;

    /** Whether these are trade goods that can only be obtained in Europe. */
    private boolean tradeGoods;

    /** Whether this type of goods can be stored in a warehouse. */
    private boolean storable;

    /** What this goods type is stored as. */
    private GoodsType storedAs;

    /** What this goods type is made from. */
    private GoodsType madeFrom;

    /** What this goods type can make. */
    private GoodsType makes;

    /** The initial amount of this goods type in a market. */
    private int initialAmount;

    /** The initial <em>minimum</em> sales price for this type of goods. */
    private int initialPrice;

    /** The initial market price difference for this type of goods. */
    private int priceDiff;

    /**
     * The number of units required to breed this type of goods. This
     * obviously only applies to animals.
     */
    private int breedingNumber = INFINITY;

    /**
     * The price of this type of goods.  This is only used for goods
     * that can not be traded in the market, such as hammers.
     */
    private int price = INFINITY;


    /**
     * Create a new goods type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public GoodsType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this a farmed goods type?
     *
     * @return True if this is a farmed goods type.
     */
    public boolean isFarmed() {
        return isFarmed;
    }

    /**
     * Is this an edible goods type?
     *
     * @return True if this is a food type.
     */
    public boolean isFoodType() {
        return isFood;
    }

    /**
     * Do warehouse storage limits not apply to this goods type?
     *
     * @return True if unlimited amounts of this goods type can be stored.
     */
    public boolean limitIgnored() {
        return ignoreLimit;
    }

    /**
     * Is this a goods type native to the New World?
     *
     * @return True if this goods type is native to the New World.
     */
    public boolean isNewWorldGoodsType() {
        return newWorldGoods;
    }

    /**
     * Is this goods type made from a New World goods type?
     *
     * @return True if this goods type is made from New World goods.
     */
    public boolean isNewWorldLuxuryType() {
        return madeFrom != null && madeFrom.isNewWorldGoodsType();
    }

    /**
     * Is this type of goods is required for building a BuildableType?
     *
     * @return True if this is a simple building material.
     * @see BuildableType
     */
    public boolean isBuildingMaterial() {
        return buildingMaterial;
    }

    /**
     * Set the building material state.
     *
     * @param newBuildingMaterial The new building material state.
     */
    public void setBuildingMaterial(final boolean newBuildingMaterial) {
        this.buildingMaterial = newBuildingMaterial;
    }

    /**
     * Is this goods type a military goods type?
     *
     * @return True if this is a military goods type.
     */
    public boolean isMilitaryGoods() {
        return militaryGoods;
    }

    /**
     * Set the military goods state.
     *
     * @param newMilitaryGoods The new military goods state.
     */
    public void setMilitaryGoods(final boolean newMilitaryGoods) {
        this.militaryGoods = newMilitaryGoods;
    }

    /**
     * Is this a trade goods type?
     *
     * @return True if this goods type is trade goods.
     */
    public boolean isTradeGoods() {
        return tradeGoods;
    }

    /**
     * Does this type of goods produce liberty?
     *
     * @return True if this goods type produces liberty.
     */
    public boolean isLibertyType() {
        return containsModifierKey("model.modifier.liberty");
    }

    /**
     * Does this type of goods produce immigration?
     *
     * @return True if this goods type produces immigration.
     */
    public boolean isImmigrationType() {
        return containsModifierKey("model.modifier.immigration");
    }

    /**
     * Is this goods type storable?
     *
     * @return True if this goods type is storable.
     */
    public boolean isStorable() {
        return storable;
    }

    /**
     * Is this type of goods stored as something else?
     *
     * @return True if this type of goods is stored as another type.
     */
    public boolean isStoredAs() {
        return storedAs != null;
    }

    /**
     * What type of goods is this goods type stored as?
     *
     * @return The <code>GoodsType</code> this is stored as, usually itself.
     */
    public GoodsType getStoredAs() {
        return (storedAs == null) ? this : storedAs;
    }

    /**
     * Can this goods type be made into somthing?
     *
     * @return True if this <code>GoodsType</code> can be made into something.
     */
    public boolean isRawMaterial() {
        return makes != null;
    }

    /**
     * Is this goods type made from somthing?
     *
     * @return True if this <code>GoodsType</code> is made from something.
     */
    public boolean isRefined() {
        return madeFrom != null;
    }

    /**
     * What is this goods made into?
     *
     * @return The <code>GoodsType</code> this is made into, if anything.
     */
    public GoodsType getOutputType() {
        return makes;
    }

    /**
     * What is this goods type made from?
     *
     * @return The <code>GoodsType</code> this is made from, if anything.
     */
    public GoodsType getInputType() {
        return madeFrom;
    }

    /**
     * Get the default initial amount of this goods type in a market.
     *
     * @return The default initial amount.
     */
    public int getInitialAmount() {
        return initialAmount;
    }

    /**
     * Get the initial <em>minimum</em> sales price for this type
     * of goods.  The actual initial sales price in a particular
     * Market may be higher.  This method is only used for initializing
     * Markets.
     *
     * @return The initial sell price.
     * @see Market
     */
    int getInitialSellPrice() {
        return initialPrice;
    }

    /**
     * The default initial purchase price for this goods type.
     *
     * @return The default initial purchase price.
     */
    int getInitialBuyPrice() {
        return initialPrice + priceDiff;
    }

    /**
     * The default initial price difference (between purchase and sale price)
     * for this type of goods.
     *
     * @return The default initial price difference.
     */
    int getPriceDifference() {
        return priceDiff;
    }

    /**
     * Get the breeding number for this goods type.
     *
     * @return The breeding number.
     */
    public int getBreedingNumber() {
        return breedingNumber;
    }

    /**
     * Set the breeding number.
     *
     * @param newBreedingNumber The new breeding number.
     */
    public void setBreedingNumber(final int newBreedingNumber) {
        this.breedingNumber = newBreedingNumber;
    }

    /**
     * Is this type of goods breedable?
     *
     * @return True if this <code>GoodsType</code> is breedable.
     */
    public boolean isBreedable() {
        return breedingNumber != INFINITY;
    }

    /**
     * Get the price of a non-tradeable goods type.
     *
     * @return The price.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Set the price value.
     *
     * @param newPrice The new price value.
     */
    public void setPrice(final int newPrice) {
        this.price = newPrice;
    }


    /**
     * Gets the i18n-ed name for this goods type.
     *
     * @return The name of this <code>GoodsType</code>.
     */
    public StringTemplate getLabel() {
        return StringTemplate.key(getNameKey());
    }

    /**
     * Gets the "workingAs" key for the profession that makes this goods type.
     *
     * @return The "workingAs" message key.
     */
    public final String getWorkingAsKey() {
        return getId() + ".workingAs";
    }

    /**
     * Gets the production chain of the goods type, beginning with a
     * raw material that can not be produced from any other.  The last
     * element of the production chain is the goods type itself.
     *
     * @return The production chain of this <code>GoodsType</code> as a list.
     */
    public List<GoodsType> getProductionChain() {
        List<GoodsType> result = new ArrayList<GoodsType>();
        GoodsType currentGoods = this;
        while (currentGoods != null) {
            result.add(0, currentGoods);
            currentGoods = currentGoods.madeFrom;
        }
        return result;
    }

    /**
     * Is this type of goods required somewhere in the chain for
     * producing a BuildableType, and is not itself buildable.
     *
     * @return True if a raw building type.
     * @see BuildableType
     */
    public boolean isRawBuildingMaterial() {
        if (this.madeFrom != null) return false;

        GoodsType refinedType = makes;
        while (refinedType != null) {
            if (refinedType.isBuildingMaterial()) return true;
            refinedType = refinedType.makes;
        }
        return false;
    }


    // Serialization

    private static final String BREEDING_NUMBER_TAG = "breeding-number";
    private static final String IGNORE_LIMIT_TAG = "ignore-limit";
    private static final String INITIAL_AMOUNT_TAG = "initial-amount";
    private static final String INITIAL_PRICE_TAG = "initial-price";
    private static final String IS_FARMED_TAG = "is-farmed";
    private static final String IS_FOOD_TAG = "is-food";
    private static final String MADE_FROM_TAG = "made-from";
    private static final String MARKET_TAG = "market";
    private static final String NEW_WORLD_GOODS_TAG = "new-world-goods";
    private static final String PRICE_TAG = "price";
    private static final String PRICE_DIFFERENCE_TAG = "price-difference";
    private static final String STORABLE_TAG = "storable";
    private static final String STORED_AS_TAG = "stored-as";
    private static final String TRADE_GOODS_TAG = "trade-goods";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, IS_FARMED_TAG, isFarmed);

        writeAttribute(out, IS_FOOD_TAG, isFood);

        writeAttribute(out, IGNORE_LIMIT_TAG, ignoreLimit);

        writeAttribute(out, NEW_WORLD_GOODS_TAG, newWorldGoods);

        writeAttribute(out, TRADE_GOODS_TAG, tradeGoods);

        writeAttribute(out, STORABLE_TAG, storable);

        if (breedingNumber != INFINITY) {
            writeAttribute(out, BREEDING_NUMBER_TAG, breedingNumber);
        }

        if (price != INFINITY) {
            writeAttribute(out, PRICE_TAG, price);
        }

        if (madeFrom != null) {
            writeAttribute(out, MADE_FROM_TAG, madeFrom);
        }

        if (storedAs != null) {
            writeAttribute(out, STORED_AS_TAG, storedAs);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (initialAmount > 0) {
            out.writeStartElement(MARKET_TAG);

            writeAttribute(out, INITIAL_AMOUNT_TAG, initialAmount);

            writeAttribute(out, INITIAL_PRICE_TAG, initialPrice);

            writeAttribute(out, PRICE_DIFFERENCE_TAG, priceDiff);

            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        isFarmed = getAttribute(in, IS_FARMED_TAG, false);

        isFood = getAttribute(in, IS_FOOD_TAG, false);

        ignoreLimit = getAttribute(in, IGNORE_LIMIT_TAG, false);

        newWorldGoods = getAttribute(in, NEW_WORLD_GOODS_TAG, false);

        tradeGoods = getAttribute(in, TRADE_GOODS_TAG, false);

        breedingNumber = getAttribute(in, BREEDING_NUMBER_TAG, INFINITY);

        price = getAttribute(in, PRICE_TAG, INFINITY);

        madeFrom = spec.getType(in, MADE_FROM_TAG, GoodsType.class,
                                (GoodsType)null);
        if (madeFrom != null) madeFrom.makes = this;

        storable = getAttribute(in, STORABLE_TAG, true);

        storedAs = spec.getType(in, STORED_AS_TAG, GoodsType.class,
                                (GoodsType)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (MARKET_TAG.equals(tag)) {
            initialAmount = getAttribute(in, INITIAL_AMOUNT_TAG, 0);

            initialPrice = getAttribute(in, INITIAL_PRICE_TAG, 1);

            priceDiff = getAttribute(in, PRICE_DIFFERENCE_TAG, 1);

            closeTag(in, MARKET_TAG);

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goods-type".
     */
    public static String getXMLElementTagName() {
        return "goods-type";
    }
}
