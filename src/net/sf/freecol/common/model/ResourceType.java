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

import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


public final class ResourceType extends FreeColGameObjectType {

    private static int nextIndex = 0;

    private int minValue;
    private int maxValue;

    // ------------------------------------------------------------ constructors

    public ResourceType() {
        setIndex(nextIndex++);
    }

    // ------------------------------------------------------------ retrieval methods

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public Set<Modifier> getProductionModifier(GoodsType goodsType, UnitType unitType) {
        return featureContainer.getModifierSet(goodsType.getId(), unitType);
    }

    public GoodsType getBestGoodsType() {
        GoodsType bestType = null;
        float bestValue = 0f;
        for (Modifier modifier : featureContainer.getModifiers()) {
            GoodsType goodsType = Specification.getSpecification().getGoodsType(modifier.getId());
            float value = goodsType.getInitialSellPrice() * modifier.applyTo(100);
            if (bestType == null || value > bestValue) {
                bestType = goodsType;
                bestValue = value;
            }
        }
        return bestType;
    }

    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        if (hasAttribute(in, "maximum-value")) {
            maxValue = Integer.parseInt(in.getAttributeValue(null, "maximum-value"));
            minValue = getAttribute(in, "minimum-value", 0);
        } else {
            maxValue = -1;
            minValue = -1;
        }
    }

}
