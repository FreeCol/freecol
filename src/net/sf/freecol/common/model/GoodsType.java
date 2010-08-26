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
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public final class GoodsType extends FreeColGameObjectType {

    private boolean isFarmed;
    private boolean isFood;
    private boolean ignoreLimit;
    private boolean newWorldGoods;

    /**
     * Whether this type of goods is required for building.
     */
    private boolean buildingMaterial;

    /**
     * Whether this type of goods is required for building equipment
     * that grants an offence bonus or defence bonus.
     */
    private boolean militaryGoods;

    /**
     * Whether these are trade goods that can only be obtained in
     * Europe.
     */
    private boolean tradeGoods;

    /**
     * Whether this type of goods can be stored in a warehouse.
     */
    private boolean storable;

    private GoodsType madeFrom;
    private GoodsType makes;
    private GoodsType storedAs;
    
    private int initialAmount;
    private int initialPrice;
    private int priceDiff;

    /**
     * The number of units required to breed this type of goods. This
     * obviously only applies to animals.
     */
    private int breedingNumber = INFINITY;

    /**
     * The price of this type of goods. This is only used for goods
     * that can not be traded in the market, such as hammers.
     */
    private int price = INFINITY;


    // ----------------------------------------------------------- constructors

    public GoodsType(String id, Specification specification) {
        super(id, specification);
    }

    // ----------------------------------------------------------- retriveal methods

    public StringTemplate getLabel(boolean sellable) {
        if (sellable) {
            return StringTemplate.key(getNameKey());
        } else {
            return StringTemplate.template("model.goods.goodsBoycotted")
                .add("%goods%", getNameKey());
        }
    }

    public boolean isRawMaterial() {
        return makes != null;
    }

    public boolean isRefined() {
        return madeFrom != null;
    }

    public GoodsType getRawMaterial() {
        return madeFrom;
    }

    public GoodsType getProducedMaterial() {
        return makes;
    }

    public boolean isFarmed() {
        return isFarmed;
    }

    public boolean limitIgnored() {
        return ignoreLimit;
    }

    public boolean isNewWorldGoodsType() {
        return newWorldGoods;
    }

    public boolean isNewWorldLuxuryType() {
        return (madeFrom != null && madeFrom.isNewWorldGoodsType());
    }

    public final String getWorkingAsKey() {
        return getId() + ".workingAs";
    }

    /**
     * Get the <code>ImmigrationType</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isImmigrationType() {
        return !getModifierSet("model.modifier.immigration").isEmpty();
    }

    /**
     * Get the <code>LibertyType</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLibertyType() {
        return !getModifierSet("model.modifier.liberty").isEmpty();
    }

    public boolean isStorable() {
        return storable;
    }

    public boolean isStoredAs() {
        return storedAs!=null;
    }

    public GoodsType getStoredAs() {
        if (storedAs==null) {
            return this;
        } else {
            return storedAs;
        }
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    public int getInitialSellPrice() {
        return initialPrice;
    }

    public int getInitialBuyPrice() {
        return initialPrice + priceDiff;
    }

    public int getPriceDifference() {
        return priceDiff;
    }

    // TODO: give this some meaning
    // Originally intended for when there are no static variables
    public boolean isFoodType() {
        return isFood;
    }
    
    public GoodsType outputType() {
        return makes;
    }

    public GoodsType inputType() {
        return madeFrom;
    }

    /**
     * Returns true if this type of goods is required for building a
     * BuildableType.
     *
     * @return a <code>boolean</code> value
     * @see BuildableType
     */
    public boolean isBuildingMaterial() {
        return buildingMaterial;
    }

    /**
     * Set the <code>BuildingMaterial</code> value.
     *
     * @param newBuildingMaterial The new BuildingMaterial value.
     */
    public void setBuildingMaterial(final boolean newBuildingMaterial) {
        this.buildingMaterial = newBuildingMaterial;
    }

    /**
     * Returns the production chain of the goods type, beginning with
     * a raw material that can not be produced from any other. The
     * last element of the production chain is the goods type itself.
     *
     * @return  the production chain of the goods type
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
     * Returns true if this type of goods is required for producing a
     * type of goods required for building a BuildableType.
     *
     * @return a <code>boolean</code> value
     * @see BuildableType
     */
    public boolean isRawBuildingMaterial() {
        if (this.madeFrom!=null) {
            return false;
        }
        GoodsType refinedType = makes;
        while (refinedType != null) {
            if (refinedType.isBuildingMaterial()) {
                return true;
            } else {
                refinedType = refinedType.makes;
            }
        }
        return false;
    }

    /**
     * Get the <code>MilitaryGoods</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isMilitaryGoods() {
        return militaryGoods;
    }

    /**
     * Set the <code>MilitaryGoods</code> value.
     *
     * @param newMilitaryGoods The new MilitaryGoods value.
     */
    public void setMilitaryGoods(final boolean newMilitaryGoods) {
        this.militaryGoods = newMilitaryGoods;
    }

    /**
     * Get the <code>TradeGoods</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isTradeGoods() {
        return tradeGoods;
    }

    /**
     * Set the <code>TradeGoods</code> value.
     *
     * @param newTradeGoods The new TradeGoods value.
     */
    public void setTradeGoods(final boolean newTradeGoods) {
        this.tradeGoods = newTradeGoods;
    }


    /**
     * Whether this type of goods produces liberty points.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLibertyGoodsType() {
        return getFeatureContainer().containsModifierKey("model.modifier.liberty");
    }

    /**
     * Whether this type of goods causes immigration.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isImmigrationGoodsType() {
        return getFeatureContainer().containsModifierKey("model.modifier.immigration");
    }

    /**
     * Get the <code>BreedingNumber</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getBreedingNumber() {
        return breedingNumber;
    }

    /**
     * Set the <code>BreedingNumber</code> value.
     *
     * @param newBreedingNumber The new BreedingNumber value.
     */
    public void setBreedingNumber(final int newBreedingNumber) {
        this.breedingNumber = newBreedingNumber;
    }

    /**
     * Returns <code>true</code> if this type of Goods is
     * breedable. This should only be true for animals, such as
     * horses.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isBreedable() {
        return breedingNumber != INFINITY;
    }

    /**
     * Get the <code>Price</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getPrice() {
        return price;
    }

    /**
     * Set the <code>Price</code> value.
     *
     * @param newPrice The new Price value.
     */
    public void setPrice(final int newPrice) {
        this.price = newPrice;
    }

    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        isFarmed = getAttribute(in, "is-farmed", false);
        isFood = getAttribute(in, "is-food", false);
        ignoreLimit = getAttribute(in, "ignore-limit", false);
        newWorldGoods = getAttribute(in, "new-world-goods", false);
        breedingNumber = getAttribute(in, "breeding-number", INFINITY);
        price = getAttribute(in, "price", INFINITY);

        madeFrom = getSpecification().getType(in, "made-from", GoodsType.class, null);
        if (madeFrom != null) {
            madeFrom.makes = this;
        }

        storable = getAttribute(in, "storable", true);
        storedAs = getSpecification().getType(in, "stored-as", GoodsType.class, null);
    }

    public void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("market".equals(childName)) {
            initialAmount = Integer.parseInt(in.getAttributeValue(null, "initial-amount"));
            initialPrice = getAttribute(in, "initial-price", 1);
            priceDiff = getAttribute(in, "price-difference", 1);
            in.nextTag(); // close this element
        } else {
            super.readChild(in);
        }
    }


    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXMLImpl(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("is-farmed", Boolean.toString(isFarmed));
        out.writeAttribute("is-food", Boolean.toString(isFood));
        out.writeAttribute("ignore-limit", Boolean.toString(ignoreLimit));
        out.writeAttribute("new-world-goods", Boolean.toString(newWorldGoods));
        out.writeAttribute("storable", Boolean.toString(storable));
        if (breedingNumber != INFINITY) {
            out.writeAttribute("breeding-number", Integer.toString(breedingNumber));
        }
        if (price != INFINITY) {
            out.writeAttribute("price", Integer.toString(price));
        }
        if (madeFrom != null) {
            out.writeAttribute("made-from", madeFrom.getId());
        }
        if (storedAs != null) {
            out.writeAttribute("stored-as", storedAs.getId());
        }
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        if (initialAmount > 0) {
            out.writeStartElement("market");
            out.writeAttribute("initial-amount", Integer.toString(initialAmount));
            out.writeAttribute("initial-price", Integer.toString(initialPrice));
            out.writeAttribute("price-difference", Integer.toString(priceDiff));
            out.writeEndElement();
        }
    }

    public static String getXMLElementTagName() {
        return "goods-type";
    }


}
