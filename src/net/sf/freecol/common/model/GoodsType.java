
package net.sf.freecol.common.model;


import java.util.Map;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class GoodsType extends FreeColGameObjectType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    private boolean isFarmed;
    private boolean isFood;
    private boolean ignoreLimit;

    private GoodsType madeFrom;
    private GoodsType makes;
    
    private GoodsType storedAs;
    private boolean storable;

    private String art;
    
    private int initialAmount;
    private int initialPrice;
    private int priceDiff;

    // ----------------------------------------------------------- constructors

    public GoodsType(int index) {
        setIndex(index);
    }

    // ----------------------------------------------------------- retriveal methods

    public String getName(boolean sellable) {
        if (sellable) {
            return getName();
        } else {
            return getName() + " (" + Messages.message("model.goods.Boycotted") + ")";
        }
    }

    public boolean isRawMaterial() {
        return makes != null;
    }

    public boolean isRefined() {
        return madeFrom != null;
    }

    public GoodsType getRawMaterial() {
        return madeFrom;
    }

    public GoodsType getProducedMaterial() {
        return makes;
    }

    public boolean isFarmed() {
        return isFarmed;
    }

    public boolean limitIgnored() {
        return ignoreLimit;
    }

    public boolean isStorable() {
        return storable;
    }

    public GoodsType getStoredAs() {
        return storedAs;
    }

    public int getInitialAmount() {
        return initialAmount;
    }

    public int getInitialSellPrice() {
        return initialPrice;
    }

    public int getInitialBuyPrice() {
        return initialPrice + priceDiff;
    }

    public int getPriceDifference() {
        return priceDiff;
    }

    // TODO: give this some meaning
    // Originally intended for when there are no static variables
    public boolean isFoodType() {
        return isFood;
    }
    
    public String getArt() {
        return art;
    }

    public GoodsType outputType() {
        return makes;
    }

    public GoodsType inputType() {
        return madeFrom;
    }

    // ------------------------------------------------------------ API methods

    public void readFromXmlElement( Node xml, Map<String, GoodsType> goodsTypeByRef ) {

        setID(Xml.attribute(xml, "id"));
        isFarmed = Xml.booleanAttribute(xml, "is-farmed");
        isFood = Xml.booleanAttribute(xml, "is-food", false);
        ignoreLimit = Xml.booleanAttribute(xml, "ignore-limit", false);
        art = Xml.attribute(xml, "art");

        if (Xml.hasAttribute(xml, "made-from")) {
            String  madeFromRef = Xml.attribute(xml, "made-from");
            GoodsType  rawMaterial = goodsTypeByRef.get(madeFromRef);
            madeFrom = rawMaterial;
            if (rawMaterial != null) {
                rawMaterial.makes = this;
            }
        }

        storable = Xml.booleanAttribute(xml, "storable", true);
        if (Xml.hasAttribute(xml, "stored-as")) {
            storedAs = goodsTypeByRef.get(Xml.attribute(xml, "stored-as"));
        }

        // Only expected child is 'market'
        Xml.Method method = new Xml.Method() {
            public void invokeOn(Node xml) {
                initialAmount = Xml.intAttribute(xml, "initial-amount");
                initialPrice = Xml.intAttribute(xml, "initial-price");
                priceDiff = Xml.intAttribute(xml, "price-difference");
            }
        };
        Xml.forEachChild(xml, method);
    }

}
