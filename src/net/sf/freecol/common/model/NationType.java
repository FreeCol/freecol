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
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Represents the type of one of the nations present in the game.
 */
public abstract class NationType extends FreeColGameObjectType {

    public static enum SettlementNumber {
        LOW, AVERAGE, HIGH;

        /**
         * Get a message key for this settlement number.
         *
         * @return A message key.
         */
        public String getKey() {
            return "settlementNumber." + getEnumKey(this);
        }
    }

    public static enum AggressionLevel {
        LOW, AVERAGE, HIGH;

        /**
         * Get a message key for this aggression level.
         *
         * @return A message key.
         */
        public String getKey() {
            return "aggressionLevel." + getEnumKey(this);
        }
    }


    /** The number of settlements this Nation has. */
    private SettlementNumber numberOfSettlements = SettlementNumber.AVERAGE;

    /** The aggression of this Nation. */
    private AggressionLevel aggression = AggressionLevel.AVERAGE;

    /** The types of settlement this Nation has. */
    private List<SettlementType> settlementTypes = null;


    /**
     * Default nation type constructor.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public NationType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the settlement types.
     *
     * @return A list of <code>SettlementType</code>s.
     */
    public final List<SettlementType> getSettlementTypes() {
        return (settlementTypes == null)
            ? Collections.<SettlementType>emptyList()
            : settlementTypes;
    }

    /**
     * Add a settlement type.
     *
     * @param settlementType The <code>SettlementType</code> to add.
     */
    private void addSettlementType(SettlementType settlementType) {
        if (settlementTypes == null) settlementTypes = new ArrayList<>();
        settlementTypes.add(settlementType);
    }

    /**
     * Add settlement types.
     *
     * @param types A list of <code>SettlementType</code>s to add.
     */
    private void addSettlementTypes(List<SettlementType> types) {
        if (settlementTypes == null) settlementTypes = new ArrayList<>();
        settlementTypes.addAll(types);
    }

    /**
     * Gets the settlement type for the national capital.
     *
     * @return The capital <code>SettlementType</code>.
     */
    public SettlementType getCapitalType() {
        return getSettlementType(true);
    }

    /**
     * Gets the settlement type for a settlement of this nation.
     *
     * @param isCapital If true, get the capital type.
     * @return The settlement type.
     */
    public SettlementType getSettlementType(boolean isCapital) {
        return find(getSettlementTypes(), s -> s.isCapital() == isCapital);
    }

    /**
     * Get a settlement type by identifier.
     *
     * @param id The object identifier.
     * @return The settlement type.
     */
    public SettlementType getSettlementType(String id) {
        return find(getSettlementTypes(), s -> id.equals(s.getId()));
    }

    /**
     * Get the national number of settlements.
     *
     * @return The <code>SettlementNumber</code>.
     */
    public final SettlementNumber getNumberOfSettlements() {
        return numberOfSettlements;
    }

    /**
     * Get the national aggression.
     *
     * @return The national <code>AggressionLevel</code>.
     */
    public final AggressionLevel getAggression() {
        return aggression;
    }

    /**
     * Whether this is a EuropeanNation, i.e. a player or a REF.
     *
     * @return True if this is an European nation.
     */
    public abstract boolean isEuropean();

    /**
     * Whether this is a IndianNation.
     *
     * @return True if this is a native nation.
     */
    public abstract boolean isIndian();

    /**
     * Whether this is a EuropeanREFNation.
     *
     * @return True if this is a REF nation.
     */
    public abstract boolean isREF();


    // Serialization

    private static final String AGGRESSION_TAG = "aggression";
    private static final String NUMBER_OF_SETTLEMENTS_TAG = "number-of-settlements";
    private static final String SETTLEMENT_TAG = "settlement";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NUMBER_OF_SETTLEMENTS_TAG, numberOfSettlements);

        xw.writeAttribute(AGGRESSION_TAG, aggression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (SettlementType settlementType : getSettlementTypes()) {
            settlementType.toXML(xw, SETTLEMENT_TAG);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        NationType parent = xr.getType(spec, EXTENDS_TAG,
                                       NationType.class, this);

        numberOfSettlements = xr.getAttribute(NUMBER_OF_SETTLEMENTS_TAG,
            SettlementNumber.class, parent.numberOfSettlements);

        aggression = xr.getAttribute(AGGRESSION_TAG,
                                     AggressionLevel.class, parent.aggression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            settlementTypes = null;
        }

        final Specification spec = getSpecification();
        NationType parent = xr.getType(spec, EXTENDS_TAG,
                                       NationType.class, this);
        if (parent != this) {
            if (parent.settlementTypes != null) {
                addSettlementTypes(parent.settlementTypes);
            }

            addFeatures(parent);
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (SETTLEMENT_TAG.equals(tag)) {
            addSettlementType(new SettlementType(xr, spec));

        } else {
            super.readChild(xr);
        }
    }
}
