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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * A type of goods, derived from the specification.
 */
public final class GoodsType extends FreeColGameObjectType {

    private static final float DEFAULT_PRODUCTION_WEIGHT = 1.0f;
    private static final float DEFAULT_LOW_PRODUCTION_THRESHOLD = 0.0f;
    private static final float DEFAULT_ZERO_PRODUCTION_FACTOR = 1.0f;

    /** A comparator to impose a useful order on goods types. */
    public static final Comparator<GoodsType> goodsTypeComparator
        = new Comparator<GoodsType>() {
            private int rank(GoodsType g) {
                return (!g.isStorable() || g.isTradeGoods()) ? -1
                    : (g.isFoodType()) ? 1
                    : (g.isNewWorldGoodsType()) ? 2
                    : (g.isFarmed()) ? 3
                    : (g.isRawMaterial()) ? 4
                    : (g.isNewWorldLuxuryType()) ? 5
                    : (g.isRefined()) ? 6
                    : -1;
            }

            @Override
            public int compare(GoodsType g1, GoodsType g2) {
                int r1 = rank(g1);
                int r2 = rank(g2);
                return (r1 != r2) ? r1 - r2
                : g1.getId().compareTo(g2.getId());
            }
        };

    /** Is this a farmed goods type. */
    private boolean isFarmed;

    /** Is this a food type. */
    private boolean isFood;

    /** Does this goods type ignore warehouse limits. */
    private boolean ignoreLimit;

    /** Is this goods type native to the New World. */
    private boolean newWorldGoods;

    /**
     * Whether this type of goods is required for building equipment
     * that grants an offence bonus or defence bonus.
     */
    private boolean isMilitary = false;

    /**
     * Whether this type of goods is required for building. (Derived
     * attribute)
     */
    private boolean buildingMaterial = false;

    /** Whether these are trade goods that can only be obtained in Europe. */
    private boolean tradeGoods;

    /** Whether this type of goods can be stored in a warehouse. */
    private boolean storable;

    /** What this goods type is stored as. */
    private GoodsType storedAs;

    /** What this goods type is made from. */
    private GoodsType madeFrom;

    /** What this goods type can make.  (Derived attribute) */
    private GoodsType makes = null;

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
     * A weight for the potential production of this goods type at a
     * colony site.
     */
    private float productionWeight = DEFAULT_PRODUCTION_WEIGHT;

    /**
     * A threshold amount of potential production of this goods type
     * at a colony site, below which the score for the site is reduced.
     */
    private float lowProductionThreshold = DEFAULT_LOW_PRODUCTION_THRESHOLD;

    /**
     * The multiplicative factor with which to penalize a colony site
     * with zero production of this goods type, scaling linearly to
     * unity when the amount reaches lowResourceThreshold.
     */
    private float zeroProductionFactor = DEFAULT_ZERO_PRODUCTION_FACTOR;


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
     * Is this goods type a military goods type?
     *
     * @return True if this is a military goods type.
     */
    public boolean isMilitaryGoods() {
        return isMilitary;
    }

