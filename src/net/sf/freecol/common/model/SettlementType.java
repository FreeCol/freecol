/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The various types of settlements in the game.
 */
public class SettlementType extends FreeColSpecObjectType {

    public static final String TAG = "settlementType";

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
    private List<PlunderType> plunderTypes = null;

    /** The gifts this SettlementType generates when visited by a scout. */
    private RandomRange gifts = null;


    /**
     * Creates a new settlement type.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public SettlementType(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new settlement type.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
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
     * Get the list of plunder types.
     *
     * @return The list of {@code PlunderType}s.
     */
    public List<PlunderType> getPlunderTypes() {
        return (this.plunderTypes == null)
            ? Collections.<PlunderType>emptyList()
            : this.plunderTypes;
    }

    /**
     * Set the plunder types.
     *
     * @param plunderTypes The new plunder type list.
     */
    protected void setPlunderTypes(List<PlunderType> plunderTypes) {
        if (this.plunderTypes == null) {
            this.plunderTypes = new ArrayList<>();
        } else {
            this.plunderTypes.clear();
        }
        this.plunderTypes.addAll(plunderTypes);
    }

    /**
     * Gets the plunder range available for the supplied unit.
     *
     * @param unit The {@code Unit} to check.
     * @return The plunder range, or null if none applicable.
     */
    public final RandomRange getPlunderRange(Unit unit) {
        if (this.plunderTypes == null) return null;
        PlunderType pt = find(this.plunderTypes, p -> p.appliesTo(unit));
        return (pt == null) ? null : pt.getPlunder();
    }

    /**
     * Add a plunder type.
     *
     * @param pt The {@code PlunderType} to add.
     */
    private void addPlunderType(PlunderType pt) {
        if (this.plunderTypes == null) this.plunderTypes = new ArrayList<>();
        this.plunderTypes.add(pt);
    }
    
    /**
     * Get the range of gifts available to a unit.
     *
     * @return A range of gifts, or null if none applicable.
     */
    public final RandomRange getGifts() {
        return this.gifts;
    }

