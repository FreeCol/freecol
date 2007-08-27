
package net.sf.freecol.common.model;


import java.util.Map;

import net.sf.freecol.common.util.Xml;

import org.w3c.dom.Node;


public final class GoodsType
{
    public static final  String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final  String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION = "$Revision$";

    public int        index;
    public String     id;
    public String     name;
    public boolean   isFarmed;
    public boolean   isFood;
    public boolean   ignoreLimit;
/*
    public boolean improvedByPlowing = false;
    public boolean improvedByRiver = false;
    public boolean improvedByRoad = false;
*/
    public GoodsType  madeFrom;
    public GoodsType  makes;
    
    public GoodsType  storedAs;
    public boolean   storable;

    private String art;
    
    public int initialAmount;
    public int initialPrice;
    public int priceDiff;

    // ----------------------------------------------------------- constructors

    public GoodsType(int index) {
        this.index = index;
    }

    // ----------------------------------------------------------- retriveal methods

    public int getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
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
    public boolean isFoodType() {
        return isFood;
    }
    
    public String getArt() {
        return art;
    }

/*
    public boolean isImprovedByPlowing() {

        return improvedByPlowing;

    }

    public boolean isImprovedByRoad() {

        return improvedByRoad;

    }

    public boolean isImprovedByRiver() {

        return improvedByRiver;

    }
*/
    // ------------------------------------------------------------ API methods

    public void readFromXmlElement( Node xml, Map<String, GoodsType> goodsTypeByRef ) {

        name = Xml.attribute(xml, "id");
        String[] buffer = name.split("\\.");
        id = buffer[buffer.length - 1];
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

/*
        if (Xml.hasAttribute(xml, "improved-by-plowing") &&
            Xml.booleanAttribute(xml, "improved-by-plowing")) {
            improvedByPlowing = true;
        }
        if (Xml.hasAttribute(xml, "improved-by-river") &&
            Xml.booleanAttribute(xml, "improved-by-river")) {
            improvedByRiver = true;
        }
        if (Xml.hasAttribute(xml, "improved-by-road") &&
            Xml.booleanAttribute(xml, "improved-by-road")) {
            improvedByRoad = true;
        }
*/
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
