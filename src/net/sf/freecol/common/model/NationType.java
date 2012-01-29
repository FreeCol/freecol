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
import java.util.Locale;

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
    private List<SettlementType> settlementTypes = new ArrayList<SettlementType>();


    public NationType(String id, Specification specification) {
        super(id, specification);
        setModifierIndex(Modifier.NATION_PRODUCTION_INDEX);
    }

    /**
     * Get the <code>TypeOfSettlement</code> value.
     *
     * @return an <code>SettlementType</code> value
     */
    public final List<SettlementType> getSettlementTypes() {
        return settlementTypes;
    }

    /**
     * Return the <code>SettlementType</code> of the nation type's
     * capital.
     *
     * @return a <code>SettlementType</code> value
     */
    public SettlementType getCapitalType() {
        return getSettlementType(true);
    }

    public SettlementType getSettlementType(boolean isCapital) {

        for (SettlementType settlementType : settlementTypes) {
            if (settlementType.isCapital() == isCapital) {
                return settlementType;
            }
        }
        // @compat 0.9.x
        // TODO: remove compatibility code and throw exception instead
        String id = "model.settlement." + getId().substring(getId().lastIndexOf(".") + 1)
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
        return type;
        // end compatibility code
    }

    public SettlementType getSettlementType(String id) {

        for (SettlementType settlementType : settlementTypes) {
            if (id.equals(settlementType.getId())) {
                return settlementType;
            }
        }
        return null;
    }

    /**
     * Get the <code>NumberOfSettlements</code> value.
     *
     * @return a <code>SettlementNumber</code> value
     */
    public final SettlementNumber getNumberOfSettlements() {
        return numberOfSettlements;
    }

    /**
     * Set the <code>NumberOfSettlements</code> value.
     *
     * @param newNumberOfSettlements The new NumberOfSettlements value.
     */
    public final void setNumberOfSettlements(final SettlementNumber newNumberOfSettlements) {
        this.numberOfSettlements = newNumberOfSettlements;
    }

    /**
     * Get the <code>Aggression</code> value.
     *
     * @return an <code>AggressionLevel</code> value
     */
    public final AggressionLevel getAggression() {
        return aggression;
    }

    /**
     * Set the <code>Aggression</code> value.
     *
     * @param newAggression The new Aggression value.
     */
    public final void setAggression(final AggressionLevel newAggression) {
        this.aggression = newAggression;
    }

    /**
     * Whether this is a EuropeanNation, i.e. a player or a REF.
     *
     */
    public abstract boolean isEuropean();

    /**
     * Whether this is a IndianNation.
     *
     */
    public abstract boolean isIndian();

    /**
     * Whether this is a EuropeanREFNation.
     *
     */
    public abstract boolean isREF();

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String extendString = in.getAttributeValue(null, "extends");
        NationType parent = (extendString == null) ? this :
            getSpecification().getType(extendString, NationType.class);
        String valueString = in.getAttributeValue(null,
            "number-of-settlements");
        if (valueString == null) {
            numberOfSettlements = parent.numberOfSettlements;
        } else {
            numberOfSettlements = Enum.valueOf(SettlementNumber.class,
                valueString.toUpperCase(Locale.US));
        }

        valueString = in.getAttributeValue(null, "aggression");
        if (valueString == null) {
            aggression = parent.aggression;
        } else {
            aggression = Enum.valueOf(AggressionLevel.class,
                valueString.toUpperCase(Locale.US));
        }

        if (parent != this) {
            getSettlementTypes().addAll(parent.getSettlementTypes());
            getFeatureContainer().add(parent.getFeatureContainer());
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }

    }

    /**
     * Reads a child object.
     *
     * @param in The XML stream to read.
     * @exception XMLStreamException if an error occurs
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("settlement".equals(childName)) {
            String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            SettlementType settlementType
                = new SettlementType(id, getSpecification());
            settlementType.readFromXML(in);
            settlementTypes.add(settlementType);
        } else {
            super.readChild(in);
        }
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("number-of-settlements",
            numberOfSettlements.toString().toLowerCase(Locale.US));
        out.writeAttribute("aggression",
            aggression.toString().toLowerCase(Locale.US));
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        for (SettlementType settlementType : settlementTypes) {
            settlementType.toXML(out, "settlement");
        }
    }

}
