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

import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public final class ResourceType extends FreeColGameObjectType {

    private int minValue;
    private int maxValue;

    // ------------------------------------------------------------ constructors

    public ResourceType(String id, Specification specification) {
        super(id, specification);
    }

    // ------------------------------------------------------------ retrieval methods

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public Set<Modifier> getProductionModifier(GoodsType goodsType, UnitType unitType) {
        return getModifierSet(goodsType.getId(), unitType);
    }

    public GoodsType getBestGoodsType() {
        GoodsType bestType = null;
        float bestValue = 0f;
        for (Modifier modifier : getModifiers()) {
            GoodsType goodsType = getSpecification().getGoodsType(modifier.getId());
            float value = goodsType.getInitialSellPrice() * modifier.applyTo(100);
            if (bestType == null || value > bestValue) {
                bestType = goodsType;
                bestValue = value;
            }
        }
        return bestType;
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
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

        if (maxValue > -1) {
            out.writeAttribute("maximum-value", Integer.toString(maxValue));
            out.writeAttribute("minimum-value", Integer.toString(minValue));
        }
    }

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

        if (hasAttribute(in, "maximum-value")) {
            maxValue = Integer.parseInt(in.getAttributeValue(null,
                    "maximum-value"));
            minValue = getAttribute(in, "minimum-value", 0);
        } else {
            maxValue = -1;
            minValue = -1;
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "resource-type".
     */
    public static String getXMLElementTagName() {
        return "resource-type";
    }
}