    /**
     * Gets the warehouse capacity of this settlement.
     *
     * @return The warehouse capacity of this settlement.
     */
    public int getWarehouseCapacity() {
        return GoodsContainer.CARGO_SIZE * getClaimableRadius();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        SettlementType o = copyInCast(other, SettlementType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.capital = o.isCapital();
        this.visibleRadius = o.getVisibleRadius();
        this.claimableRadius = o.getClaimableRadius();
        this.extraClaimableRadius = o.getExtraClaimableRadius();
        this.wanderingRadius = o.getWanderingRadius();
        this.minimumSize = o.getMinimumSize();
        this.maximumSize = o.getMaximumSize();
        this.minimumGrowth = o.getMinimumGrowth();
        this.maximumGrowth = o.getMaximumGrowth();
        this.tradeBonus = o.getTradeBonus();
        this.convertThreshold = o.getConvertThreshold();
        this.setPlunderTypes(o.getPlunderTypes());
        this.gifts = o.getGifts();
        return true;
    }

        
    // Serialization

    private static final String CAPITAL_TAG = "capital";
    private static final String CLAIMABLE_RADIUS_TAG = "claimable-radius";
    private static final String CONVERT_THRESHOLD_TAG = "convert-threshold";
    private static final String EXTRA_CLAIMABLE_RADIUS_TAG = "extra-claimable-radius";
    private static final String GIFTS_TAG = "gifts";
    private static final String MAXIMUM_GROWTH_TAG = "maximum-growth";
    private static final String MAXIMUM_SIZE_TAG = "maximum-size";
    private static final String MINIMUM_GROWTH_TAG = "minimum-growth";
    private static final String MINIMUM_SIZE_TAG = "minimum-size";
    private static final String TRADE_BONUS_TAG = "trade-bonus";
    private static final String VISIBLE_RADIUS_TAG = "visible-radius";
    private static final String WANDERING_RADIUS_TAG = "wandering-radius";
    // @compat 0.11.3
    private static final String OLD_CLAIMABLE_RADIUS_TAG = "claimableRadius";
    private static final String OLD_CONVERT_THRESHOLD_TAG = "convertThreshold";
    private static final String OLD_EXTRA_CLAIMABLE_RADIUS_TAG = "extraClaimableRadius";
    private static final String OLD_MAXIMUM_GROWTH_TAG = "maximumGrowth";
    private static final String OLD_MAXIMUM_SIZE_TAG = "maximumSize";
    private static final String OLD_MINIMUM_GROWTH_TAG = "minimumGrowth";
    private static final String OLD_MINIMUM_SIZE_TAG = "minimumSize";
    private static final String OLD_TRADE_BONUS_TAG = "tradeBonus";
    private static final String OLD_VISIBLE_RADIUS_TAG = "visibleRadius";
    private static final String OLD_WANDERING_RADIUS_TAG = "wanderingRadius";
    // end @compat 0.11.3


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

        if (this.plunderTypes != null) {
            for (PlunderType pt : this.plunderTypes) pt.toXML(xw);
        }

        if (this.gifts != null) {
            xw.writeStartElement(GIFTS_TAG);
                
            this.gifts.writeAttributes(xw);

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        capital = xr.getAttribute(CAPITAL_TAG, capital);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MINIMUM_SIZE_TAG)) {
            minimumSize = xr.getAttribute(OLD_MINIMUM_SIZE_TAG, minimumSize);
        } else
        // end @compat 0.11.3
            minimumSize = xr.getAttribute(MINIMUM_SIZE_TAG, minimumSize);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MAXIMUM_SIZE_TAG)) {
            maximumSize = xr.getAttribute(OLD_MAXIMUM_SIZE_TAG, maximumSize);
        } else
        // end @compat 0.11.3
            maximumSize = xr.getAttribute(MAXIMUM_SIZE_TAG, maximumSize);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_VISIBLE_RADIUS_TAG)) {
            visibleRadius = xr.getAttribute(OLD_VISIBLE_RADIUS_TAG, visibleRadius);
        } else
        // end @compat 0.11.3
            visibleRadius = xr.getAttribute(VISIBLE_RADIUS_TAG, visibleRadius);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_CLAIMABLE_RADIUS_TAG)) {
            claimableRadius = xr.getAttribute(OLD_CLAIMABLE_RADIUS_TAG, claimableRadius);
        } else
        // end @compat 0.11.3
            claimableRadius = xr.getAttribute(CLAIMABLE_RADIUS_TAG, claimableRadius);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_EXTRA_CLAIMABLE_RADIUS_TAG)) {
            extraClaimableRadius = xr.getAttribute(OLD_EXTRA_CLAIMABLE_RADIUS_TAG, extraClaimableRadius);
        } else
        // end @compat 0.11.3
            extraClaimableRadius = xr.getAttribute(EXTRA_CLAIMABLE_RADIUS_TAG, extraClaimableRadius);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_WANDERING_RADIUS_TAG)) {
            wanderingRadius = xr.getAttribute(OLD_WANDERING_RADIUS_TAG, wanderingRadius);
        } else
        // end @compat 0.11.3
            wanderingRadius = xr.getAttribute(WANDERING_RADIUS_TAG, wanderingRadius);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MINIMUM_GROWTH_TAG)) {
            minimumGrowth = xr.getAttribute(OLD_MINIMUM_GROWTH_TAG, minimumGrowth);
        } else
        // end @compat 0.11.3
            minimumGrowth = xr.getAttribute(MINIMUM_GROWTH_TAG, minimumGrowth);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_MAXIMUM_GROWTH_TAG)) {
            maximumGrowth = xr.getAttribute(OLD_MAXIMUM_GROWTH_TAG, maximumGrowth);
        } else
        // end @compat 0.11.3
            maximumGrowth = xr.getAttribute(MAXIMUM_GROWTH_TAG, maximumGrowth);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_TRADE_BONUS_TAG)) {
            tradeBonus = xr.getAttribute(OLD_TRADE_BONUS_TAG, tradeBonus);
        } else
        // end @compat 0.11.3
            tradeBonus = xr.getAttribute(TRADE_BONUS_TAG, tradeBonus);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_CONVERT_THRESHOLD_TAG)) {
            convertThreshold = xr.getAttribute(OLD_CONVERT_THRESHOLD_TAG, convertThreshold);
        } else
        // end @compat 0.11.3
            convertThreshold = xr.getAttribute(CONVERT_THRESHOLD_TAG, convertThreshold);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            this.plunderTypes = null;
            this.gifts = null;
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
            this.gifts = new RandomRange(xr);
            xr.closeTag(GIFTS_TAG);

        } else if (PlunderType.TAG.equals(tag)) {
            addPlunderType(new PlunderType(xr, getSpecification()));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
