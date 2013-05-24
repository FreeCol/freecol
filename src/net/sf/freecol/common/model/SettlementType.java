/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * The various types of settlements in the game.
 */
public class SettlementType extends FreeColGameObjectType {

    /** Whether this SettlementType is a capital. */
    private boolean capital = false;

    /** How many tiles this SettlementType can see. */
    private int visibleRadius = 2;

    /** How many tiles this SettlementType can claim. */
    private int claimableRadius = 1;

    /**
     * The extra radius beyond the claimableRadius where wandering
     * units may claim as yet unclaimed tiles.
     */
    private int extraClaimableRadius = 2;

    /** How far units from this SettlementType may roam. */
    private int wanderingRadius = 4;

    /** The minimum number of units for this SettlementType. */
    private int minimumSize = 3;

    /** The maximum number of units for this SettlementType. */
    private int maximumSize = 10;

    /** The minimum number of tiles to grow this SettlementType. */
    private int minimumGrowth = 1;

    /** The maximum number of tiles to grown this SettlementType. */
    private int maximumGrowth = 10;

    /**
     * The general trade bonus, roughly proportional to the settlement
     * size and general sophistication.
     */
    private int tradeBonus = 1;

    /** The threshold at which a new convert occurs. */
    private int convertThreshold = 100;

    /** The plunder this SettlementType generates when destroyed. */
    private List<RandomRange> plunder = null;

    /** The gifts this SettlementType generates when visited by a scout. */
    private List<RandomRange> gifts = null;


    /**
     * Creates a new settlement type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public SettlementType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new settlement type.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @param specification The <code>Specification</code> to refer to.
     * @exception XMLStreamException if there is problem reading the stream.
     */
    public SettlementType(FreeColXMLReader xr,
                          Specification specification) throws XMLStreamException {
        super(specification);

        readFromXML(xr);
    }


    /**
     * Is this a capital settlement type?
     *
     * @return True if this is a capital.
     */
    public final boolean isCapital() {
        return capital;
    }

    /**
     * Get the minimum size of this settlement type.
     *
     * @return The minimum settlement size.
     */
    public final int getMinimumSize() {
        return minimumSize;
    }

    /**
     * Get the maximum size of this settlement type.
     *
     * @return The maximum settlement size.
     */
    public final int getMaximumSize() {
        return maximumSize;
    }

    /**
     * Get the visible radius of this settlement type.
     *
     * @return The visible radius.
     */
    public final int getVisibleRadius() {
        return visibleRadius;
    }

    /**
     * Get the claimable radius of this settlement type.
     *
     * @return The claimable radius.
     */
    public final int getClaimableRadius() {
        return claimableRadius;
    }

    /**
     * Get the extra claimable radius.
     *
     * @return The extra claimable radius.
     */
    public final int getExtraClaimableRadius() {
        return extraClaimableRadius;
    }

    /**
     * Get the wandering radius for this settlement type.
     *
     * @return The wandering radius.
     */
    public final int getWanderingRadius() {
        return wanderingRadius;
    }

    /**
     * Get the minimum growth value.
     *
     * @return The minimum number of tiles to try to grow this
     *     settlement type by.
     */
    public final int getMinimumGrowth() {
        return minimumGrowth;
    }

    /**
     * Get the maximum growth value.
     *
     * @return The maximum number of tiles to try to grow this
     *     settlement type by.
     */
    public final int getMaximumGrowth() {
        return maximumGrowth;
    }

    /**
     * Gets the trade bonus.
     *
     * @return The general bonus to trade.
     */
    public final int getTradeBonus() {
        return tradeBonus;
    }

    /**
     * Gets the convert threshold for this settlement.
     *
     * @return The convert threshold.
     */
    public int getConvertThreshold() {
        return convertThreshold;
    }

    /**
     * Gets the plunder range available for the supplied unit.
     *
     * @param unit The <code>Unit</code> to check.
     * @return The plunder range, or null if none applicable.
     */
    public final RandomRange getPlunderRange(Unit unit) {
        if (plunder == null) return null;

        for (RandomRange range : plunder) {
            List<Scope> scopes = range.getScopes();
            if (scopes.isEmpty()) return range;
            for (Scope scope : scopes) {
                if (scope.appliesTo(unit)) return range;
            }
        }
        return null;
    }

    /**
     * Get the range of gifts available to a unit.
     *
     * @param unit The <code>Unit</code> to check.
     * @return A range of gifts, or null if none applicable.
     */
    public final RandomRange getGifts(Unit unit) {
        if (gifts == null) return null;

        for (RandomRange range : gifts) {
            List<Scope> scopes = range.getScopes();
            if (scopes.isEmpty()) return range;
            for (Scope scope : scopes) {
                if (scope.appliesTo(unit)) return range;
            }
        }
        return null;
    }


