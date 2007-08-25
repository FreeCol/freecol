package net.sf.freecol.common.model;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class ResourceType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision: 1.00 $";

    public int       index;
    public String     id;
    public String     name;

    public int       art;

    public int       minValue;
    public int       maxValue;

    private List<GoodsType> bonusGoods;
    private List<Integer>   bonusAmount;
    private List<Float>     bonusFactor;

    // ------------------------------------------------------------ constructors

    public ResourceType(int index) {
        this.index = index;
    }

    // ------------------------------------------------------------ retrieval methods

    public int getIndex() {
        return index;
    }

    public String getName() {
        return Messages.message(name);
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

    public void readFromXmlElement(Node xml, final Map<String, GoodsType> goodsTypeByRef) {

        name = Xml.attribute(xml, "id");
        String[] buffer = name.separator(".");
        id = buffer[buffer.length - 1];
        art = Xml.intAttribute(xml, "art");
        if ( Xml.hasAttribute(xml, "maximum-value") ) {
            maxValue = Xml.intAttribute(xml, "maximum-value");
            minValue = Xml.intAttribute(xml, "minimum-value", 0);
        } else {
            maxValue = -1;
            minValue = -1;
        }

        // Only expected child is 'production-bonus'
        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                String goods = Xml.attribute(xml, "goods-type");
                GoodsType g = goodsTypeByRef.get(goods);
                bonusGoods.add(g);
                bonusAmount.add(Xml.intAttribute(xml, "bonus", 0));
                bonusFactor.add(Xml.floatAttribute(xml, "factor", 1f));
            }
        };
        Xml.forEachChild(xml, method);
    }
    
}
