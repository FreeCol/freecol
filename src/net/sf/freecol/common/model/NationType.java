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

/**
 * Represents one of the nations present in the game.
 */
public abstract class NationType extends FreeColGameObjectType {

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
        return null;
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
     * Whether this is a EuropeanNation, i.e. a player or a REF.
     *
     */
    public abstract boolean isEuropean();

    /**
     * Whether this is a EuropeanREFNation.
     *
     */
    public abstract boolean isREF();

    public void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("settlement".equals(childName)) {
            String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            SettlementType settlementType = new SettlementType(id, getSpecification());
            settlementType.readFromXML(in);
            settlementTypes.add(settlementType);
        } else {
            super.readChild(in);
        }
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        for (SettlementType settlementType : settlementTypes) {
            settlementType.toXML(out, "settlement");
        }
    }

}
