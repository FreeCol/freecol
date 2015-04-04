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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * The types of resources (e.g. fish bonus) found on a tile.
 */
public final class ResourceType extends FreeColGameObjectType {

    /** Maximum and minimum values for this resource type. */
    private int maxValue, minValue;


    /**
     * Creates a new resource type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public ResourceType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Gets the maximum value for this resource.
     *
     * @return The maximum value.
     */
    public int getMaxValue() {
        return maxValue;
    }

    /**
     * Gets the minimum value for this resource.
     *
     * @return The minimum value.
     */
    public int getMinValue() {
        return minValue;
    }


    /**
     * Get the best goods type to make with this resource type.
     *
     * @return The best <code>GoodsType</code>.
     */
    public GoodsType getBestGoodsType() {
        final Specification spec = getSpecification();
        GoodsType bestType = null;
        float bestValue = 0f;
        for (Modifier modifier : getModifiers()) {
            GoodsType goodsType = spec.getGoodsType(modifier.getId());
            float value = spec.getInitialPrice(goodsType) * modifier.applyTo(100);
            if (bestType == null || value > bestValue) {
                bestType = goodsType;
                bestValue = value;
            }
        }
        return bestType;
    }


    // Serialization

    private static final String MAXIMUM_VALUE_TAG = "maximum-value";
    private static final String MINIMUM_VALUE_TAG = "minimum-value";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (maxValue > -1) {
            xw.writeAttribute(MAXIMUM_VALUE_TAG, maxValue);
            xw.writeAttribute(MINIMUM_VALUE_TAG, minValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        maxValue = xr.getAttribute(MAXIMUM_VALUE_TAG, -1);
        minValue = xr.getAttribute(MINIMUM_VALUE_TAG, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "resource-type".
     */
    public static String getXMLElementTagName() {
        return "resource-type";
    }
}