    /**
     * Gets the warehouse capacity of this settlement.
     *
     * @return The warehouse capacity of this settlement.
     */
    public int getWarehouseCapacity() {
        return GoodsContainer.CARGO_SIZE * getClaimableRadius();
    }

    /**
     * Add a gift.
     *
     * @param gift The gift to add.
     */
    private void addGift(RandomRange gift) {
        if (gifts == null) gifts = new ArrayList<RandomRange>();
        gifts.add(gift);
    }

    /**
     * Add a plunder.
     *
     * @param range The plunder to add.
     */
    private void addPlunder(RandomRange range) {
        if (plunder == null) plunder = new ArrayList<RandomRange>();
        plunder.add(range);
    }


    // Serialization

    private static final String CAPITAL_TAG = "capital";
    private static final String CLAIMABLE_RADIUS_TAG = "claimableRadius";
    private static final String CONVERT_THRESHOLD_TAG = "convertThreshold";
    private static final String EXTRA_CLAIMABLE_RADIUS_TAG = "extraClaimableRadius";
    private static final String GIFTS_TAG = "gifts";
    private static final String MAXIMUM_GROWTH_TAG = "maximumGrowth";
    private static final String MAXIMUM_SIZE_TAG = "maximumSize";
    private static final String MINIMUM_GROWTH_TAG = "minimumGrowth";
    private static final String MINIMUM_SIZE_TAG = "minimumSize";
    private static final String PLUNDER_TAG = "plunder";
    private static final String TRADE_BONUS_TAG = "tradeBonus";
    private static final String VISIBLE_RADIUS_TAG = "visibleRadius";
    private static final String WANDERING_RADIUS_TAG = "wanderingRadius";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(CAPITAL_TAG, capital);

        xw.writeAttribute(MINIMUM_SIZE_TAG, minimumSize);

        xw.writeAttribute(MAXIMUM_SIZE_TAG, maximumSize);

        xw.writeAttribute(VISIBLE_RADIUS_TAG, visibleRadius);

        xw.writeAttribute(CLAIMABLE_RADIUS_TAG, claimableRadius);

        xw.writeAttribute(EXTRA_CLAIMABLE_RADIUS_TAG, extraClaimableRadius);

        xw.writeAttribute(WANDERING_RADIUS_TAG, wanderingRadius);

        xw.writeAttribute(MINIMUM_GROWTH_TAG, minimumGrowth);

        xw.writeAttribute(MAXIMUM_GROWTH_TAG, maximumGrowth);

        xw.writeAttribute(TRADE_BONUS_TAG, tradeBonus);

        xw.writeAttribute(CONVERT_THRESHOLD_TAG, convertThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (plunder != null) {
            for (RandomRange range : plunder) range.toXML(xw, PLUNDER_TAG);
        }

        if (gifts != null) {
            for (RandomRange range : gifts) range.toXML(xw, GIFTS_TAG);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        capital = xr.getAttribute(CAPITAL_TAG, capital);

        minimumSize = xr.getAttribute(MINIMUM_SIZE_TAG, minimumSize);

        maximumSize = xr.getAttribute(MAXIMUM_SIZE_TAG, maximumSize);

        visibleRadius = xr.getAttribute(VISIBLE_RADIUS_TAG, visibleRadius);

        claimableRadius = xr.getAttribute(CLAIMABLE_RADIUS_TAG,
                                          claimableRadius);

        extraClaimableRadius = xr.getAttribute(EXTRA_CLAIMABLE_RADIUS_TAG,
                                               extraClaimableRadius);

        wanderingRadius = xr.getAttribute(WANDERING_RADIUS_TAG,
                                          wanderingRadius);

        minimumGrowth = xr.getAttribute(MINIMUM_GROWTH_TAG, minimumGrowth);

        maximumGrowth = xr.getAttribute(MAXIMUM_GROWTH_TAG, maximumGrowth);

        tradeBonus = xr.getAttribute(TRADE_BONUS_TAG, tradeBonus);

        convertThreshold = xr.getAttribute(CONVERT_THRESHOLD_TAG,
                                           convertThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            plunder = null;
            gifts = null;
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (GIFTS_TAG.equals(tag)) {
            addGift(new RandomRange(xr));

        } else if (PLUNDER_TAG.equals(tag)) {
            addPlunder(new RandomRange(xr));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "settlementType".
     */
    public static String getXMLElementTagName() {
        return "settlementType";
    }
}
