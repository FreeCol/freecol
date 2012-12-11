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


/**
 * Represents one of the nations present in the game.
 */
public abstract class NationType extends FreeColGameObjectType {

    public static enum SettlementNumber { LOW, AVERAGE, HIGH }

    public static enum AggressionLevel { LOW, AVERAGE, HIGH }


    /**
     * The number of settlements this Nation has.
     */
    private SettlementNumber numberOfSettlements = SettlementNumber.AVERAGE;

    /**
     * The aggression of this Nation.
     */
    private AggressionLevel aggression = AggressionLevel.AVERAGE;

    /**
     * The types of settlement this Nation has.
     */
    private List<SettlementType> settlementTypes = null;


    /**
     * Default nation type constructor.
     *
     * @param id The nation type identifier.
     * @param specification The containing <code>Specification</code>.
     */
    public NationType(String id, Specification specification) {
        super(id, specification);

        setModifierIndex(Modifier.NATION_PRODUCTION_INDEX);
    }

    /**
     * Get the settlement types.
     *
     * @return A list of <code>SettlementType</code>s.
     */
    public final List<SettlementType> getSettlementTypes() {
        return (settlementTypes == null) ? new ArrayList<SettlementType>()
            : settlementTypes;
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
        for (SettlementType settlementType : getSettlementTypes()) {
            if (settlementType.isCapital() == isCapital) {
                return settlementType;
            }
        }
        // @compat 0.9.x
        // TODO: remove compatibility code and throw exception instead
        String id = "model.settlement."
            + getId().substring(getId().lastIndexOf(".") + 1)
            + (isCapital ? ".capital" : "");
        SettlementType type = new SettlementType(id, getSpecification());
        if (isCapital) {
            type.setCapital(true);
            type.setPlunder(new RandomRange(100, 2, 6, 1500));
            type.setGifts(new RandomRange(100, 2, 6, 200));
        } else {
            type.setPlunder(new RandomRange(50, 2, 6, 1000));
            type.setGifts(new RandomRange(50, 2, 6, 100));
        }
        // end compatibility code
        return type;
    }

    /**
     * Get a settlement type by id.
     *
     * @param id The id to check.
     * @return The settlement type.
     */
    public SettlementType getSettlementType(String id) {
        for (SettlementType settlementType : getSettlementTypes()) {
            if (id.equals(settlementType.getId())) {
                return settlementType;
            }
        }
        return null;
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
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, NUMBER_OF_SETTLEMENTS_TAG, numberOfSettlements);

        writeAttribute(out, AGGRESSION_TAG, aggression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (SettlementType settlementType : getSettlementTypes()) {
            settlementType.toXML(out, SETTLEMENT_TAG);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final Specification spec = getSpecification();
        NationType parent = spec.getType(in, EXTENDS_TAG,
                                         NationType.class, this);

        numberOfSettlements = getAttribute(in, NUMBER_OF_SETTLEMENTS_TAG,
            SettlementNumber.class, parent.numberOfSettlements);

        aggression = getAttribute(in, AGGRESSION_TAG,
                                  AggressionLevel.class, parent.aggression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            // Clear containers
            settlementTypes = null;
        }

        final Specification spec = getSpecification();
        NationType parent = spec.getType(in, EXTENDS_TAG,
                                         NationType.class, this);

        super.readChildren(in);

        if (parent != this) {
            if (parent.settlementTypes != null) {
                if (settlementTypes == null) {
                    settlementTypes = new ArrayList<SettlementType>();
                }
                settlementTypes.addAll(parent.settlementTypes);
            }

            addFeatures(parent);
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (SETTLEMENT_TAG.equals(tag)) {
            String id = getAttribute(in, ID_ATTRIBUTE_TAG, (String)null);
            SettlementType settlementType = new SettlementType(id, spec);
            settlementType.readFromXML(in);
            if (settlementTypes == null) {
                settlementTypes = new ArrayList<SettlementType>();
            }
            settlementTypes.add(settlementType);

        } else {
            super.readChild(in);
        }
    }
}
