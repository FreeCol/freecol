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
import java.util.Map;
import java.util.Random;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public final class ResourceType extends FreeColGameObjectType
{

    private String art;

    private int minValue;
    private int maxValue;

    private List<GoodsType> bonusGoods;
    private List<Integer> bonusAmount;
    private List<Float> bonusFactor;

    // ------------------------------------------------------------ constructors

    public ResourceType(int index) {
        setIndex(index);
    }

    // ------------------------------------------------------------ retrieval methods

    public String getArt() {
        return art;
    }

    public int getRandomValue() {
        if (minValue == maxValue)
            return maxValue;

        Random rand = new Random();
        return (minValue + rand.nextInt(maxValue-minValue+1));
    }
    
    public int getBonus(GoodsType g) {
        int bonusIndex = bonusGoods.indexOf(g);
        if (bonusIndex >= 0) {
            return bonusAmount.get(bonusIndex);
        }
        return 0;
    }
    
    public float getFactor(GoodsType g) {
        int bonusIndex = bonusGoods.indexOf(g);
        if (bonusIndex >= 0) {
            return bonusFactor.get(bonusIndex);
        }
        return 1;
    }

    public List<GoodsType> getBonusTypeList() {
        return bonusGoods;
    }

    public List<Integer> getBonusAmountList() {
        return bonusAmount;
    }

    public List<Float> getBonusFactorList() {
        return bonusFactor;
    }

    public GoodsType getBestGoodsType() {
        if (bonusGoods.size() == 1) {
            return bonusGoods.get(0);
        }
        GoodsType bestType = null;
        int bestValue = 0;
        for (int i = 0; i < bonusGoods.size(); i++) {
            GoodsType g = bonusGoods.get(i);
            // TODO: Use bonusFactor too
            if (bestType == null || g.getInitialSellPrice() * bonusAmount.get(i) > bestValue) {
                bestType = g;
                bestValue = g.getInitialSellPrice() * bonusAmount.get(i);
            }
        }
        return bestType;
    }

    /**
     * Returns a <code>String</code> with the output/s of this ResourceType.
     */
    public String getOutputString() {
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
    }

    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in, null);
    }

    public void readFromXML(XMLStreamReader in, final Map<String, GoodsType> goodsTypeByRef)
            throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        art = in.getAttributeValue(null, "art");
        if (hasAttribute(in, "maximum-value")) {
            maxValue = Integer.parseInt(in.getAttributeValue(null, "maximum-value"));
            minValue = getAttribute(in, "minimum-value", 0);
        } else {
            maxValue = -1;
            minValue = -1;
        }

        bonusGoods = new ArrayList<GoodsType>();
        bonusAmount = new ArrayList<Integer>();
        bonusFactor = new ArrayList<Float>();
        // Only expected child is 'production-bonus'
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String goods = in.getAttributeValue(null, "goods-type");
            GoodsType g = goodsTypeByRef.get(goods);
            bonusGoods.add(g);
            bonusAmount.add(getAttribute(in, "bonus", 0));
            bonusFactor.add(getAttribute(in, "factor", 1f));
            in.nextTag(); // close this element
        }
    }
    
}
