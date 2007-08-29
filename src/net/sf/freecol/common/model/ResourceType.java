package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class ResourceType extends FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision: 1.00 $";

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

    public void readFromXmlElement(Node xml, final Map<String, GoodsType> goodsTypeByRef) {

        setID(Xml.attribute(xml, "id"));
        art = Xml.attribute(xml, "art");
        if ( Xml.hasAttribute(xml, "maximum-value") ) {
            maxValue = Xml.intAttribute(xml, "maximum-value");
            minValue = Xml.intAttribute(xml, "minimum-value", 0);
        } else {
            maxValue = -1;
            minValue = -1;
        }

        bonusGoods = new ArrayList<GoodsType>();
        bonusAmount = new ArrayList<Integer>();
        bonusFactor = new ArrayList<Float>();
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