    // @compat 0.10.x
    // Needed by Specification fixup010x()
    public void setMilitary() {
        this.isMilitary = true;
    }
    // end @compat 0.10.x

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
        return containsModifierKey(Modifier.LIBERTY);
    }

    /**
     * Does this type of goods produce immigration?
     *
     * @return True if this goods type produces immigration.
     */
    public boolean isImmigrationType() {
        return containsModifierKey(Modifier.IMMIGRATION);
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

    public void setPrice(final int newPrice) {
        this.price = newPrice;
    }

    /**
     * Get the production weight.
     *
     * @return The production weight.
     */
    public float getProductionWeight() {
        return productionWeight;
    }

    /**
     * Get the low production threshold.
     *
     * @return The low production threshold.
     */
    public float getLowProductionThreshold() {
        return lowProductionThreshold;
    }

    /**
     * Get the zero production factor.
     *
     * @return The zero production factor.
     */
    public float getZeroProductionFactor() {
        return zeroProductionFactor;
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
     * Note that this does not really handle goods that are stored as
     * something else as is the case for grain etc.
     * FIXME: fix or retire this routine?
     *
     * @return The production chain of this <code>GoodsType</code> as
     *     a list.
     */
    public List<GoodsType> getProductionChain() {
        List<GoodsType> result = new ArrayList<>();
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

    /**
     * Get all the equivalent goods types, in the sense that they are
     * stored as this type.
     *
     * @return A set of equivalent <code>GoodsType</code>s, which
     *     must include this one.
     */
    public Set<GoodsType> getEquivalentTypes() {
        Set<GoodsType> result = new HashSet<>();
        for (GoodsType type : getSpecification().getGoodsTypeList()) {
            if (type == this
                || type.getStoredAs() == this) result.add(type);
        }
        return result;
    }
        
    /**
     * Set the derived fields for the goods types in a specification.
     *
     * The "derived" fields are: buildingMaterial + makes
     * - buildingMaterial depends on whether a GoodsType is present on
     *   a BuildableType requiredGoods list
     * - makes depends on whether a GoodsType madeFrom field refers
     *   to another
     *   
     * This is called from Specification.clean() when the
     * specification is fully read.  We must wait until then as the
     * made-from field can change in extended specifications and mods.
     * The current example of which is horses, which is made-from food
     * in the classic ruleset and made-from grain in the freecol
     * ruleset.
     *
     * @param spec The <code>Specification<code> to operate on.
     */
    public static void setDerivedAttributes(Specification spec) {
        // Reset to default state
        for (GoodsType g : spec.getGoodsTypeList()) {
            g.buildingMaterial = false;
            g.makes = null;
        }

        // Set buildingMaterial attribute
        List<BuildableType> buildableTypes = new ArrayList<>();
        buildableTypes.addAll(spec.getBuildingTypeList());
        buildableTypes.addAll(spec.getUnitTypeList());
        buildableTypes.addAll(spec.getRoles());
        for (BuildableType b : buildableTypes) {
            for (AbstractGoods ag : b.getRequiredGoods()) {
                ag.getType().buildingMaterial = true;
            }
        }

        // Set makes attribute
        for (GoodsType g : spec.getGoodsTypeList()) {
            if (g.madeFrom != null) g.madeFrom.makes = g;
        }
    }


    // Serialization

    private static final String BREEDING_NUMBER_TAG = "breeding-number";
    private static final String IGNORE_LIMIT_TAG = "ignore-limit";
    private static final String INITIAL_AMOUNT_TAG = "initial-amount";
    private static final String INITIAL_PRICE_TAG = "initial-price";
    private static final String IS_FARMED_TAG = "is-farmed";
    private static final String IS_FOOD_TAG = "is-food";
    private static final String IS_MILITARY_TAG = "is-military";
    private static final String LOW_PRODUCTION_THRESHOLD_TAG = "low-production-threshold";
    private static final String MADE_FROM_TAG = "made-from";
    private static final String MARKET_TAG = "market";
    private static final String NEW_WORLD_GOODS_TAG = "new-world-goods";
    private static final String PRICE_TAG = "price";
    private static final String PRICE_DIFFERENCE_TAG = "price-difference";
    private static final String PRODUCTION_WEIGHT_TAG = "production-weight";
    private static final String STORABLE_TAG = "storable";
    private static final String STORED_AS_TAG = "stored-as";
    private static final String TRADE_GOODS_TAG = "trade-goods";
    private static final String ZERO_PRODUCTION_FACTOR_TAG = "zero-production-factor";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(IS_FARMED_TAG, isFarmed);

        xw.writeAttribute(IS_FOOD_TAG, isFood);

        xw.writeAttribute(IS_MILITARY_TAG, isMilitary);

        xw.writeAttribute(IGNORE_LIMIT_TAG, ignoreLimit);

        xw.writeAttribute(NEW_WORLD_GOODS_TAG, newWorldGoods);

        xw.writeAttribute(TRADE_GOODS_TAG, tradeGoods);

        xw.writeAttribute(STORABLE_TAG, storable);

        if (breedingNumber != INFINITY) {
            xw.writeAttribute(BREEDING_NUMBER_TAG, breedingNumber);
        }

        if (price != INFINITY) {
            xw.writeAttribute(PRICE_TAG, price);
        }

        if (madeFrom != null) {
            xw.writeAttribute(MADE_FROM_TAG, madeFrom);
        }

        if (storedAs != null) {
            xw.writeAttribute(STORED_AS_TAG, storedAs);
        }

        xw.writeAttribute(PRODUCTION_WEIGHT_TAG, productionWeight);

        if (lowProductionThreshold > DEFAULT_LOW_PRODUCTION_THRESHOLD) {
            xw.writeAttribute(LOW_PRODUCTION_THRESHOLD_TAG,
                              lowProductionThreshold);
        }

        if (0.0 <= zeroProductionFactor
            && zeroProductionFactor < DEFAULT_ZERO_PRODUCTION_FACTOR) {
            xw.writeAttribute(ZERO_PRODUCTION_FACTOR_TAG, zeroProductionFactor);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (initialAmount > 0) {
            xw.writeStartElement(MARKET_TAG);

            xw.writeAttribute(INITIAL_AMOUNT_TAG, initialAmount);

            xw.writeAttribute(INITIAL_PRICE_TAG, initialPrice);

            xw.writeAttribute(PRICE_DIFFERENCE_TAG, priceDiff);

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        isFarmed = xr.getAttribute(IS_FARMED_TAG, false);

        isFood = xr.getAttribute(IS_FOOD_TAG, false);

        isMilitary = xr.getAttribute(IS_MILITARY_TAG, false);

        ignoreLimit = xr.getAttribute(IGNORE_LIMIT_TAG, false);

        newWorldGoods = xr.getAttribute(NEW_WORLD_GOODS_TAG, false);

        tradeGoods = xr.getAttribute(TRADE_GOODS_TAG, false);

        breedingNumber = xr.getAttribute(BREEDING_NUMBER_TAG, INFINITY);

        price = xr.getAttribute(PRICE_TAG, INFINITY);

        madeFrom = xr.getType(spec, MADE_FROM_TAG, GoodsType.class,
                              (GoodsType)null);

        storable = xr.getAttribute(STORABLE_TAG, true);

        storedAs = xr.getType(spec, STORED_AS_TAG, GoodsType.class,
                              (GoodsType)null);

        productionWeight = xr.getAttribute(PRODUCTION_WEIGHT_TAG,
            DEFAULT_PRODUCTION_WEIGHT);
        // @compat 0.10.7
        if (!xr.hasAttribute(PRODUCTION_WEIGHT_TAG) && isFarmed) {
            // Use something a bit more than the default for old games
            productionWeight = 3.0f;
        }
        // end @compat 0.10.7

        lowProductionThreshold = xr.getAttribute(LOW_PRODUCTION_THRESHOLD_TAG,
            DEFAULT_LOW_PRODUCTION_THRESHOLD);

        zeroProductionFactor = xr.getAttribute(ZERO_PRODUCTION_FACTOR_TAG,
            DEFAULT_ZERO_PRODUCTION_FACTOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (MARKET_TAG.equals(tag)) {
            initialAmount = xr.getAttribute(INITIAL_AMOUNT_TAG, 0);

            initialPrice = xr.getAttribute(INITIAL_PRICE_TAG, 1);

            priceDiff = xr.getAttribute(PRICE_DIFFERENCE_TAG, 1);

            xr.closeTag(MARKET_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goods-type".
     */
    public static String getXMLElementTagName() {
        return "goods-type";
    }
}
