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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;

public final class ResourceType extends FreeColGameObjectType
{

    private String art;

    private int minValue;
    private int maxValue;

    private Map<GoodsType, Modifier> modifiers = new HashMap<GoodsType, Modifier>();

    // ------------------------------------------------------------ constructors

    public ResourceType(int index) {
        setIndex(index);
    }

    // ------------------------------------------------------------ retrieval methods

    public String getArt() {
        return art;
    }

    public Map<GoodsType, Modifier> getModifiers() {
        return modifiers;
    }

    // TODO: remove this
    public int getRandomValue() {
        if (minValue == maxValue)
            return maxValue;

        Random rand = new Random();
        return (minValue + rand.nextInt(maxValue-minValue+1));
    }

    public Modifier getProductionBonus(GoodsType goodsType) {
        return modifiers.get(goodsType);
    }

    public GoodsType getBestGoodsType() {
        GoodsType bestType = null;
        float bestValue = 0f;
        for (Map.Entry<GoodsType, Modifier> entry : modifiers.entrySet()) {
            float value = entry.getKey().getInitialSellPrice() * entry.getValue().applyTo(100);
            if (bestType == null || value > bestValue) {
                bestType = entry.getKey();
                bestValue = value;
            }
        }
        return bestType;
    }

    /**
     * Returns a <code>String</code> with the output/s of this ResourceType.
     */
    public String getOutputString() {
        /** TODO: make something more useful
        if (bonusGoods.size() == 0) {
            return (new String("No Bonus"));
        }
        String s = new String("");
        for (int i = 0; i < bonusGoods.size(); i++) {
            if (i > 0) {
                s += ", ";
            }
            s += bonusAmount.get(i).toString() + " " + bonusGoods.get(i);
        }
        return s;
        */
        return "";
    }

    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        art = in.getAttributeValue(null, "art");
        if (hasAttribute(in, "maximum-value")) {
            maxValue = Integer.parseInt(in.getAttributeValue(null, "maximum-value"));
            minValue = getAttribute(in, "minimum-value", 0);
        } else {
            maxValue = -1;
            minValue = -1;
        }
    }

    public void readChildren(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if (Modifier.getXMLElementTagName().equals(childName)) {
                Modifier modifier = new Modifier(in);
                if (modifier.getSource() == null) {
                    modifier.setSource(this);
                }
                modifiers.put(specification.getGoodsType(modifier.getId()), modifier);
                specification.addModifier(modifier);
            } else {
                super.readChild(in, specification);
            }
        }        
    }
    
}
